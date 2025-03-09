package com.zjf.fincialsystem.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.blankj.utilcode.util.EncryptUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.StringUtils;
import com.zjf.fincialsystem.app.FinanceApplication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyStore;

/**
 * 安全工具类
 * 提供加密解密和安全相关操作
 */
public class SecurityUtils {
    
    private static final String TAG = "SecurityUtils";
    private static final String KEY_DATABASE_KEY = "key_database_key";
    private static final String SALT = "FinanceApp_Salt_2023";
    private static final String PREFS_NAME = "finance_security_prefs";
    
    private static Context context;
    private static KeyStore keyStore;
    
    /**
     * 初始化
     * @param context 上下文
     */
    public static void init(Context context) {
        try {
            SecurityUtils.context = context.getApplicationContext();
            
            // 初始化其他安全相关配置
            LogUtils.i(TAG, "安全工具初始化成功");
        } catch (Exception e) {
            LogUtils.e(TAG, "安全工具初始化失败：" + e.getMessage(), e);
            throw new RuntimeException("安全工具初始化失败", e);
        }
    }
    
    /**
     * 获取数据库加密密钥
     * @return 数据库加密密钥
     */
    public static String getDatabaseKey() {
        Context context = FinanceApplication.getAppContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        String key = prefs.getString(KEY_DATABASE_KEY, null);
        if (key == null) {
            // 首次运行，生成随机密钥
            key = generateRandomKey();
            prefs.edit().putString(KEY_DATABASE_KEY, key).apply();
        }
        
        return key;
    }
    
    /**
     * 生成随机密钥
     * @return 随机密钥
     */
    private static String generateRandomKey() {
        try {
            SecureRandom random = new SecureRandom();
            byte[] keyBytes = new byte[32];
            random.nextBytes(keyBytes);
            return Base64.encodeToString(keyBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            LogUtils.e(TAG, "生成随机密钥失败：" + e.getMessage());
            // 如果生成失败，使用默认密钥
            return "DefaultDatabaseKey123!@#";
        }
    }
    
    /**
     * 哈希密码
     * @param password 密码
     * @return 哈希后的密码
     */
    public static String hashPassword(String password) {
        try {
            // 添加盐值
            String saltedPassword = password + SALT;
            
            // 使用SHA-256哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            LogUtils.e(TAG, "哈希密码失败：" + e.getMessage());
            return password; // 如果哈希失败，返回原始密码
        }
    }
    
    /**
     * 验证密码
     * @param inputPassword 输入的密码
     * @param storedHash 存储的哈希值
     * @return 是否匹配
     */
    public static boolean verifyPassword(String inputPassword, String storedHash) {
        String inputHash = hashPassword(inputPassword);
        return inputHash.equals(storedHash);
    }
    
    /**
     * 检查密码强度
     * 至少8位，包含大小写字母和数字
     * @param password 密码
     * @return 是否符合强度要求
     */
    public static boolean isPasswordStrong(String password) {
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
        return password.matches(pattern);
    }
    
    /**
     * 生成随机验证码
     * @param length 验证码长度
     * @return 随机验证码
     */
    public static String generateVerificationCode(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    /**
     * 生成UUID
     * @return UUID字符串
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * AES加密
     * @param data 待加密数据
     * @param key 密钥
     * @return 加密后的数据
     */
    public static String encryptAES(String data, String key) {
        byte[] keyBytes = key.getBytes();
        byte[] encryptBytes = EncryptUtils.encryptAES2Base64(data.getBytes(), keyBytes, "AES/ECB/PKCS5Padding", null);
        return Base64.encodeToString(encryptBytes, Base64.NO_WRAP);
    }
    
    /**
     * AES解密
     * @param encryptedData 加密数据
     * @param key 密钥
     * @return 解密后的数据
     */
    public static String decryptAES(String encryptedData, String key) {
        try {
            byte[] keyBytes = key.getBytes();
            byte[] decryptBytes = EncryptUtils.decryptBase64AES(Base64.decode(encryptedData, Base64.NO_WRAP), keyBytes, "AES/ECB/PKCS5Padding", null);
            return new String(decryptBytes);
        } catch (Exception e) {
            LogUtils.e("Decrypt error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 掩码显示卡号
     * 例如：1234 5678 9012 3456 -> 1234 **** **** 3456
     * @param cardNumber 卡号
     * @return 掩码后的卡号
     */
    public static String maskCardNumber(String cardNumber) {
        if (StringUtils.isEmpty(cardNumber) || cardNumber.length() < 8) {
            return cardNumber;
        }
        
        return cardNumber.replaceAll("(?<=\\d{4})\\d(?=\\d{4})", "*");
    }
} 