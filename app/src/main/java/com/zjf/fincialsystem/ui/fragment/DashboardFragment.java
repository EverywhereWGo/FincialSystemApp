package com.zjf.fincialsystem.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.FragmentDashboardBinding;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.model.User;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.repository.StatisticsRepository;
import com.zjf.fincialsystem.repository.TransactionRepository;
import com.zjf.fincialsystem.repository.UserRepository;
import com.zjf.fincialsystem.ui.activity.AddTransactionActivity;
import com.zjf.fincialsystem.ui.activity.ReportActivity;
import com.zjf.fincialsystem.ui.activity.TransactionListActivity;
import com.zjf.fincialsystem.ui.adapter.TransactionAdapter;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NumberUtils;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.utils.TokenManager;
import com.zjf.fincialsystem.ui.activity.MainActivity;
import com.zjf.fincialsystem.ui.activity.TransactionDetailActivity;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;

/**
 * 仪表盘Fragment
 * 显示用户财务概览，包括收支统计、消费趋势等
 */
public class DashboardFragment extends Fragment {
    
    private static final String TAG = "DashboardFragment";
    private FragmentDashboardBinding binding;
    private TransactionAdapter transactionAdapter;
    private TransactionRepository transactionRepository;
    private StatisticsRepository statisticsRepository;
    private UserRepository userRepository;
    private boolean isDataFromCache = false;
    private boolean isDataLoaded = false;
    private int scrollX = 0;
    private int scrollY = 0;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化存储库
        initRepositories();
        
        // 初始化视图
        initViews();
        
        // 添加滚动监听
        setupScrollListener();
        
