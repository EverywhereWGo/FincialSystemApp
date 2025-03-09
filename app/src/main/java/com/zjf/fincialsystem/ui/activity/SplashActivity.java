package com.zjf.fincialsystem.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.repository.UserRepository;
import com.zjf.fincialsystem.utils.LogUtils;

/**
 * 闪屏页
 * 显示应用启动画面，并根据登录状态跳转到相应页面
 */
public class SplashActivity extends AppCompatActivity {
    
    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DELAY = 1500; // 闪屏页显示时间，单位毫秒
    private UserRepository userRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置全屏沉浸式
        setupFullscreen();
        
        setContentView(R.layout.activity_splash);
        
        // 初始化用户仓库
        userRepository = new UserRepository(this);
        
        // 使用Handler延迟跳转
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, SPLASH_DELAY);
    }
    
    /**
     * 设置全屏沉浸式
     */
    private void setupFullscreen() {
        try {
            // 隐藏ActionBar
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
            
            // 使用androidx兼容库的方式实现透明沉浸式状态栏
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            
            // 设置状态栏为透明
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            
            // 设置导航栏为透明
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            
            // 确保内容可以延伸到状态栏和导航栏
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            // 隐藏系统栏，但允许滑动手势显示
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // 支持刘海屏
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                getWindow().setAttributes(layoutParams);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "设置全屏沉浸式失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据登录状态跳转到相应页面
     */
    private void navigateToNextScreen() {
        // 检查用户是否已登录
        if (userRepository.isLoggedIn()) {
            // 已登录，跳转到主页
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // 未登录，跳转到登录页
            startActivity(new Intent(this, LoginActivity.class));
        }
        
        // 结束当前Activity
        finish();
    }
} 