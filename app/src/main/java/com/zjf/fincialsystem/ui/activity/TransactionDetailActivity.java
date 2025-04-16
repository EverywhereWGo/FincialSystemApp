package com.zjf.fincialsystem.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityTransactionDetailBinding;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.repository.CategoryRepository;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.repository.TransactionRepository;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.StatusBarUtils;

import java.text.DecimalFormat;

/**
 * 交易详情页
 * 展示特定交易的详细信息
 */
public class TransactionDetailActivity extends AppCompatActivity {

    private static final String TAG = "TransactionDetailActivity";
    private static final String EXTRA_TRANSACTION_ID = "extra_transaction_id";

    private ActivityTransactionDetailBinding binding;
    private TransactionRepository transactionRepository;
    private long transactionId = -1;
    private Transaction transaction = null;
    private CategoryRepository categoryRepository;
    // 结果码
    private static final int REQUEST_EDIT_TRANSACTION = 1001;

    /**
     * 创建启动此活动的意图
     */
    public static Intent createIntent(Context context, long transactionId) {
        Intent intent = new Intent(context, TransactionDetailActivity.class);
        intent.putExtra(EXTRA_TRANSACTION_ID, transactionId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化视图绑定
        binding = ActivityTransactionDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置状态栏
        setupStatusBar();

        // 获取传递的交易ID
        transactionId = getIntent().getLongExtra(EXTRA_TRANSACTION_ID, -1);
        if (transactionId == -1) {
            Toast.makeText(this, "无效的交易ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化仓库
        transactionRepository = new TransactionRepository(this);
        categoryRepository = new CategoryRepository(this);
        // 初始化点击事件
        initClickListeners();

        // 加载交易数据
        loadTransactionData();
    }

    /**
     * 设置沉浸式状态栏
     */
    private void setupStatusBar() {
        StatusBarUtils.setImmersiveStatusBar(this, false);

        // 调整工具栏内边距，避免与状态栏重叠
        StatusBarUtils.adjustToolbarForStatusBar(binding.toolbar, this);
    }

    /**
     * 初始化点击事件
     */
    private void initClickListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener(v -> finish());

        // 编辑按钮
        binding.btnEdit.setOnClickListener(v -> {
            if (transaction != null) {
                Intent intent = AddTransactionActivity.createIntent(this, transaction);
                startActivityForResult(intent, REQUEST_EDIT_TRANSACTION);
            }
        });

        // 删除按钮
        binding.btnDelete.setOnClickListener(v -> {
            if (transaction != null) {
                showDeleteConfirmDialog();
            }
        });

        // 图片点击放大
        binding.ivReceipt.setOnClickListener(v -> {
            if (transaction != null && transaction.getImagePath() != null) {
                // 这里可以实现点击查看大图的功能
                Toast.makeText(this, "查看大图功能将在后续版本实现", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 加载交易数据
     */
    private void loadTransactionData() {
        try {
            transactionRepository.getTransaction(transactionId, new RepositoryCallback<Transaction>() {
                @Override
                public void onSuccess(Transaction data) {
                    transaction = data;
                    runOnUiThread(() -> updateUI(transaction));
                }

                @Override
                public void onError(String error) {
                    LogUtils.e(TAG, "加载交易数据失败：" + error);
                    runOnUiThread(() -> {
                        Toast.makeText(TransactionDetailActivity.this, "加载交易数据失败", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void isCacheData(boolean isCache) {
                    // 实现默认方法，从缓存加载时不做特殊处理
                    LogUtils.d(TAG, "交易数据从缓存加载: " + isCache);
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "加载交易数据异常：" + e.getMessage(), e);
            Toast.makeText(this, "加载交易数据失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 更新UI
     */
    private void updateUI(Transaction transaction) {
        try {
            if (transaction == null) {
                return;
            }

            // 格式化金额
            DecimalFormat decimalFormat = new DecimalFormat("¥#,##0.00");
            String formattedAmount = decimalFormat.format(transaction.getAmount());

            // 设置金额和颜色
            if (transaction.getType() == Transaction.TYPE_EXPENSE) {
                binding.tvAmount.setText("-" + formattedAmount);
                binding.tvAmount.setTextColor(ContextCompat.getColor(this, R.color.colorExpense));
            } else {
                binding.tvAmount.setText("+" + formattedAmount);
                binding.tvAmount.setTextColor(ContextCompat.getColor(this, R.color.colorIncome));
            }

            // 设置分类信息
            categoryRepository.getCategoryById(transaction.getCategoryId(), new RepositoryCallback<Category>() {
                @Override
                public void onSuccess(Category category) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (category != null) {
                                binding.tvCategoryName.setText(category.getName());
                                // 设置分类图标
                                try {
                                    String iconName = category.getIconName();
                                    if (iconName != null && !iconName.isEmpty()) {
                                        int iconResId = getResources().getIdentifier(iconName, "drawable", getPackageName());
                                        // 设置背景颜色
                                        if (category.getColor() != null && !category.getColor().isEmpty()) {
                                            int color = android.graphics.Color.parseColor(category.getColor());
                                            binding.ivCategoryIcon.getBackground().setTint(color);
                                        }
                                        if (iconResId != 0) {
                                            Drawable iconDrawable = ContextCompat.getDrawable(TransactionDetailActivity.this, iconResId);
                                            binding.ivCategoryIcon.setImageDrawable(iconDrawable);
                                        }
                                    }
                                } catch (Exception e) {
                                    LogUtils.e(TAG, "设置分类图标失败：" + e.getMessage(), e);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onError(String error) {
                }
            });

            // 设置日期
            binding.tvDate.setText(transaction.getCreateTime());

            // 设置交易类型
            String typeText = transaction.getType() == Transaction.TYPE_EXPENSE ? "支出" : "收入";
            binding.tvType.setText(typeText);

            // 设置支付方式（如果有）
            String paymentMethod = transaction.getPaymentMethod();
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                binding.tvPayment.setText(paymentMethod);
            } else {
                binding.tvPaymentLabel.setVisibility(View.GONE);
                binding.tvPayment.setVisibility(View.GONE);
            }

            // 获取交易详情中的数据
            String remark = transaction.getRemark(); // 备注字段
            String note = transaction.getNote();     // 交易说明字段
            String description = transaction.getDescription(); // 描述字段

            // 处理描述卡片 - 显示note字段
            if (note != null && !note.isEmpty()) {
                binding.tvDescription.setText(note);
                binding.cardDescription.setVisibility(View.VISIBLE);
            } else if (description != null && !description.isEmpty()) {
                binding.tvDescription.setText(description);
                binding.cardDescription.setVisibility(View.VISIBLE);
            } else {
                binding.tvDescription.setText("无描述");
                binding.cardDescription.setVisibility(View.VISIBLE);
            }

            // 处理备注卡片 - 显示remark字段
            if (remark != null && !remark.isEmpty()) {
                binding.tvNote.setText(remark);
                binding.cardNote.setVisibility(View.VISIBLE);
            } else {
                binding.cardNote.setVisibility(View.GONE);
            }

            // 记录调试日志
            LogUtils.d(TAG, "交易数据: note=" + note + ", remark=" + remark + ", description=" + description);

            // 设置地点（如果有）
            String location = transaction.getLocation();
            if (location != null && !location.isEmpty()) {
                binding.tvLocation.setText(location);
                binding.tvLocationLabel.setVisibility(View.VISIBLE);
                binding.tvLocation.setVisibility(View.VISIBLE);
            } else {
                binding.tvLocationLabel.setVisibility(View.GONE);
                binding.tvLocation.setVisibility(View.GONE);
            }

            // 设置创建时间（如果有）
            String createTime = transaction.getCreateTime();
            if (createTime != null && !createTime.isEmpty()) {
                binding.tvCreateTime.setText(createTime);
                binding.tvCreateTimeLabel.setVisibility(View.VISIBLE);
                binding.tvCreateTime.setVisibility(View.VISIBLE);
            } else {
                binding.tvCreateTimeLabel.setVisibility(View.GONE);
                binding.tvCreateTime.setVisibility(View.GONE);
            }

            // 设置创建者（如果有）
            String createBy = transaction.getCreateBy();
            if (createBy != null && !createBy.isEmpty()) {
                binding.tvCreateBy.setText(createBy);
                binding.tvCreateByLabel.setVisibility(View.VISIBLE);
                binding.tvCreateBy.setVisibility(View.VISIBLE);
            } else {
                binding.tvCreateByLabel.setVisibility(View.GONE);
                binding.tvCreateBy.setVisibility(View.GONE);
            }

            // 设置同步状态（如果有）
            int syncState = transaction.getSyncState();
            String syncStateText;
            switch (syncState) {
                case 0:
                    syncStateText = "未同步";
                    break;
                case 1:
                    syncStateText = "已同步";
                    break;
                case 2:
                    syncStateText = "同步失败";
                    break;
                default:
                    syncStateText = "未知状态: " + syncState;
                    break;
            }
            binding.tvSyncState.setText(syncStateText);

            // 设置图片（如果有）
            String imagePath = transaction.getImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                binding.cardImage.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(imagePath)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_error)
                        .into(binding.ivReceipt);
            } else {
                binding.cardImage.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            LogUtils.e(TAG, "更新UI异常：" + e.getMessage(), e);
        }
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除交易")
                .setMessage("确定要删除这笔交易吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> deleteTransaction())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除交易
     */
    private void deleteTransaction() {
        try {
            transactionRepository.deleteTransaction(transactionId, new RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    runOnUiThread(() -> {
                        Toast.makeText(TransactionDetailActivity.this, "交易已删除", Toast.LENGTH_SHORT).show();
                        // 设置结果码为RESULT_OK，通知MainActivity刷新数据
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    LogUtils.e(TAG, "删除交易失败：" + error);
                    runOnUiThread(() -> {
                        Toast.makeText(TransactionDetailActivity.this, "删除交易失败", Toast.LENGTH_SHORT).show();
                        // 即使删除失败，也设置结果为RESULT_CANCELED，表示操作未完成
                        setResult(RESULT_CANCELED);
                    });
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "删除交易异常：" + e.getMessage(), e);
            Toast.makeText(this, "删除交易失败", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 只有当交易对象为null时才重新加载数据，避免重复加载
        if (transactionId != -1 && transaction == null) {
            loadTransactionData();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EDIT_TRANSACTION && resultCode == RESULT_OK) {
            // 交易编辑成功，重新加载数据
            loadTransactionData();

            // 同时设置结果码，通知MainActivity刷新首页数据
            setResult(RESULT_OK);
            LogUtils.d(TAG, "编辑交易成功，设置结果码RESULT_OK以通知MainActivity刷新数据");
        }
    }
} 