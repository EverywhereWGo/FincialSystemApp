package com.zjf.fincialsystem.utils;

/**
 * 应用中使用的常量
 */
public class Constants {
    
    // SharedPreferences配置
    public static final String PREF_NAME = "fincialsystem_prefs";
    
    // Token相关
    public static final String PREF_KEY_TOKEN = "key_token";
    public static final String PREF_KEY_TOKEN_EXPIRY = "key_token_expiry";
    
    // 用户信息相关
    public static final String PREF_KEY_USER_ID = "key_user_id";
    public static final String PREF_KEY_USERNAME = "key_username";
    
    // 主题相关
    public static final String PREF_KEY_THEME = "key_theme";
    
    // 缓存相关
    public static final String PREF_CACHE_DIR = "cache_dir";
    
    // 网络请求相关
    public static final int REQUEST_TIMEOUT = 15; // 15秒
    
    // 定义响应码
    public static final int CODE_SUCCESS = 200;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_SERVER_ERROR = 500;
} 