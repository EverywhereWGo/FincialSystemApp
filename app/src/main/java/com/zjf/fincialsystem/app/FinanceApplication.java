package com.zjf.fincialsystem.app;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.zjf.fincialsystem.db.DataCacheManager;
import com.zjf.fincialsystem.db.DatabaseManager;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.utils.TokenManager;
import com.zjf.fincialsystem.utils.Constants;
import com.zjf.fincialsystem.utils.SharedPreferencesUtils;

import java.util.Date;

/**
 * 应用全局Application
 */
public class FinanceApplication extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 保存应用上下文
        appContext = getApplicationContext();
        
        // 初始化工具库
        Utils.init(this);
        
        // 初始化日志工具
        initLogger();
        
        // 初始化数据库
        initDatabase();
        
        // 初始化网络
        initNetwork();
        
        // 初始化数据缓存
        initDataCache();
        
        // 确保TokenManager也被初始化
        initTokenManager();
        
        LogUtils.i("FinanceApplication", "应用初始化完成");
    }
    
    /**
     * 获取应用上下文
     * @return 应用上下文
     */
    public static Context getAppContext() {
        return appContext;
    }
    
    /**
     * 初始化日志工具
     */
    private void initLogger() {
        LogUtils.getConfig()
                .setLogSwitch(true)
                .setConsoleSwitch(true)
                .setGlobalTag("FinanceSystem");
    }
    
    /**
     * 初始化数据库
     */
    private void initDatabase() {
        try {
            DatabaseManager.getInstance().init(this);
            LogUtils.d("FinanceApplication", "数据库初始化成功");
        } catch (Exception e) {
            LogUtils.e("FinanceApplication", "数据库初始化失败", e);
        }
    }
    
    /**
     * 初始化网络
     */
    private void initNetwork() {
        try {
            // 初始化网络管理器
            NetworkManager.getInstance().init();
            LogUtils.i("FinanceApplication", "网络管理器初始化成功");
        } catch (Exception e) {
            LogUtils.e("FinanceApplication", "网络管理器初始化失败", e);
        }
    }
    
    /**
     * 初始化数据缓存
     */
    private void initDataCache() {
        try {
            // 获取实例初始化
            DataCacheManager.getInstance(this);
            LogUtils.d("FinanceApplication", "数据缓存管理器初始化成功");
        } catch (Exception e) {
            LogUtils.e("FinanceApplication", "数据缓存管理器初始化失败", e);
        }
    }
    
    private void initTokenManager() {
        try {
            // 获取已保存的token
            String token = SharedPreferencesUtils.getStringPreference(
                    appContext, Constants.PREF_NAME, Constants.PREF_KEY_TOKEN, "");
            
            if (!TextUtils.isEmpty(token)) {
                long expiryTime = SharedPreferencesUtils.getLongPreference(
                        appContext, Constants.PREF_NAME, Constants.PREF_KEY_TOKEN_EXPIRY, 0);
                
                // 检查token是否过期
                if (expiryTime > System.currentTimeMillis()) {
                    // 设置token到TokenManager
                    TokenManager.getInstance().setToken(token);
                    TokenManager.getInstance().setExpiryTime(expiryTime);
                    
                    LogUtils.i("FinanceApplication", "从存储恢复Token成功，过期时间: " + new Date(expiryTime));
                } else {
                    LogUtils.w("FinanceApplication", "存储的Token已过期，过期时间: " + new Date(expiryTime));
                    // 清除过期token
                    SharedPreferencesUtils.removePreference(appContext, Constants.PREF_NAME, Constants.PREF_KEY_TOKEN);
                    SharedPreferencesUtils.removePreference(appContext, Constants.PREF_NAME, Constants.PREF_KEY_TOKEN_EXPIRY);
                }
            } else {
                LogUtils.d("FinanceApplication", "未找到存储的Token");
            }
        } catch (Exception e) {
            LogUtils.e("FinanceApplication", "初始化TokenManager失败: " + e.getMessage(), e);
        }
    }
} 