package com.zjf.fincialsystem.utils;

import android.text.TextUtils;
import java.util.regex.Pattern;

/**
 * 验证工具类
 * 提供各种格式验证方法
 */
public class ValidationUtils {
    
    private static final String TAG = "ValidationUtils";
    
    /**
     * 验证邮箱格式
     * @param email 邮箱地址
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }
        
        // 基本的邮箱格式验证
        String emailPattern = 
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" +
            "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        
        return Pattern.compile(emailPattern).matcher(email).matches();
    }
    
    /**
     * 验证手机号格式
     * 支持中国大陆手机号（11位数字，以1开头）
     * @param phoneNumber 手机号码
     * @return 是否有效
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }
        
        // 中国大陆手机号格式验证（11位数字，以1开头）
        String phonePattern = "^1[3-9]\\d{9}$";
        
        return Pattern.compile(phonePattern).matcher(phoneNumber).matches();
    }
    
    /**
     * 验证用户名格式
     * 5-20位字母、数字、下划线组合，不能纯数字
     * @param username 用户名
     * @return 是否有效
     */
    public static boolean isValidUsername(String username) {
        if (TextUtils.isEmpty(username)) {
            return false;
        }
        
        // 5-20位字母、数字、下划线组合
        String usernamePattern = "^[a-zA-Z0-9_]{5,20}$";
        
        // 不能纯数字
        String pureNumberPattern = "^[0-9]+$";
        
        return Pattern.compile(usernamePattern).matcher(username).matches() && 
               !Pattern.compile(pureNumberPattern).matcher(username).matches();
    }
    
    /**
     * 验证密码格式
     * 至少8位，包含大小写字母和数字
     * @param password 密码
     * @return 是否有效
     */
    public static boolean isValidPassword(String password) {
        // 使用已有的安全工具类中的密码强度检查
        return SecurityUtils.isPasswordStrong(password);
    }
} 