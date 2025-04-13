package com.zjf.fincialsystem.ui.activity;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityAddTransactionBinding;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.repository.TransactionRepository;
import com.zjf.fincialsystem.ui.viewmodel.AddTransactionViewModel;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.StatusBarUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.bumptech.glide.Glide;

/**
 * 添加交易记录Activity
 */
public class AddTransactionActivity extends AppCompatActivity {
    
    private static final String TAG = "AddTransactionActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final String EXTRA_TRANSACTION = "extra_transaction";
    private ActivityAddTransactionBinding binding;
    private AddTransactionViewModel viewModel;
    private List<Category> categories = new ArrayList<>();
    private Date selectedDate = new Date();
    
    // 当前选中的分类
    private String selectedCategory = null;
    private View selectedCategoryView = null;
    private long selectedCategoryId = -1;
    
    /**
     * 创建启动此活动的意图（用于编辑现有交易）
     */
    public static Intent createIntent(Context context, Transaction transaction) {
        Intent intent = new Intent(context, AddTransactionActivity.class);
        intent.putExtra(EXTRA_TRANSACTION, transaction);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.d(TAG, "AddTransactionActivity onCreate 开始执行");
        
        // 设置沉浸式状态栏
        setupStatusBar();
        
        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 初始化ViewModel
        TransactionRepository repository = new TransactionRepository(this);
        viewModel = new AddTransactionViewModel(repository);
        
        // 检查是否是编辑模式
        Transaction existingTransaction = null;
        if (getIntent().hasExtra(EXTRA_TRANSACTION)) {
            existingTransaction = (Transaction) getIntent().getSerializableExtra(EXTRA_TRANSACTION);
            if (existingTransaction != null) {
                // 设置标题为"编辑交易"
                binding.tvTitle.setText(R.string.edit_transaction);
            }
        }
        
        // 初始化视图
        initViews();
        
        // 设置ViewModel的观察者
        setupViewModelObservers();
        
        // 如果是编辑模式，填充现有数据
        if (existingTransaction != null) {
            fillExistingTransactionData(existingTransaction);
        }
        
        LogUtils.d(TAG, "AddTransactionActivity onCreate 执行完成");
    }
    
