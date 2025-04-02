package com.zjf.fincialsystem.ui.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.FragmentBudgetEditBinding;
import com.zjf.fincialsystem.model.Budget;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.network.model.AddBudgetRequest;
import com.zjf.fincialsystem.repository.BudgetRepository;
import com.zjf.fincialsystem.repository.CategoryRepository;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.TokenManager;
import com.zjf.fincialsystem.utils.StatusBarUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 预算添加/编辑Fragment
 */
public class BudgetEditFragment extends Fragment {

    private static final String TAG = "BudgetEditFragment";
    private static final String ARG_BUDGET = "budget";
    private static final String ARG_EDIT_MODE = "edit_mode";

    private FragmentBudgetEditBinding binding;
    private BudgetRepository budgetRepository;
    private CategoryRepository categoryRepository;
    private List<Category> expenseCategories = new ArrayList<>();
    private Budget existingBudget;
    private boolean editMode = false;
    private OnBudgetEditListener listener;

    /**
     * 创建新实例
     * @param budget 要编辑的预算，如果为null则为添加模式
     * @return Fragment实例
     */
    public static BudgetEditFragment newInstance(@Nullable Budget budget) {
        BudgetEditFragment fragment = new BudgetEditFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_EDIT_MODE, budget != null);
        if (budget != null) {
            args.putSerializable(ARG_BUDGET, budget);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 尝试将父Fragment设置为监听器
        if (getParentFragment() instanceof OnBudgetEditListener) {
            listener = (OnBudgetEditListener) getParentFragment();
        }
        // 如果父Fragment不是监听器，尝试将Activity设置为监听器
        else if (context instanceof OnBudgetEditListener) {
            listener = (OnBudgetEditListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            editMode = getArguments().getBoolean(ARG_EDIT_MODE, false);
            if (editMode) {
                existingBudget = (Budget) getArguments().getSerializable(ARG_BUDGET);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBudgetEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 设置沉浸式状态栏
        setupStatusBar();
        
        // 设置标题栏顶部内边距，避免与状态栏重叠
        adjustLayoutToolbarPadding();

        // 初始化仓库
        initRepositories();

        // 设置标题
        binding.tvTitle.setText(editMode ? R.string.edit_budget : R.string.add_budget);

        // 设置返回按钮点击事件
        binding.btnBack.setOnClickListener(v -> navigateBack());

        // 设置保存按钮点击事件
        binding.btnSave.setOnClickListener(v -> saveBudget());

        // 加载分类数据
        loadCategories();

        // 初始化下拉菜单
        initDropdowns();

        // 如果是编辑模式，填充现有数据
        if (editMode && existingBudget != null) {
            fillExistingData();
        }
    }

    /**
     * 设置沉浸式状态栏
     */
    private void setupStatusBar() {
        try {
            if (getActivity() != null) {
                // 使用StatusBarUtils类统一处理状态栏，避免不同处理方式导致的闪烁
                StatusBarUtils.setImmersiveStatusBar(getActivity(), true);
                
                // 注意：这里不要修改Window的透明度和系统UI可见性，
                // 以避免与主界面设置不同导致的切换闪烁
                
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
     * 初始化下拉菜单
     */
    private void initDropdowns() {
        Context context = getContext();
        if (context == null) return;

        // 设置周期选择
        String[] periods = {getString(R.string.budget_monthly), getString(R.string.budget_yearly)};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, periods);
        binding.spinnerPeriod.setAdapter(periodAdapter);
        binding.spinnerPeriod.setText(periods[0], false);

        // 设置提醒阈值选择
        String[] thresholds = {"50%", "80%", "90%", "100%"};
        ArrayAdapter<String> thresholdAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, thresholds);
        binding.spinnerNotifyPercent.setAdapter(thresholdAdapter);
        binding.spinnerNotifyPercent.setText(thresholds[1], false); // 默认80%
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
                    updateCategorySpinner();
                }

                @Override
                public void onError(String error) {
                    LogUtils.e(TAG, "加载分类数据失败：" + error);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "加载分类数据失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "加载分类数据失败：" + e.getMessage(), e);
        }
    }

    /**
     * 更新分类下拉框
     */
    private void updateCategorySpinner() {
        if (!isAdded() || getContext() == null) return;

        // 限制预算类别只有餐饮、购物、交通、住房、娱乐五个
        List<String> restrictedCategories = new ArrayList<>();
        restrictedCategories.add("餐饮");
        restrictedCategories.add("购物");
        restrictedCategories.add("交通");
        restrictedCategories.add("住房");
        restrictedCategories.add("娱乐");
        
        // 只显示限制列表中的分类
        List<String> categoryNames = new ArrayList<>();
        for (Category category : expenseCategories) {
            if (restrictedCategories.contains(category.getName())) {
                categoryNames.add(category.getName());
            }
        }
        
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, categoryNames);
        binding.spinnerCategory.setAdapter(categoryAdapter);

        // 如果是编辑模式并且存在预算，选择对应的分类
        if (editMode && existingBudget != null && existingBudget.getCategory() != null) {
            binding.spinnerCategory.setText(existingBudget.getCategory().getName(), false);
        }
    }

    /**
     * 填充已有预算数据到表单
     */
    private void fillExistingData() {
        if (existingBudget == null) return;

        // 设置金额
        binding.etAmount.setText(String.valueOf(existingBudget.getAmount()));

        // 设置周期
        if (Budget.PERIOD_YEARLY.equals(existingBudget.getPeriod())) {
            binding.spinnerPeriod.setText(getString(R.string.budget_yearly), false);
        } else {
            binding.spinnerPeriod.setText(getString(R.string.budget_monthly), false);
        }

        // 设置通知开关
        binding.switchNotify.setChecked(existingBudget.isNotifyEnabled());

        // 设置通知阈值
        int percentIndex = 1; // 默认是80%
        switch (existingBudget.getNotifyPercent()) {
            case 50:
                percentIndex = 0;
                break;
            case 80:
                percentIndex = 1;
                break;
            case 90:
                percentIndex = 2;
                break;
            case 100:
                percentIndex = 3;
                break;
        }
        String[] thresholds = {"50%", "80%", "90%", "100%"};
        binding.spinnerNotifyPercent.setText(thresholds[percentIndex], false);
    }

    /**
     * 保存预算
     */
    private void saveBudget() {
        try {
            Context context = getContext();
            if (context == null) {
                return;
            }

            // 获取金额
            String amountStr = binding.etAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(context, "请输入预算金额", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(context, "预算金额必须大于0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "请输入有效的金额", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取周期
            String periodStr = binding.spinnerPeriod.getText().toString().trim();
            String period = Budget.PERIOD_MONTHLY;
            if (getString(R.string.budget_yearly).equals(periodStr)) {
                period = Budget.PERIOD_YEARLY;
            }

            // 获取分类
            String categoryName = binding.spinnerCategory.getText().toString().trim();
            if (categoryName.isEmpty()) {
                Toast.makeText(context, "请选择分类", Toast.LENGTH_SHORT).show();
                return;
            }

            // 查找分类ID
            Category selectedCategory = null;
            for (Category category : expenseCategories) {
                if (category.getName().equals(categoryName)) {
                    selectedCategory = category;
                    break;
                }
            }

            if (selectedCategory == null) {
                Toast.makeText(context, "无效的分类", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取通知设置
            boolean notifyEnabled = binding.switchNotify.isChecked();

            // 获取通知阈值
            String notifyPercentStr = binding.spinnerNotifyPercent.getText().toString().trim();
            int notifyPercent = 80;
            if (notifyPercentStr.contains("50")) {
                notifyPercent = 50;
            } else if (notifyPercentStr.contains("90")) {
                notifyPercent = 90;
            } else if (notifyPercentStr.contains("100")) {
                notifyPercent = 100;
            }

            // 根据模式执行添加或编辑操作
            if (editMode && existingBudget != null) {
                // 编辑现有预算
                existingBudget.setAmount(amount);
                existingBudget.setPeriod(period);
                existingBudget.setCategoryId(selectedCategory.getId());
                existingBudget.setNotifyPercent(notifyPercent);
                existingBudget.setNotifyEnabled(notifyEnabled);

                // 更新预算
                budgetRepository.updateBudget(existingBudget.getId(), existingBudget, new RepositoryCallback<Budget>() {
                    @Override
                    public void onSuccess(Budget data) {
                        if (getActivity() == null || !isAdded()) {
                            return;
                        }

                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(context, R.string.budget_save_success, Toast.LENGTH_SHORT).show();
                            if (listener != null) {
                                listener.onBudgetSaved(data);
                            }
                            navigateBack();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() == null || !isAdded()) {
                            return;
                        }

                        LogUtils.e(TAG, "更新预算失败：" + error);
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(context, "更新预算失败：" + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                // 新建预算
                long userId = TokenManager.getInstance().getUserId();
                if (userId <= 0) {
                    Toast.makeText(context, "用户未登录，无法创建预算", Toast.LENGTH_SHORT).show();
                    return;
                }

                AddBudgetRequest request = new AddBudgetRequest(selectedCategory.getId(), amount, period);
                request.setUserId(userId);
                request.setNotifyPercent(notifyPercent);
                request.setNotifyEnabled(notifyEnabled);

                // 保存预算
                budgetRepository.addBudget(request, new RepositoryCallback<Budget>() {
                    @Override
                    public void onSuccess(Budget data) {
                        if (getActivity() == null || !isAdded()) {
                            return;
                        }

                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(context, R.string.budget_save_success, Toast.LENGTH_SHORT).show();
                            if (listener != null) {
                                listener.onBudgetSaved(data);
                            }
                            navigateBack();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() == null || !isAdded()) {
                            return;
                        }

                        LogUtils.e(TAG, "添加预算失败：" + error);
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(context, "添加预算失败：" + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "保存预算失败：" + e.getMessage(), e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "保存预算失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 返回上一级
     */
    private void navigateBack() {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * 调整标题栏的顶部内边距，避免与状态栏重叠
     */
    private void adjustLayoutToolbarPadding() {
        try {
            if (getContext() == null || binding == null || binding.layoutToolbar == null) return;
            
            // 使用StatusBarUtils统一处理工具栏调整，确保一致性
            StatusBarUtils.adjustToolbarForStatusBar(binding.layoutToolbar, getContext());
            
            // 由于没有直接的binding.scrollView，我们不再尝试调整滚动视图
            // 直接处理工具栏就足够避免闪烁问题
            
            LogUtils.d(TAG, "已调整标题栏布局");
        } catch (Exception e) {
            LogUtils.e(TAG, "调整标题栏内边距时出错: " + e.getMessage());
        }
    }

    /**
     * 预算编辑完成监听器
     */
    public interface OnBudgetEditListener {
        void onBudgetSaved(Budget budget);
    }
} 