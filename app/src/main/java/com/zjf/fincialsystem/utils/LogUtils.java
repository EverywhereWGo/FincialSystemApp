package com.zjf.fincialsystem.utils;

import android.content.Context;
import android.util.Log;

import com.zjf.fincialsystem.BuildConfig;

/**
 * 日志工具类
 */
public class LogUtils {
    
    private static final String TAG = "FinancialSystem";
    private static boolean isDebug = BuildConfig.DEBUG; // 默认根据BuildConfig决定
    private static Context context;
    
    /**
     * 初始化
     * @param context 上下文
     */
    public static void init(Context context) {
        LogUtils.context = context.getApplicationContext();
        Log.i(TAG, "日志工具初始化成功，当前模式：" + (isDebug ? "Debug" : "Release"));
    }
    
    /**
     * 设置是否为调试模式
     * @param debug 是否调试模式
     */
    public static void setDebug(boolean debug) {
        isDebug = debug;
        Log.i(TAG, "日志级别已设置为: " + (isDebug ? "DEBUG" : "RELEASE"));
    }
    
    /**
     * 调试日志
     * @param msg 日志内容
     */
    public static void d(String msg) {
        d(TAG, msg);
    }
    
    /**
     * 调试日志
     * @param tag 标签
     * @param msg 日志内容
     */
    public static void d(String tag, String msg) {
        if (isDebug) {
            Log.d(tag, msg);
        }
    }
    
    /**
     * 信息日志
     * @param msg 日志内容
     */
    public static void i(String msg) {
        i(TAG, msg);
    }
    
    /**
     * 信息日志
     * @param tag 标签
     * @param msg 日志内容
     */
    public static void i(String tag, String msg) {
        if (isDebug) {
            Log.i(tag, msg);
        }
    }
    
    /**
     * 警告日志
     * @param msg 日志内容
     */
    public static void w(String msg) {
        w(TAG, msg);
    }
    
    /**
     * 警告日志
     * @param tag 标签
     * @param msg 日志内容
     */
    public static void w(String tag, String msg) {
        if (isDebug) {
            Log.w(tag, msg);
        }
    }
    
    /**
     * 错误日志
     * @param msg 日志内容
     */
    public static void e(String msg) {
        e(TAG, msg);
    }
    
    /**
     * 错误日志
     * @param tag 标签
     * @param msg 日志内容
     */
    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }
    
    /**
     * 错误日志
     * @param msg 日志内容
     * @param tr 异常
     */
    public static void e(String msg, Throwable tr) {
        e(TAG, msg, tr);
    }
    
    /**
     * 错误日志
     * @param tag 标签
     * @param msg 日志内容
     * @param tr 异常
     */
    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
    }
} 