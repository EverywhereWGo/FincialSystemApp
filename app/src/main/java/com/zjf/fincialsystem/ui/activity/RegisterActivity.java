package com.zjf.fincialsystem.ui.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityRegisterBinding;
import com.zjf.fincialsystem.network.model.RegisterRequest;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.repository.UserRepository;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.ValidationUtils;
import com.zjf.fincialsystem.utils.SecurityUtils;

/**
 * 注册Activity
 */
public class RegisterActivity extends AppCompatActivity {
    
    private static final String TAG = "RegisterActivity";
    private ActivityRegisterBinding binding;
    private UserRepository userRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 初始化仓库
        userRepository = new UserRepository(this);
        
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
            
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化视图失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 注册
     */
    private void register() {
        try {
            // 清除所有错误
            binding.tilUsername.setError(null);
            binding.tilPassword.setError(null);
            binding.tilConfirmPassword.setError(null);
            binding.tilName.setError(null);
            binding.tilEmail.setError(null);
            binding.tilPhone.setError(null);
            
            // 获取用户输入
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String phone = binding.etPhone.getText().toString().trim();
            
            // 验证输入
            if (TextUtils.isEmpty(username)) {
                binding.tilUsername.setError(getString(R.string.username_empty));
                return;
            }
            
            // 验证用户名格式
            if (!ValidationUtils.isValidUsername(username)) {
                binding.tilUsername.setError(getString(R.string.username_format_error));
                return;
            }
            
            if (TextUtils.isEmpty(password)) {
                binding.tilPassword.setError(getString(R.string.password_empty));
                return;
            }
            
            // 验证密码强度
            if (!ValidationUtils.isValidPassword(password)) {
                binding.tilPassword.setError(getString(R.string.password_too_simple));
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                binding.tilConfirmPassword.setError(getString(R.string.password_not_match));
                return;
            }
            
            if (TextUtils.isEmpty(name)) {
                binding.tilName.setError(getString(R.string.field_required));
                return;
            }
            
            // 验证手机号格式
            if (!TextUtils.isEmpty(phone) && !ValidationUtils.isValidPhoneNumber(phone)) {
                binding.tilPhone.setError(getString(R.string.phone_format_error));
                return;
            }
            
            // 验证邮箱格式
            if (!TextUtils.isEmpty(email) && !ValidationUtils.isValidEmail(email)) {
                binding.tilEmail.setError(getString(R.string.email_format_error));
                return;
            }
            
            // 显示加载对话框
            showLoading(true);
            
            // 创建注册请求
            RegisterRequest registerRequest = new RegisterRequest(username, password, name, email, phone);
            
            // 调用注册方法
            userRepository.register(registerRequest, new RepositoryCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    // 隐藏加载对话框
                    showLoading(false);
                    
                    // 处理注册成功
                    Toast.makeText(RegisterActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
                    finish();
                }
                
                @Override
                public void onError(String error) {
                    // 隐藏加载对话框
                    showLoading(false);
                    
                    // 处理注册失败
                    LogUtils.e(TAG, "注册失败：" + error);
                    Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
            
        } catch (Exception e) {
            LogUtils.e(TAG, "注册失败：" + e.getMessage(), e);
            Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
            showLoading(false);
        }
    }
    
    /**
     * 显示或隐藏加载对话框
     * @param show 是否显示
     */
    private void showLoading(boolean show) {
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnRegister.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnRegister.setEnabled(true);
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