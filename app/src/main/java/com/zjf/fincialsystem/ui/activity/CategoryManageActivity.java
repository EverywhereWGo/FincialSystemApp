package com.zjf.fincialsystem.ui.activity;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityCategoryManageBinding;
import com.zjf.fincialsystem.databinding.ItemColorBinding;
import com.zjf.fincialsystem.databinding.ItemIconBinding;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.network.api.CategoryApiService;
import com.zjf.fincialsystem.network.model.AddCategoryRequest;
import com.zjf.fincialsystem.ui.adapter.CategoryAdapter;
import com.zjf.fincialsystem.utils.ColorUtil;
import com.zjf.fincialsystem.utils.IconUtil;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.StatusBarUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 分类管理页面
 */
public class CategoryManageActivity extends AppCompatActivity {

    private static final String TAG = "CategoryManageActivity";
    private ActivityCategoryManageBinding binding;
    private CategoryAdapter adapter;
    private List<Category> categories = new ArrayList<>();
    private int currentType = Category.TYPE_EXPENSE; // 默认显示支出分类
    private CategoryApiService apiService;
    
    // 对话框选中项
    private String selectedIconName = "ic_food";
    private String selectedColor = "#F44336";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置沉浸式状态栏
        setupStatusBar();
        
        binding = ActivityCategoryManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 初始化API服务
        apiService = NetworkManager.getInstance().getService(CategoryApiService.class);
        
        // 初始化视图
        initViews();
        
