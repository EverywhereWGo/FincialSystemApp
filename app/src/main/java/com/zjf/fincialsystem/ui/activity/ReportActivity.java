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
        
        // 增加底部边距，确保X轴标签完整显示
        chart.setExtraBottomOffset(20f);
        
        // 设置X轴
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.text_secondary));
        // 设置X轴标签数量上限，防止标签重叠
        xAxis.setLabelCount(7, true);
        // 在标签过多时使标签倾斜
        xAxis.setLabelRotationAngle(45f);
        
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
                        // 解析数据
                        List<Map<String, Object>> categoryStats = (List<Map<String, Object>>) data.get("categoryStats");
                        
                        if (categoryStats != null && !categoryStats.isEmpty()) {
                            List<PieEntry> entries = new ArrayList<>();
                            
                            // 遍历分类统计数据
                            for (Map<String, Object> stat : categoryStats) {
                                String categoryName = (String) stat.get("categoryName");
                                double amount = statisticsRepository.getDoubleValue(stat, "amount", 0.0);
                                float percentage = (float) statisticsRepository.getDoubleValue(stat, "percentage", 0.0);
                                
                                entries.add(new PieEntry((float) amount, categoryName, stat));
                            }
                            
                            // 设置饼图数据
                            setPieChartData(entries);
                        } else {
                            // 没有数据
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
        statisticsRepository.getTrend(0, "monthly", new RepositoryCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                runOnUiThread(() -> {
                    try {
                        // 解析趋势数据
                        List<Map<String, Object>> trendData = (List<Map<String, Object>>) data.get("trendData");
                        
                        if (trendData != null && !trendData.isEmpty()) {
                            List<BarEntry> entries = new ArrayList<>();
                            List<String> xLabels = new ArrayList<>();
                            
                            // 遍历趋势数据
                            for (int i = 0; i < trendData.size(); i++) {
                                Map<String, Object> item = trendData.get(i);
                                String date = (String) item.get("date");
                                double amount = statisticsRepository.getDoubleValue(item, "amount", 0.0);
                                
                                entries.add(new BarEntry(i, (float) amount));
                                // 将日期格式转换为"月/日"的简短形式
                                xLabels.add(DateUtils.formatShortDate(date));
                            }
                            
                            // 设置柱状图数据
                            setBarChartData(entries, xLabels);
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
                LogUtils.e(TAG, "获取每日交易数据失败：" + error);
                runOnUiThread(() -> {
                    binding.barChart.setNoDataText(getString(R.string.data_load_failed));
                    binding.barChart.invalidate();
                });
            }
        });
    }
    
    /**
     * 设置柱状图数据
     */
    private void setBarChartData(List<BarEntry> entries, List<String> xLabels) {
        // 检查是否已有数据
        boolean hasExistingData = binding.barChart.getData() != null && 
                                 binding.barChart.getData().getDataSetCount() > 0;
        
        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.daily_transactions));
        dataSet.setColor(ContextCompat.getColor(this, R.color.expense));
        
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);
        data.setValueTextSize(10f);
        
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