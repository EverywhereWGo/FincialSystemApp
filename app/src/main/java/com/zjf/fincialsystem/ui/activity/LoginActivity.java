package com.zjf.fincialsystem.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityLoginBinding;
import com.zjf.fincialsystem.db.DatabaseManager;
import com.zjf.fincialsystem.model.LoginHistory;
import com.zjf.fincialsystem.model.User;
import com.zjf.fincialsystem.network.model.LoginResponse;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.repository.UserRepository;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.SharedPreferencesUtils;
import com.zjf.fincialsystem.utils.Constants;
import com.zjf.fincialsystem.utils.TokenManager;

import java.util.Date;

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
        binding.btnLogin.setOnClickListener(v -> login());
        
        // 注册按钮点击事件
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
        
        // 忘记密码点击事件
        binding.btnForgetPassword.setOnClickListener(v -> {
            // TODO: 实现忘记密码功能
            Toast.makeText(this, "忘记密码功能待实现", Toast.LENGTH_SHORT).show();
        });
        
        // 微信登录点击事件
        binding.btnWechatLogin.setOnClickListener(v -> {
            // TODO: 实现微信登录功能
            Toast.makeText(this, "微信登录功能待实现", Toast.LENGTH_SHORT).show();
        });
        
        // 指纹登录点击事件
        binding.btnFingerprintLogin.setOnClickListener(v -> {
            // TODO: 实现指纹登录功能
            Toast.makeText(this, "指纹登录功能待实现", Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * 登录
     */
    private void login() {
        // 获取输入
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        
        // 验证输入
        if (TextUtils.isEmpty(username)) {
            binding.etUsername.setError(getString(R.string.field_required));
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError(getString(R.string.field_required));
            return;
        }
        
        // 显示加载中
        showLoading(true);
        
        // 使用仓库层登录
        userRepository.login(username, password, new RepositoryCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse loginResponse) {
                runOnUiThread(() -> {
                    // 隐藏加载中
                    showLoading(false);
                    
                    // 处理登录成功
                    handleLoginSuccess(loginResponse);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // 隐藏加载中
                    showLoading(false);
                    
                    // 处理登录失败
                    handleLoginFailure(error);
                });
            }
        });
    }
    
    /**
     * 处理登录成功
     * @param loginResponse 登录响应数据
     */
    private void handleLoginSuccess(LoginResponse loginResponse) {
        try {
            LogUtils.d(TAG, "登录成功，获取到Token");
            
            // 保存token到TokenManager
            TokenManager.getInstance().saveToken(loginResponse.getToken(), loginResponse.getExpiryTime());
            
            // 额外保存到SharedPreferences，确保多重保障
            SharedPreferencesUtils.setStringPreference(this, 
                    Constants.PREF_NAME, 
                    Constants.PREF_KEY_TOKEN, 
                    loginResponse.getToken());
            
            SharedPreferencesUtils.setLongPreference(this,
                    Constants.PREF_NAME,
                    Constants.PREF_KEY_TOKEN_EXPIRY,
                    loginResponse.getExpiryTime());
            
            // 保存用户数据到本地数据库
            if (loginResponse.getUser() != null) {
                saveUserToLocalDb(loginResponse.getUser());
                LogUtils.d(TAG, "更新用户信息成功");
            }
            
            // 保存登录历史
            saveLoginHistory(loginResponse);
            LogUtils.d(TAG, "记录登录历史成功");
            
            // 跳转到主页
            LogUtils.d(TAG, "跳转到主页");
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } catch (Exception e) {
            LogUtils.e(TAG, "处理登录成功时发生错误", e);
            handleLoginFailure("登录处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理登录失败
     * @param errorMessage 错误信息
     */
    private void handleLoginFailure(String errorMessage) {
        LogUtils.d(TAG, "登录失败: " + errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 将用户信息保存到本地数据库
     * @param user 用户信息
     */
    private void saveUserToLocalDb(User user) {
        try {
            // 获取数据库管理器
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // 尝试获取用户
            User existingUser = dbManager.getUserDao().queryByUsername(user.getUsername());
            
            if (existingUser != null) {
                // 用户已存在，更新
                existingUser.setLastLoginTime(new Date());
                existingUser.resetFailedAttempts();
                dbManager.getUserDao().update(existingUser);
                LogUtils.d(TAG, "更新用户信息成功");
            } else {
                // 用户不存在，插入
                dbManager.getUserDao().insert(user);
                LogUtils.d(TAG, "保存用户信息成功");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "保存用户信息到本地数据库失败", e);
        }
    }
    
    /**
     * 记录登录历史
     * @param loginResponse 登录响应
     */
    private void saveLoginHistory(LoginResponse loginResponse) {
        try {
            // 创建登录历史记录
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setUserId(loginResponse.getUser().getId());
            loginHistory.setLoginTime(new Date(loginResponse.getLoginTime()));
            loginHistory.setIpAddress(loginResponse.getIpAddress());
            loginHistory.setDeviceInfo(loginResponse.getDeviceInfo());
            loginHistory.setSuccess(true);
            
            // 保存登录历史
            DatabaseManager.getInstance().getLoginHistoryDao().insert(loginHistory);
            LogUtils.d(TAG, "记录登录历史成功");
        } catch (Exception e) {
            LogUtils.e(TAG, "记录登录历史失败", e);
        }
    }
    
    /**
     * 显示/隐藏加载中
     * @param show 是否显示
     */
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
    }
} 