        // 加载分类数据
        loadCategories();
    }
    
    /**
     * 设置沉浸式状态栏
     */
    private void setupStatusBar() {
        // 使用StatusBarUtils工具类设置沉浸式状态栏
        StatusBarUtils.setImmersiveStatusBar(this, true);
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        // 设置工具栏与状态栏的距离
        adjustToolbarPadding();
        
        // 设置工具栏
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        // 设置Tab选择监听
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    currentType = Category.TYPE_EXPENSE;
                } else {
                    currentType = Category.TYPE_INCOME;
                }
                loadCategories();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        // 设置RecyclerView
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(categories);
        binding.recyclerView.setAdapter(adapter);
        
        // 设置点击监听
        adapter.setOnCategoryClickListener(new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClicked(Category category) {
                // 点击分类项无操作
            }
            
            @Override
            public void onEditClicked(Category category) {
                showEditCategoryDialog(category);
            }
            
            @Override
            public void onDeleteClicked(Category category) {
                if (category.isDefault()) {
                    Toast.makeText(CategoryManageActivity.this, 
                            R.string.default_category_cannot_delete, Toast.LENGTH_SHORT).show();
                    return;
                }
                showDeleteConfirmDialog(category);
            }
        });
        
        // 设置添加按钮点击事件
        binding.fabAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }
    
    /**
     * 调整工具栏与状态栏的距离
     */
    private void adjustToolbarPadding() {
        try {
            // 获取状态栏高度
            int statusBarHeight = StatusBarUtils.getStatusBarHeight(this);
            
            // 设置AppBarLayout的顶部内边距
            if (binding.toolbar != null && binding.toolbar.getParent() instanceof View) {
                View appBarLayout = (View) binding.toolbar.getParent();
                
                appBarLayout.setPadding(
                        appBarLayout.getPaddingLeft(),
                        statusBarHeight, // 添加状态栏高度的顶部内边距
                        appBarLayout.getPaddingRight(),
                        appBarLayout.getPaddingBottom()
                );
                
                LogUtils.d(TAG, "已调整工具栏内边距，状态栏高度: " + statusBarHeight + "px");
            } else {
                // 备用方法：直接调整工具栏
                if (binding.toolbar != null) {
                    binding.toolbar.setPadding(
                            binding.toolbar.getPaddingLeft(),
                            statusBarHeight + binding.toolbar.getPaddingTop(),
                            binding.toolbar.getPaddingRight(),
                            binding.toolbar.getPaddingBottom()
                    );
                    LogUtils.d(TAG, "使用备用方法调整工具栏内边距");
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "调整工具栏内边距时出错: " + e.getMessage());
        }
    }
    
    /**
     * 加载分类数据
     */
    private void loadCategories() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        
        apiService.getCategories(1, 20, null, currentType).enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, 
                                   Response<ApiResponse<List<Category>>> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.VISIBLE);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Category>> apiResponse = response.body();
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        categories = apiResponse.getData();
                        adapter.updateData(categories);
                        
                        // 显示空状态
                        binding.tvEmpty.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        // API请求成功但返回错误
                        Toast.makeText(CategoryManageActivity.this, 
                                apiResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    // API请求失败
                    Toast.makeText(CategoryManageActivity.this, 
                            R.string.data_load_failed, Toast.LENGTH_SHORT).show();
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                LogUtils.e(TAG, "加载分类失败", t);
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(CategoryManageActivity.this, 
                        R.string.network_error, Toast.LENGTH_SHORT).show();
                binding.tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
    
    /**
     * 显示添加分类对话框
     */
    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // 初始化对话框控件
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextInputLayout tilCategoryName = dialogView.findViewById(R.id.tilCategoryName);
        TextInputEditText etCategoryName = dialogView.findViewById(R.id.etCategoryName);
        RadioGroup rgCategoryType = dialogView.findViewById(R.id.rgCategoryType);
        RadioButton rbExpense = dialogView.findViewById(R.id.rbExpense);
        RadioButton rbIncome = dialogView.findViewById(R.id.rbIncome);
        LinearLayout llIconContainer = dialogView.findViewById(R.id.llIconContainer);
        LinearLayout llColorContainer = dialogView.findViewById(R.id.llColorContainer);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        
        // 设置标题
        tvTitle.setText(R.string.add_category);
        
        // 设置分类类型
        if (currentType == Category.TYPE_EXPENSE) {
            rbExpense.setChecked(true);
        } else {
            rbIncome.setChecked(true);
        }
        
        // 初始化图标选择
        initIconSelection(llIconContainer);
        
        // 初始化颜色选择
        initColorSelection(llColorContainer);
        
        // 设置按钮点击事件
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            String name = etCategoryName.getText().toString().trim();
            
            if (TextUtils.isEmpty(name)) {
                tilCategoryName.setError(getString(R.string.category_name_empty));
                return;
            }
            
            // 获取选中的分类类型
            int type = rbExpense.isChecked() ? Category.TYPE_EXPENSE : Category.TYPE_INCOME;
            
            // 创建分类添加请求
            AddCategoryRequest request = new AddCategoryRequest(name, type, selectedIconName, selectedColor);
            
            // 调用API添加分类
            addCategory(request, dialog);
        });
    }
    
    /**
     * 显示编辑分类对话框
     */
    private void showEditCategoryDialog(Category category) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // 初始化对话框控件
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextInputLayout tilCategoryName = dialogView.findViewById(R.id.tilCategoryName);
        TextInputEditText etCategoryName = dialogView.findViewById(R.id.etCategoryName);
        RadioGroup rgCategoryType = dialogView.findViewById(R.id.rgCategoryType);
        RadioButton rbExpense = dialogView.findViewById(R.id.rbExpense);
        RadioButton rbIncome = dialogView.findViewById(R.id.rbIncome);
        LinearLayout llIconContainer = dialogView.findViewById(R.id.llIconContainer);
        LinearLayout llColorContainer = dialogView.findViewById(R.id.llColorContainer);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        
        // 设置标题
        tvTitle.setText(R.string.edit_category);
        
        // 设置当前值
        etCategoryName.setText(category.getName());
        if (category.getType() == Category.TYPE_EXPENSE) {
            rbExpense.setChecked(true);
        } else {
            rbIncome.setChecked(true);
        }
        
        // 设置当前选中的图标和颜色
        selectedIconName = category.getIcon();
        selectedColor = category.getColor();
        
        // 初始化图标选择
        initIconSelection(llIconContainer);
        
        // 初始化颜色选择
        initColorSelection(llColorContainer);
        
        // 设置按钮点击事件
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            String name = etCategoryName.getText().toString().trim();
            
            if (TextUtils.isEmpty(name)) {
                tilCategoryName.setError(getString(R.string.category_name_empty));
                return;
            }
            
            // 获取选中的分类类型
            int type = rbExpense.isChecked() ? Category.TYPE_EXPENSE : Category.TYPE_INCOME;
            
            // 更新分类对象
            category.setName(name);
            category.setType(type);
            category.setIcon(selectedIconName);
            category.setColor(selectedColor);
            
            // 调用API更新分类
            updateCategory(category, dialog);
        });
    }
    
    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(Category category) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete)
                .setMessage(R.string.delete_category_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteCategory(category))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    /**
     * 初始化图标选择
     */
    private void initIconSelection(LinearLayout container) {
        container.removeAllViews();
        
        List<IconUtil.IconItem> icons = IconUtil.getAllIcons();
        
        for (IconUtil.IconItem icon : icons) {
            ItemIconBinding iconBinding = ItemIconBinding.inflate(getLayoutInflater());
            
            // 设置图标
            iconBinding.ivIcon.setImageResource(icon.getResourceId());
            
            // 设置选中状态
            if (icon.getName().equals(selectedIconName)) {
                iconBinding.frameIcon.setBackgroundColor(Color.parseColor(selectedColor));
            }
            
            // 设置点击事件
            iconBinding.frameIcon.setOnClickListener(v -> {
                selectedIconName = icon.getName();
                // 更新所有图标的选中状态
                for (int i = 0; i < container.getChildCount(); i++) {
                    View child = container.getChildAt(i);
                    ItemIconBinding childBinding = ItemIconBinding.bind(child);
                    if (child == v) {
                        childBinding.frameIcon.setBackgroundColor(Color.parseColor(selectedColor));
                    } else {
                        childBinding.frameIcon.setBackgroundResource(R.drawable.circle_background);
                    }
                }
            });
            
            container.addView(iconBinding.getRoot());
        }
    }
    
    /**
     * 初始化颜色选择
     */
    private void initColorSelection(LinearLayout container) {
        container.removeAllViews();
        
        List<String> colors = ColorUtil.getAllColors();
        
        for (String color : colors) {
            ItemColorBinding colorBinding = ItemColorBinding.inflate(getLayoutInflater());
            
            // 设置颜色
            colorBinding.viewColor.setBackgroundColor(Color.parseColor(color));
            
            // 设置选中状态
            if (color.equals(selectedColor)) {
                colorBinding.viewColor.setScaleX(1.2f);
                colorBinding.viewColor.setScaleY(1.2f);
            }
            
            // 设置点击事件
            colorBinding.viewColor.setOnClickListener(v -> {
                selectedColor = color;
                // 更新所有颜色的选中状态
                for (int i = 0; i < container.getChildCount(); i++) {
                    View child = container.getChildAt(i);
                    if (child == v) {
                        child.setScaleX(1.2f);
                        child.setScaleY(1.2f);
                    } else {
                        child.setScaleX(1.0f);
                        child.setScaleY(1.0f);
                    }
                }
            });
            
            container.addView(colorBinding.getRoot());
        }
    }
    
    /**
     * 添加分类
     */
    private void addCategory(AddCategoryRequest request, AlertDialog dialog) {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        apiService.addCategory(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, 
                                   Response<ApiResponse<String>> response) {
                binding.progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        dialog.dismiss();
                        
                        // 创建新分类对象
                        Category newCategory = new Category();
                        newCategory.setName(request.getName());
                        newCategory.setType(request.getType());
                        newCategory.setIcon(request.getIcon());
                        newCategory.setColor(request.getColor());
                        
                        // 假设服务器生成的ID
                        newCategory.setId(System.currentTimeMillis());
                        
                        // 刷新列表
                        categories.add(newCategory);
                        adapter.updateData(categories);
                        
                        Toast.makeText(CategoryManageActivity.this, "分类添加成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CategoryManageActivity.this, apiResponse.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CategoryManageActivity.this, "添加失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(CategoryManageActivity.this, "网络错误：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 更新分类
     */
    private void updateCategory(Category category, AlertDialog dialog) {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        apiService.updateCategory(category).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, 
                                   Response<ApiResponse<String>> response) {
                binding.progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        dialog.dismiss();
                        
                        // 更新列表中的分类
                        for (int i = 0; i < categories.size(); i++) {
                            if (categories.get(i).getId() == category.getId()) {
                                categories.set(i, category);
                                break;
                            }
                        }
                        adapter.updateData(categories);
                        
                        Toast.makeText(CategoryManageActivity.this, "分类更新成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CategoryManageActivity.this, apiResponse.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CategoryManageActivity.this, "更新失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(CategoryManageActivity.this, "网络错误：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 删除分类
     */
    private void deleteCategory(Category category) {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        apiService.deleteCategory(String.valueOf(category.getId())).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, 
                                   Response<ApiResponse<String>> response) {
                binding.progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // 从列表中移除分类
                        categories.remove(category);
                        adapter.updateData(categories);
                        
                        Toast.makeText(CategoryManageActivity.this, "分类删除成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CategoryManageActivity.this, apiResponse.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CategoryManageActivity.this, "删除失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(CategoryManageActivity.this, "网络错误：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
} 