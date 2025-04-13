package com.zjf.fincialsystem.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences工具类
 * 封装SharedPreferences操作
 */
public class SharedPreferencesUtils {
    
    /**
     * 获取SharedPreferences实例
     */
    private static SharedPreferences getSharedPreferences(Context context, String name) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }
    
    /**
     * 保存字符串到SharedPreferences
     */
    public static void setStringPreference(Context context, String prefsName, String key, String value) {
        SharedPreferences.Editor editor = getSharedPreferences(context, prefsName).edit();
        editor.putString(key, value);
        editor.apply();
    }
    
    /**
     * 从SharedPreferences获取字符串
     */
    public static String getStringPreference(Context context, String prefsName, String key, String defaultValue) {
        return getSharedPreferences(context, prefsName).getString(key, defaultValue);
    }
    
    /**
     * 保存布尔值到SharedPreferences
     */
    public static void setBooleanPreference(Context context, String prefsName, String key, boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences(context, prefsName).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
    
    /**
     * 从SharedPreferences获取布尔值
     */
    public static boolean getBooleanPreference(Context context, String prefsName, String key, boolean defaultValue) {
        return getSharedPreferences(context, prefsName).getBoolean(key, defaultValue);
    }
    
    /**
     * 保存整数到SharedPreferences
     */
    public static void setIntPreference(Context context, String prefsName, String key, int value) {
        SharedPreferences.Editor editor = getSharedPreferences(context, prefsName).edit();
        editor.putInt(key, value);
        editor.apply();
    }
    
    /**
     * 从SharedPreferences获取整数
     */
    public static int getIntPreference(Context context, String prefsName, String key, int defaultValue) {
        return getSharedPreferences(context, prefsName).getInt(key, defaultValue);
    }
    
    /**
     * 保存长整数到SharedPreferences
     */
    public static void setLongPreference(Context context, String prefsName, String key, long value) {
        SharedPreferences.Editor editor = getSharedPreferences(context, prefsName).edit();
        editor.putLong(key, value);
        editor.apply();
    }
    
    /**
     * 从SharedPreferences获取长整数
     */
    public static long getLongPreference(Context context, String prefsName, String key, long defaultValue) {
        return getSharedPreferences(context, prefsName).getLong(key, defaultValue);
    }
    
    /**
     * 保存浮点数到SharedPreferences
     */
    public static void setFloatPreference(Context context, String prefsName, String key, float value) {
        SharedPreferences.Editor editor = getSharedPreferences(context, prefsName).edit();
        editor.putFloat(key, value);
        editor.apply();
    }
    
    /**
     * 从SharedPreferences获取浮点数
     */
    public static float getFloatPreference(Context context, String prefsName, String key, float defaultValue) {
        return getSharedPreferences(context, prefsName).getFloat(key, defaultValue);
    }
    
    /**
     * 移除指定的SharedPreferences键值对
     */
    public static void removePreference(Context context, String prefsName, String key) {
        SharedPreferences.Editor editor = getSharedPreferences(context, prefsName).edit();
        editor.remove(key);
        editor.apply();
    }
    
    /**
     * 清除指定的SharedPreferences
     */
    public static void clearPreferences(Context context, String prefsName) {
        SharedPreferences.Editor editor = getSharedPreferences(context, prefsName).edit();
        editor.clear();
        editor.apply();
    }
    
    /**
     * 检查SharedPreferences是否包含指定的键
     */
    public static boolean containsKey(Context context, String prefsName, String key) {
        return getSharedPreferences(context, prefsName).contains(key);
    }
    
    /**
     * 保存用户ID到SharedPreferences
     * @param context 上下文
     * @param userId 用户ID
     */
    public static void saveUserId(Context context, long userId) {
        setLongPreference(context, Constants.PREF_NAME, Constants.PREF_KEY_USER_ID, userId);
    }
    
    /**
     * 获取用户ID
     * @param context 上下文
     * @return 用户ID，默认返回-1
     */
    public static long getUserId(Context context) {
        return getLongPreference(context, Constants.PREF_NAME, Constants.PREF_KEY_USER_ID, -1);
    }
} 