package com.zjf.fincialsystem.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityMainBinding;
import com.zjf.fincialsystem.ui.fragment.BudgetFragment;
import com.zjf.fincialsystem.ui.fragment.DashboardFragment;
import com.zjf.fincialsystem.ui.fragment.NotificationFragment;
import com.zjf.fincialsystem.ui.fragment.ProfileFragment;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.utils.SharedPreferencesUtils;
import com.zjf.fincialsystem.utils.Constants;
import com.zjf.fincialsystem.utils.TokenManager;

import java.util.Date;

/**
 * 主页
 * 应用的主界面，包含底部导航栏和各个功能模块
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    
    // 结果码
    public static final int REQUEST_ADD_TRANSACTION = 1001;
    public static final int REQUEST_TRANSACTION_DETAIL = 1002;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置布局
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 设置状态栏
        setupStatusBar();
        
        // 确保网络管理器已初始化
        ensureNetworkManagerInitialized();
        
        // 检查用户登录状态
        if (!TokenManager.getInstance().isLoggedIn()) {
            LogUtils.e("MainActivity", "用户未登录，跳转到登录页面");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // 初始化视图
        initViews();
        
        // 默认显示首页
        switchFragment(new DashboardFragment());
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
        }
    }
    
    /**
     * 确保NetworkManager已初始化
     */
    public void ensureNetworkManagerInitialized() {
        try {
            if (NetworkManager.getInstance().getRetrofit() == null) {
                LogUtils.d("MainActivity", "NetworkManager未初始化，正在初始化...");
                NetworkManager.getInstance().init();
            }
            
            // 检查用户登录状态
            if (!TokenManager.getInstance().isLoggedIn()) {
                LogUtils.e("MainActivity", "用户未登录，尝试重新获取登录状态");
                
                // 从SharedPreferences恢复登录信息
                String token = SharedPreferencesUtils.getStringPreference(this, 
                        Constants.PREF_NAME, Constants.PREF_KEY_TOKEN, "");
                
                if (!TextUtils.isEmpty(token)) {
                    LogUtils.d("MainActivity", "找到存储的token: " + token.substring(0, Math.min(8, token.length())) + "...");
                    
                    // 获取过期时间
                    long expiryTime = SharedPreferencesUtils.getLongPreference(this,
                            Constants.PREF_NAME, Constants.PREF_KEY_TOKEN_EXPIRY, 0);
                    
                    if (expiryTime > System.currentTimeMillis()) {
                        LogUtils.d("MainActivity", "Token未过期，过期时间: " + new Date(expiryTime));
                        
                        // 重新设置token
                        TokenManager.getInstance().setToken(token);
                        TokenManager.getInstance().setExpiryTime(expiryTime);

                        // 确保用户ID被设置
                        long userId = SharedPreferencesUtils.getLongPreference(this,
                                Constants.PREF_NAME, Constants.PREF_KEY_USER_ID, 1);
                        // 如果没有找到用户ID，设置默认值1，避免-1导致的自动退出登录
                        if (userId <= 0) {
                            userId = 1;
                            // 保存这个默认值
                            SharedPreferencesUtils.setLongPreference(this,
                                Constants.PREF_NAME, Constants.PREF_KEY_USER_ID, userId);
                        }
                        LogUtils.d("MainActivity", "用户ID: " + userId);
                        
                        LogUtils.d("MainActivity", "Token已恢复，状态: " + TokenManager.getInstance().isLoggedIn());
                    } else {
                        LogUtils.e("MainActivity", "Token已过期，过期时间: " + new Date(expiryTime));
                    }
                } else {
                    LogUtils.e("MainActivity", "未找到有效的token信息");
                }
            } else {
                LogUtils.i("MainActivity", "NetworkManager已就绪，用户已登录");
            }
        } catch (Exception e) {
            LogUtils.e("MainActivity", "确认NetworkManager初始化时发生错误", e);
        }
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        // 设置底部导航栏
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                switchFragment(new DashboardFragment());
                return true;
            } else if (itemId == R.id.nav_notification) {
                switchFragment(new NotificationFragment());
                return true;
            } else if (itemId == R.id.nav_budget) {
                switchFragment(new BudgetFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                switchFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
        
        // 设置悬浮按钮
        binding.fab.setOnClickListener(v -> {
            // 跳转到记账页面
            Intent intent = new Intent(this, AddTransactionActivity.class);
            startActivityForResult(intent, REQUEST_ADD_TRANSACTION);
        });
        
        // 修正FAB位置（如果使用ConstraintLayout）
        if (binding.bottomAppBar != null) {
            binding.fab.setTranslationY(-getResources().getDimensionPixelSize(R.dimen.fab_offset));
        }
    }
    
    /**
     * 切换Fragment
     * @param fragment 要显示的Fragment
     */
    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        LogUtils.d(TAG, "收到活动结果 - requestCode: " + requestCode + ", resultCode: " + resultCode);
        
        // 只有当结果为RESULT_OK时才刷新数据
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ADD_TRANSACTION || requestCode == REQUEST_TRANSACTION_DETAIL) {
                // 刷新DashboardFragment
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof DashboardFragment) {
                    LogUtils.d(TAG, "正在刷新DashboardFragment数据");
                    DashboardFragment dashboardFragment = (DashboardFragment) currentFragment;
                    // 通知DashboardFragment刷新数据
                    dashboardFragment.refreshData();
                }
            }
        } else {
            LogUtils.d(TAG, "操作取消或失败，不刷新数据");
        }
    }
} 