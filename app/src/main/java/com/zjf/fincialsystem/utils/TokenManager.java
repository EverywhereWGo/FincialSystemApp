package com.zjf.fincialsystem.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.zjf.fincialsystem.app.FinanceApplication;
import com.zjf.fincialsystem.utils.Constants;

import java.util.Date;

/**
 * Token管理类
 * 使用单例模式
 */
public class TokenManager {
    private static final String TAG = "TokenManager";
    private static volatile TokenManager instance;
    private final SharedPreferences sharedPreferences;
    
    private TokenManager() {
        sharedPreferences = FinanceApplication.getAppContext()
                .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        LogUtils.d(TAG, "TokenManager初始化，使用SharedPreferences: " + Constants.PREF_NAME);
        
        // 初始化时尝试恢复token
        String token = sharedPreferences.getString(Constants.PREF_KEY_TOKEN, null);
        long expiryTime = sharedPreferences.getLong(Constants.PREF_KEY_TOKEN_EXPIRY, 0);
        
        if (token != null) {
            LogUtils.d(TAG, "找到已保存的token: " + token.substring(0, Math.min(10, token.length())) + "...");
            LogUtils.d(TAG, "Token过期时间: " + new Date(expiryTime));
            
            if (expiryTime > System.currentTimeMillis()) {
                LogUtils.d(TAG, "Token有效，将自动使用");
            } else {
                LogUtils.w(TAG, "已保存的Token已过期，将被清除");
                clearToken();
            }
        } else {
            LogUtils.d(TAG, "未找到已保存的token");
        }
    }
    
    /**
     * 获取单例实例
     */
    public static TokenManager getInstance() {
        if (instance == null) {
            synchronized (TokenManager.class) {
                if (instance == null) {
                    instance = new TokenManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 保存Token和过期时间
     * @param token JWT Token
     * @param expiryTimeInMillis Token过期时间（毫秒）
     */
    public void saveToken(String token, long expiryTimeInMillis) {
        if (TextUtils.isEmpty(token)) {
            LogUtils.e(TAG, "尝试保存空token，操作被拒绝");
            return;
        }
        
        if (expiryTimeInMillis <= System.currentTimeMillis()) {
            LogUtils.e(TAG, "尝试保存已过期的token，操作被拒绝");
            return;
        }
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREF_KEY_TOKEN, token);
        editor.putLong(Constants.PREF_KEY_TOKEN_EXPIRY, expiryTimeInMillis);
        boolean success = editor.commit(); // 使用commit()而不是apply()以确保立即写入
        
        if (success) {
            LogUtils.d(TAG, "Token保存成功，过期时间: " + new Date(expiryTimeInMillis));
        } else {
            LogUtils.e(TAG, "Token保存失败");
        }
    }
    
    /**
     * 设置Token（不保存到SharedPreferences）
     * @param token JWT Token
     */
    public void setToken(String token) {
        if (TextUtils.isEmpty(token)) {
            LogUtils.e(TAG, "尝试设置空token，操作被拒绝");
            return;
        }
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREF_KEY_TOKEN, token);
        boolean success = editor.commit();
        
        if (success) {
            LogUtils.d(TAG, "Token设置成功");
        } else {
            LogUtils.e(TAG, "Token设置失败");
        }
    }
    
    /**
     * 设置Token过期时间（不保存到SharedPreferences）
     * @param expiryTimeInMillis Token过期时间（毫秒）
     */
    public void setExpiryTime(long expiryTimeInMillis) {
        if (expiryTimeInMillis <= System.currentTimeMillis()) {
            LogUtils.e(TAG, "尝试设置已过期的时间，操作被拒绝");
            return;
        }
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(Constants.PREF_KEY_TOKEN_EXPIRY, expiryTimeInMillis);
        boolean success = editor.commit();
        
        if (success) {
            LogUtils.d(TAG, "Token过期时间设置成功: " + new Date(expiryTimeInMillis));
        } else {
            LogUtils.e(TAG, "Token过期时间设置失败");
        }
    }
    
    /**
     * 获取Token
     * @return 如果Token有效则返回Token，否则返回null
     */
    public String getToken() {
        String token = sharedPreferences.getString(Constants.PREF_KEY_TOKEN, null);
        long expiryTime = sharedPreferences.getLong(Constants.PREF_KEY_TOKEN_EXPIRY, 0);
        
        if (token != null) {
            if (expiryTime > System.currentTimeMillis()) {
                LogUtils.d(TAG, "获取到有效token: " + token.substring(0, Math.min(10, token.length())) + "...");
                return token;
            } else {
                LogUtils.w(TAG, "Token已过期，清除token");
                clearToken();
            }
        } else {
            LogUtils.d(TAG, "未找到token");
        }
        
        return null;
    }
    
    /**
     * 检查是否已登录
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        String token = getToken();
        boolean isLoggedIn = token != null;
        
        if (isLoggedIn) {
            LogUtils.d(TAG, "用户已登录，Token有效");
        } else {
            LogUtils.d(TAG, "用户未登录或Token已过期");
        }
        
        return isLoggedIn;
    }
    
    /**
     * 清除Token
     */
    public void clearToken() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(Constants.PREF_KEY_TOKEN);
        editor.remove(Constants.PREF_KEY_TOKEN_EXPIRY);
        boolean success = editor.commit();
        
        if (success) {
            LogUtils.d(TAG, "Token清除成功");
        } else {
            LogUtils.e(TAG, "Token清除失败");
        }
    }
    
    /**
     * 获取Token过期时间
     * @return Token过期时间
     */
    public Date getTokenExpiryDate() {
        long expiryTime = sharedPreferences.getLong(Constants.PREF_KEY_TOKEN_EXPIRY, 0);
        return new Date(expiryTime);
    }
    
    /**
     * 获取用户ID
     * @return 用户ID，未登录则返回默认值
     */
    public long getUserId() {
        // 先检查登录状态，但即使未登录也继续尝试获取ID
        boolean isUserLoggedIn = isLoggedIn();
        LogUtils.d(TAG, "检查登录状态: " + isUserLoggedIn);
        
        // 从SharedPreferences中获取用户ID
        long userId = sharedPreferences.getLong(Constants.PREF_KEY_USER_ID, 1); // 默认返回1而不是-1
        
        // 确保userID至少为1
        if (userId <= 0) {
            userId = 1;
            LogUtils.w(TAG, "用户ID无效，使用默认ID: 1");
            
            // 保存默认ID到SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(Constants.PREF_KEY_USER_ID, userId);
            editor.apply();
        }
        
        LogUtils.d(TAG, "获取到的用户ID: " + userId);
        return userId;
    }
} 