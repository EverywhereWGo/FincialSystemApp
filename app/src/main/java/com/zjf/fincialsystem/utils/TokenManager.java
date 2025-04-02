package com.zjf.fincialsystem.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.zjf.fincialsystem.app.FinanceApplication;
import com.zjf.fincialsystem.utils.Constants;

import java.util.Date;

/**
 * Token管理类
 * 使用单例模式
 */
public class TokenManager {
    
    private static final String KEY_TOKEN = Constants.PREF_KEY_TOKEN;
    private static final String KEY_TOKEN_EXPIRY = Constants.PREF_KEY_TOKEN_EXPIRY;
    
    private static volatile TokenManager instance;
    private final SharedPreferences sharedPreferences;
    
    private TokenManager() {
        // 使用Constants中定义的PREF_NAME
        sharedPreferences = FinanceApplication.getAppContext()
                .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        LogUtils.d("TokenManager", "初始化TokenManager, 使用SharedPreferences: " + Constants.PREF_NAME);
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
     * 保存Token
     * @param token JWT Token
     * @param expiryTimeInMillis Token过期时间（毫秒）
     */
    public void saveToken(String token, long expiryTimeInMillis) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putLong(KEY_TOKEN_EXPIRY, expiryTimeInMillis);
        editor.apply();
    }
    
    /**
     * 设置Token（不保存到SharedPreferences）
     * @param token JWT Token
     */
    public void setToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }
    
    /**
     * 设置Token过期时间（不保存到SharedPreferences）
     * @param expiryTimeInMillis Token过期时间（毫秒）
     */
    public void setExpiryTime(long expiryTimeInMillis) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_TOKEN_EXPIRY, expiryTimeInMillis);
        editor.apply();
    }
    
    /**
     * 获取Token
     * @return 如果Token有效则返回Token，否则返回null
     */
    public String getToken() {
        String token = sharedPreferences.getString(KEY_TOKEN, null);
        long expiryTime = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);
        
        // 检查Token是否过期
        if (token != null && expiryTime > System.currentTimeMillis()) {
            return token;
        } else {
            // Token已过期，清除
            clearToken();
            return null;
        }
    }
    
    /**
     * 清除Token
     */
    public void clearToken() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_TOKEN_EXPIRY);
        editor.apply();
    }
    
    /**
     * 检查是否已登录
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        String token = getToken();
        boolean isLoggedIn = token != null;
        
        // 输出调试信息
        if (isLoggedIn) {
            LogUtils.d("TokenManager", "用户已登录，Token: " + token);
            LogUtils.d("TokenManager", "Token过期时间: " + getTokenExpiryDate());
        } else {
            LogUtils.d("TokenManager", "用户未登录，尝试从SharedPreferences直接获取");
            
            // 再次尝试从SharedPreferences获取
            String tokenFromPrefs = sharedPreferences.getString(KEY_TOKEN, null);
            if (tokenFromPrefs != null) {
                long expiryTime = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);
                if (expiryTime > System.currentTimeMillis()) {
                    LogUtils.d("TokenManager", "从SharedPreferences找到有效Token");
                    // 保存到TokenManager实例中
                    saveToken(tokenFromPrefs, expiryTime);
                    return true;
                } else {
                    LogUtils.d("TokenManager", "SharedPreferences中的Token已过期");
                }
            } else {
                LogUtils.d("TokenManager", "SharedPreferences中未找到Token");
            }
        }
        
        return isLoggedIn;
    }
    
    /**
     * 获取Token过期时间
     * @return Token过期时间
     */
    public Date getTokenExpiryDate() {
        long expiryTime = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);
        return new Date(expiryTime);
    }
    
    /**
     * 获取用户ID
     * @return 用户ID，未登录则返回默认值
     */
    public long getUserId() {
        // 先检查登录状态，但即使未登录也继续尝试获取ID
        boolean isUserLoggedIn = isLoggedIn();
        LogUtils.d("TokenManager", "检查登录状态: " + isUserLoggedIn);
        
        // 从SharedPreferences中获取用户ID
        long userId = sharedPreferences.getLong(Constants.PREF_KEY_USER_ID, 1); // 默认返回1而不是-1
        LogUtils.d("TokenManager", "获取到的用户ID: " + userId);
        
        return userId;
    }
} 