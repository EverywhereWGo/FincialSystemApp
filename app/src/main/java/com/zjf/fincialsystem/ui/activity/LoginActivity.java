package com.zjf.fincialsystem.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityLoginBinding;
import com.zjf.fincialsystem.db.DatabaseManager;
import com.zjf.fincialsystem.model.LoginHistory;
import com.zjf.fincialsystem.model.User;
import com.zjf.fincialsystem.network.model.LoginRequest;
import com.zjf.fincialsystem.network.model.LoginResponse;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.repository.UserRepository;
import com.zjf.fincialsystem.utils.DeviceUtils;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.SharedPreferencesUtils;
import com.zjf.fincialsystem.utils.Constants;
import com.zjf.fincialsystem.utils.TokenManager;
import com.zjf.fincialsystem.network.NetworkManager;

import java.util.Date;
import java.util.UUID;

/**
 * 登录页
 * 用户登录界面，包含用户名/密码登录、忘记密码、注册账号等功能
 */
public class LoginActivity extends AppCompatActivity {
    
    private static final String TAG = "LoginActivity";
    
    private ActivityLoginBinding binding;
    private UserRepository userRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
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
        
        // 登录按钮点击事件
        binding.btnLogin.setOnClickListener(v -> {
            // 获取用户名和密码
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            
            // 验证用户名和密码
            if (TextUtils.isEmpty(username)) {
                binding.etUsername.setError(getString(R.string.field_required));
                return;
            }
            
            if (TextUtils.isEmpty(password)) {
                binding.etPassword.setError(getString(R.string.field_required));
                return;
            }
            
            // 显示加载对话框
            showLoading(true);
            
            // 输出登录信息
            LogUtils.d(TAG, "准备登录，用户名: " + username);
            
            // 创建登录请求
            LoginRequest loginRequest = createLoginRequest(username, password);
            
            // 调用登录方法
            userRepository.login(loginRequest, new RepositoryCallback<LoginResponse>() {
                @Override
                public void onSuccess(LoginResponse result) {
                    // 隐藏加载对话框
                    showLoading(false);
                    
                    // 处理登录成功
                    handleLoginSuccess(result);
                }
                
                @Override
                public void onError(String error) {
                    // 隐藏加载对话框
                    showLoading(false);
                    
                    // 处理登录失败
                    handleLoginFailure(error);
                }
            });
        });
        
        // 注册按钮点击事件
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
        
        // 忘记密码点击事件
        binding.btnForgetPassword.setOnClickListener(v -> {
            // TODO: 实现忘记密码功能
            Toast.makeText(this, "忘记密码功能待实现", Toast.LENGTH_SHORT).show();
        });

    }
    
    /**
     * 创建登录请求
     */
    private LoginRequest createLoginRequest(String username, String password) {
        // 获取设备信息
        String deviceInfo = DeviceUtils.getDeviceInfo();
        LoginRequest loginRequest = new LoginRequest(username, password, deviceInfo);
        
        LogUtils.d(TAG, "登录请求参数: 用户名=" + username + ", 设备信息=" + deviceInfo);
        
        return loginRequest;
    }
    
    /**
     * 处理登录成功
     * @param loginResponse 登录响应
     */
    private void handleLoginSuccess(LoginResponse loginResponse) {
        LogUtils.d(TAG, "登录成功");
        
        // 保存token信息
        if (loginResponse.getToken() != null) {
            TokenManager.getInstance().saveToken(
                    loginResponse.getToken(),
                    loginResponse.getExpiryTime()
            );
            
            LogUtils.d(TAG, "保存token成功，过期时间: " + new Date(loginResponse.getExpiryTime()));
        }
        
        // 保存用户信息
        User user = loginResponse.getUser();
        if (user != null) {
            // 保存用户信息到本地数据库
            saveUserToLocalDb(user);
            // 保存用户ID到SharedPreferences
            SharedPreferencesUtils.saveUserId(this, user.getId());
            LogUtils.d(TAG, "保存用户信息成功: " + user.toString());
        } else {
            LogUtils.w(TAG, "登录成功但未返回用户信息，将尝试单独获取");
            // TODO: 这里可以添加获取用户信息的逻辑
        }
        
        // 保存登录历史记录
        saveLoginHistory(loginResponse);
        
        // 跳转到主界面
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    
    /**
     * 处理登录失败
     * @param errorMessage 错误信息
     */
    private void handleLoginFailure(String errorMessage) {
        LogUtils.e(TAG, "登录失败: " + errorMessage);
        
        Toast.makeText(this, "登录失败: " + errorMessage, Toast.LENGTH_SHORT).show();
        
        // 清除token和登录状态
        TokenManager.getInstance().clearToken();
    }
    
    /**
     * 将用户信息保存到本地数据库
     * @param user 用户信息
     */
    private void saveUserToLocalDb(User user) {
        try {
            // 获取数据库实例
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // 检查是否已存在该用户
            User existingUser = dbManager.getUserDao().getUserById(user.getId());
            
            if (existingUser != null) {
                // 更新用户信息
                dbManager.getUserDao().updateUser(user);
                LogUtils.d(TAG, "更新本地用户信息");
            } else {
                // 添加新用户
                dbManager.getUserDao().insertUser(user);
                LogUtils.d(TAG, "添加用户到本地数据库");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "保存用户信息到本地数据库失败", e);
        }
    }
    
    /**
     * 保存登录历史记录
     * @param loginResponse 登录响应
     */
    private void saveLoginHistory(LoginResponse loginResponse) {
        try {
            // 构建登录历史记录
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setUserId(loginResponse.getUser() != null ? loginResponse.getUser().getId() : 0);
            
            // 设置登录时间
            if (loginResponse.getLoginTime() > 0) {
                loginHistory.setLoginTime(new Date(loginResponse.getLoginTime()));
            } else if (loginResponse.getUser() != null && loginResponse.getUser().getLastLoginTime() != null) {
                // 尝试从用户信息的最后登录时间获取
                try {
                    Date loginTime = loginResponse.getUser().getLastLoginTimeAsDate();
                    if (loginTime != null) {
                        loginHistory.setLoginTime(loginTime);
                    } else {
                        loginHistory.setLoginTime(new Date()); // 使用当前时间作为备选
                    }
                } catch (Exception e) {
                    loginHistory.setLoginTime(new Date()); // 解析失败使用当前时间
                }
            } else {
                loginHistory.setLoginTime(new Date()); // 使用当前时间作为备选
            }
            
            loginHistory.setIpAddress(loginResponse.getIpAddress());
            loginHistory.setDeviceInfo(loginResponse.getDeviceInfo());
            loginHistory.setSuccess(true);
            
            // 保存到数据库
            DatabaseManager.getInstance().getLoginHistoryDao().insertLoginHistory(loginHistory);
            
            LogUtils.d(TAG, "保存登录历史记录成功");
        } catch (Exception e) {
            LogUtils.e(TAG, "保存登录历史记录失败", e);
        }
    }
    
    /**
     * 显示或隐藏加载对话框
     * @param show 是否显示
     */
    private void showLoading(boolean show) {
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnLogin.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnLogin.setEnabled(true);
        }
    }
} 