package com.zjf.fincialsystem.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.FragmentReportBinding;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.repository.StatisticsRepository;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.utils.LogUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 统计Fragment
 */
public class StatisticsFragment extends Fragment {

    private static final String TAG = "StatisticsFragment";
    private FragmentReportBinding binding;
    private StatisticsRepository statisticsRepository;
    private Date currentDate = new Date();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化统计数据仓库
        statisticsRepository = new StatisticsRepository(requireContext());
        
        // 初始化视图
        initViews();
        
        // 加载数据
        loadData();
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        // 初始化饼图
        setupPieChart(binding.pieChartExpense, "支出分布");
        setupPieChart(binding.pieChartIncome, "收入分布");
        
        // 设置刷新按钮
        binding.btnRetry.setOnClickListener(v -> loadData());
    }
    
    /**
     * 设置饼图基本属性
     */
    private void setupPieChart(PieChart pieChart, String centerText) {
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setCenterText(centerText);
        pieChart.setCenterTextSize(16f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.setNoDataText(getString(R.string.no_data));
    }
    
    /**
     * 更新饼图数据
     */
    private void updatePieChart(PieChart pieChart, List<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        
        PieData data = new PieData(dataSet);
        data.setValueTextSize(14f);
        
        pieChart.setData(data);
        pieChart.invalidate();
        pieChart.animateY(1000);
    }
    
    /**
     * 加载数据
     */
    private void loadData() {
        loadCategoryExpenseStatistics();
        // 加载其他统计数据...
    }

    /**
     * 加载分类支出统计
     */
    private void loadCategoryExpenseStatistics() {
        LogUtils.d(TAG, "开始加载分类支出统计...");
        
        if (binding == null) {
            LogUtils.e(TAG, "绑定为空，无法加载分类支出统计");
            return;
        }
        
        // 清除饼图并显示加载状态
        binding.pieChartExpense.clear();
        binding.pieChartExpense.setNoDataText(getString(R.string.loading));
        binding.pieChartExpense.invalidate();

        // 获取当前月份的开始和结束时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        long startDate = DateUtils.getFirstDayOfMonth(year, month).getTime();
        long endDate = DateUtils.getLastDayOfMonth(year, month).getTime();
        
        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentDate);
        LogUtils.d(TAG, "分类支出统计日期范围：" + yearMonth + " (" + DateUtils.formatDate(new Date(startDate)) + " 到 " + DateUtils.formatDate(new Date(endDate)) + ")");

        // 使用repository获取支出分类数据
        statisticsRepository.getExpenseByCategory(startDate, endDate, new RepositoryCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                if (getActivity() == null || !isAdded() || binding == null) {
                    LogUtils.e(TAG, "Fragment已分离，无法更新UI");
                    return;
                }
                
                LogUtils.d(TAG, "收到分类支出数据：" + (data != null ? data.toString() : "null"));
                
                requireActivity().runOnUiThread(() -> {
                    try {
                        // 获取分类数据
                        List<Map<String, Object>> categories;
                        if (data != null && data.containsKey("categories") && data.get("categories") instanceof List) {
                            categories = (List<Map<String, Object>>) data.get("categories");
                            LogUtils.d(TAG, "分类数量：" + categories.size());
                            
                            // 记录每个分类的详细信息
                            for (Map<String, Object> category : categories) {
                                // 记录分类键值，以便调试
                                StringBuilder keysInfo = new StringBuilder("分类键值: ");
                                for (String key : category.keySet()) {
                                    keysInfo.append(key).append("=").append(category.get(key)).append(", ");
                                }
                                LogUtils.d(TAG, keysInfo.toString());
                                
                                // 获取分类名称
                                String name = null;
                                if (category.containsKey("name")) {
                                    name = (String) category.get("name");
                                } else if (category.containsKey("categoryName")) {
                                    name = (String) category.get("categoryName");
                                }
                                
                                // 获取金额
                                double amount = 0;
                                if (category.containsKey("amount")) {
                                    Object amountObj = category.get("amount");
                                    if (amountObj instanceof Number) {
                                        amount = ((Number) amountObj).doubleValue();
                                    } else if (amountObj instanceof String) {
                                        try {
                                            amount = Double.parseDouble((String) amountObj);
                                        } catch (NumberFormatException e) {
                                            LogUtils.e(TAG, "解析金额失败：" + amountObj);
                                        }
                                    }
                                }
                                
                                LogUtils.d(TAG, "分类详情：名称=" + name + ", 金额=" + amount);
                            }
                        } else {
                            categories = new ArrayList<>();
                            LogUtils.w(TAG, "分类数据为空或格式不正确");
                        }
                        
                        // 如果没有数据，显示空状态
                        if (categories.isEmpty()) {
                            binding.pieChartExpense.setNoDataText(getString(R.string.no_data));
                            binding.pieChartExpense.invalidate();
                            LogUtils.d(TAG, "没有分类数据，显示空状态");
                            return;
                        }
                        
                        // 创建饼图条目
                        List<PieEntry> entries = new ArrayList<>();
                        float totalExpense = 0;
                        
                        // 计算总支出
                        for (Map<String, Object> category : categories) {
                            Object amountObj = category.get("amount");
                            double amount = 0;
                            if (amountObj instanceof Number) {
                                amount = ((Number) amountObj).doubleValue();
                            } else if (amountObj instanceof String) {
                                try {
                                    amount = Double.parseDouble((String) amountObj);
                                } catch (NumberFormatException e) {
                                    LogUtils.e(TAG, "解析金额失败：" + amountObj);
                                }
                            }
                            totalExpense += amount;
                        }
                        
                        LogUtils.d(TAG, "总支出：" + totalExpense);
                        
                        // 如果总支出为0，显示空状态
                        if (totalExpense <= 0) {
                            binding.pieChartExpense.setNoDataText(getString(R.string.no_data));
                            binding.pieChartExpense.invalidate();
                            LogUtils.d(TAG, "总支出为0，显示空状态");
                            return;
                        }
                        
                        // 创建饼图条目
                        for (Map<String, Object> category : categories) {
                            // 获取分类名称
                            String name = null;
                            if (category.containsKey("name")) {
                                name = (String) category.get("name");
                            } else if (category.containsKey("categoryName")) {
                                name = (String) category.get("categoryName");
                            }
                            
                            // 如果名称为空，使用"未知"
                            if (name == null || name.isEmpty()) {
                                name = getString(R.string.unknown);
                            }
                            
                            // 获取金额
                            Object amountObj = category.get("amount");
                            double amount = 0;
                            if (amountObj instanceof Number) {
                                amount = ((Number) amountObj).doubleValue();
                            } else if (amountObj instanceof String) {
                                try {
                                    amount = Double.parseDouble((String) amountObj);
                                } catch (NumberFormatException e) {
                                    LogUtils.e(TAG, "解析金额失败：" + amountObj);
                                }
                            }
                            
                            // 只添加金额大于0的分类
                            if (amount > 0) {
                                PieEntry entry = new PieEntry((float) amount, name);
                                entries.add(entry);
                                LogUtils.d(TAG, "添加饼图条目：" + name + ", 金额：" + amount);
                            }
                        }
                        
                        // 按金额降序排序
                        Collections.sort(entries, (e1, e2) -> Float.compare(e2.getValue(), e1.getValue()));
                        
                        // 如果没有有效条目，显示空状态
                        if (entries.isEmpty()) {
                            binding.pieChartExpense.setNoDataText(getString(R.string.no_data));
                            binding.pieChartExpense.invalidate();
                            LogUtils.d(TAG, "没有有效的饼图条目，显示空状态");
                            return;
                        }
                        
                        // 更新饼图
                        updatePieChart(binding.pieChartExpense, entries);
                        LogUtils.d(TAG, "已更新饼图，条目数：" + entries.size());
                        
                    } catch (Exception e) {
                        LogUtils.e(TAG, "处理分类支出数据失败：" + e.getMessage(), e);
                        binding.pieChartExpense.setNoDataText(getString(R.string.data_load_failed));
                        binding.pieChartExpense.invalidate();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null || !isAdded() || binding == null) {
                    return;
                }
                
                LogUtils.e(TAG, "获取分类支出数据失败: " + error);
                
                requireActivity().runOnUiThread(() -> {
                    binding.pieChartExpense.setNoDataText(getString(R.string.data_load_failed));
                    binding.pieChartExpense.invalidate();
                });
            }

            @Override
            public void isCacheData(boolean isCache) {
                if (isCache) {
                    LogUtils.d(TAG, "使用缓存的分类支出数据");
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 