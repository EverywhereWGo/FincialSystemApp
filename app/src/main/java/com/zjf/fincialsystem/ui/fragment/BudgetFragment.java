package com.zjf.fincialsystem.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.DialogBudgetEditBinding;
import com.zjf.fincialsystem.databinding.FragmentBudgetBinding;
import com.zjf.fincialsystem.model.Budget;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.network.model.AddBudgetRequest;
import com.zjf.fincialsystem.repository.BudgetRepository;
import com.zjf.fincialsystem.repository.CategoryRepository;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.ui.adapter.BudgetAdapter;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.StatusBarUtils;
import com.zjf.fincialsystem.utils.TokenManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 预算管理Fragment
 */
public class BudgetFragment extends Fragment implements BudgetAdapter.OnBudgetClickListener, BudgetEditFragment.OnBudgetEditListener {
    
    private static final String TAG = "BudgetFragment";
    private FragmentBudgetBinding binding;
    private BudgetAdapter adapter;
    private BudgetRepository budgetRepository;
    private CategoryRepository categoryRepository;
    private List<Category> expenseCategories = new ArrayList<>();
    private Calendar currentDate = Calendar.getInstance();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBudgetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 设置状态栏
        setupStatusBar();
        
        // 初始化仓库
        initRepositories();
        
        // 初始化视图
        initViews();
        
        // 加载分类数据
        loadCategories();
        
