package com.zjf.fincialsystem.application;

import android.app.Application;
import android.content.Context;

import com.zjf.fincialsystem.utils.LogUtils;

/**
 * 应用全局Application类
 */
public class FinancialApplication extends Application {

    private static Context appContext;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 保存应用上下文
        appContext = getApplicationContext();
        
        // 初始化日志工具
        LogUtils.init(this);
        LogUtils.setDebug(true); // 强制设置为调试模式
        LogUtils.d("FinancialApplication", "应用启动");
        
        // ... 其他初始化代码 ...
    }
    
    /**
     * 获取应用上下文
     * @return 应用上下文
     */
    public static Context getAppContext() {
        return appContext;
    }
} 