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

import com.blankj.utilcode.util.ToastUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.FragmentDashboardBinding;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.model.TrendData;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.util.Pair;

import com.google.gson.Gson;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

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
    // 添加x轴标签集合
    private List<String> xAxisLabels = new ArrayList<>();
    
    // 添加统计类型和周期类型的变量
    private int periodType = 1; // 默认为月度
    private int statisticType = 0; // 默认为支出
    
    // 添加ResourceObserver类
    private abstract class ResourceObserver<T> implements Observer<T> {
        private Disposable disposable;
        
        @Override
        public void onSubscribe(Disposable d) {
            disposable = d;
        }
        
        @Override
        public void onComplete() {
            // 默认实现，可在子类中重写
        }
        
        public void dispose() {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
        }
    }
    
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
                // 使用宿主Activity的startActivityForResult
                requireActivity().startActivityForResult(intent, MainActivity.REQUEST_ADD_TRANSACTION);
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
        transactionAdapter = new TransactionAdapter(getContext());
        binding.rvTransactions.setAdapter(transactionAdapter);
        
        // 设置交易记录点击事件
        transactionAdapter.setOnItemClickListener(transaction -> {
            try {
                // 跳转到交易详情页
                Intent intent = TransactionDetailActivity.createIntent(requireContext(), transaction.getId());
                // 使用宿主Activity的startActivityForResult
                requireActivity().startActivityForResult(intent, MainActivity.REQUEST_TRANSACTION_DETAIL);
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
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < xAxisLabels.size()) {
                    return xAxisLabels.get(index);
                }
                return "";
            }
        });
        
        // 增强X轴标签可读性
        xAxis.setTextSize(10f);
        xAxis.setTextColor(getResources().getColor(R.color.text_primary));
        xAxis.setDrawGridLines(false);
        if (!xAxisLabels.isEmpty()) {
            xAxis.setLabelCount(Math.min(xAxisLabels.size(), 7)); // 限制显示的标签数量，避免拥挤
        } else {
            xAxis.setLabelCount(7); // 默认值
        }
        
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
        LogUtils.d(TAG, "DashboardFragment恢复");
        
        if (!isDataLoaded) {
            // 首次加载数据
            LogUtils.d(TAG, "首次加载数据");
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
     * 刷新数据
     * 供外部调用，重新加载所有数据
     */
    public void refreshData() {
        LogUtils.d(TAG, "刷新DashboardFragment数据");
        // 设置标记，表示数据需要重新加载
        isDataLoaded = false;
        // 立即加载数据
        loadData();
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
    public void loadConsumptionTrendData() {
        LogUtils.d(TAG, "开始加载消费趋势数据，周期类型: " + periodType + "，统计类型: " + statisticType);
        
        // 检查图表和加载视图是否已初始化
        if (binding.lineChart == null || binding.loadingView == null) {
            LogUtils.e(TAG, "图表或加载视图未初始化");
            return;
        }
        
        // 显示加载指示器，隐藏无趋势数据提示
        binding.loadingView.setVisibility(View.VISIBLE);
        binding.tvNoTrendData.setVisibility(View.GONE);
        
        // 根据periodType确定查询的时间段类型
        StatisticsRepository.PeriodType type;
        switch (periodType) {
            case 0:
                type = StatisticsRepository.PeriodType.YEARLY;
                break;
            case 1:
                type = StatisticsRepository.PeriodType.MONTHLY;
                break;
            case 2:
                type = StatisticsRepository.PeriodType.WEEKLY;
                break;
            case 3:
            default:
                type = StatisticsRepository.PeriodType.DAILY;
                break;
        }
        
        // 根据statisticType确定是加载支出、收入或净收入趋势
        statisticsRepository.getTrendData(type, statisticType)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new ResourceObserver<List<TrendData>>() {
                @Override
                public void onNext(List<TrendData> trendDataList) {
                    LogUtils.d(TAG, "趋势数据获取成功，数据条数: " + trendDataList.size());
                    binding.loadingView.setVisibility(View.GONE);
                    
                    // 检查是否有趋势数据
                    if (trendDataList.isEmpty()) {
                        LogUtils.d(TAG, "趋势数据为空");
                        binding.tvNoTrendData.setVisibility(View.VISIBLE);
                        binding.lineChart.setVisibility(View.GONE);
                        return;
                    }
                    
                    // 显示图表，隐藏无趋势数据提示
                    binding.tvNoTrendData.setVisibility(View.GONE);
                    binding.lineChart.setVisibility(View.VISIBLE);
                    
                    // 确保数据点是连续的（特别是月份）
                    trendDataList = ensureConsecutiveData(trendDataList, type);
                    
                    // 准备图表数据
                    List<Entry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    
                    for (int i = 0; i < trendDataList.size(); i++) {
                        TrendData data = trendDataList.get(i);
                        // 添加数据点 - 即使金额为0也添加
                        entries.add(new Entry(i, (float) data.getAmount()));
                        
                        // 根据周期类型添加标签
                        String label = formatLabel(data, type);
                        labels.add(label);
                        
                        // 添加到全局标签集合
                        if (xAxisLabels.size() <= i) {
                            xAxisLabels.add(label);
                        } else {
                            xAxisLabels.set(i, label);
                        }
                    }
                    
                    LogUtils.d(TAG, "准备绘制图表，点数: " + entries.size() + ", 标签数: " + labels.size());
                    
                    // 设置图表
                    setupChart(entries, labels);
                }
                
                @Override
                public void onError(Throwable e) {
                    LogUtils.e(TAG, "加载趋势数据失败: " + e.getMessage());
                    binding.loadingView.setVisibility(View.GONE);
                    binding.tvNoTrendData.setVisibility(View.VISIBLE);
                    binding.tvNoTrendData.setText("加载趋势数据失败: " + e.getMessage());
                    binding.lineChart.setVisibility(View.GONE);
                    ToastUtils.showShort("加载趋势数据失败: " + e.getMessage());
                }
                
                @Override
                public void onComplete() {
                    LogUtils.d(TAG, "趋势数据加载完成");
                }
            });
    }
    
    /**
     * 确保数据点是连续的，特别是月份
     * 如果发现月份不连续（比如从1月跳到4月），则添加中间缺失的月份，金额为0
     * @param dataList 原始数据列表
     * @param type 周期类型
     * @return 确保连续的数据列表
     */
    private List<TrendData> ensureConsecutiveData(List<TrendData> dataList, StatisticsRepository.PeriodType type) {
        if (dataList.isEmpty() || type != StatisticsRepository.PeriodType.MONTHLY) {
            return dataList; // 如果不是月度数据，或列表为空，直接返回
        }
        
        List<TrendData> result = new ArrayList<>();
        
        // 按年份和月份排序
        Collections.sort(dataList, (a, b) -> {
            int yearDiff = a.getYear() - b.getYear();
            if (yearDiff != 0) return yearDiff;
            return a.getMonth() - b.getMonth();
        });
        
        // 获取第一个和最后一个数据点
        TrendData first = dataList.get(0);
        TrendData last = dataList.get(dataList.size() - 1);
        
        // 计算总月数
        int totalMonths = (last.getYear() - first.getYear()) * 12 + last.getMonth() - first.getMonth() + 1;
        
        LogUtils.d(TAG, "数据范围: " + first.getYear() + "-" + first.getMonth() + " 到 " + 
                  last.getYear() + "-" + last.getMonth() + "，总月数: " + totalMonths);
        
        // 创建一个映射，将年月映射到数据
        java.util.Map<String, TrendData> dataMap = new java.util.HashMap<>();
        for (TrendData data : dataList) {
            String key = data.getYear() + "-" + data.getMonth();
            dataMap.put(key, data);
        }
        
        // 填充完整的月份序列
        int currentYear = first.getYear();
        int currentMonth = first.getMonth();
        
        for (int i = 0; i < totalMonths; i++) {
            String key = currentYear + "-" + currentMonth;
            if (dataMap.containsKey(key)) {
                // 使用现有数据
                result.add(dataMap.get(key));
            } else {
                // 创建填充数据
                TrendData fillerData = new TrendData();
                fillerData.setYear(currentYear);
                fillerData.setMonth(currentMonth);
                fillerData.setAmount(0);
                fillerData.setCount(0);
                result.add(fillerData);
                LogUtils.d(TAG, "添加填充数据点: " + currentYear + "-" + currentMonth);
            }
            
            // 移动到下一个月
            currentMonth++;
            if (currentMonth > 12) {
                currentMonth = 1;
                currentYear++;
            }
        }
        
        LogUtils.d(TAG, "处理后的数据点数量: " + result.size() + "，原始数据点数量: " + dataList.size());
        return result;
    }
    
    // 根据不同的时间周期类型格式化标签
    private String formatLabel(TrendData data, StatisticsRepository.PeriodType type) {
        switch (type) {
            case YEARLY:
                return data.getYear() + "年";
            case MONTHLY:
                return data.getMonth() + "月";
            case WEEKLY:
                return "第" + data.getWeek() + "周";
            case DAILY:
            default:
                return data.getDay() + "日";
        }
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

    /**
     * 设置图表
     * @param entries 数据点集合
     * @param labels X轴标签集合
     */
    private void setupChart(List<Entry> entries, List<String> labels) {
        if (binding.lineChart == null || entries == null || entries.isEmpty()) {
            LogUtils.e(TAG, "图表或数据为空，无法设置图表");
            return;
        }
        
        try {
            // 创建数据集
            LineDataSet dataSet = new LineDataSet(entries, "");
            dataSet.setDrawFilled(true);
            dataSet.setDrawCircles(true);
            dataSet.setLineWidth(2f);
            dataSet.setCircleRadius(4f);
            dataSet.setDrawValues(true);
            dataSet.setValueTextSize(9f);
            
            // 使用LINEAR模式而不是CUBIC_BEZIER，确保直线连接所有点
            dataSet.setMode(LineDataSet.Mode.LINEAR);
            
            // 设置圆点样式
            dataSet.setDrawCircleHole(true);
            dataSet.setCircleHoleRadius(2f);
            dataSet.setFormLineWidth(1f);
            dataSet.setFormSize(15.f);
            
            // 设置是否在折线图中每个数据点处绘制小圆点
            dataSet.setDrawCircles(true);
            
            // 启用数据点数值显示
            dataSet.setDrawValues(true);
            
            // 禁用虚线
            dataSet.enableDashedLine(0, 0, 0);
            
            // 格式化数值显示
            dataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return NumberUtils.formatAmount(value);
                }
            });
            
            // 设置颜色
            int lineColor = ContextCompat.getColor(requireContext(), R.color.chart_line);
            int fillColor = ContextCompat.getColor(requireContext(), R.color.chart_fill);
            
            // 根据统计类型选择不同颜色
            if (statisticType == 0 || statisticType == 1) {
                // 支出
                lineColor = ContextCompat.getColor(requireContext(), R.color.expense);
                fillColor = ContextCompat.getColor(requireContext(), R.color.expense_light);
            } else if (statisticType == 2) {
                // 收入
                lineColor = ContextCompat.getColor(requireContext(), R.color.income);
                fillColor = ContextCompat.getColor(requireContext(), R.color.income_light);
            }
            
            dataSet.setColor(lineColor);
            dataSet.setCircleColor(lineColor);
            dataSet.setFillColor(fillColor);
            
            // 创建LineData对象
            LineData lineData = new LineData(dataSet);
            
            // 配置X轴
            XAxis xAxis = binding.lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setDrawGridLines(false);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            
            // 配置Y轴
            YAxis leftAxis = binding.lineChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setAxisMinimum(0f); // 从0开始
            
            // 禁用右侧Y轴
            binding.lineChart.getAxisRight().setEnabled(false);
            
            // 禁用描述
            binding.lineChart.getDescription().setEnabled(false);
            
            // 禁用图例
            binding.lineChart.getLegend().setEnabled(false);
            
            // 恢复图表的交互性
            binding.lineChart.setNoDataText("无数据");
            binding.lineChart.setDrawGridBackground(false);
            binding.lineChart.setTouchEnabled(true);
            binding.lineChart.setDragEnabled(true);
            binding.lineChart.setScaleEnabled(true);
            binding.lineChart.setPinchZoom(true);
            binding.lineChart.setDoubleTapToZoomEnabled(true);
            
            // 设置数据
            binding.lineChart.setData(lineData);
            
            // 设置合适的视口范围
            binding.lineChart.setVisibleXRangeMaximum(Math.min(entries.size(), 7)); // 最多显示7个点
            binding.lineChart.setVisibleXRangeMinimum(2); // 至少显示2个点
            
            // 设置合理的边距
            binding.lineChart.setExtraOffsets(10, 10, 10, 10);
            
            // 绘制图表 - 使用完整绘制而不是增量绘制
            binding.lineChart.notifyDataSetChanged();
            binding.lineChart.invalidate();
            
            LogUtils.d(TAG, "图表设置完成，数据点数量: " + entries.size());
        } catch (Exception e) {
            LogUtils.e(TAG, "设置图表失败: " + e.getMessage(), e);
            ToastUtils.showShort("设置图表失败");
        }
    }
} 