        // 加载预算数据
        loadBudgetData();
    }
    
    /**
     * 设置状态栏
     */
    private void setupStatusBar() {
        try {
            if (getActivity() != null) {
                // 使用StatusBarUtils统一处理状态栏，确保与编辑界面一致
                StatusBarUtils.setImmersiveStatusBar(getActivity(), true);
                LogUtils.d(TAG, "状态栏设置完成");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "设置状态栏失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化仓库
     */
    private void initRepositories() {
        try {
            Context context = getContext();
            if (context != null) {
                budgetRepository = new BudgetRepository(context);
                categoryRepository = new CategoryRepository(context);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化仓库失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        try {
            // 设置日期
            updatePeriodText();
            
            // 设置上个月点击事件
            binding.btnPrevious.setOnClickListener(v -> {
                currentDate.add(Calendar.MONTH, -1);
                updatePeriodText();
                loadBudgetDataWithoutClear();
            });
            
            // 设置下个月点击事件
            binding.btnNext.setOnClickListener(v -> {
                currentDate.add(Calendar.MONTH, 1);
                updatePeriodText();
                loadBudgetDataWithoutClear();
            });
            
            // 设置日期文本点击事件，弹出日期选择对话框
            binding.tvPeriod.setOnClickListener(v -> {
                showMonthYearPicker();
            });
            
            // 设置添加预算按钮
            binding.btnAddBudget.setOnClickListener(v -> showBudgetDialog(null));
            binding.btnAddFirstBudget.setOnClickListener(v -> showBudgetDialog(null));
            
            // 设置重试按钮
            binding.btnRetry.setOnClickListener(v -> loadBudgetData());
            
            // 设置列表
            adapter = new BudgetAdapter(getContext());
            adapter.setOnBudgetClickListener(this);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.recyclerView.setAdapter(adapter);
            
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化视图失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 更新周期文本
     */
    private void updatePeriodText() {
        // 使用带年份的格式
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH) + 1;
        String period = year + "年" + month + "月";
        
        if (binding != null) {
            binding.tvPeriod.setText(period);
            LogUtils.d(TAG, "当前选择的预算期间: " + period);
        }
    }
    
    /**
     * 加载分类数据
     */
    private void loadCategories() {
        try {
            if (categoryRepository == null) {
                LogUtils.e(TAG, "分类仓库为空，无法加载分类数据");
                return;
            }
            
            categoryRepository.getCategories(Category.TYPE_EXPENSE, new RepositoryCallback<List<Category>>() {
                @Override
                public void onSuccess(List<Category> data) {
                    expenseCategories = data;
                }
                
                @Override
                public void onError(String error) {
                    LogUtils.e(TAG, "加载分类数据失败：" + error);
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "加载分类数据失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 加载预算数据
     */
    private void loadBudgetData() {
        try {
            if (budgetRepository == null) {
                LogUtils.e(TAG, "预算仓库为空，无法加载预算数据");
                return;
            }
            
            // 显示加载中
            showLoading();
            
            // 获取月度预算数据，传递当前选择的日期
            budgetRepository.getBudgets(Budget.PERIOD_MONTHLY, currentDate, new RepositoryCallback<List<Budget>>() {
                @Override
                public void onSuccess(List<Budget> data) {
                    if (getActivity() == null || !isAdded()) {
                        return;
                    }
                    
                    getActivity().runOnUiThread(() -> {
                        if (data != null && !data.isEmpty()) {
                            adapter.setData(data);
                            showContent();
                        } else {
                            showEmpty();
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    if (getActivity() == null || !isAdded()) {
                        return;
                    }
                    
                    LogUtils.e(TAG, "加载预算数据失败：" + error);
                    getActivity().runOnUiThread(() -> {
                        showError();
                    });
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "加载预算数据失败：" + e.getMessage(), e);
            showError();
        }
    }
    
    /**
     * 加载预算数据 - 切换日期时使用，不清空当前数据避免闪烁
     */
    private void loadBudgetDataWithoutClear() {
        try {
            if (budgetRepository == null) {
                LogUtils.e(TAG, "预算仓库为空，无法加载预算数据");
                return;
            }
            
            // 不显示加载中状态，保持当前数据显示
            // 只有在当前没有显示内容（如首次加载或显示错误）时才显示加载状态
            if (binding.recyclerView.getVisibility() != View.VISIBLE) {
                showLoading();
            }
            
            // 获取月度预算数据，传递当前选择的日期
            budgetRepository.getBudgets(Budget.PERIOD_MONTHLY, currentDate, new RepositoryCallback<List<Budget>>() {
                @Override
                public void onSuccess(List<Budget> data) {
                    if (getActivity() == null || !isAdded()) {
                        return;
                    }
                    
                    getActivity().runOnUiThread(() -> {
                        if (data != null && !data.isEmpty()) {
                            // 平滑更新数据，不会闪烁
                            adapter.updateDataSmoothly(data);
                            showContent();
                        } else {
                            showEmpty();
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    if (getActivity() == null || !isAdded()) {
                        return;
                    }
                    
                    LogUtils.e(TAG, "加载预算数据失败：" + error);
                    getActivity().runOnUiThread(() -> {
                        showError();
                    });
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "加载预算数据失败：" + e.getMessage(), e);
            showError();
        }
    }
    
    /**
     * 显示预算设置对话框
     * @param budget 要编辑的预算，为null表示新增
     */
    private void showBudgetDialog(Budget budget) {
        try {
            // 创建并显示BudgetEditFragment
            BudgetEditFragment fragment = BudgetEditFragment.newInstance(budget);
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right)
                    .commit();
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "显示预算编辑页面失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 删除预算
     */
    private void deleteBudget(Budget budget) {
        try {
            Context context = getContext();
            if (context == null) {
                return;
            }
            
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.delete_budget)
                    .setMessage(R.string.delete_budget_confirm)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        budgetRepository.deleteBudget(budget.getId(), new RepositoryCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean data) {
                                if (getActivity() == null || !isAdded()) {
                                    return;
                                }
                                
                                getActivity().runOnUiThread(() -> {
                                    // 刷新列表
                                    loadBudgetData();
                                    Toast.makeText(context, R.string.budget_delete_success, Toast.LENGTH_SHORT).show();
                                });
                            }
                            
                            @Override
                            public void onError(String error) {
                                if (getActivity() == null || !isAdded()) {
                                    return;
                                }
                                
                                LogUtils.e(TAG, "删除预算失败：" + error);
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(context, "删除预算失败：" + error, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } catch (Exception e) {
            LogUtils.e(TAG, "删除预算失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 显示加载中
     */
    private void showLoading() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
            binding.errorLayout.setVisibility(View.GONE);
            binding.emptyLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示内容
     */
    private void showContent() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.errorLayout.setVisibility(View.GONE);
            binding.emptyLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示错误
     */
    private void showError() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.GONE);
            binding.errorLayout.setVisibility(View.VISIBLE);
            binding.emptyLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示空视图
     */
    private void showEmpty() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.GONE);
            binding.errorLayout.setVisibility(View.GONE);
            binding.emptyLayout.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 显示年月选择器
     */
    private void showMonthYearPicker() {
        if (getContext() == null) {
            return;
        }
        
        // 创建一个对话框
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_month_year_picker, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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
        
        // 设置初始选中的年份和月份
        yearPicker.setValue(currentDate.get(Calendar.YEAR));
        monthPicker.setValue(currentDate.get(Calendar.MONTH) + 1); // 月份是从0开始的，所以+1
        
        // 创建并显示对话框
        AlertDialog dialog = builder.create();
        
        // 设置确定按钮
        dialogView.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            // 获取选中的年月
            int selectedYear = yearPicker.getValue();
            int selectedMonth = monthPicker.getValue() - 1; // 月份是从0开始的，所以-1
            
            // 记录日志
            LogUtils.d(TAG, "用户选择了日期: " + selectedYear + "年" + (selectedMonth + 1) + "月");
            
            // 更新日期
            currentDate.set(Calendar.YEAR, selectedYear);
            currentDate.set(Calendar.MONTH, selectedMonth); 
            currentDate.set(Calendar.DAY_OF_MONTH, 1); // 设置为当月第一天
            
            // 更新UI
            updatePeriodText();
            loadBudgetDataWithoutClear();
            
            // 关闭对话框
            dialog.dismiss();
        });
        
        // 设置取消按钮
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    @Override
    public void onBudgetClick(Budget budget) {
        // 点击预算条目
        showBudgetDialog(budget);
    }
    
    @Override
    public void onEditClick(Budget budget) {
        // 点击编辑按钮
        showBudgetDialog(budget);
    }
    
    @Override
    public void onDeleteClick(Budget budget) {
        // 点击删除按钮
        deleteBudget(budget);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onBudgetSaved(Budget budget) {
        // 预算保存成功后刷新列表
        loadBudgetData();
        if (getContext() != null) {
            Toast.makeText(getContext(), R.string.budget_save_success, Toast.LENGTH_SHORT).show();
        }
    }
} 