package com.zjf.fincialsystem.ui.activity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityReportBinding;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.repository.StatisticsRepository;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NumberUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 报表Activity
 */
public class ReportActivity extends AppCompatActivity {
    
    private static final String TAG = "ReportActivity";
    private ActivityReportBinding binding;
    private Date currentDate = new Date(); // 当前选择的日期
    private StatisticsRepository statisticsRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置沉浸式状态栏
        setupStatusBar();
        
        binding = ActivityReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 初始化仓库
        statisticsRepository = new StatisticsRepository(this);
        
        // 初始化视图
        initViews();
        
        // 加载数据
        loadData();
    }
    
    /**
     * 设置沉浸式状态栏
     */
    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        try {
            // 设置返回按钮点击事件
            binding.btnBack.setOnClickListener(v -> onBackPressed());
            
            // 设置当前月份
            updatePeriodText();
            
            // 设置上一个月点击事件
            binding.btnPrevious.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(currentDate);
                calendar.add(Calendar.MONTH, -1);
                currentDate = calendar.getTime();
                updatePeriodText();
                loadData();
            });
            
            // 设置下一个月点击事件
            binding.btnNext.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(currentDate);
                calendar.add(Calendar.MONTH, 1);
                currentDate = calendar.getTime();
                updatePeriodText();
                loadData();
            });
            
            // 设置重试按钮点击事件
            binding.btnRetry.setOnClickListener(v -> loadData());
            
            // 设置饼图
            setupPieChart();
            
            // 设置柱状图
            setupBarChart();
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化视图失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化饼图
     */
    private void setupPieChart() {
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.setExtraOffsets(5, 10, 5, 5);
        binding.pieChart.setDragDecelerationFrictionCoef(0.95f);
        binding.pieChart.setCenterText(getString(R.string.expense_distribution));
        binding.pieChart.setCenterTextSize(16f);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleColor(Color.WHITE);
        binding.pieChart.setTransparentCircleColor(Color.WHITE);
        binding.pieChart.setTransparentCircleAlpha(110);
        binding.pieChart.setHoleRadius(58f);
        binding.pieChart.setTransparentCircleRadius(61f);
        binding.pieChart.setDrawCenterText(true);
        binding.pieChart.setRotationAngle(0);
        binding.pieChart.setRotationEnabled(true);
        binding.pieChart.setHighlightPerTapEnabled(true);
        binding.pieChart.setEntryLabelColor(Color.WHITE);
        binding.pieChart.setEntryLabelTextSize(12f);
        
        Legend l = binding.pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(10f);
    }

    /**
     * 初始化柱状图
     */
    private void setupBarChart() {
        BarChart chart = binding.barChart;

        // 设置图表样式
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setHighlightPerTapEnabled(true);
        chart.getLegend().setEnabled(false);

        // 设置数值显示在柱状图上方
        chart.setDrawValueAboveBar(true);

        // 设置X轴
        XAxis xAxis = chart.getXAxis();
        // 隐藏X轴及其标签
        xAxis.setEnabled(false);
        xAxis.setDrawLabels(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);

        // 设置左Y轴
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(getResources().getColor(R.color.text_secondary));

        // 禁用右Y轴
        chart.getAxisRight().setEnabled(false);

        // 设置无数据文本
        chart.setNoDataText(getString(R.string.no_data));
    }
    
    /**
     * 更新期间文本
     */
    private void updatePeriodText() {
        binding.tvPeriod.setText(DateUtils.formatMonth(currentDate));
    }
    
    /**
     * 加载数据
     */
    private void loadData() {
        try {
            // 显示加载中
            if (binding.contentLayout.getVisibility() != android.view.View.VISIBLE) {
                // 只有第一次加载或发生错误后重试时才显示加载进度条和隐藏内容
                binding.progressBar.setVisibility(android.view.View.VISIBLE);
                binding.contentLayout.setVisibility(android.view.View.GONE);
                binding.errorLayout.setVisibility(android.view.View.GONE);
            } else {
                // 已经有内容显示，添加轻量级加载指示器，不隐藏现有内容
                // 这里可以添加一个小的进度指示器或轻微降低内容透明度
            }
            
            // 获取当前月份的开始和结束日期
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDate);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Date startDate = calendar.getTime();
            
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Date endDate = calendar.getTime();
            
            // 加载收入支出概览
            loadOverview();
            
            // 加载支出分类统计
            loadExpenseByCategory(startDate.getTime(), endDate.getTime());
            
            // 加载每日交易统计
            loadDailyTransactions(startDate.getTime(), endDate.getTime());
            
        } catch (Exception e) {
            LogUtils.e(TAG, "加载数据失败：" + e.getMessage(), e);
            showError();
        }
    }
    
    /**
     * 加载收入支出概览
     */
    private void loadOverview() {
        String period = "monthly";
        statisticsRepository.getOverview(period, new RepositoryCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                runOnUiThread(() -> {
                    try {
                        // 获取收入支出数据
                        double income = statisticsRepository.getDoubleValue(data, "totalIncome", 0.0);
                        double expense = statisticsRepository.getDoubleValue(data, "totalExpense", 0.0);
                        double balance = statisticsRepository.getDoubleValue(data, "totalBalance", 0.0);
                        
                        // 设置动画过渡
                        animateTextChange(binding.tvIncomeTotal, NumberUtils.formatAmountWithCurrency(income));
                        animateTextChange(binding.tvExpenseTotal, NumberUtils.formatAmountWithCurrency(expense));
                        animateTextChange(binding.tvBalance, NumberUtils.formatAmountWithCurrency(balance));
                        
                        // 显示内容
                        binding.progressBar.setVisibility(android.view.View.GONE);
                        binding.contentLayout.setVisibility(android.view.View.VISIBLE);
                        
                    } catch (Exception e) {
                        LogUtils.e(TAG, "设置概览数据失败：" + e.getMessage(), e);
                        showError();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                LogUtils.e(TAG, "获取概览数据失败：" + error);
                runOnUiThread(() -> showError());
            }
        });
    }
    
    /**
     * 设置文本带动画效果
     */
    private void animateTextChange(final TextView textView, final String newText) {
        if (textView.getVisibility() == android.view.View.VISIBLE) {
            // 如果已经可见，使用动画过渡
            textView.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        textView.setText(newText);
                        textView.animate()
                                .alpha(1f)
                                .setDuration(150)
                                .start();
                    })
                    .start();
        } else {
            // 如果不可见，直接设置
            textView.setText(newText);
        }
    }
    
    /**
     * 加载支出分类统计
     */
    private void loadExpenseByCategory(long startDate, long endDate) {
        statisticsRepository.getExpenseByCategory(startDate, endDate, new RepositoryCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                runOnUiThread(() -> {
                    try {
                        // 解析数据 - 支持多种可能的数据格式
                        List<Map<String, Object>> categoryStats = null;
                        
                        // 尝试获取标准格式
                        if (data.containsKey("categoryStats")) {
                            categoryStats = (List<Map<String, Object>>) data.get("categoryStats");
                        } else if (data.containsKey("categories")) {
                            // 备选格式 1
                            categoryStats = (List<Map<String, Object>>) data.get("categories");
                        } else if (data.containsKey("data")) {
                            // 备选格式 2
                            Object dataObj = data.get("data");
                            if (dataObj instanceof List) {
                                categoryStats = (List<Map<String, Object>>) dataObj;
                            } else if (dataObj instanceof Map) {
                                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                                if (dataMap.containsKey("categoryStats")) {
                                    categoryStats = (List<Map<String, Object>>) dataMap.get("categoryStats");
                                } else if (dataMap.containsKey("categories")) {
                                    categoryStats = (List<Map<String, Object>>) dataMap.get("categories");
                                }
                            }
                        }
                        
                        // 如果仍然没有找到数据，尝试在顶层中查找list类型的值
                        if (categoryStats == null) {
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                if (entry.getValue() instanceof List) {
                                    categoryStats = (List<Map<String, Object>>) entry.getValue();
                                    LogUtils.d(TAG, "找到备选类别数据在字段: " + entry.getKey());
                                    break;
                                }
                            }
                        }
                        
                        if (categoryStats != null && !categoryStats.isEmpty()) {
                            LogUtils.d(TAG, "支出分类数据数量: " + categoryStats.size());
                            List<PieEntry> entries = new ArrayList<>();
                            
                            // 遍历分类统计数据
                            for (Map<String, Object> stat : categoryStats) {
                                String categoryName = "";
                                if (stat.containsKey("categoryName")) {
                                    categoryName = (String) stat.get("categoryName");
                                } else if (stat.containsKey("name")) {
                                    categoryName = (String) stat.get("name");
                                }
                                
                                double amount = 0;
                                if (stat.containsKey("amount")) {
                                    amount = statisticsRepository.getDoubleValue(stat, "amount", 0.0);
                                } else if (stat.containsKey("value")) {
                                    amount = statisticsRepository.getDoubleValue(stat, "value", 0.0);
                                }
                                
                                // 确保有分类名称和金额大于0
                                if (!categoryName.isEmpty() && amount > 0) {
                                    entries.add(new PieEntry((float) amount, categoryName, stat));
                                    LogUtils.d(TAG, "添加饼图数据: " + categoryName + " = " + amount);
                                }
                            }
                            
                            // 设置饼图数据
                            if (!entries.isEmpty()) {
                                setPieChartData(entries);
                            } else {
                                // 解析到类别但没有有效数据
                                binding.pieChart.setNoDataText(getString(R.string.no_data));
                                binding.pieChart.invalidate();
                            }
                        } else {
                            // 没有数据
                            LogUtils.d(TAG, "没有支出分类数据");
                            binding.pieChart.setNoDataText(getString(R.string.no_data));
                            binding.pieChart.invalidate();
                        }
                        
                    } catch (Exception e) {
                        LogUtils.e(TAG, "设置支出分类数据失败：" + e.getMessage(), e);
                        binding.pieChart.setNoDataText(getString(R.string.data_load_failed));
                        binding.pieChart.invalidate();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                LogUtils.e(TAG, "获取支出分类数据失败：" + error);
                runOnUiThread(() -> {
                    binding.pieChart.setNoDataText(getString(R.string.data_load_failed));
                    binding.pieChart.invalidate();
                });
            }
        });
    }
    
    /**
     * 设置饼图数据
     */
    private void setPieChartData(List<PieEntry> entries) {
        // 检查是否已有数据和动画
        boolean hasExistingData = binding.pieChart.getData() != null && 
                                 binding.pieChart.getData().getDataSetCount() > 0;
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        
        // 设置饼图颜色
        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.MATERIAL_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);
        colors.add(ColorTemplate.getHoloBlue());
        dataSet.setColors(colors);
        
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        
        // 设置数据，如果已有数据则使用更平滑的更新方式
        binding.pieChart.setData(data);
        binding.pieChart.highlightValues(null);
        
        if (hasExistingData) {
            // 如果已有数据，使用更短的动画
            binding.pieChart.animateY(300, Easing.EaseInOutQuad);
        } else {
            // 首次加载，使用完整动画
            binding.pieChart.animateY(1000, Easing.EaseInOutQuad);
        }
        
        binding.pieChart.invalidate();
    }
    
    /**
     * 加载每日交易统计
     */
    private void loadDailyTransactions(long startDate, long endDate) {
        // 获取支出数据
        statisticsRepository.getTrend(0, "monthly", new RepositoryCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> expenseData) {
                // 获取收入数据
                statisticsRepository.getTrend(1, "monthly", new RepositoryCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> incomeData) {
                        runOnUiThread(() -> {
                            try {
                                // 解析支出趋势数据
                                List<Map<String, Object>> expenseTrend = (List<Map<String, Object>>) expenseData.get("trendData");
                                // 解析收入趋势数据
                                List<Map<String, Object>> incomeTrend = (List<Map<String, Object>>) incomeData.get("trendData");
                                
                                boolean hasData = (expenseTrend != null && !expenseTrend.isEmpty()) || 
                                                (incomeTrend != null && !incomeTrend.isEmpty());
                                
                                if (hasData) {
                                    // 准备柱状图数据
                                    List<BarEntry> expenseEntries = new ArrayList<>();
                                    List<BarEntry> incomeEntries = new ArrayList<>();
                                    List<String> xLabels = new ArrayList<>();
                                    
                                    // 合并日期集合作为X轴标签
                                    Map<String, Integer> dateIndexMap = new HashMap<>();
                                    
                                    // 首先处理支出数据，创建日期索引映射
                                    if (expenseTrend != null) {
                                        for (int i = 0; i < expenseTrend.size(); i++) {
                                            Map<String, Object> item = expenseTrend.get(i);
                                            String date = "";
                                            if (item.containsKey("date")) {
                                                date = (String) item.get("date");
                                            } else if (item.containsKey("month")) {
                                                date = (String) item.get("month");
                                            } else if (item.containsKey("day")) {
                                                date = (String) item.get("day");
                                            }
                                            
                                            if (!date.isEmpty() && !dateIndexMap.containsKey(date)) {
                                                dateIndexMap.put(date, xLabels.size());
                                                xLabels.add(DateUtils.formatShortDate(date));
                                            }
                                        }
                                    }
                                    
                                    // 然后处理收入数据，继续构建日期索引映射
                                    if (incomeTrend != null) {
                                        for (int i = 0; i < incomeTrend.size(); i++) {
                                            Map<String, Object> item = incomeTrend.get(i);
                                            String date = "";
                                            if (item.containsKey("date")) {
                                                date = (String) item.get("date");
                                            } else if (item.containsKey("month")) {
                                                date = (String) item.get("month");
                                            } else if (item.containsKey("day")) {
                                                date = (String) item.get("day");
                                            }
                                            
                                            if (!date.isEmpty() && !dateIndexMap.containsKey(date)) {
                                                dateIndexMap.put(date, xLabels.size());
                                                xLabels.add(DateUtils.formatShortDate(date));
                                            }
                                        }
                                    }
                                    
                                    // 初始化数据数组，对应每个日期
                                    for (int i = 0; i < xLabels.size(); i++) {
                                        expenseEntries.add(new BarEntry(i, 0f));
                                        incomeEntries.add(new BarEntry(i, 0f));
                                    }
                                    
                                    // 填充支出数据
                                    if (expenseTrend != null) {
                                        for (Map<String, Object> item : expenseTrend) {
                                            String date = "";
                                            if (item.containsKey("date")) {
                                                date = (String) item.get("date");
                                            } else if (item.containsKey("month")) {
                                                date = (String) item.get("month");
                                            } else if (item.containsKey("day")) {
                                                date = (String) item.get("day");
                                            }
                                            
                                            if (!date.isEmpty() && dateIndexMap.containsKey(date)) {
                                                int index = dateIndexMap.get(date);
                                                double amount = statisticsRepository.getDoubleValue(item, "amount", 0.0);
                                                expenseEntries.set(index, new BarEntry(index, (float) amount));
                                            }
                                        }
                                    }
                                    
                                    // 填充收入数据
                                    if (incomeTrend != null) {
                                        for (Map<String, Object> item : incomeTrend) {
                                            String date = "";
                                            if (item.containsKey("date")) {
                                                date = (String) item.get("date");
                                            } else if (item.containsKey("month")) {
                                                date = (String) item.get("month");
                                            } else if (item.containsKey("day")) {
                                                date = (String) item.get("day");
                                            }
                                            
                                            if (!date.isEmpty() && dateIndexMap.containsKey(date)) {
                                                int index = dateIndexMap.get(date);
                                                double amount = statisticsRepository.getDoubleValue(item, "amount", 0.0);
                                                incomeEntries.set(index, new BarEntry(index, (float) amount));
                                            }
                                        }
                                    }
                                    
                                    // 设置柱状图数据（同时显示收入和支出）
                                    setBarChartData(expenseEntries, incomeEntries, xLabels);
                                } else {
                                    // 没有数据
                                    binding.barChart.setNoDataText(getString(R.string.no_data));
                                    binding.barChart.invalidate();
                                }
                                
                            } catch (Exception e) {
                                LogUtils.e(TAG, "设置每日交易数据失败：" + e.getMessage(), e);
                                binding.barChart.setNoDataText(getString(R.string.data_load_failed));
                                binding.barChart.invalidate();
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        // 如果收入数据获取失败，仍然尝试显示支出数据
                        runOnUiThread(() -> {
                            try {
                                List<Map<String, Object>> expenseTrend = (List<Map<String, Object>>) expenseData.get("trendData");
                                
                                if (expenseTrend != null && !expenseTrend.isEmpty()) {
                                    List<BarEntry> entries = new ArrayList<>();
                                    List<String> xLabels = new ArrayList<>();
                                    
                                    // 遍历趋势数据
                                    for (int i = 0; i < expenseTrend.size(); i++) {
                                        Map<String, Object> item = expenseTrend.get(i);
                                        String date = "";
                                        if (item.containsKey("date")) {
                                            date = (String) item.get("date");
                                        } else if (item.containsKey("month")) {
                                            date = (String) item.get("month");
                                        }
                                        
                                        double amount = statisticsRepository.getDoubleValue(item, "amount", 0.0);
                                        
                                        entries.add(new BarEntry(i, (float) amount));
                                        // 将日期格式转换为"月/日"的简短形式
                                        xLabels.add(DateUtils.formatShortDate(date));
                                    }
                                    
                                    // 只设置支出数据
                                    setBarChartDataSingle(entries, xLabels);
                                } else {
                                    binding.barChart.setNoDataText(getString(R.string.no_data));
                                    binding.barChart.invalidate();
                                }
                            } catch (Exception e) {
                                LogUtils.e(TAG, "设置支出数据失败：" + e.getMessage(), e);
                                binding.barChart.setNoDataText(getString(R.string.data_load_failed));
                                binding.barChart.invalidate();
                            }
                        });
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                LogUtils.e(TAG, "获取每日交易数据失败：" + error);
                runOnUiThread(() -> {
                    binding.barChart.setNoDataText(getString(R.string.data_load_failed));
                    binding.barChart.invalidate();
                });
            }
        });
    }

    private void setBarChartDataSingle(List<BarEntry> entries, List<String> xLabels) {
        // 检查是否已有数据
        boolean hasExistingData = binding.barChart.getData() != null &&
                binding.barChart.getData().getDataSetCount() > 0;

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.expense));
        dataSet.setColor(ContextCompat.getColor(this, R.color.expense));
        dataSet.setValueTextSize(12f); // 增大数值文本大小

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);
        data.setValueTextSize(12f);

        // 即使设置了X轴标签，由于X轴已禁用，所以也不会显示
        binding.barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        binding.barChart.setData(data);
        binding.barChart.setFitBars(true);

        if (hasExistingData) {
            // 已有数据，使用短动画
            binding.barChart.animateY(300);
        } else {
            // 首次加载，使用完整动画
            binding.barChart.animateY(1000);
        }

        binding.barChart.invalidate();
    }

    private void setBarChartData(List<BarEntry> expenseEntries, List<BarEntry> incomeEntries, List<String> xLabels) {
        // 检查是否已有数据
        boolean hasExistingData = binding.barChart.getData() != null &&
                binding.barChart.getData().getDataSetCount() > 0;

        BarDataSet expenseDataSet = new BarDataSet(expenseEntries, getString(R.string.expense));
        expenseDataSet.setColor(ContextCompat.getColor(this, R.color.expense));
        expenseDataSet.setValueTextSize(12f); // 增大数值文本大小

        BarDataSet incomeDataSet = new BarDataSet(incomeEntries, getString(R.string.income));
        incomeDataSet.setColor(ContextCompat.getColor(this, R.color.income));
        incomeDataSet.setValueTextSize(12f); // 增大数值文本大小

        float groupSpace = 0.08f;
        float barSpace = 0.03f;
        float barWidth = 0.4f; // 每个柱宽度

        BarData data = new BarData(expenseDataSet, incomeDataSet);
        data.setBarWidth(barWidth);
        data.setValueTextSize(12f);

        // 即使设置了X轴标签，由于X轴已禁用，所以也不会显示
        binding.barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        binding.barChart.setData(data);

        // 确保X轴可以容纳所有分组
        binding.barChart.getXAxis().setAxisMinimum(0);
        binding.barChart.getXAxis().setAxisMaximum(xLabels.size());

        // 设置分组
        binding.barChart.groupBars(0, groupSpace, barSpace);
        binding.barChart.setFitBars(true);

        // 设置图例可见
        binding.barChart.getLegend().setEnabled(true);

        if (hasExistingData) {
            // 已有数据，使用短动画
            binding.barChart.animateY(300);
        } else {
            // 首次加载，使用完整动画
            binding.barChart.animateY(1000);
        }

        binding.barChart.invalidate();
    }
    
    /**
     * 显示错误
     */
    private void showError() {
        binding.progressBar.setVisibility(android.view.View.GONE);
        binding.contentLayout.setVisibility(android.view.View.GONE);
        binding.errorLayout.setVisibility(android.view.View.VISIBLE);
        
        // 设置重试按钮点击事件
        binding.btnRetry.setOnClickListener(v -> loadData());
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 