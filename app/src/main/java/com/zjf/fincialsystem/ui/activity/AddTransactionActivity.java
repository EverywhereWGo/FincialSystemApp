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
import android.widget.TextView;

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

import java.util.Map;
import java.util.HashMap;

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
    
    // 存储正在编辑的交易记录
    private Transaction existingTransaction = null;
    
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
            binding.btnBack.setOnClickListener(v -> {
                // 设置结果为取消
                setResult(RESULT_CANCELED);
                finish();
            });
            
            // 设置保存按钮点击事件
            binding.btnSave.setOnClickListener(v -> saveTransaction());
            
            // 初始化分类卡片
            initCategorySpinner();
            
            // 初始化日期选择 - 默认设置为当天日期
            if (selectedDate == null) {
                selectedDate = new Date(); // 确保selectedDate不为null
                LogUtils.d(TAG, "初始化selectedDate为当前日期");
            }
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
                // 无论当前是否显示支出界面，都保存最新的分类数据
                if (binding.rbExpense.isChecked()) {
                    // 更新全局分类列表
                    categories.clear();
                    categories.addAll(expenseCategories);
                    LogUtils.d(TAG, "已更新全局分类列表(支出)，当前大小: " + categories.size());
                    
                    // 打印所有分类ID和名称，方便调试
                    for (Category category : categories) {
                        LogUtils.d(TAG, "支出分类: " + category.getName() + ", ID: " + category.getId());
                    }
                    
                    // 更新UI显示这些分类
                    loadCategories(Transaction.TYPE_EXPENSE);
                }
            } else {
                LogUtils.w(TAG, "观察到的支出分类数据为空");
            }
        });
        
        // 观察收入分类数据
        viewModel.getIncomeCategories().observe(this, incomeCategories -> {
            if (incomeCategories != null && !incomeCategories.isEmpty()) {
                LogUtils.d(TAG, "观察到收入分类数据更新，数量: " + incomeCategories.size());
                // 无论当前是否显示收入界面，都保存最新的分类数据
                if (binding.rbIncome.isChecked()) {
                    // 更新全局分类列表
                    categories.clear();
                    categories.addAll(incomeCategories);
                    LogUtils.d(TAG, "已更新全局分类列表(收入)，当前大小: " + categories.size());
                    
                    // 打印所有分类ID和名称，方便调试
                    for (Category category : categories) {
                        LogUtils.d(TAG, "收入分类: " + category.getName() + ", ID: " + category.getId());
                    }
                    
                    // 更新UI显示这些分类
                    loadCategories(Transaction.TYPE_INCOME);
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
            // 加载分类
            int type = binding.rbIncome.isChecked() ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;
            loadCategories(type);
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化分类选择器失败: " + e.getMessage());
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
            
            // 先隐藏所有分类卡片
            if (binding.cardFood != null) binding.cardFood.setVisibility(View.GONE);
            if (binding.cardShopping != null) binding.cardShopping.setVisibility(View.GONE);
            if (binding.cardHousing != null) binding.cardHousing.setVisibility(View.GONE);
            if (binding.cardTransport != null) binding.cardTransport.setVisibility(View.GONE);
            if (binding.cardMedical != null) binding.cardMedical.setVisibility(View.GONE);
            if (binding.cardEducation != null) binding.cardEducation.setVisibility(View.GONE);
            if (binding.cardEntertainment != null) binding.cardEntertainment.setVisibility(View.GONE);
            if (binding.cardMore != null) binding.cardMore.setVisibility(View.GONE);
            
            LogUtils.d(TAG, "加载分类开始，类型: " + (type == Transaction.TYPE_INCOME ? "收入" : "支出"));
            
            // 根据类型获取对应的分类数据
            List<Category> currentCategories = new ArrayList<>();
            if (type == Transaction.TYPE_INCOME) {
                currentCategories = viewModel.getIncomeCategories().getValue();
                LogUtils.d(TAG, "获取收入分类数据: " + (currentCategories != null ? currentCategories.size() : 0) + "个");
                
                // 重要：确保全局categories变量更新为当前类型的分类列表
                if (currentCategories != null && !currentCategories.isEmpty()) {
                    categories.clear();
                    categories.addAll(currentCategories);
                    LogUtils.d(TAG, "全局分类列表已更新为收入分类，大小: " + categories.size());
                    
                    // 打印分类列表，便于调试
                    for (Category category : categories) {
                        LogUtils.d(TAG, "收入分类: " + category.getName() + ", ID: " + category.getId());
                    }
                }
            } else {
                currentCategories = viewModel.getExpenseCategories().getValue();
                LogUtils.d(TAG, "获取支出分类数据: " + (currentCategories != null ? currentCategories.size() : 0) + "个");
                
                // 重要：确保全局categories变量更新为当前类型的分类列表
                if (currentCategories != null && !currentCategories.isEmpty()) {
                    categories.clear();
                    categories.addAll(currentCategories);
                    LogUtils.d(TAG, "全局分类列表已更新为支出分类，大小: " + categories.size());
                    
                    // 打印分类列表，便于调试
                    for (Category category : categories) {
                        LogUtils.d(TAG, "支出分类: " + category.getName() + ", ID: " + category.getId());
                    }
                }
            }
            
            // 如果分类数据不为空，则动态显示分类卡片
            if (currentCategories != null && !currentCategories.isEmpty()) {
                displayCategoriesOnCards(currentCategories, type);
            } else {
                // 如果分类数据为空，则使用默认的分类（保留原有的静态分类逻辑作为备用）
                LogUtils.d(TAG, "分类数据为空，使用默认分类显示");
                displayDefaultCategories(type);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "加载分类时出错: " + e.getMessage(), e);
            Toast.makeText(this, "加载分类失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 在卡片上显示分类数据
     */
    private void displayCategoriesOnCards(List<Category> categories, int type) {
        try {
            // 设置可见卡片的数量，最多显示8个卡片
            int visibleCardCount = Math.min(categories.size(), 8);
            LogUtils.d(TAG, "准备显示分类卡片，总数: " + visibleCardCount);
            
            // 获取所有可用的卡片视图
            List<androidx.cardview.widget.CardView> cardViews = new ArrayList<>();
            if (binding.cardFood != null) cardViews.add(binding.cardFood);
            if (binding.cardShopping != null) cardViews.add(binding.cardShopping);
            if (binding.cardHousing != null) cardViews.add(binding.cardHousing);
            if (binding.cardTransport != null) cardViews.add(binding.cardTransport);
            if (binding.cardMedical != null) cardViews.add(binding.cardMedical);
            if (binding.cardEducation != null) cardViews.add(binding.cardEducation);
            if (binding.cardEntertainment != null) cardViews.add(binding.cardEntertainment);
            if (binding.cardMore != null) cardViews.add(binding.cardMore);
            
            // 隐藏所有卡片
            for (androidx.cardview.widget.CardView cardView : cardViews) {
                cardView.setVisibility(View.GONE);
            }
            
            // 记录分类ID与卡片的映射关系
            StringBuilder mappingLog = new StringBuilder("分类ID与卡片的映射关系: \n");
            
            // 准备卡片与预期分类名称的映射
            Map<androidx.cardview.widget.CardView, String> cardToExpectedName = new HashMap<>();
            if (binding.cardFood != null) cardToExpectedName.put(binding.cardFood, "餐饮");
            if (binding.cardShopping != null) cardToExpectedName.put(binding.cardShopping, "购物");
            if (binding.cardHousing != null) cardToExpectedName.put(binding.cardHousing, "住房");
            if (binding.cardTransport != null) cardToExpectedName.put(binding.cardTransport, "交通");
            if (binding.cardMedical != null) cardToExpectedName.put(binding.cardMedical, "医疗");
            if (binding.cardEducation != null) cardToExpectedName.put(binding.cardEducation, "教育");
            if (binding.cardEntertainment != null) cardToExpectedName.put(binding.cardEntertainment, "娱乐");
            if (binding.cardMore != null) cardToExpectedName.put(binding.cardMore, "其他");
            
            // 为每个卡片寻找最匹配的分类
            Map<androidx.cardview.widget.CardView, Category> cardToCategoryMap = new HashMap<>();
            
            // 先处理特殊卡片（住房和交通）
            for (androidx.cardview.widget.CardView cardView : cardViews) {
                String expectedName = cardToExpectedName.get(cardView);
                if (expectedName == null) continue;
                
                // 寻找完全匹配的分类
                Category matchedCategory = null;
                for (Category category : categories) {
                    if (category.getName().equals(expectedName)) {
                        matchedCategory = category;
                        break;
                    }
                }
                
                // 如果没有完全匹配，寻找包含的分类
                if (matchedCategory == null) {
                    for (Category category : categories) {
                        if (category.getName().contains(expectedName) || 
                            expectedName.contains(category.getName())) {
                            matchedCategory = category;
                            break;
                        }
                    }
                }
                
                if (matchedCategory != null) {
                    cardToCategoryMap.put(cardView, matchedCategory);
                }
            }
            
            // 处理剩余未匹配的卡片和分类
            List<Category> remainingCategories = new ArrayList<>(categories);
            remainingCategories.removeAll(cardToCategoryMap.values());
            
            List<androidx.cardview.widget.CardView> unmappedCards = new ArrayList<>(cardViews);
            unmappedCards.removeAll(cardToCategoryMap.keySet());
            
            // 将剩余分类分配给未映射的卡片
            int index = 0;
            for (androidx.cardview.widget.CardView cardView : unmappedCards) {
                if (index < remainingCategories.size()) {
                    cardToCategoryMap.put(cardView, remainingCategories.get(index));
                    index++;
                }
            }
            
            // 显示卡片和设置监听器
            for (Map.Entry<androidx.cardview.widget.CardView, Category> entry : cardToCategoryMap.entrySet()) {
                androidx.cardview.widget.CardView cardView = entry.getKey();
                Category category = entry.getValue();
                
                // 获取卡片预期的分类名称
                String expectedName = cardToExpectedName.get(cardView);
                
                // 设置卡片
                setupCategoryCard(cardView, category, expectedName);
                
                // 记录映射关系
                String cardName = getCardName(cardView);
                mappingLog.append("  ").append(cardName).append(" -> ")
                         .append(category.getName()).append("(ID:").append(category.getId()).append(")\n");
            }
            
            // 打印映射日志
            LogUtils.d(TAG, mappingLog.toString());
            int visibleCount = (int) cardToCategoryMap.keySet().stream()
                    .filter(cardView -> cardView.getVisibility() == View.VISIBLE)
                    .count();
            LogUtils.d(TAG, "成功显示" + visibleCount + "个分类卡片");
        } catch (Exception e) {
            LogUtils.e(TAG, "显示分类卡片时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取卡片名称（用于日志）
     */
    private String getCardName(View cardView) {
        if (cardView == binding.cardFood) return "食物卡片";
        if (cardView == binding.cardShopping) return "购物卡片";
        if (cardView == binding.cardHousing) return "住房卡片";
        if (cardView == binding.cardTransport) return "交通卡片";
        if (cardView == binding.cardMedical) return "医疗卡片";
        if (cardView == binding.cardEducation) return "教育卡片";
        if (cardView == binding.cardEntertainment) return "娱乐卡片";
        if (cardView == binding.cardMore) return "更多卡片";
        return "未知卡片";
    }
    
    /**
     * 设置单个分类卡片
     */
    private void setupCategoryCard(androidx.cardview.widget.CardView cardView, Category category, String expectedName) {
        if (cardView == null || category == null) {
            return;
        }
        
        cardView.setVisibility(View.VISIBLE);
        
        // 设置卡片文本
        View tvName = cardView.findViewById(R.id.tv_category_name);
        View ivIcon = cardView.findViewById(R.id.iv_category_icon);
        
        if (tvName != null) {
            tvName.setVisibility(View.VISIBLE);
            if (tvName instanceof android.widget.TextView) {
                // 如果存在预期名称，使用预期名称作为显示文本
                String displayText = (expectedName != null) ? expectedName : category.getName();
                ((android.widget.TextView) tvName).setText(displayText);
                
                // 如果卡片期望的名称与分类实际名称不同，记录日志
                if (expectedName != null && !expectedName.equals(category.getName())) {
                    LogUtils.w(TAG, "卡片显示名称 '" + expectedName + 
                            "' 与分类实际名称 '" + category.getName() + "' 不同");
                }
            }
        }
        
        if (ivIcon != null) {
            ivIcon.setVisibility(View.VISIBLE);
        }
        
        // 获取分类信息
        final long categoryId = category.getId();
        final String categoryName = category.getName();
        
        // 卡片日志信息
        String cardType = getCardName(cardView);
        LogUtils.d(TAG, "设置" + cardType + ": " + categoryName + ", ID: " + categoryId);
        
        // 重要：移除之前的所有点击监听器，避免重复添加
        cardView.setOnClickListener(null);
        
        // 设置新的点击监听器，使用分类对象的真实信息
        cardView.setOnClickListener(v -> {
            String displayName = expectedName != null ? expectedName : categoryName;
            LogUtils.d(TAG, "点击了" + cardType + "，选择分类: " + displayName + ", ID: " + categoryId);
            selectCategory(v, displayName, categoryId);
        });
    }
    
    /**
     * 重载原方法以兼容已有代码
     */
    private void setupCategoryCard(androidx.cardview.widget.CardView cardView, Category category) {
        setupCategoryCard(cardView, category, null);
    }

    /**
     * 显示默认分类（当API数据为空时的备用方案）
     */
    private void displayDefaultCategories(int type) {
        LogUtils.d(TAG, "使用默认分类显示 - 类型: " + (type == Transaction.TYPE_INCOME ? "收入" : "支出"));
        
        // 创建默认分类列表
        List<Category> defaultCategories = new ArrayList<>();
        
        if (type == Transaction.TYPE_INCOME) {
            // 默认收入分类
            defaultCategories.add(createDefaultCategory(6, "工资"));
            defaultCategories.add(createDefaultCategory(7, "奖金"));
            defaultCategories.add(createDefaultCategory(8, "投资收益"));
            defaultCategories.add(createDefaultCategory(9, "兼职"));
            defaultCategories.add(createDefaultCategory(10, "退款"));
            defaultCategories.add(createDefaultCategory(11, "红包"));
            defaultCategories.add(createDefaultCategory(12, "其他收入"));
        } else {
            // 默认支出分类
            defaultCategories.add(createDefaultCategory(1, "餐饮"));
            defaultCategories.add(createDefaultCategory(2, "购物"));
            defaultCategories.add(createDefaultCategory(3, "交通"));
            defaultCategories.add(createDefaultCategory(4, "住房"));
            defaultCategories.add(createDefaultCategory(5, "医疗"));
            defaultCategories.add(createDefaultCategory(6, "教育"));
            defaultCategories.add(createDefaultCategory(7, "娱乐"));
            defaultCategories.add(createDefaultCategory(8, "其他"));
        }
        
        // 使用与普通分类相同的逻辑显示默认分类
        displayCategoriesOnCards(defaultCategories, type);
    }
    
    /**
     * 创建默认分类对象
     */
    private Category createDefaultCategory(long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }
    
    /**
     * 显示日期选择对话框
     */
    private void showDatePicker() {
        // 确保selectedDate不为空
        if (selectedDate == null) {
            selectedDate = new Date(); // 如果是null，使用当前日期作为默认值
        }
        
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
                    Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                    binding.ivImage.setImageBitmap(bitmap);
                    binding.ivImage.setVisibility(View.VISIBLE);
                    binding.btnClearImage.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "照片已添加", Toast.LENGTH_SHORT).show();
                    
                    // 如果是编辑模式，立即上传图片
                    if (existingTransaction != null) {
                        uploadImageFromBitmap(bitmap, existingTransaction.getId());
                    }
                }
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                // 处理相册选择结果
                try {
                    Uri selectedImageUri = data.getData();
                    binding.ivImage.setImageURI(selectedImageUri);
                    binding.ivImage.setVisibility(View.VISIBLE);
                    binding.btnClearImage.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "图片已添加", Toast.LENGTH_SHORT).show();
                    
                    // 如果是编辑模式，立即上传图片
                    if (existingTransaction != null) {
                        uploadImageFromUri(selectedImageUri, existingTransaction.getId());
                    }
                } catch (Exception e) {
                    LogUtils.e(TAG, "加载图片失败：" + e.getMessage(), e);
                    Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    /**
     * 从Bitmap上传图片到服务器
     * @param bitmap 图片Bitmap
     * @param transactionId 交易ID（可选，编辑模式时传入）
     */
    private void uploadImageFromBitmap(Bitmap bitmap, Long transactionId) {
        try {
            LogUtils.d(TAG, "开始从Bitmap上传图片");
            // 显示进度对话框
            showProgressDialog("正在上传图片...");
            
            // 将Bitmap转换为File
            java.io.File imageFile = com.zjf.fincialsystem.utils.FileUtils.bitmapToFile(this, bitmap, "transaction_receipt_" + System.currentTimeMillis() + ".jpg");
            
            // 调用上传方法
            uploadImageFile(imageFile, transactionId);
        } catch (Exception e) {
            LogUtils.e(TAG, "Bitmap转换文件失败：" + e.getMessage(), e);
            hideProgressDialog();
            Toast.makeText(this, "图片处理失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 从Uri上传图片到服务器
     * @param uri 图片Uri
     * @param transactionId 交易ID（可选，编辑模式时传入）
     */
    private void uploadImageFromUri(Uri uri, Long transactionId) {
        try {
            LogUtils.d(TAG, "开始从Uri上传图片");
            // 显示进度对话框
            showProgressDialog("正在上传图片...");
            
            // 获取文件路径
            String filePath = com.zjf.fincialsystem.utils.FileUtils.getPathFromUri(this, uri);
            if (filePath == null) {
                LogUtils.e(TAG, "无法从Uri获取文件路径");
                hideProgressDialog();
                Toast.makeText(this, "无法处理所选图片", Toast.LENGTH_SHORT).show();
                return;
            }
            
            java.io.File imageFile = new java.io.File(filePath);
            if (!imageFile.exists()) {
                LogUtils.e(TAG, "文件不存在：" + filePath);
                hideProgressDialog();
                Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 调用上传方法
            uploadImageFile(imageFile, transactionId);
        } catch (Exception e) {
            LogUtils.e(TAG, "Uri处理失败：" + e.getMessage(), e);
            hideProgressDialog();
            Toast.makeText(this, "图片处理失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 上传图片文件到服务器
     * @param imageFile 图片文件
     * @param transactionId 交易ID（可选，编辑模式时传入）
     */
    private void uploadImageFile(java.io.File imageFile, Long transactionId) {
        LogUtils.d(TAG, "开始上传图片文件：" + imageFile.getPath() + "，大小：" + imageFile.length());
        
        if (imageFile == null || !imageFile.exists()) {
            LogUtils.e(TAG, "图片文件不存在或为空");
            Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建文件部分，确保使用正确的参数名"file"
        okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("image/*"),
                imageFile
        );
        
        // MultipartBody.Part的表单字段名称必须与API接口中的参数名一致，这里修改为"file"
        okhttp3.MultipartBody.Part filePart = okhttp3.MultipartBody.Part.createFormData(
                "file", // 修改为服务器端要求的参数名称
                imageFile.getName(),
                requestFile
        );
        
        // 创建transactionId部分
        okhttp3.RequestBody transactionIdPart = null;
        if (transactionId != null) {
            transactionIdPart = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("text/plain"),
                    String.valueOf(transactionId)
            );
        }
        
        // 调用API上传图片
        com.zjf.fincialsystem.network.api.TransactionApiService apiService = 
                com.zjf.fincialsystem.network.NetworkManager.getInstance().getService(
                        com.zjf.fincialsystem.network.api.TransactionApiService.class);
        
        retrofit2.Call<com.zjf.fincialsystem.network.ApiResponse<com.zjf.fincialsystem.network.model.ImageUploadResponse>> call;
        
        if (transactionId != null) {
            call = apiService.uploadTransactionImage(filePart, transactionIdPart);
        } else {
            call = apiService.uploadTransactionImage(filePart, null);
        }
        
        LogUtils.d(TAG, "发起图片上传请求：" + call.request().url() + 
                 ", 文件名：" + imageFile.getName() + 
                 ", 参数名：file" + 
                 (transactionId != null ? ", transactionId=" + transactionId : ""));
        
        call.enqueue(new retrofit2.Callback<com.zjf.fincialsystem.network.ApiResponse<com.zjf.fincialsystem.network.model.ImageUploadResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<com.zjf.fincialsystem.network.ApiResponse<com.zjf.fincialsystem.network.model.ImageUploadResponse>> call,
                     retrofit2.Response<com.zjf.fincialsystem.network.ApiResponse<com.zjf.fincialsystem.network.model.ImageUploadResponse>> response) {
                hideProgressDialog();
                
                LogUtils.d(TAG, "图片上传响应码：" + response.code());
                if (!response.isSuccessful()) {
                    LogUtils.e(TAG, "图片上传失败，HTTP错误码：" + response.code() + ", 错误信息：" + response.message());
                    Toast.makeText(AddTransactionActivity.this, 
                        "图片上传失败：" + response.message() + " (" + response.code() + ")", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (response.body() == null) {
                    LogUtils.e(TAG, "图片上传失败，响应体为空");
                    Toast.makeText(AddTransactionActivity.this, "图片上传失败，服务器返回空响应", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                com.zjf.fincialsystem.network.ApiResponse<com.zjf.fincialsystem.network.model.ImageUploadResponse> apiResponse = response.body();
                LogUtils.d(TAG, "图片上传响应：" + apiResponse.getCode() + ", " + apiResponse.getMsg());
                
                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                    // 上传成功，保存返回的图片URL
                    String imageUrl = apiResponse.getData().getImageUrl();
                    LogUtils.d(TAG, "图片上传成功，URL：" + imageUrl);
                    
                    // 保存图片URL到本地变量，在创建/更新交易时使用
                    uploadedImageUrl = imageUrl;
                    
                    Toast.makeText(AddTransactionActivity.this, "图片上传成功", Toast.LENGTH_SHORT).show();
                } else {
                    LogUtils.e(TAG, "图片上传失败：" + apiResponse.getMsg());
                    Toast.makeText(AddTransactionActivity.this, "图片上传失败：" + apiResponse.getMsg(), Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<com.zjf.fincialsystem.network.ApiResponse<com.zjf.fincialsystem.network.model.ImageUploadResponse>> call,
                            Throwable t) {
                hideProgressDialog();
                LogUtils.e(TAG, "图片上传网络错误：" + t.getMessage(), t);
                Toast.makeText(AddTransactionActivity.this, "网络错误，图片上传失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // 进度对话框
    private android.app.AlertDialog progressDialog;
    
    /**
     * 显示进度对话框
     */
    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_progress, null);
            TextView tvMessage = view.findViewById(R.id.tv_message);
            tvMessage.setText(message);
            builder.setView(view);
            builder.setCancelable(false);
            progressDialog = builder.create();
        }
        progressDialog.show();
    }
    
    /**
     * 隐藏进度对话框
     */
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    
    // 存储上传的图片URL
    private String uploadedImageUrl;
    
    /**
     * 保存交易记录
     */
    private void saveTransaction() {
        try {
            LogUtils.d(TAG, "开始执行saveTransaction方法");
            
            // 验证交易类型
            boolean hasTypeSelected = binding.rbIncome.isChecked() || 
                                      binding.rbExpense.isChecked();
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
            int type = binding.rbIncome.isChecked() ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;
            
            LogUtils.d(TAG, "用户输入数据检查通过，开始创建交易记录 - 类型: " + type + ", 金额: " + amount + ", 分类ID: " + selectedCategoryId + ", 描述: " + description + ", 备注: " + note);
            
            // 显示进度提示
            Toast.makeText(this, "正在保存...", Toast.LENGTH_SHORT).show();
            
            // 如果选择了图片但还没有上传，先上传图片
            if (binding.ivImage.getVisibility() == View.VISIBLE && 
                (uploadedImageUrl == null || uploadedImageUrl.isEmpty()) && 
                binding.ivImage.getDrawable() != null) {
                    
                // 获取Bitmap
                android.graphics.drawable.Drawable drawable = binding.ivImage.getDrawable();
                if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
                    Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
                    
                    // 显示上传进度对话框
                    showProgressDialog("正在上传图片...");
                    
                    // 上传图片，完成后保存交易
                    uploadImageFromBitmap(bitmap, null);
                    
                    // 设置延迟，等待图片上传完成
                    binding.getRoot().postDelayed(() -> {
                        hideProgressDialog();
                        // 再次检查是否上传成功
                        if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
                            // 图片上传成功，继续保存
                            saveTransactionToDB(type, amount, selectedCategoryId, description, note);
                        } else {
                            // 询问用户是否继续保存
                            new android.app.AlertDialog.Builder(this)
                                .setTitle("图片上传")
                                .setMessage("图片可能尚未上传完成，您想要继续保存交易记录吗？")
                                .setPositiveButton("继续保存", (dialog, which) -> {
                                    saveTransactionToDB(type, amount, selectedCategoryId, description, note);
                                })
                                .setNegativeButton("取消", null)
                                .show();
                        }
                    }, 3000); // 给3秒时间上传
                    
                    return;
                }
            }
            
            // 直接保存到数据库
            saveTransactionToDB(type, amount, selectedCategoryId, description, note);
            
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
     * 将交易数据保存到数据库
     */
    private void saveTransactionToDB(int type, double amount, long categoryId, String description, String note) {
        try {
            // 判断是新增还是更新
            if (existingTransaction != null) {
                // 编辑模式 - 使用ViewModel更新交易记录
                LogUtils.d(TAG, "编辑模式，调用更新交易接口，交易ID: " + existingTransaction.getId());
                viewModel.updateTransaction(
                        existingTransaction.getId(),
                        type, 
                        amount, 
                        categoryId, 
                        description, 
                        selectedDate, 
                        note,
                        "",
                        uploadedImageUrl  // 添加图片URL
                ).observe(this, this::handleUpdateResult);
            } else {
                // 新增模式 - 使用ViewModel添加交易记录
                LogUtils.d(TAG, "新增模式，调用添加交易接口");
                viewModel.addTransaction(
                        type, 
                        amount, 
                        categoryId, 
                        description, 
                        selectedDate, 
                        note,
                        "",
                        uploadedImageUrl  // 添加图片URL
                ).observe(this, this::handleAddResult);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "保存到数据库失败：" + e.getMessage(), e);
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            
            // 设置结果码，告知MainActivity刷新数据
            setResult(RESULT_OK);
            
            // 关闭页面
            finish();
        } else {
            // 添加失败
            Toast.makeText(this, "添加交易记录失败，请重试", Toast.LENGTH_LONG).show();
            LogUtils.e(TAG, "交易记录添加失败");
        }
    }
    
    /**
     * 处理更新交易记录的结果
     */
    private void handleUpdateResult(Transaction transaction) {
        LogUtils.d(TAG, "收到更新交易记录的结果: " + (transaction != null ? "成功" : "失败"));
        if (transaction != null) {
            // 更新成功
            Toast.makeText(this, "更新交易记录成功", Toast.LENGTH_SHORT).show();
            LogUtils.i(TAG, "交易记录更新成功，正在关闭页面");
            
            // 设置结果码，告知MainActivity刷新数据
            setResult(RESULT_OK);
            
            // 关闭页面
            finish();
        } else {
            // 更新失败
            Toast.makeText(this, "更新交易记录失败，请重试。如果问题持续存在，请尝试重新登录应用。", Toast.LENGTH_LONG).show();
            LogUtils.e(TAG, "交易记录更新失败");
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
                // 确保加载收入分类
                loadCategories(Transaction.TYPE_INCOME);
            } else {
                binding.rbExpense.setChecked(true);
                // 确保加载支出分类
                loadCategories(Transaction.TYPE_EXPENSE);
            }
            
            // 设置金额
            binding.etAmount.setText(String.valueOf(transaction.getAmount()));
            
            // 设置日期
            if (transaction.getDate() != null) {
                selectedDate = transaction.getDate();
            } else {
                selectedDate = new Date(); // 如果日期为空，使用当前日期
                LogUtils.w(TAG, "交易记录的日期为空，使用当前日期替代");
            }
            binding.etDate.setText(DateUtils.formatDate(selectedDate));
            
            // 设置备注
            if (transaction.getRemark() != null) {
                binding.etNote.setText(transaction.getRemark());
            }
            
            // 设置描述
            if (transaction.getNote() != null) {
                binding.etDescription.setText(transaction.getNote());
            } else if (transaction.getDescription() != null) {
                binding.etDescription.setText(transaction.getDescription());
            }
            
            // 设置分类（需要在分类加载完成后选择）
            if (transaction.getCategory() != null) {
                // 保存分类ID，在分类加载完成后选中
                selectedCategory = transaction.getCategory().getName();
                selectedCategoryId = transaction.getCategory().getId();
                
                // 尝试在UI上选中对应的分类卡片
                selectCategoryCardById(selectedCategoryId);
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
    
    /**
     * 根据分类ID选中对应的分类卡片
     */
    private void selectCategoryCardById(long categoryId) {
        try {
            LogUtils.d(TAG, "尝试根据ID选择分类卡片: " + categoryId);
            
            // 先从分类列表中查找对应的分类名称
            String categoryName = null;
            for (Category category : categories) {
                if (category.getId() == categoryId) {
                    categoryName = category.getName();
                    LogUtils.d(TAG, "在categories列表中找到对应的分类名称: " + categoryName + ", ID: " + categoryId);
                    break;
                }
            }
            
            // 如果在categories中找不到，尝试从硬编码映射中查找
            if (categoryName == null) {
                categoryName = getCategoryNameById(categoryId);
                if (categoryName != null) {
                    LogUtils.d(TAG, "在硬编码映射中找到对应的分类名称: " + categoryName + ", ID: " + categoryId);
                } else {
                    LogUtils.w(TAG, "无法找到ID为" + categoryId + "的分类名称，无法选中对应的卡片");
                    return;
                }
            }
            
            // 查找合适的卡片
            View cardView = null;
            
            // 遍历所有当前可见的卡片，查找分类名称匹配的卡片
            if (binding.cardFood.getVisibility() == View.VISIBLE) {
                View tvName = binding.cardFood.findViewById(R.id.tv_category_name);
                if (tvName instanceof android.widget.TextView) {
                    String text = ((android.widget.TextView) tvName).getText().toString();
                    if (categoryName.equals(text)) {
                        cardView = binding.cardFood;
                        LogUtils.d(TAG, "在cardFood中找到匹配的分类名称: " + text);
                    }
                }
            }
            
            if (cardView == null && binding.cardShopping.getVisibility() == View.VISIBLE) {
                View tvName = binding.cardShopping.findViewById(R.id.tv_category_name);
                if (tvName instanceof android.widget.TextView) {
                    String text = ((android.widget.TextView) tvName).getText().toString();
                    if (categoryName.equals(text)) {
                        cardView = binding.cardShopping;
                        LogUtils.d(TAG, "在cardShopping中找到匹配的分类名称: " + text);
                    }
                }
            }
            
            if (cardView == null && binding.cardHousing.getVisibility() == View.VISIBLE) {
                View tvName = binding.cardHousing.findViewById(R.id.tv_category_name);
                if (tvName instanceof android.widget.TextView) {
                    String text = ((android.widget.TextView) tvName).getText().toString();
                    if (categoryName.equals(text)) {
                        cardView = binding.cardHousing;
                        LogUtils.d(TAG, "在cardHousing中找到匹配的分类名称: " + text);
                    }
                }
            }
            
            if (cardView == null && binding.cardTransport.getVisibility() == View.VISIBLE) {
                View tvName = binding.cardTransport.findViewById(R.id.tv_category_name);
                if (tvName instanceof android.widget.TextView) {
                    String text = ((android.widget.TextView) tvName).getText().toString();
                    if (categoryName.equals(text)) {
                        cardView = binding.cardTransport;
                        LogUtils.d(TAG, "在cardTransport中找到匹配的分类名称: " + text);
                    }
                }
            }
            
            if (cardView == null && binding.cardMedical.getVisibility() == View.VISIBLE) {
                View tvName = binding.cardMedical.findViewById(R.id.tv_category_name);
                if (tvName instanceof android.widget.TextView) {
                    String text = ((android.widget.TextView) tvName).getText().toString();
                    if (categoryName.equals(text)) {
                        cardView = binding.cardMedical;
                        LogUtils.d(TAG, "在cardMedical中找到匹配的分类名称: " + text);
                    }
                }
            }
            
            if (cardView == null && binding.cardEducation.getVisibility() == View.VISIBLE) {
                View tvName = binding.cardEducation.findViewById(R.id.tv_category_name);
                if (tvName instanceof android.widget.TextView) {
                    String text = ((android.widget.TextView) tvName).getText().toString();
                    if (categoryName.equals(text)) {
                        cardView = binding.cardEducation;
                        LogUtils.d(TAG, "在cardEducation中找到匹配的分类名称: " + text);
                    }
                }
            }
            
            if (cardView == null && binding.cardEntertainment.getVisibility() == View.VISIBLE) {
                View tvName = binding.cardEntertainment.findViewById(R.id.tv_category_name);
                if (tvName instanceof android.widget.TextView) {
                    String text = ((android.widget.TextView) tvName).getText().toString();
                    if (categoryName.equals(text)) {
                        cardView = binding.cardEntertainment;
                        LogUtils.d(TAG, "在cardEntertainment中找到匹配的分类名称: " + text);
                    }
                }
            }
            
            if (cardView == null && binding.cardMore.getVisibility() == View.VISIBLE) {
                View tvName = binding.cardMore.findViewById(R.id.tv_category_name);
                if (tvName instanceof android.widget.TextView) {
                    String text = ((android.widget.TextView) tvName).getText().toString();
                    if (categoryName.equals(text)) {
                        cardView = binding.cardMore;
                        LogUtils.d(TAG, "在cardMore中找到匹配的分类名称: " + text);
                    }
                }
            }
            
            // 如果找到了对应的卡片，选中它
            if (cardView != null) {
                LogUtils.d(TAG, "找到并选中分类卡片: " + categoryName + ", ID: " + categoryId);
                selectCategory(cardView, categoryName, categoryId);
            } else {
                LogUtils.w(TAG, "未找到匹配的分类卡片: " + categoryName);
                
                // 当找不到匹配的卡片时，直接设置选中状态（不涉及UI变化）
                selectedCategory = categoryName;
                selectedCategoryId = categoryId;
                LogUtils.d(TAG, "尽管未找到UI卡片，但已设置选中的分类: " + categoryName + ", ID: " + categoryId);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "根据ID选择分类卡片失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据分类ID获取分类名称（硬编码映射）
     */
    private String getCategoryNameById(long categoryId) {
        // 支出分类
        if (categoryId == 1) return "餐饮";
        if (categoryId == 2) return "购物";
        if (categoryId == 3) return "交通";
        if (categoryId == 4) return "住房";
        if (categoryId == 5) return "医疗";
        if (categoryId == 6) return "教育";
        if (categoryId == 7) return "娱乐";
        if (categoryId == 8) return "其他";
        
        // 收入分类
        if (categoryId == 6) return "工资";
        if (categoryId == 7) return "奖金";
        if (categoryId == 8) return "投资收益";
        if (categoryId == 9) return "兼职";
        if (categoryId == 10) return "退款";
        if (categoryId == 11) return "红包";
        if (categoryId == 12) return "其他收入";
        
        LogUtils.w(TAG, "在硬编码映射中未找到ID: " + categoryId + " 对应的分类名称");
        return null;
    }
    
    /**
     * 根据分类名称查找分类ID
     */
    private long findCategoryIdByName(String name) {
        if (TextUtils.isEmpty(name)) {
            LogUtils.w(TAG, "查找分类ID时名称为空");
            return -1;
        }
        
        LogUtils.d(TAG, "尝试查找分类: " + name + ", 当前分类列表大小: " + categories.size());
        
        // 避免在后面的代码中出现NPE
        if (categories == null) {
            LogUtils.w(TAG, "categories列表为null，无法查找分类");
            return getHardcodedCategoryId(name);
        }
        
        // 精确匹配：在当前加载的分类中查找
        for (Category category : categories) {
            if (category.getName().equals(name)) {
                LogUtils.d(TAG, "找到精确匹配的分类: " + name + ", ID: " + category.getId());
                return category.getId();
            }
        }
        
        // 如果没有精确匹配，尝试模糊匹配（包含或被包含关系）
        for (Category category : categories) {
            if (category.getName().contains(name) || name.contains(category.getName())) {
                LogUtils.d(TAG, "找到模糊匹配的分类: " + name + " <-> " + category.getName() + ", ID: " + category.getId());
                return category.getId();
            }
        }
        
        // 如果在categories中找不到，使用硬编码的映射
        long hardcodedId = getHardcodedCategoryId(name);
        if (hardcodedId != -1) {
            LogUtils.d(TAG, "在内置映射中找到分类: " + name + ", ID: " + hardcodedId);
            return hardcodedId;
        }
        
        // 打印所有可用分类，便于调试
        StringBuilder availableCategories = new StringBuilder("可用分类列表: ");
        for (Category category : categories) {
            availableCategories.append(category.getName()).append("(ID:").append(category.getId()).append("), ");
        }
        LogUtils.w(TAG, "未找到匹配的分类: " + name + ", " + availableCategories.toString());
        
        return -1;
    }
    
    /**
     * 基于分类名称获取硬编码的分类ID
     * 该方法作为备选方案，用于在无法通过API获取分类ID时使用
     */
    private long getHardcodedCategoryId(String categoryName) {
        if (TextUtils.isEmpty(categoryName)) {
            LogUtils.w(TAG, "分类名称为空，无法获取ID");
            return -1;
        }
        
        LogUtils.d(TAG, "尝试为分类名称获取硬编码ID: " + categoryName);
        
        // 支出类别ID映射
        if (categoryName.contains("餐饮") || categoryName.contains("食品") || categoryName.contains("吃")) {
            return 1;
        } else if (categoryName.contains("购物") || categoryName.contains("消费") || categoryName.contains("买")) {
            return 2;
        } else if (categoryName.contains("交通") || categoryName.contains("车") || categoryName.contains("地铁") || categoryName.contains("公交")) {
            return 3;
        } else if (categoryName.contains("住房") || categoryName.contains("房租") || categoryName.contains("水电") || categoryName.contains("物业")) {
            return 4;
        } else if (categoryName.contains("医疗") || categoryName.contains("健康") || categoryName.contains("药") || categoryName.contains("医院")) {
            return 5;
        } else if (categoryName.contains("教育") || categoryName.contains("学习") || categoryName.contains("书") || categoryName.contains("课程")) {
            return 6;
        } else if (categoryName.contains("娱乐") || categoryName.contains("游戏") || categoryName.contains("电影")) {
            return 7;
        } 
        
        // 收入类别ID映射
        else if (categoryName.contains("工资") || categoryName.contains("薪资") || categoryName.contains("薪水")) {
            return 6;
        } else if (categoryName.contains("奖金") || categoryName.contains("奖励") || categoryName.contains("奖项")) {
            return 7;
        } else if (categoryName.contains("投资") || categoryName.contains("股票") || categoryName.contains("基金")) {
            return 8;
        } else if (categoryName.contains("兼职") || categoryName.contains("副业") || categoryName.contains("打工")) {
            return 9; 
        } else if (categoryName.contains("退款") || categoryName.contains("报销")) {
            return 10;
        } else if (categoryName.contains("红包") || categoryName.contains("礼金")) {
            return 11;
        } else if (categoryName.contains("其他收入")) {
            return 12;
        }
        
        // 通用的"其他"分类
        else if (categoryName.contains("其他")) {
            return 8;
        }
        
        LogUtils.w(TAG, "未能找到分类名称 '" + categoryName + "' 的硬编码ID");
        return -1;
    }
    
    @Override
    public void onBackPressed() {
        // 设置结果为取消
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
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
            cardView.setCardBackgroundColor(Color.parseColor("#F5F5F5")); // 浅灰色背景
            selectedCategoryView = view;
            selectedCategory = categoryName;
            selectedCategoryId = categoryId;
            
            LogUtils.d(TAG, "选择了分类: " + categoryName + ", ID: " + categoryId);
        }
    }
} 