package com.zjf.fincialsystem.ui.activity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

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
import java.util.Collections;
import java.text.SimpleDateFormat;
import java.util.Locale;

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
        
        // 修复UI中的重复ID问题
        fixDuplicateIds();
        
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
     * 修复UI中的重复ID问题
     * 由于布局中存在重复的ID，需要在代码中修复
     */
    private void fixDuplicateIds() {
        try {
            // 找到所有LinearLayout，检查是否有重复的选择时间段布局
            LinearLayout parentLayout = (LinearLayout) binding.getRoot().findViewById(R.id.content_layout).findViewById(android.R.id.content);
            if (parentLayout != null) {
                // 遍历子视图找到重复的布局并移除
                for (int i = 0; i < parentLayout.getChildCount(); i++) {
                    View child = parentLayout.getChildAt(i);
                    if (child instanceof LinearLayout) {
                        LinearLayout childLayout = (LinearLayout) child;
                        // 检查是否包含相似的子视图，表明这是一个选择时间段布局
                        if (childLayout.getChildCount() >= 3) {
                            boolean hasButton = false;
                            boolean hasTextView = false;
                            for (int j = 0; j < childLayout.getChildCount(); j++) {
                                View subChild = childLayout.getChildAt(j);
                                if (subChild instanceof Button) {
                                    hasButton = true;
                                } else if (subChild instanceof TextView) {
                                    hasTextView = true;
                                }
                            }
                            
                            // 如果是选择时间段布局但不是第一个，则移除
                            if (hasButton && hasTextView && i > 0) {
                                LogUtils.d(TAG, "检测到重复的选择时间段布局，移除");
                                parentLayout.removeViewAt(i);
                                break;
                            }
                        }
                    }
                }
            } else {
                LogUtils.e(TAG, "找不到内容布局，无法修复重复ID问题");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "修复重复ID问题失败: " + e.getMessage(), e);
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
            binding.btnPreviousMonth.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(currentDate);
                calendar.add(Calendar.MONTH, -1);
                currentDate = calendar.getTime();
                updatePeriodText();
                loadData();
            });
            
            // 设置下一个月点击事件
            binding.btnNextMonth.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(currentDate);
                calendar.add(Calendar.MONTH, 1);
                currentDate = calendar.getTime();
                updatePeriodText();
                loadData();
            });
            
            // 设置月份文本点击事件，弹出日期选择器
            binding.tvPeriodSelection.setOnClickListener(v -> showMonthYearPicker());
            
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
     * 显示月份年份选择器对话框
     */
    private void showMonthYearPicker() {
        // 创建一个对话框
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_month_year_picker, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        // 获取对话框中的年份和月份选择器
        NumberPicker monthPicker = dialogView.findViewById(R.id.month_picker);
        NumberPicker yearPicker = dialogView.findViewById(R.id.year_picker);
        
        // 配置月份选择器
        String[] months = new String[]{"1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"};
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setDisplayedValues(months);
        
        // 配置年份选择器
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(currentYear - 5); // 允许选择从5年前到5年后的年份
        yearPicker.setMaxValue(currentYear + 5);
        
        // 获取当前选中的年月
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // 月份从0开始，展示时+1
        
        // 设置初始选中的年份和月份
        yearPicker.setValue(year);
        monthPicker.setValue(month);
        
        // 创建并显示对话框
        AlertDialog dialog = builder.create();
        
        // 设置确定按钮
        dialogView.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            // 获取选中的年月
            int selectedYear = yearPicker.getValue();
            int selectedMonth = monthPicker.getValue() - 1; // 月份从0开始，所以-1
            
            // 记录日志
            LogUtils.d(TAG, "用户选择了日期: " + selectedYear + "年" + (selectedMonth + 1) + "月");
            
            // 更新日期
            calendar.set(Calendar.YEAR, selectedYear);
            calendar.set(Calendar.MONTH, selectedMonth);
            calendar.set(Calendar.DAY_OF_MONTH, 1); // 设置为当月第一天
            currentDate = calendar.getTime();
            
            // 更新UI
            updatePeriodText();
            loadData();
            
            // 关闭对话框
            dialog.dismiss();
        });
        
        // 设置取消按钮
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            dialog.dismiss();
        });
        
        dialog.show();
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
        
        // 设置无数据文本的样式
        binding.pieChart.setNoDataText(getString(R.string.no_data));
        binding.pieChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        
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
        binding.tvPeriodSelection.setText(DateUtils.formatMonth(currentDate));
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
            
            // 清除图表数据，避免显示旧数据
            binding.pieChart.clear();
            binding.barChart.clear();
            binding.pieChart.setNoDataText(getString(R.string.loading));
            binding.barChart.setNoDataText(getString(R.string.loading));
            binding.pieChart.invalidate();
            binding.barChart.invalidate();
            
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
            
            LogUtils.d(TAG, "加载数据：当前选择的月份为 " + DateUtils.formatMonth(currentDate) + 
                      ", 开始日期=" + DateUtils.formatDate(startDate) + 
                      ", 结束日期=" + DateUtils.formatDate(endDate));
            
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
        // 获取当前选择的月份的开始和结束日期
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startTime = calendar.getTimeInMillis();
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        long endTime = calendar.getTimeInMillis();
        
        LogUtils.d(TAG, "加载概览数据: startTime=" + startTime + ", endTime=" + endTime);
        
        // 获取概览数据
        // 从currentDate中提取年月，而不是使用固定的"monthly"
        String yearMonth = String.format("%d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
        
        // 使用自定义键标识特定月份的数据
        String cacheKey = "monthly_" + yearMonth;
        
        LogUtils.d(TAG, "加载月度概览数据: " + yearMonth);
        
        statisticsRepository.getOverview(cacheKey, new RepositoryCallback<Map<String, Object>>() {
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
        LogUtils.d(TAG, "加载支出分类统计: startDate=" + startDate + ", endDate=" + endDate);
        
        // 重要：先显示加载中状态
        binding.pieChart.setNoDataText(getString(R.string.loading));
        binding.pieChart.invalidate();
        
        // 先清除旧数据
        binding.pieChart.clear();
        
        // 基于年月创建缓存键
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startDate);
        String yearMonth = String.format("%d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
        String cacheKey = "category_expense_" + yearMonth;
        
        LogUtils.d(TAG, "使用缓存键请求支出分类数据: " + cacheKey);
        
        statisticsRepository.getExpenseByCategory(startDate, endDate, new RepositoryCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                runOnUiThread(() -> {
                    try {
                        LogUtils.d(TAG, "收到支出分类数据: " + data);
                        
                        // 确保用正确的键获取分类数据
                        List<Map<String, Object>> categories;
                        if (data.containsKey("categories") && data.get("categories") instanceof List) {
                            categories = (List<Map<String, Object>>) data.get("categories");
                            LogUtils.d(TAG, "收到分类数据: " + categories.size() + " 个分类");
                        } else {
                            categories = new ArrayList<>();
                            LogUtils.d(TAG, "未找到分类数据或格式不正确，查看完整数据: " + data);
                        }
                        
                        // 获取总支出金额
                        double totalExpense = statisticsRepository.getDoubleValue(data, "totalExpense", 0.0);
                        if (totalExpense <= 0) {
                            // 如果API没有返回总额，尝试计算
                            totalExpense = 0;
                            for (Map<String, Object> category : categories) {
                                totalExpense += statisticsRepository.getDoubleValue(category, "amount", 0.0);
                            }
                        }
                        LogUtils.d(TAG, "总支出金额: " + totalExpense);
                        
                        // 确保饼图视图可见
                        binding.pieChart.setVisibility(android.view.View.VISIBLE);
                        
                        if (categories.isEmpty() || totalExpense <= 0) {
                            // 如果没有数据，显示空状态
                            binding.pieChart.setNoDataText(getString(R.string.no_data));
                            binding.pieChart.invalidate();
                            LogUtils.d(TAG, "分类数据为空或总金额为0，显示无数据提示");
                            return;
                        }
                        
                        // 创建饼图条目
                        List<PieEntry> entries = new ArrayList<>();
                        for (Map<String, Object> category : categories) {
                            // 尝试从不同的键获取分类名称
                            String name = null;
                            if (category.containsKey("name")) {
                                name = (String) category.get("name");
                            } else if (category.containsKey("categoryName")) {
                                name = (String) category.get("categoryName");
                            }
                            
                            // 如果名称为空，设置为"未知"
                            if (name == null || name.isEmpty()) {
                                name = getString(R.string.unknown);
                            }
                            
                            double amount = statisticsRepository.getDoubleValue(category, "amount", 0.0);
                            float percentage = (float) (amount / totalExpense * 100);
                            
                            LogUtils.d(TAG, "处理分类: " + name + ", 金额: " + amount + ", 占比: " + percentage + "%");
                            
                            // 即使金额为0也添加条目，但在图表中会将值很小的合并显示
                            if (amount > 0) {
                                PieEntry entry = new PieEntry((float) amount, name);
                                entries.add(entry);
                                LogUtils.d(TAG, "添加饼图条目: " + name + ", 值: " + amount);
                            }
                        }
                        
                        if (entries.isEmpty()) {
                            binding.pieChart.setNoDataText(getString(R.string.no_data));
                            binding.pieChart.invalidate();
                            LogUtils.d(TAG, "处理后没有有效的饼图条目，显示无数据提示");
                            return;
                        }
                        
                        // 按金额降序排序
                        Collections.sort(entries, (e1, e2) -> Float.compare(e2.getValue(), e1.getValue()));
                        LogUtils.d(TAG, "排序后的饼图条目数量: " + entries.size());
                        
                        // 设置饼图数据
                        setupPieChartData(entries);
                        LogUtils.d(TAG, "已设置饼图数据，条目数: " + entries.size());
                        
                    } catch (Exception e) {
                        LogUtils.e(TAG, "设置分类数据失败：" + e.getMessage(), e);
                        binding.pieChart.setVisibility(android.view.View.VISIBLE);
                        binding.pieChart.setNoDataText(getString(R.string.data_load_failed));
                        binding.pieChart.invalidate();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                LogUtils.e(TAG, "获取分类数据失败：" + error);
                runOnUiThread(() -> {
                    binding.pieChart.setVisibility(android.view.View.VISIBLE);
                    binding.pieChart.setNoDataText(getString(R.string.data_load_failed));
                    binding.pieChart.invalidate();
                });
            }
            
            @Override
            public void isCacheData(boolean isCache) {
                if (isCache) {
                    LogUtils.d(TAG, "使用缓存数据");
                }
            }
        });
    }
    
    /**
     * 设置饼图数据 - 完全重写版本
     */
    private void setupPieChartData(List<PieEntry> entries) {
        LogUtils.d(TAG, "开始设置饼图数据，条目数: " + entries.size());
        
        // 确保图表可见
        binding.pieChart.setVisibility(android.view.View.VISIBLE);
        
        try {
            // 重置图表
            binding.pieChart.clear();
            
            // 创建数据集
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
            
            // 设置值格式
            dataSet.setValueFormatter(new PercentFormatter(binding.pieChart));
            dataSet.setValueTextSize(11f);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);
            
            // 创建饼图数据
            PieData data = new PieData(dataSet);
            binding.pieChart.setData(data);
            
            // 更新图表
            binding.pieChart.invalidate();
            
            // 应用动画
            binding.pieChart.animateY(1000, Easing.EaseInOutQuad);
            
            LogUtils.d(TAG, "饼图数据设置完成");
        } catch (Exception e) {
            LogUtils.e(TAG, "设置饼图数据时发生错误: " + e.getMessage(), e);
            // 出错时显示错误信息
            binding.pieChart.setNoDataText(getString(R.string.data_load_failed));
            binding.pieChart.invalidate();
        }
    }
    
    /**
     * 加载每日交易统计
     */
    private void loadDailyTransactions(long startDate, long endDate) {
        // 提取currentDate的年月
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Calendar月份从0开始
        
        LogUtils.d(TAG, "加载每日交易数据，年=" + year + "，月=" + month + "，使用的日期=" + 
                   DateUtils.formatDate(currentDate) + "，传入的startDate=" + 
                   DateUtils.formatDate(new Date(startDate)));
        
        // 清除当前图表数据并显示加载状态
        binding.barChart.clear();
        binding.barChart.setNoDataText(getString(R.string.loading));
        binding.barChart.invalidate();
        
        // 基于年月创建缓存键以确保每个月份有不同的缓存
        String cacheKey = "daily_transactions_" + year + "-" + month;
        
        // 使用新API获取每日交易数据 - 确保传递当前选择的年月
        statisticsRepository.getDailyTransactions(year, month, new RepositoryCallback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> dailyData) {
                runOnUiThread(() -> {
                    try {
                        if (dailyData != null && !dailyData.isEmpty()) {
                            LogUtils.d(TAG, "每日交易数据加载成功: " + dailyData.size() + " 条记录");
                            
                            // 处理并显示每日交易数据
                            processAndDisplayDailyData(dailyData, year, month);
                        } else {
                            LogUtils.w(TAG, "每日交易数据为空");
                            binding.barChart.clear();
                            binding.barChart.setNoDataText(getString(R.string.no_data));
                            binding.barChart.invalidate();
                        }
                    } catch (Exception e) {
                        LogUtils.e(TAG, "处理每日交易数据失败: " + e.getMessage(), e);
                        binding.barChart.clear();
                        binding.barChart.setNoDataText(getString(R.string.data_load_failed));
                        binding.barChart.invalidate();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                LogUtils.e(TAG, "获取每日交易数据失败: " + error);
                runOnUiThread(() -> {
                    binding.barChart.clear();
                    binding.barChart.setNoDataText(getString(R.string.data_load_failed));
                    binding.barChart.invalidate();
                });
            }
            
            @Override
            public void isCacheData(boolean isCache) {
                if (isCache) {
                    LogUtils.d(TAG, "使用缓存的每日交易数据: " + cacheKey);
                }
            }
        });
    }
    
    /**
     * 处理并显示每日交易数据
     * 
     * @param dailyData 每日交易数据列表
     * @param year 年份
     * @param month 月份(1-12)
     */
    private void processAndDisplayDailyData(List<Map<String, Object>> dailyData, int year, int month) {
        // 处理API返回的每日交易数据
        Map<String, Float> expenseByDay = new HashMap<>();
        Map<String, Float> incomeByDay = new HashMap<>();
        
        // 创建日期格式化器
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // 先生成该月所有日期
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1); // 月份从0开始
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // 初始化所有日期的数据为0
        for (int day = 1; day <= daysInMonth; day++) {
            calendar.set(year, month - 1, day);
            String dateStr = dateFormat.format(calendar.getTime());
            expenseByDay.put(dateStr, 0f);
            incomeByDay.put(dateStr, 0f);
        }
        
        LogUtils.d(TAG, "初始化月份 " + year + "-" + month + " 的" + daysInMonth + "天数据");
        
        // 填充有交易记录的日期的数据
        boolean hasAnyData = false; // 标记整个月是否有任何数据
        
        for (Map<String, Object> dayData : dailyData) {
            try {
                // 从每日数据中提取日期
                String day = (String) dayData.get("day");
                if (day == null) {
                    continue;
                }
                
                float expense = 0f;
                float income = 0f;
                
                // 获取支出金额
                if (dayData.containsKey("expense")) {
                    Object expenseObj = dayData.get("expense");
                    if (expenseObj instanceof Number) {
                        expense = ((Number) expenseObj).floatValue();
                    } else if (expenseObj instanceof String) {
                        try {
                            expense = Float.parseFloat((String) expenseObj);
                        } catch (NumberFormatException e) {
                            LogUtils.w(TAG, "无法解析支出金额: " + expenseObj);
                        }
                    }
                }
                
                // 获取收入金额
                if (dayData.containsKey("income")) {
                    Object incomeObj = dayData.get("income");
                    if (incomeObj instanceof Number) {
                        income = ((Number) incomeObj).floatValue();
                    } else if (incomeObj instanceof String) {
                        try {
                            income = Float.parseFloat((String) incomeObj);
                        } catch (NumberFormatException e) {
                            LogUtils.w(TAG, "无法解析收入金额: " + incomeObj);
                        }
                    }
                }
                
                // 更新对应日期的数据
                expenseByDay.put(day, expense);
                incomeByDay.put(day, income);
                
                // 检查是否有有效数据
                if (expense > 0 || income > 0) {
                    hasAnyData = true;
                }
                
                LogUtils.d(TAG, "日期 " + day + " 有交易数据: 支出=" + expense + ", 收入=" + income);
            } catch (Exception e) {
                LogUtils.e(TAG, "处理日交易数据失败: " + e.getMessage(), e);
            }
        }
        
        // 如果整个月都没有任何数据，显示无数据提示
        if (!hasAnyData) {
            binding.barChart.setNoDataText(getString(R.string.no_data));
            binding.barChart.invalidate();
            LogUtils.d(TAG, "整个月没有任何交易数据，显示无数据提示");
            return;
        }
        
        // 对日期进行排序
        List<String> sortedDays = new ArrayList<>(expenseByDay.keySet());
        Collections.sort(sortedDays); // 按日期排序
        
        LogUtils.d(TAG, "排序后的日期数: " + sortedDays.size() + ", 第一天: " + 
                  (sortedDays.isEmpty() ? "无" : sortedDays.get(0)) + ", 最后一天: " + 
                  (sortedDays.isEmpty() ? "无" : sortedDays.get(sortedDays.size() - 1)));
        
        // 创建图表数据
        List<BarEntry> expenseEntries = new ArrayList<>();
        List<BarEntry> incomeEntries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        
        // 为每个有数据的日期创建数据点
        int index = 0;
        for (int i = 0; i < sortedDays.size(); i++) {
            String day = sortedDays.get(i);
            float expense = expenseByDay.getOrDefault(day, 0f);
            float income = incomeByDay.getOrDefault(day, 0f);
            
            // 跳过支出和收入都为0的日期
            if (expense <= 0 && income <= 0) {
                continue;
            }
            
            expenseEntries.add(new BarEntry(index, expense));
            incomeEntries.add(new BarEntry(index, income));
            
            // 格式化日期标签为"日"格式
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("d", Locale.getDefault()); // 只显示日
                Date date = inputFormat.parse(day);
                String dayLabel = outputFormat.format(date);
                xLabels.add(dayLabel);
            } catch (Exception e) {
                // 如果解析失败，使用日期的最后部分
                String dayLabel = day.substring(day.lastIndexOf("-") + 1);
                xLabels.add(dayLabel);
            }
            
            index++; // 只为有数据的日期增加索引
        }
        
        // 再次检查处理后是否有数据
        if (expenseEntries.isEmpty() && incomeEntries.isEmpty()) {
            binding.barChart.setNoDataText(getString(R.string.no_data));
            binding.barChart.invalidate();
            LogUtils.d(TAG, "处理后没有有效的柱状图条目，显示无数据提示");
            return;
        }
        
        // 设置柱状图数据
        setBarChartData(expenseEntries, incomeEntries, xLabels);
    }

    private void setBarChartData(List<BarEntry> expenseEntries, List<BarEntry> incomeEntries, List<String> xLabels) {
        // 配置图表基本设置
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.setTouchEnabled(true); // 确保触摸交互启用
        binding.barChart.setDragEnabled(true); // 确保可以拖动
        binding.barChart.setScaleEnabled(true); // 允许缩放
        binding.barChart.setPinchZoom(true); // 启用双指缩放
        binding.barChart.setDoubleTapToZoomEnabled(true); // 启用双击缩放
        binding.barChart.setHighlightPerDragEnabled(true); // 拖动时突出显示
        binding.barChart.setDrawGridBackground(false);
        binding.barChart.setDrawBorders(false);
        
        // 创建数据集
        BarDataSet expenseDataSet = new BarDataSet(expenseEntries, getString(R.string.expense));
        expenseDataSet.setColor(ContextCompat.getColor(this, R.color.expense));
        expenseDataSet.setValueTextSize(12f);
        expenseDataSet.setValueTextColor(Color.BLACK);
        
        BarDataSet incomeDataSet = new BarDataSet(incomeEntries, getString(R.string.income));
        incomeDataSet.setColor(ContextCompat.getColor(this, R.color.income));
        incomeDataSet.setValueTextSize(12f);
        incomeDataSet.setValueTextColor(Color.BLACK);
        
        // 设置柱状图分组参数 - 调整为更宽松的布局以便更好滑动
        float groupSpace = 0.25f; // 组间距
        float barSpace = 0.05f; // 柱间距
        float barWidth = 0.30f; // 柱宽度
        
        // 创建柱状图数据对象
        BarData data = new BarData(expenseDataSet, incomeDataSet);
        data.setBarWidth(barWidth);
        
        // 启用X轴并设置
        XAxis xAxis = binding.barChart.getXAxis();
        xAxis.setEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // 确保X轴位于底部
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setTextSize(12f);
        xAxis.setLabelRotationAngle(0); // 水平显示标签
        xAxis.setLabelCount(xLabels.size()); // 确保显示所有标签
        xAxis.setDrawLabels(true);
        xAxis.setDrawAxisLine(true);
        xAxis.setTextColor(getResources().getColor(R.color.text_primary));
        xAxis.setCenterAxisLabels(true); // 将标签居中显示在柱组下方
        xAxis.setAvoidFirstLastClipping(true);
        
        // 设置左Y轴
        YAxis leftAxis = binding.barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(getResources().getColor(R.color.text_secondary));
        
        // 禁用右Y轴
        binding.barChart.getAxisRight().setEnabled(false);
        
        // 设置图例
        binding.barChart.getLegend().setEnabled(true);
        binding.barChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        binding.barChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        binding.barChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        binding.barChart.getLegend().setDrawInside(false);
        
        // 如果数据点较多，调整图表宽度使其可以滚动
        if (xLabels.size() > 5) {
            // 动态设置图表的布局参数，确保它足够宽以容纳所有数据点
            ViewGroup.LayoutParams layoutParams = binding.barChart.getLayoutParams();
            // 每组数据的宽度 = (2个柱子宽度 + 柱子间隔) + 组间距
            float groupWidth = 2 * barWidth + barSpace + groupSpace;
            // 计算所需的总宽度 = 数据点数量 * 每组宽度 * 屏幕密度
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int desiredWidth = Math.max(screenWidth, (int)(xLabels.size() * groupWidth * 100));
            // 设置最小宽度，确保可以滚动
            layoutParams.width = desiredWidth;
            binding.barChart.setLayoutParams(layoutParams);
            
            LogUtils.d(TAG, "调整图表宽度: " + desiredWidth + "px，以适应" + xLabels.size() + "组数据");
        }
        
        // 设置数据 - 必须在设置轴范围和分组之前
        binding.barChart.setData(data);
        
        // 确保X轴可以容纳所有分组
        float axisMin = -0.5f;
        // 特别注意：为了确保可以滑动，X轴最大值必须大于数据点的最大索引
        float axisMax = xLabels.size() + 0.5f; // 增加右侧空间
        binding.barChart.getXAxis().setAxisMinimum(axisMin);
        binding.barChart.getXAxis().setAxisMaximum(axisMax);
        
        // 设置分组 - 在设置轴范围后调用
        binding.barChart.groupBars(0, groupSpace, barSpace);
        binding.barChart.setFitBars(false); // 不自动调整以适应屏幕
        
        // 设置可视范围 - 这对于滚动非常重要
        int visibleCount = Math.min(5, xLabels.size());
        float rangeMaximum = visibleCount > 0 ? (visibleCount * (2 * barWidth + barSpace) + groupSpace) : 5;
        binding.barChart.setVisibleXRangeMaximum(rangeMaximum);
        binding.barChart.setVisibleXRangeMinimum(2); // 至少显示2组数据
        
        // 增加底部和侧边边距，确保有足够空间显示标签
        binding.barChart.setExtraBottomOffset(35f);
        binding.barChart.setExtraLeftOffset(10f);
        binding.barChart.setExtraRightOffset(10f);
        
        // 强制启用横向滚动
        binding.barChart.setDragXEnabled(true);
        binding.barChart.setScaleXEnabled(true);
        binding.barChart.setDragYEnabled(false); // 禁用垂直拖动，专注于水平滚动
        binding.barChart.setScaleYEnabled(false); // 禁用垂直缩放
        
        // 根据数据量自动缩放视图
        if (xLabels.size() <= 5) {
            binding.barChart.fitScreen(); // 如果数据少，则适应屏幕
        } else {
            binding.barChart.moveViewToX(0); // 滚动到开始位置
        }
        
        // 设置滚动监听器
        binding.barChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                LogUtils.d(TAG, "图表手势开始: " + lastPerformedGesture.name());
            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                LogUtils.d(TAG, "图表手势结束: " + lastPerformedGesture.name());
            }

            @Override
            public void onChartLongPressed(MotionEvent me) {}

            @Override
            public void onChartDoubleTapped(MotionEvent me) {}

            @Override
            public void onChartSingleTapped(MotionEvent me) {}

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
                LogUtils.d(TAG, "图表飞滑：X速度=" + velocityX + ", Y速度=" + velocityY);
            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                LogUtils.d(TAG, "图表缩放：X=" + scaleX + ", Y=" + scaleY);
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                // 记录较大的移动
                if (Math.abs(dX) > 10 || Math.abs(dY) > 10) {
                    LogUtils.d(TAG, "图表拖动：X=" + dX + ", Y=" + dY);
                }
            }
        });
        
        // 应用动画
        binding.barChart.animateY(1000);
        
        // 刷新图表
        binding.barChart.invalidate();
        
        // 日志输出当前设置
        LogUtils.d(TAG, "图表设置：数据点数量=" + xLabels.size() + 
                  ", 可视范围=" + binding.barChart.getVisibleXRange() + 
                  ", 当前X轴范围=[" + binding.barChart.getXChartMin() + "," + binding.barChart.getXChartMax() + "]");
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