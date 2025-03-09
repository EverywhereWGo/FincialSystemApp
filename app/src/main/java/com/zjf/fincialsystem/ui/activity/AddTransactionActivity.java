package com.zjf.fincialsystem.ui.activity;

import android.app.DatePickerDialog;
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

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityAddTransactionBinding;
import com.zjf.fincialsystem.db.DatabaseManager;
import com.zjf.fincialsystem.db.dao.TransactionDao;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.utils.LogUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 添加交易记录Activity
 */
public class AddTransactionActivity extends AppCompatActivity {
    
    private static final String TAG = "AddTransactionActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private ActivityAddTransactionBinding binding;
    private List<Category> categories = new ArrayList<>();
    private Date selectedDate = new Date();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置沉浸式状态栏
        setupStatusBar();
        
        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 初始化视图
        initViews();
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
            
            // 设置保存按钮点击事件
            binding.btnSave.setOnClickListener(v -> saveTransaction());
            
            // 初始化分类下拉框
            initCategorySpinner();
            
            // 初始化日期选择
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
        binding.cardFood.setOnClickListener(v -> selectCategory(v, "餐饮"));
        binding.cardShopping.setOnClickListener(v -> selectCategory(v, "购物"));
        binding.cardHousing.setOnClickListener(v -> selectCategory(v, "住房"));
        binding.cardTransport.setOnClickListener(v -> selectCategory(v, "交通"));
        binding.cardMedical.setOnClickListener(v -> selectCategory(v, "医疗"));
        binding.cardEducation.setOnClickListener(v -> selectCategory(v, "教育"));
        binding.cardEntertainment.setOnClickListener(v -> selectCategory(v, "娱乐"));
        binding.cardMore.setOnClickListener(v -> showMoreCategories());
    }

    // 当前选中的分类
    private String selectedCategory = null;
    private View selectedCategoryView = null;

    /**
     * 选择分类
     */
    private void selectCategory(View view, String categoryName) {
        if (view instanceof androidx.cardview.widget.CardView) {
            androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) view;
            
            // 如果当前点击的是已选中的卡片，则取消选中
            if (view == selectedCategoryView) {
                cardView.setCardBackgroundColor(Color.WHITE);
                selectedCategoryView = null;
                selectedCategory = null;
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
        }
    }

    /**
     * 显示更多分类选项
     */
    private void showMoreCategories() {
        // 这里可以实现跳转到分类管理页面或显示更多分类的对话框
        Toast.makeText(this, "显示更多分类功能待实现", Toast.LENGTH_SHORT).show();
    }

    /**
     * 加载分类
     */
    private void loadCategories(int type) {
        // 重置选中状态
        if (selectedCategoryView instanceof androidx.cardview.widget.CardView) {
            ((androidx.cardview.widget.CardView) selectedCategoryView).setCardBackgroundColor(Color.WHITE);
        }
        selectedCategory = null;
        selectedCategoryView = null;
        
        // 根据类型显示/隐藏不同的分类卡片
        if (type == Transaction.TYPE_INCOME) {
            // 收入类别的卡片可见性设置
            binding.cardFood.setVisibility(View.GONE);
            binding.cardShopping.setVisibility(View.GONE);
            binding.cardHousing.setVisibility(View.GONE);
            binding.cardTransport.setVisibility(View.GONE);
            binding.cardMedical.setVisibility(View.GONE);
            binding.cardEducation.setVisibility(View.GONE);
            binding.cardEntertainment.setVisibility(View.GONE);
            
            // TODO: 在这里设置收入类别的卡片可见性
            // 例如：工资、奖金等
            Toast.makeText(this, "收入分类选项待实现", Toast.LENGTH_SHORT).show();
        } else {
            // 支出类别的卡片可见性设置
            binding.cardFood.setVisibility(View.VISIBLE);
            binding.cardShopping.setVisibility(View.VISIBLE);
            binding.cardHousing.setVisibility(View.VISIBLE);
            binding.cardTransport.setVisibility(View.VISIBLE);
            binding.cardMedical.setVisibility(View.VISIBLE);
            binding.cardEducation.setVisibility(View.VISIBLE);
            binding.cardEntertainment.setVisibility(View.VISIBLE);
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
            // 获取输入
            String amountStr = binding.etAmount.getText().toString().trim();
            String description = binding.etDescription.getText().toString().trim();
            String note = binding.etNote.getText().toString().trim();
            
            // 验证输入
            if (TextUtils.isEmpty(amountStr)) {
                binding.etAmount.setError("请输入有效金额");
                LogUtils.d(TAG, "保存失败：金额为空");
                return;
            }
            
            if (selectedCategory == null) {
                Toast.makeText(this, "请选择一个分类", Toast.LENGTH_SHORT).show();
                LogUtils.d(TAG, "保存失败：未选择分类");
                return;
            }
            
            // 解析金额
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                binding.etAmount.setError("请输入有效金额");
                LogUtils.d(TAG, "保存失败：金额格式不正确");
                return;
            }
            
            // 如果金额为0，提示错误
            if (amount == 0) {
                binding.etAmount.setError("金额不能为0");
                LogUtils.d(TAG, "保存失败：金额不能为0");
                return;
            }
            
            // 获取所选的分类ID
            long categoryId = 1; // 默认分类ID
            
            // 根据选中的分类名称找到对应的分类ID
            if (selectedCategory != null) {
                if ("餐饮".equals(selectedCategory)) {
                    categoryId = 4;
                } else if ("购物".equals(selectedCategory)) {
                    categoryId = 5;
                } else if ("交通".equals(selectedCategory)) {
                    categoryId = 6;
                } else if ("住房".equals(selectedCategory)) {
                    categoryId = 7;
                } else if ("医疗".equals(selectedCategory)) {
                    categoryId = 8;
                } else if ("教育".equals(selectedCategory)) {
                    categoryId = 9;
                } else if ("娱乐".equals(selectedCategory)) {
                    categoryId = 10;
                }
            }
            
            // 检查DatabaseManager实例
            if (DatabaseManager.getInstance() == null) {
                LogUtils.e(TAG, "DatabaseManager实例为空");
                Toast.makeText(this, "数据库未初始化", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 检查数据库状态
            String dbStatus = DatabaseManager.getInstance().checkDatabaseStatus();
            LogUtils.d(TAG, "数据库状态检查:\n" + dbStatus);
            
            // 获取交易类型
            int type = binding.rbIncome.isChecked() ? Transaction.TYPE_INCOME : 
                      (binding.rbExpense.isChecked() ? Transaction.TYPE_EXPENSE : Transaction.TYPE_TRANSFER);
            
            LogUtils.d(TAG, "准备创建交易对象 - 类型: " + type + ", 金额: " + amount + ", 分类ID: " + categoryId);
            
            // 创建交易对象
            Transaction transaction = new Transaction();
            transaction.setUserId(1); // 这里应该使用当前登录用户的ID
            transaction.setCategoryId(categoryId);
            transaction.setType(type);
            transaction.setAmount(amount);
            transaction.setDate(selectedDate);
            
            LogUtils.d(TAG, "设置交易描述: " + description + ", 备注: " + note);
            
            transaction.setDescription(description);
            transaction.setNote(note);
            
            // 如果有图片，保存图片路径（这里简化处理）
            if (binding.ivImage.getVisibility() == View.VISIBLE && binding.ivImage.getDrawable() != null) {
                // 在实际应用中，这里应该保存图片到文件系统并记录路径
                transaction.setImagePath("image_path_placeholder");
                LogUtils.d(TAG, "设置图片路径");
            }
            
            // 设置创建和更新时间
            long currentTime = System.currentTimeMillis();
            transaction.setCreatedAt(currentTime);
            transaction.setUpdatedAt(currentTime);
            
            // 保存到数据库
            try {
                LogUtils.d(TAG, "开始将交易数据保存到数据库");
                TransactionDao transactionDao = DatabaseManager.getInstance().getTransactionDao();
                
                if (transactionDao == null) {
                    LogUtils.e(TAG, "TransactionDao为空");
                    Toast.makeText(this, "数据访问对象未初始化", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                long id = transactionDao.insert(transaction);
                LogUtils.d(TAG, "insert方法返回的ID: " + id);
                
                if (id > 0) {
                    LogUtils.d(TAG, "交易记录保存成功，ID：" + id);
                    Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    LogUtils.e(TAG, "交易保存失败，insert方法返回ID <= 0");
                    Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "数据库操作异常: " + e.getClass().getName() + ": " + e.getMessage());
                Toast.makeText(this, "数据库操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "保存交易记录失败：" + e.getClass().getName() + ": " + e.getMessage(), e);
            
            // 打印堆栈跟踪以便调试
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            LogUtils.e(TAG, "异常堆栈: " + sw.toString());
            
            Toast.makeText(this, R.string.operation_failed, Toast.LENGTH_SHORT).show();
        }
    }
    
    // 添加清除图片的方法
    private void clearImage() {
        binding.ivImage.setImageBitmap(null);
        binding.ivImage.setVisibility(View.GONE);
        binding.btnClearImage.setVisibility(View.GONE);
        Toast.makeText(this, "图片已清除", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 