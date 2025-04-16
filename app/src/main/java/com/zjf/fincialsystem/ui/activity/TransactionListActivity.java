package com.zjf.fincialsystem.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityTransactionListBinding;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.repository.TransactionRepository;
import com.zjf.fincialsystem.ui.adapter.TransactionAdapter;
import com.zjf.fincialsystem.ui.activity.TransactionDetailActivity;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.StatusBarUtils;

import java.util.List;

/**
 * 交易记录列表页面
 */
public class TransactionListActivity extends AppCompatActivity {
    
    private static final String TAG = "TransactionListActivity";
    private ActivityTransactionListBinding binding;
    private TransactionAdapter adapter;
    private androidx.appcompat.widget.Toolbar toolbar;
    private TransactionRepository transactionRepository;
    private boolean isDataFromCache = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置沉浸式状态栏
        setupStatusBar();
        
        binding = ActivityTransactionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 初始化仓库
        transactionRepository = new TransactionRepository(this);
        
        initViews();
        loadData();
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
        // 设置状态栏占位视图高度
        View statusBarPlaceholder = binding.statusBarPlaceholder;
        if (statusBarPlaceholder != null) {
            ViewGroup.LayoutParams layoutParams = statusBarPlaceholder.getLayoutParams();
            layoutParams.height = StatusBarUtils.getStatusBarHeight(this);
            statusBarPlaceholder.setLayoutParams(layoutParams);
        }
        
        // 设置标题栏（修改实现，避免与应用主题ActionBar冲突）
        toolbar = binding.toolbar;
        toolbar.setTitle(R.string.transactions);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // 设置RecyclerView
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this);
        binding.rvTransactions.setAdapter(adapter);
        
        // 设置点击事件
        adapter.setOnItemClickListener(transaction -> {
            try {
                // 跳转到交易详情页
                Intent intent = TransactionDetailActivity.createIntent(this, transaction.getId());
                startActivity(intent);
            } catch (Exception e) {
                LogUtils.e(TAG, "跳转到交易详情页失败：" + e.getMessage(), e);
                Toast.makeText(this, R.string.operation_failed, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置添加按钮点击事件
        binding.fabAdd.setOnClickListener(v -> {
            // 跳转到添加交易页面
            startActivity(new Intent(this, AddTransactionActivity.class));
        });
    }
    
    /**
     * 加载数据
     */
    private void loadData() {
        showLoading(true);
        showError(false);
        showEmptyView(false);
        
        // 获取交易记录列表
        transactionRepository.getTransactions(new RepositoryCallback<List<Transaction>>() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                runOnUiThread(() -> {
                    // 更新UI
                    adapter.setData(transactions);
                    
                    // 检查是否有数据
                    if (transactions == null || transactions.isEmpty()) {
                        showEmptyView(true);
                    } else {
                        showEmptyView(false);
                    }
                    
                    // 显示是否来自缓存的提示
                    if (isDataFromCache) {
                        Toast.makeText(TransactionListActivity.this, 
                                "使用缓存数据 - 网络连接不可用", Toast.LENGTH_SHORT).show();
                    }
                    
                    // 隐藏加载中
                    showLoading(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    LogUtils.e(TAG, "加载交易记录失败：" + error);
                    showError(true);
                    showLoading(false);
                    
                    // 显示错误提示
                    binding.tvErrorMessage.setText(error);
                });
            }
            
            @Override
            public void isCacheData(boolean isCache) {
                isDataFromCache = isCache;
            }
        });
    }
    
    /**
     * 显示加载中视图
     */
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    /**
     * 显示空视图
     */
    private void showEmptyView(boolean show) {
        binding.emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.rvTransactions.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    /**
     * 显示错误视图
     */
    private void showError(boolean show) {
        binding.errorView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            binding.rvTransactions.setVisibility(View.GONE);
        }
    }
    
    /**
     * 删除交易记录
     */
    private void deleteTransaction(Transaction transaction) {
        transactionRepository.deleteTransaction(transaction.getId(), new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(TransactionListActivity.this, 
                            R.string.transaction_deleted, Toast.LENGTH_SHORT).show();
                    // 重新加载数据
                    loadData();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(TransactionListActivity.this, 
                            error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 在页面恢复时刷新数据，以便显示最新的交易记录
        loadData();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 