    /**
     * 设置沉浸式状态栏
     */
    private void setupStatusBar() {
        // 使用StatusBarUtils工具类设置沉浸式状态栏
        com.zjf.fincialsystem.utils.StatusBarUtils.setImmersiveStatusBar(this, true);
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        try {
            // 设置状态栏高度，确保顶部导航栏不会被状态栏遮挡
            adjustTopLayoutPadding();
            
            // 设置返回按钮点击事件
            binding.btnBack.setOnClickListener(v -> onBackPressed());
            
            // 设置保存按钮点击事件
            binding.btnSave.setOnClickListener(v -> saveTransaction());
            
            // 初始化分类卡片
            initCategorySpinner();
            
            // 初始化日期选择 - 默认设置为当天日期
            selectedDate = new Date(); // 确保selectedDate不为null
            binding.etDate.setText(DateUtils.formatDate(selectedDate));
            binding.etDate.setOnClickListener(v -> showDatePicker());
            
            // 设置交易类型切换事件
            binding.radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rb_income) {
                    loadCategories(Transaction.TYPE_INCOME);
                } else if (checkedId == R.id.rb_expense) {
                    loadCategories(Transaction.TYPE_EXPENSE);
                }
            });
            
            // 设置选择图片按钮点击事件
            binding.btnTakePhoto.setText(R.string.select_from_gallery);
            
            // 添加调试日志
            LogUtils.d(TAG, "设置图片按钮点击事件");
            
            binding.btnTakePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogUtils.d(TAG, "图片按钮被点击");
                    openGallery();
                }
            });
            
            // 设置清除图片按钮
            binding.btnClearImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogUtils.d(TAG, "清除图片按钮被点击");
                    clearImage();
                }
            });
            
            // 默认加载支出分类
            loadCategories(Transaction.TYPE_EXPENSE);
            
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化视图失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 设置ViewModel的观察者
     */
    private void setupViewModelObservers() {
        // 观察支出分类数据
        viewModel.getExpenseCategories().observe(this, expenseCategories -> {
            if (expenseCategories != null && !expenseCategories.isEmpty()) {
                LogUtils.d(TAG, "观察到支出分类数据更新，数量: " + expenseCategories.size());
                // 可以使用这些分类数据来更新UI
                if (binding.rbExpense.isChecked()) {
                    // 如果当前显示的是支出界面，则更新分类
                    categories = expenseCategories;
                    // 更新UI显示这些分类
                }
            } else {
                LogUtils.w(TAG, "观察到的支出分类数据为空");
            }
        });
        
        // 观察收入分类数据
        viewModel.getIncomeCategories().observe(this, incomeCategories -> {
            if (incomeCategories != null && !incomeCategories.isEmpty()) {
                LogUtils.d(TAG, "观察到收入分类数据更新，数量: " + incomeCategories.size());
                // 可以使用这些分类数据来更新UI
                if (binding.rbIncome.isChecked()) {
                    // 如果当前显示的是收入界面，则更新分类
                    categories = incomeCategories;
                    // 更新UI显示这些分类
                }
            } else {
                LogUtils.w(TAG, "观察到的收入分类数据为空");
            }
        });
    }
    
    /**
     * 调整顶部布局的内边距，确保不被状态栏遮挡
     */
    private void adjustTopLayoutPadding() {
        try {
            // 获取状态栏高度
            int statusBarHeight = com.zjf.fincialsystem.utils.StatusBarUtils.getStatusBarHeight(this);
            
            // 直接获取滚动视图
            View statusBarGradient = binding.getRoot().findViewById(R.id.status_bar_gradient);
            if (statusBarGradient != null && statusBarGradient.getParent() instanceof View) {
                View scrollView = (View) statusBarGradient.getParent();
                
                if (scrollView instanceof androidx.core.widget.NestedScrollView) {
                    View contentLayout = ((androidx.core.widget.NestedScrollView) scrollView).getChildAt(0);
                    if (contentLayout != null) {
                        contentLayout.setPadding(
                                contentLayout.getPaddingLeft(),
                                statusBarHeight + contentLayout.getPaddingTop(), // 添加状态栏高度
                                contentLayout.getPaddingRight(),
                                contentLayout.getPaddingBottom()
                        );
                    }
                }
            } else {
                // 备用方法：直接为Activity根布局设置内边距
                View rootView = binding.getRoot();
                if (rootView != null) {
                    View contentView = rootView.findViewById(android.R.id.content);
                    if (contentView == null) contentView = rootView;
                    
                    contentView.setPadding(
                            contentView.getPaddingLeft(),
                            statusBarHeight + contentView.getPaddingTop(),
                            contentView.getPaddingRight(),
                            contentView.getPaddingBottom()
                    );
                }
                
                LogUtils.d(TAG, "使用备用方法调整顶部布局内边距");
            }
            
            LogUtils.d(TAG, "已调整顶部布局内边距，状态栏高度: " + statusBarHeight + "px");
        } catch (Exception e) {
            LogUtils.e(TAG, "调整顶部布局内边距时出错: " + e.getMessage());
        }
    }
    
    /**
     * 初始化分类下拉框
     */
    private void initCategorySpinner() {
        try {
            // 初始化分类卡片点击事件
            setupCategoryCardClickListeners();
            
            // 加载分类
            int type = binding.rbIncome.isChecked() ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;
            loadCategories(type);
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化分类选择器失败: " + e.getMessage());
        }
    }

    /**
     * 设置分类卡片点击事件
     */
    private void setupCategoryCardClickListeners() {
        // 设置所有分类卡片的点击事件
        binding.cardFood.setOnClickListener(v -> {
            // 根据当前类型设置不同ID
            if (binding.rbIncome.isChecked()) {
                selectCategory(v, "工资", 6); // 收入-工资
            } else {
                selectCategory(v, "餐饮", 4); // 支出-餐饮
            }
        });
        binding.cardShopping.setOnClickListener(v -> {
            // 根据当前类型设置不同ID
            if (binding.rbIncome.isChecked()) {
                selectCategory(v, "奖金", 7); // 收入-奖金
            } else {
                selectCategory(v, "购物", 5); // 支出-购物
            }
        });
        binding.cardHousing.setOnClickListener(v -> {
            // 根据当前类型设置不同ID
            if (binding.rbIncome.isChecked()) {
                selectCategory(v, "投资收益", 8); // 收入-投资收益
            } else {
                selectCategory(v, "住房", 7); // 支出-住房
            }
        });
        binding.cardTransport.setOnClickListener(v -> {
            // 根据当前类型设置不同ID
            if (binding.rbIncome.isChecked()) {
                selectCategory(v, "兼职", 9); // 收入-兼职
            } else {
                selectCategory(v, "交通", 6); // 支出-交通
            }
        });
        binding.cardMedical.setOnClickListener(v -> {
            // 根据当前类型设置不同ID
            if (binding.rbIncome.isChecked()) {
                selectCategory(v, "退款", 10); // 收入-退款
            } else {
                selectCategory(v, "医疗", 8); // 支出-医疗
            }
        });
        binding.cardEducation.setOnClickListener(v -> {
            // 根据当前类型设置不同ID
            if (binding.rbIncome.isChecked()) {
                selectCategory(v, "红包", 11); // 收入-红包
            } else {
                selectCategory(v, "教育", 9); // 支出-教育
            }
        });
        binding.cardEntertainment.setOnClickListener(v -> {
            // 根据当前类型设置不同ID
            if (binding.rbIncome.isChecked()) {
                selectCategory(v, "其他收入", 12); // 收入-其他收入
            } else {
                selectCategory(v, "娱乐", 10); // 支出-娱乐
            }
        });
        binding.cardMore.setOnClickListener(v -> selectCategory(v, "其他", 11)); // 支出-其他
    }

    /**
     * 选择分类
     */
    private void selectCategory(View view, String categoryName, long categoryId) {
        if (view instanceof androidx.cardview.widget.CardView) {
            androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) view;
            
            // 如果当前点击的是已选中的卡片，则取消选中
            if (view == selectedCategoryView) {
                cardView.setCardBackgroundColor(Color.WHITE);
                selectedCategoryView = null;
                selectedCategory = null;
                selectedCategoryId = -1;
                return;
            }
            
            // 重置之前选中的卡片
            if (selectedCategoryView instanceof androidx.cardview.widget.CardView) {
                ((androidx.cardview.widget.CardView) selectedCategoryView).setCardBackgroundColor(Color.WHITE);
            }
            
            // 设置当前选中的卡片
            cardView.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary));
            selectedCategoryView = view;
            selectedCategory = categoryName;
            selectedCategoryId = categoryId;
            
            LogUtils.d(TAG, "选择了分类: " + categoryName + ", ID: " + categoryId);
        }
    }

    /**
     * 加载分类
     */
    private void loadCategories(int type) {
        try {
            // 重置选中状态
            if (selectedCategoryView instanceof androidx.cardview.widget.CardView) {
                ((androidx.cardview.widget.CardView) selectedCategoryView).setCardBackgroundColor(Color.WHITE);
            }
            selectedCategory = null;
            selectedCategoryView = null;
            selectedCategoryId = -1;
            
            // 根据类型显示/隐藏不同的分类卡片
            if (type == Transaction.TYPE_INCOME) {
                // 收入类别的卡片可见性设置
                if (binding.cardFood != null) binding.cardFood.setVisibility(View.VISIBLE);
                if (binding.cardShopping != null) binding.cardShopping.setVisibility(View.VISIBLE);
                if (binding.cardHousing != null) binding.cardHousing.setVisibility(View.VISIBLE);
                if (binding.cardTransport != null) binding.cardTransport.setVisibility(View.VISIBLE);
                if (binding.cardMedical != null) binding.cardMedical.setVisibility(View.VISIBLE);
                if (binding.cardEducation != null) binding.cardEducation.setVisibility(View.VISIBLE);
                if (binding.cardEntertainment != null) binding.cardEntertainment.setVisibility(View.VISIBLE);
                
                // 设置收入类别的文本
                try {
                    // 工资
                    if (binding.cardFood != null) {
                        View tvName = binding.cardFood.findViewById(R.id.tv_category_name);
                        View ivIcon = binding.cardFood.findViewById(R.id.iv_category_icon);
                        if (tvName != null) tvName.setVisibility(View.VISIBLE);
                        if (ivIcon != null) ivIcon.setVisibility(View.VISIBLE);
                        if (tvName instanceof android.widget.TextView) {
                            ((android.widget.TextView) tvName).setText("工资");
                        }
                    }
                    
                    // 奖金
                    if (binding.cardShopping != null) {
                        View tvName = binding.cardShopping.findViewById(R.id.tv_category_name);
                        View ivIcon = binding.cardShopping.findViewById(R.id.iv_category_icon);
                        if (tvName != null) tvName.setVisibility(View.VISIBLE);
                        if (ivIcon != null) ivIcon.setVisibility(View.VISIBLE);
                        if (tvName instanceof android.widget.TextView) {
                            ((android.widget.TextView) tvName).setText("奖金");
                        }
                    }
                    
                    // 投资收益
                    if (binding.cardHousing != null) {
                        View tvName = binding.cardHousing.findViewById(R.id.tv_category_name);
                        View ivIcon = binding.cardHousing.findViewById(R.id.iv_category_icon);
                        if (tvName != null) tvName.setVisibility(View.VISIBLE);
                        if (ivIcon != null) ivIcon.setVisibility(View.VISIBLE);
                        if (tvName instanceof android.widget.TextView) {
                            ((android.widget.TextView) tvName).setText("投资收益");
                        }
                    }
                    
                    // 兼职
                    if (binding.cardTransport != null) {
                        View tvName = binding.cardTransport.findViewById(R.id.tv_category_name);
                        View ivIcon = binding.cardTransport.findViewById(R.id.iv_category_icon);
                        if (tvName != null) tvName.setVisibility(View.VISIBLE);
                        if (ivIcon != null) ivIcon.setVisibility(View.VISIBLE);
                        if (tvName instanceof android.widget.TextView) {
                            ((android.widget.TextView) tvName).setText("兼职");
                        }
                    }
                    
                    // 退款
                    if (binding.cardMedical != null) {
                        View tvName = binding.cardMedical.findViewById(R.id.tv_category_name);
                        View ivIcon = binding.cardMedical.findViewById(R.id.iv_category_icon);
                        if (tvName != null) tvName.setVisibility(View.VISIBLE);
                        if (ivIcon != null) ivIcon.setVisibility(View.VISIBLE);
                        if (tvName instanceof android.widget.TextView) {
                            ((android.widget.TextView) tvName).setText("退款");
                        }
                    }
                    
                    // 红包
                    if (binding.cardEducation != null) {
                        View tvName = binding.cardEducation.findViewById(R.id.tv_category_name);
                        View ivIcon = binding.cardEducation.findViewById(R.id.iv_category_icon);
                        if (tvName != null) tvName.setVisibility(View.VISIBLE);
                        if (ivIcon != null) ivIcon.setVisibility(View.VISIBLE);
                        if (tvName instanceof android.widget.TextView) {
                            ((android.widget.TextView) tvName).setText("红包");
                        }
                    }
                    
                    // 其他收入
                    if (binding.cardEntertainment != null) {
                        View tvName = binding.cardEntertainment.findViewById(R.id.tv_category_name);
                        View ivIcon = binding.cardEntertainment.findViewById(R.id.iv_category_icon);
                        if (tvName != null) tvName.setVisibility(View.VISIBLE);
                        if (ivIcon != null) ivIcon.setVisibility(View.VISIBLE);
                        if (tvName instanceof android.widget.TextView) {
                            ((android.widget.TextView) tvName).setText("其他收入");
                        }
                    }
                } catch (Exception e) {
                    LogUtils.e(TAG, "设置收入类别文本出错: " + e.getMessage(), e);
                }
            } else {
                // 支出类别的卡片可见性设置
                if (binding.cardFood != null) binding.cardFood.setVisibility(View.VISIBLE);
                if (binding.cardShopping != null) binding.cardShopping.setVisibility(View.VISIBLE);
                if (binding.cardHousing != null) binding.cardHousing.setVisibility(View.VISIBLE);
                if (binding.cardTransport != null) binding.cardTransport.setVisibility(View.VISIBLE);
                if (binding.cardMedical != null) binding.cardMedical.setVisibility(View.VISIBLE);
                if (binding.cardEducation != null) binding.cardEducation.setVisibility(View.VISIBLE);
                if (binding.cardEntertainment != null) binding.cardEntertainment.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "加载分类时出错: " + e.getMessage(), e);
            Toast.makeText(this, "加载分类失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示日期选择对话框
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    selectedDate = selectedCalendar.getTime();
                    binding.etDate.setText(DateUtils.formatDate(selectedDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        
        datePickerDialog.show();
    }
    
    /**
     * 打开相册选择图片
     */
    private void openGallery() {
        LogUtils.d(TAG, "openGallery方法被调用");
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            
            // 检查是否有应用可以处理这个Intent
            if (intent.resolveActivity(getPackageManager()) != null) {
                LogUtils.d(TAG, "启动相册选择器");
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
            } else {
                LogUtils.e(TAG, "没有找到处理图片选择的应用");
                Toast.makeText(this, "无法访问相册", Toast.LENGTH_SHORT).show();
                
                // 尝试使用备用方法
                Intent backupIntent = new Intent(Intent.ACTION_GET_CONTENT);
                backupIntent.setType("image/*");
                if (backupIntent.resolveActivity(getPackageManager()) != null) {
                    LogUtils.d(TAG, "使用备用方法启动文件选择器");
                    startActivityForResult(backupIntent, REQUEST_PICK_IMAGE);
                } else {
                    Toast.makeText(this, "设备不支持图片选择功能", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "打开相册失败：" + e.getMessage(), e);
            Toast.makeText(this, "打开相册失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // 处理拍照结果
                if (data.getExtras() != null) {
                    binding.ivImage.setImageBitmap((android.graphics.Bitmap) data.getExtras().get("data"));
                    binding.ivImage.setVisibility(android.view.View.VISIBLE);
                    binding.btnClearImage.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "照片已添加", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                // 处理相册选择结果
                try {
                    binding.ivImage.setImageURI(data.getData());
                    binding.ivImage.setVisibility(android.view.View.VISIBLE);
                    binding.btnClearImage.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "图片已添加", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    LogUtils.e(TAG, "加载图片失败：" + e.getMessage(), e);
                    Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    /**
     * 保存交易记录
     */
    private void saveTransaction() {
        try {
            LogUtils.d(TAG, "开始执行saveTransaction方法");
            
            // 验证交易类型
            boolean hasTypeSelected = binding.rbIncome.isChecked() || 
                                      binding.rbExpense.isChecked() || 
                                      binding.rbTransfer.isChecked();
            if (!hasTypeSelected) {
                Toast.makeText(this, "交易类型不能为空，请选择交易类型", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 验证分类选择
            if (selectedCategoryId == -1) {
                Toast.makeText(this, "交易分类不能为空，请选择分类", Toast.LENGTH_SHORT).show();
                LogUtils.d(TAG, "保存失败：未选择分类");
                return;
            }
            
            // 验证金额
            String amountStr = binding.etAmount.getText().toString().trim();
            if (TextUtils.isEmpty(amountStr)) {
                binding.etAmount.setError("金额不能为空");
                binding.etAmount.requestFocus();
                LogUtils.d(TAG, "保存失败：金额为空");
                return;
            }
            
            // 验证日期
            String dateStr = binding.etDate.getText().toString().trim();
            if (TextUtils.isEmpty(dateStr)) {
                binding.etDate.setError("日期不能为空");
                binding.etDate.requestFocus();
                LogUtils.d(TAG, "保存失败：日期为空");
                return;
            }
            
            // 解析金额
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                binding.etAmount.setError("请输入有效金额");
                binding.etAmount.requestFocus();
                LogUtils.d(TAG, "保存失败：金额格式不正确");
                return;
            }
            
            // 如果金额为0或负数，提示错误
            if (amount <= 0) {
                binding.etAmount.setError("金额必须大于0");
                binding.etAmount.requestFocus();
                Toast.makeText(this, "交易金额必须大于0元", Toast.LENGTH_SHORT).show();
                LogUtils.d(TAG, "保存失败：金额必须大于0");
                return;
            }
            
            // 获取描述和备注
            String description = binding.etDescription.getText().toString().trim();
            String note = binding.etNote.getText().toString().trim();
            
            // 获取交易类型
            int type = binding.rbIncome.isChecked() ? Transaction.TYPE_INCOME : 
                      (binding.rbExpense.isChecked() ? Transaction.TYPE_EXPENSE : Transaction.TYPE_TRANSFER);
            
            LogUtils.d(TAG, "用户输入数据检查通过，开始创建交易记录 - 类型: " + type + ", 金额: " + amount + ", 分类ID: " + selectedCategoryId + ", 描述: " + description);
            
            // 显示进度提示
            Toast.makeText(this, "正在保存...", Toast.LENGTH_SHORT).show();
            
            // 使用ViewModel添加交易记录
            viewModel.addTransaction(type, amount, selectedCategoryId, description, selectedDate)
                    .observe(this, this::handleAddResult);
            
        } catch (Exception e) {
            LogUtils.e(TAG, "保存交易记录失败：" + e.getClass().getName() + ": " + e.getMessage(), e);
            
            // 打印堆栈跟踪以便调试
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            LogUtils.e(TAG, "异常堆栈: " + sw.toString());
            
            Toast.makeText(this, R.string.operation_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理添加交易记录的结果
     */
    private void handleAddResult(Transaction transaction) {
        LogUtils.d(TAG, "收到添加交易记录的结果: " + (transaction != null ? "成功" : "失败"));
        if (transaction != null) {
            // 添加成功
            Toast.makeText(this, "添加交易记录成功", Toast.LENGTH_SHORT).show();
            LogUtils.i(TAG, "交易记录添加成功，正在关闭页面");
            // 关闭页面
            finish();
        } else {
            // 添加失败
            Toast.makeText(this, "添加交易记录失败，请重试", Toast.LENGTH_LONG).show();
            LogUtils.e(TAG, "交易记录添加失败");
        }
    }
    
    // 添加清除图片的方法
    private void clearImage() {
        binding.ivImage.setImageBitmap(null);
        binding.ivImage.setVisibility(View.GONE);
        binding.btnClearImage.setVisibility(View.GONE);
        Toast.makeText(this, "图片已清除", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 填充现有交易数据
     */
    private void fillExistingTransactionData(Transaction transaction) {
        try {
            // 设置交易类型
            if (transaction.getType() == Transaction.TYPE_INCOME) {
                binding.rbIncome.setChecked(true);
            } else {
                binding.rbExpense.setChecked(true);
            }
            
            // 设置金额
            binding.etAmount.setText(String.valueOf(transaction.getAmount()));
            
            // 设置日期
            selectedDate = transaction.getDate();
            binding.etDate.setText(DateUtils.formatDate(selectedDate));
            
            // 设置备注
            if (transaction.getNote() != null) {
                binding.etNote.setText(transaction.getNote());
            }
            
            // 设置描述
            if (transaction.getDescription() != null) {
                binding.etDescription.setText(transaction.getDescription());
            }
            
            // 设置分类（需要在分类加载完成后选择）
            if (transaction.getCategory() != null) {
                // 保存分类ID，在分类加载完成后选中
                selectedCategory = transaction.getCategory().getName();
                selectedCategoryId = transaction.getCategory().getId();
                // TODO: 在分类加载完成后选中对应的分类卡片
            }
            
            // 设置图片（如果有）
            String imagePath = transaction.getImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                binding.ivImage.setVisibility(View.VISIBLE);
                binding.btnClearImage.setVisibility(View.VISIBLE);
                
                // 加载图片
                Glide.with(this)
                        .load(imagePath)
                        .into(binding.ivImage);
            }
            
            LogUtils.d(TAG, "已填充现有交易数据：" + transaction.toString());
        } catch (Exception e) {
            LogUtils.e(TAG, "填充现有交易数据失败：" + e.getMessage(), e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 