        // 加载数据
        if (!isDataLoaded) {
            loadData();
        } else {
            // 数据已加载，恢复滚动位置
            restoreScrollPosition();
        }
    }
    
    /**
     * 初始化存储库
     */
    private void initRepositories() {
        LogUtils.d(TAG, "初始化仓库对象");
        
        // 确保TokenManager已经正确初始化
        if (TokenManager.getInstance().isLoggedIn()) {
            LogUtils.d(TAG, "TokenManager已就绪，用户已登录");
        } else {
            LogUtils.e(TAG, "TokenManager未就绪或用户未登录");
        }
        
        Context context = requireContext().getApplicationContext();
        transactionRepository = new TransactionRepository(context);
        statisticsRepository = new StatisticsRepository(context);
        userRepository = new UserRepository(context);
        
        // 输出用户登录状态日志
        LogUtils.d(TAG, "用户登录状态: " + TokenManager.getInstance().isLoggedIn());
        LogUtils.d(TAG, "统计仓库初始化成功");
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        // 设置标题
        binding.tvTitle.setText(R.string.financial_overview);
        
        // 设置添加记录按钮点击事件
        binding.cardAddRecord.setOnClickListener(v -> {
            try {
                // 跳转到记录页面
                Intent intent = new Intent(requireContext(), AddTransactionActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                LogUtils.e(TAG, "跳转到记录页面失败：" + e.getMessage(), e);
                Toast.makeText(requireContext(), R.string.operation_failed, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置查看报表按钮点击事件
        binding.cardViewReport.setOnClickListener(v -> {
            try {
                // 跳转到报表页面
                Intent intent = new Intent(requireContext(), ReportActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                LogUtils.e(TAG, "跳转到报表页面失败：" + e.getMessage(), e);
                Toast.makeText(requireContext(), R.string.operation_failed, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置"查看全部"按钮点击事件
        binding.tvViewAll.setOnClickListener(v -> {
            try {
                LogUtils.d(TAG, "点击查看全部交易记录");
                // 跳转到交易记录列表页面
                Intent intent = new Intent(requireContext(), TransactionListActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                LogUtils.e(TAG, "跳转到交易记录列表页面失败：" + e.getMessage(), e);
                Toast.makeText(requireContext(), R.string.operation_failed, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 初始化交易记录列表
        transactionAdapter = new TransactionAdapter();
        binding.rvTransactions.setAdapter(transactionAdapter);
        
        // 设置交易记录点击事件
        transactionAdapter.setOnItemClickListener(transaction -> {
            try {
                // 跳转到交易详情页
                Intent intent = TransactionDetailActivity.createIntent(requireContext(), transaction.getId());
                startActivity(intent);
            } catch (Exception e) {
                LogUtils.e(TAG, "跳转到交易详情页失败：" + e.getMessage(), e);
                Toast.makeText(requireContext(), R.string.operation_failed, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置重试按钮点击事件
        binding.btnRetry.setOnClickListener(v -> {
            showError(false);
            showLoading(true);
            loadData();
        });
        
        // 设置折线图
        setupChart();
    }
    
    /**
     * 设置折线图
     */
    private void setupChart() {
        LineChart chart = binding.lineChart;
        
        // 设置图表样式
        chart.getDescription().setEnabled(false);
        
        // 恢复图表的触摸功能
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setHighlightPerTapEnabled(true);
        chart.getLegend().setEnabled(false);
        
        // 增加底部边距，确保X轴标签完整显示
        chart.setExtraBottomOffset(20f);
        
        // 设置X轴
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        // 设置X轴标签数量上限，防止标签重叠
        xAxis.setLabelCount(7, true);
        // 在标签过多时使标签倾斜
        xAxis.setLabelRotationAngle(45f);
        
        // 设置左Y轴
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        
        // 禁用右Y轴
        chart.getAxisRight().setEnabled(false);
        
        // 添加自定义触摸监听器，处理滑动冲突
        chart.setOnTouchListener((v, event) -> {
            // 当用户按下或移动时，阻止父视图拦截触摸事件
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN ||
                    event.getAction() == android.view.MotionEvent.ACTION_MOVE) {
                // 告诉父视图不要拦截触摸事件
                v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            // 当用户抬起或取消触摸时，恢复父视图拦截权限
            else if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                    event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            
            // 返回false让图表处理事件
            return false;
        });
    }
    
    /**
     * 设置滚动监听
     */
    private void setupScrollListener() {
        if (binding != null) {
            try {
                // 为NestedScrollView添加滚动监听器
                NestedScrollView scrollView = (NestedScrollView) binding.getRoot();
                scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
                    @Override
                    public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                        // 保存当前滚动位置
                        if (Math.abs(DashboardFragment.this.scrollY - scrollY) > 50) {
                            // 只在滚动变化明显时记录日志，减少日志量
                            LogUtils.d(TAG, "滚动位置发生变化: 从 " + oldScrollY + " 到 " + scrollY);
                        }
                        DashboardFragment.this.scrollX = scrollX;
                        DashboardFragment.this.scrollY = scrollY;
                    }
                });
                LogUtils.d(TAG, "滚动监听器设置成功");
            } catch (Exception e) {
                LogUtils.e(TAG, "设置滚动监听器失败: " + e.getMessage(), e);
            }
        } else {
            LogUtils.e(TAG, "binding为null，无法设置滚动监听器");
        }
    }
    
    /**
     * 恢复滚动位置
     */
    private void restoreScrollPosition() {
        if (binding != null) {
            // 只有当滚动位置不为0时才恢复，即从其他页面返回
            if (scrollY > 0) {
                LogUtils.d(TAG, "恢复滚动位置: scrollX=" + scrollX + ", scrollY=" + scrollY);
                // 使用post方法确保在下一个UI循环中执行滚动操作，此时视图已完全绘制
                new Handler().postDelayed(() -> {
                    if (binding != null) {
                        NestedScrollView scrollView = (NestedScrollView) binding.getRoot();
                        scrollView.scrollTo(scrollX, scrollY);
                        LogUtils.d(TAG, "滚动位置已恢复: scrollY=" + scrollY);
                    }
                }, 200); // 延迟200毫秒，确保视图已完全绘制
            } else {
                LogUtils.d(TAG, "滚动位置为顶部，无需恢复");
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        LogUtils.d(TAG, "DashboardFragment恢复，刷新数据");
        
        if (!isDataLoaded) {
            // 首次加载数据
            // 延迟加载数据，确保TokenManager完全初始化
            new Handler().postDelayed(this::loadData, 500);
        } else {
            // 数据已加载，从其他页面返回时只恢复滚动位置
            LogUtils.d(TAG, "数据已加载，仅恢复滚动位置");
            restoreScrollPosition();
        }
    }
    
    /**
     * 加载数据
     */
    private void loadData() {
        LogUtils.d(TAG, "开始加载仪表盘数据");
        
        // 确保用户已登录
        if (!TokenManager.getInstance().isLoggedIn()) {
            LogUtils.e(TAG, "用户未登录，无法加载数据");
            
            // 尝试从MainActivity获取用户登录状态
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).ensureNetworkManagerInitialized();
                
                // 再次检查用户登录状态
                if (TokenManager.getInstance().isLoggedIn()) {
                    LogUtils.d(TAG, "通过MainActivity确认用户已登录，继续加载数据");
                } else {
                    showError(true);
                    return;
                }
            } else {
                showError(true);
                return;
            }
        }
        
        // 隐藏错误视图
        showError(false);
        // 显示加载中
        showLoading(true);
        
        // 加载概览数据
        loadOverviewData();
        // 加载消费趋势数据
        loadConsumptionTrendData();
        // 加载最近交易
        loadRecentTransactions();
        
        // 标记数据已加载
        isDataLoaded = true;
    }
    
    /**
     * 加载收支概览数据
     */
    private void loadOverviewData() {
        // 获取当前月份
        String currentMonth = DateUtils.formatMonth(new Date());
        String period = "monthly";
        
        LogUtils.d(TAG, "开始加载收支概览数据: " + period);
        
        if (statisticsRepository == null) {
            LogUtils.e(TAG, "统计仓库为null，无法加载数据");
            showError(true);
            return;
        }
        
        try {
            statisticsRepository.getOverview(period, new RepositoryCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> data) {
                    if (getActivity() == null || !isAdded()) return;
                    
                    LogUtils.d(TAG, "收支概览数据加载成功: " + data);
                    
                    requireActivity().runOnUiThread(() -> {
                        try {
                            // 设置数据
                            double income = 0.0;
                            double expense = 0.0;
                            double balance = 0.0;
                            
                            // 使用安全类型转换方法获取数据
                            if (data.containsKey("totalIncome")) {
                                income = NumberUtils.toDouble(data.get("totalIncome"), 0.0);
                                LogUtils.d(TAG, "收入: " + income + ", 数据类型: " + (data.get("totalIncome") != null ? data.get("totalIncome").getClass().getName() : "null"));
                            } else {
                                LogUtils.w(TAG, "返回数据中没有totalIncome字段");
                            }
                            
                            if (data.containsKey("totalExpense")) {
                                expense = NumberUtils.toDouble(data.get("totalExpense"), 0.0);
                                LogUtils.d(TAG, "支出: " + expense + ", 数据类型: " + (data.get("totalExpense") != null ? data.get("totalExpense").getClass().getName() : "null"));
                            } else {
                                LogUtils.w(TAG, "返回数据中没有totalExpense字段");
                            }
                            
                            if (data.containsKey("totalBalance")) {
                                balance = NumberUtils.toDouble(data.get("totalBalance"), 0.0);
                                LogUtils.d(TAG, "余额: " + balance + ", 数据类型: " + (data.get("totalBalance") != null ? data.get("totalBalance").getClass().getName() : "null"));
                            } else {
                                LogUtils.w(TAG, "返回数据中没有totalBalance字段");
                            }
                            
                            // 格式化金额
                            binding.tvIncome.setText(NumberUtils.formatAmountWithCurrency(income));
                            binding.tvExpense.setText(NumberUtils.formatAmountWithCurrency(expense));
                            binding.tvBalance.setText(NumberUtils.formatAmountWithCurrency(balance));
                            
                            // 设置进度条
                            if (income > 0) {
                                int progress = (int) (expense * 100 / income);
                                binding.progressBarExpense.setProgress(progress);
                                // 设置支出占收入比例文本
                                binding.tvExpensePercentage.setText(getString(R.string.expense_percentage, progress));
                            } else {
                                binding.progressBarExpense.setProgress(0);
                                // 收入为0时显示0%
                                binding.tvExpensePercentage.setText(getString(R.string.expense_percentage, 0));
                            }
                            
                            showLoading(false);
                            
                            // 显示缓存数据提示
                            if (isDataFromCache) {
                                Toast.makeText(requireContext(), "显示缓存数据 - 网络不可用", Toast.LENGTH_SHORT).show();
                            }
                            
                            // 数据加载完成，如果所有数据都已加载，则恢复滚动位置
                            if (isDataLoaded && getActivity() != null && isAdded()) {
                                new Handler().postDelayed(() -> {
                                    if (binding != null) {
                                        restoreScrollPosition();
                                    }
                                }, 300); // 等待300毫秒，确保所有UI元素都已渲染完成
                            }
                        } catch (Exception e) {
                            LogUtils.e(TAG, "设置收支概览数据失败", e);
                            showError(true);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    if (getActivity() == null || !isAdded()) return;
                    
                    requireActivity().runOnUiThread(() -> {
                        LogUtils.e(TAG, "获取收支概览失败: " + error);
                        // 检查是否是服务器返回空数据的错误信息
                        if (error != null && error.contains("服务器返回空数据")) {
                            // 这种情况视为正常，只是没有数据，显示0值
                            try {
                                // 设置数据为0
                                binding.tvIncome.setText(NumberUtils.formatAmountWithCurrency(0.0));
                                binding.tvExpense.setText(NumberUtils.formatAmountWithCurrency(0.0));
                                binding.tvBalance.setText(NumberUtils.formatAmountWithCurrency(0.0));
                                
                                // 设置进度条为0
                                binding.progressBarExpense.setProgress(0);
                                binding.tvExpensePercentage.setText(getString(R.string.expense_percentage, 0));
                                
                                // 隐藏加载中
                                showLoading(false);
                                // 隐藏错误
                                showError(false);
                            } catch (Exception e) {
                                LogUtils.e(TAG, "设置零值失败", e);
                                showError(true);
                            }
                        } else {
                            // 真正的错误情况
                            showError(true);
                        }
                    });
                }
                
                @Override
                public void isCacheData(boolean isCache) {
                    isDataFromCache = isCache;
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "加载收支概览数据失败", e);
            showError(true);
        }
    }
    
    /**
     * 加载消费趋势数据
     */
    private void loadConsumptionTrendData() {
        int type = 0; // 支出
        String period = "monthly";
        
        statisticsRepository.getTrend(type, period, new RepositoryCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                if (getActivity() == null || !isAdded()) return;
                
                requireActivity().runOnUiThread(() -> {
                    try {
                        // 解析趋势数据
                        LineChart chart = binding.lineChart;
                        List<Entry> entries = new ArrayList<>();
                        List<String> xAxisLabels = new ArrayList<>();
                        
                        if (data.containsKey("trendData")) {
                            List<Map<String, Object>> trendData = (List<Map<String, Object>>) data.get("trendData");
                            
                            // 遍历趋势数据
                            if (trendData != null && !trendData.isEmpty()) {
                                for (int i = 0; i < trendData.size(); i++) {
                                    Map<String, Object> item = trendData.get(i);
                                    String date = (String) item.get("date");
                                    double amount = NumberUtils.toDouble(item.get("amount"), 0.0);
                                    
                                    entries.add(new Entry(i, (float) amount));
                                    // 将日期格式转换为"月/日"的简短形式
                                    xAxisLabels.add(DateUtils.formatShortDate(date));
                                }
                            }
                        }
                        
                        // 设置X轴标签
                        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
                        
                        // 创建数据集
                        if (!entries.isEmpty()) {
                            LineDataSet dataSet = new LineDataSet(entries, "支出趋势");
                            dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.expense));
                            dataSet.setDrawCircles(true);
                            dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.expense));
                            dataSet.setCircleRadius(4f);
                            dataSet.setCircleHoleRadius(2f);
                            dataSet.setCircleHoleColor(Color.WHITE);
                            dataSet.setLineWidth(2f);
                            dataSet.setDrawValues(false);
                            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                            dataSet.setDrawFilled(true);
                            dataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.expense_light));
                            dataSet.setFillAlpha(50);
                            
                            // 设置数据
                            LineData lineData = new LineData(dataSet);
                            chart.setData(lineData);
                            
                            // 使用简短的动画，避免过长的动画影响用户体验
                            chart.animateX(300); // 使用300毫秒的短动画
                            chart.setVisibleXRangeMaximum(entries.size());
                            
                            // 刷新图表
                            chart.invalidate();
                        }
                    } catch (Exception e) {
                        LogUtils.e(TAG, "设置消费趋势数据失败", e);
                    }
                });
            }

            @Override
            public void onError(String error) {
                LogUtils.e(TAG, "获取消费趋势失败: " + error);
            }
        });
    }
    
    /**
     * 加载最近交易记录
     */
    private void loadRecentTransactions() {
        transactionRepository.getTransactions(new RepositoryCallback<List<Transaction>>() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                if (getActivity() == null || !isAdded()) return;
                
                requireActivity().runOnUiThread(() -> {
                    try {
                        // 过滤最近5条交易记录
                        List<Transaction> recentTransactions = transactions;
                        if (transactions.size() > 5) {
                            recentTransactions = transactions.subList(0, 5);
                        }
                        
                        // 更新UI
                        transactionAdapter.setData(recentTransactions);
                        
                        // 显示空视图
                        binding.tvNoTransactions.setVisibility(recentTransactions.isEmpty() ? View.VISIBLE : View.GONE);
                        binding.rvTransactions.setVisibility(recentTransactions.isEmpty() ? View.GONE : View.VISIBLE);
                    } catch (Exception e) {
                        LogUtils.e(TAG, "设置最近交易记录失败", e);
                    }
                });
            }

            @Override
            public void onError(String error) {
                LogUtils.e(TAG, "获取最近交易记录失败: " + error);
                
                if (getActivity() == null || !isAdded()) return;
                
                requireActivity().runOnUiThread(() -> {
                    binding.tvNoTransactions.setVisibility(View.VISIBLE);
                    binding.rvTransactions.setVisibility(View.GONE);
                });
            }
        });
    }
    
    /**
     * 显示/隐藏加载中
     * @param show 是否显示
     */
    private void showLoading(boolean show) {
        if (binding != null) {
            binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
            binding.errorLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示/隐藏错误
     * @param show 是否显示
     */
    private void showError(boolean show) {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
            binding.errorLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // 页面暂停时记录滚动位置
        if (binding != null) {
            NestedScrollView scrollView = (NestedScrollView) binding.getRoot();
            scrollX = scrollView.getScrollX();
            scrollY = scrollView.getScrollY();
            LogUtils.d(TAG, "页面暂停，记录滚动位置: scrollX=" + scrollX + ", scrollY=" + scrollY);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存滚动位置和数据加载状态
        outState.putInt("scrollX", scrollX);
        outState.putInt("scrollY", scrollY);
        outState.putBoolean("isDataLoaded", isDataLoaded);
        LogUtils.d(TAG, "保存实例状态: scrollY=" + scrollY + ", isDataLoaded=" + isDataLoaded);
    }
    
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // 恢复滚动位置和数据加载状态
        if (savedInstanceState != null) {
            scrollX = savedInstanceState.getInt("scrollX", 0);
            scrollY = savedInstanceState.getInt("scrollY", 0);
            isDataLoaded = savedInstanceState.getBoolean("isDataLoaded", false);
            LogUtils.d(TAG, "恢复实例状态: scrollY=" + scrollY + ", isDataLoaded=" + isDataLoaded);
        }
    }
} 