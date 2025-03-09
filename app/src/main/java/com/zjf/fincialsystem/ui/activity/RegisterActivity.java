package com.zjf.fincialsystem.ui.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityRegisterBinding;
import com.zjf.fincialsystem.utils.LogUtils;

/**
 * 注册Activity
 */
public class RegisterActivity extends AppCompatActivity {
    
    private static final String TAG = "RegisterActivity";
    private ActivityRegisterBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 初始化视图
        initViews();
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        try {
            // 设置标题栏
            setSupportActionBar(binding.toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.register);
            
            // 设置注册按钮点击事件
            binding.btnRegister.setOnClickListener(v -> register());
            
            // TODO: 初始化其他视图
            
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化视图失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 注册
     */
    private void register() {
        try {
            // 获取用户输入
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
            
            // 验证输入
            if (username.isEmpty()) {
                binding.etUsername.setError(getString(R.string.username_empty));
                return;
            }
            
            if (password.isEmpty()) {
                binding.etPassword.setError(getString(R.string.password_empty));
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                binding.etConfirmPassword.setError(getString(R.string.password_not_match));
                return;
            }
            
            // TODO: 实现注册逻辑
            
            Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            LogUtils.e(TAG, "注册失败：" + e.getMessage(), e);
            Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
        }
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