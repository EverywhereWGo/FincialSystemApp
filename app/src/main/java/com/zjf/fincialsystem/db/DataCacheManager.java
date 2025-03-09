package com.zjf.fincialsystem.db;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zjf.fincialsystem.model.Budget;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.utils.LogUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据缓存管理器
 * 用于管理网络数据的本地缓存，实现离线模式和数据持久化
 */
public class DataCacheManager {
    private static final String TAG = "DataCacheManager";
    private static final String PREF_NAME = "fincialsystem_data_cache";
    private static final String KEY_CATEGORIES = "categories";
    private static final String KEY_TRANSACTIONS = "transactions";
    private static final String KEY_BUDGETS = "budgets";
    private static final String KEY_STATISTICS = "statistics";
    private static final String KEY_TIMESTAMP_PREFIX = "timestamp_";
    
    private static DataCacheManager instance;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    
    // 内存缓存
    private Map<String, Object> memoryCache;
    
    private DataCacheManager(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        memoryCache = new HashMap<>();
    }
    
    /**
     * 获取单例实例
     */
    public static DataCacheManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DataCacheManager.class) {
                if (instance == null) {
                    instance = new DataCacheManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 清除所有缓存数据
     */
    public void clearAllCaches() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        memoryCache.clear();
        LogUtils.d(TAG, "所有缓存数据已清除");
    }
    
    /**
     * 获取缓存过期时间
     * @param key 缓存键
     * @return 过期时间（单位：毫秒）
     */
    private long getCacheExpiry(String key) {
        // 默认缓存过期时间为30分钟
        long defaultExpiry = 30 * 60 * 1000L;
        
        // 从偏好设置中获取过期时间戳
        return sharedPreferences.getLong(KEY_TIMESTAMP_PREFIX + key, 0);
    }
    
    /**
     * 获取缓存时长
     * @param key 缓存键
     * @return 缓存时长（单位：毫秒）
     */
    private long getCacheDuration(String key) {
        // 根据不同的缓存类型设定不同的过期时间
        if (key.startsWith("overview_")) {
            // 概览数据缓存时间较短
            return 30 * 60 * 1000; // 30分钟
        } else if (key.startsWith("trends_")) {
            // 趋势数据缓存时间中等
            return 60 * 60 * 1000; // 1小时
        } else if (key.equals(KEY_CATEGORIES)) {
            // 分类数据缓存时间较长
            return 24 * 60 * 60 * 1000; // 24小时
        } else if (key.equals(KEY_TRANSACTIONS) || key.startsWith("transactions_")) {
            // 交易记录缓存时间中等
            return 2 * 60 * 60 * 1000; // 2小时
        } else if (key.equals(KEY_BUDGETS) || key.startsWith("budgets_")) {
            // 预算数据缓存时间较长
            return 12 * 60 * 60 * 1000; // 12小时
        } else {
            // 默认缓存时间
            return 60 * 60 * 1000; // 1小时
        }
    }
    
    /**
     * 判断缓存是否有效
     * @param key 缓存键
     * @return 缓存是否有效
     */
    public boolean isCacheValid(String key) {
        try {
            long timestamp = sharedPreferences.getLong(KEY_TIMESTAMP_PREFIX + key, 0);
            if (timestamp == 0) {
                return false; // 不存在时间戳，缓存无效
            }
            
            long expiry = getCacheExpiry(key);
            long currentTime = System.currentTimeMillis();
            
            boolean isValid = currentTime < expiry;
            if (!isValid) {
                LogUtils.d(TAG, "缓存已过期: " + key + ", 过期时间: " + 
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(expiry)) + 
                        ", 当前时间: " + 
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(currentTime)));
            } else {
                LogUtils.d(TAG, "缓存有效: " + key + ", 将在 " + 
                        ((expiry - currentTime) / (60 * 1000)) + " 分钟后过期");
            }
            
            return isValid;
        } catch (Exception e) {
            LogUtils.e(TAG, "检查缓存有效性出错: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 保存分类列表缓存
     * @param categories 分类列表
     */
    public void saveCategories(List<Category> categories) {
        saveCache(KEY_CATEGORIES, categories);
    }
    
    /**
     * 获取分类列表缓存
     * @return 分类列表，如无缓存则返回空列表
     */
    public List<Category> getCategories() {
        Type type = new TypeToken<List<Category>>(){}.getType();
        return getCache(KEY_CATEGORIES, type, new ArrayList<>());
    }
    
    /**
     * 保存交易记录列表缓存
     * @param transactions 交易记录列表
     */
    public void saveTransactions(List<Transaction> transactions) {
        saveCache(KEY_TRANSACTIONS, transactions);
    }
    
    /**
     * 获取交易记录列表缓存
     * @return 交易记录列表，如无缓存则返回空列表
     */
    public List<Transaction> getTransactions() {
        Type type = new TypeToken<List<Transaction>>(){}.getType();
        return getCache(KEY_TRANSACTIONS, type, new ArrayList<>());
    }
    
    /**
     * 保存预算列表缓存
     * @param budgets 预算列表
     */
    public void saveBudgets(List<Budget> budgets) {
        saveCache(KEY_BUDGETS, budgets);
    }
    
    /**
     * 获取预算列表缓存
     * @return 预算列表，如无缓存则返回空列表
     */
    public List<Budget> getBudgets() {
        Type type = new TypeToken<List<Budget>>(){}.getType();
        return getCache(KEY_BUDGETS, type, new ArrayList<>());
    }
    
    /**
     * 保存统计数据到缓存
     * @param key 缓存键
     * @param statistics 统计数据
     */
    public void saveStatistics(String key, Map<String, Object> statistics) {
        if (statistics == null) {
            LogUtils.e(TAG, "不能保存空的统计数据");
            return;
        }
        
        try {
            String statisticsJson = gson.toJson(statistics);
            
            // 保存到磁盘
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, statisticsJson);
            editor.putLong(KEY_TIMESTAMP_PREFIX + key, System.currentTimeMillis());
            editor.apply();
            
            // 同步更新内存缓存
            memoryCache.put(key, statistics);
            memoryCache.put(KEY_TIMESTAMP_PREFIX + key, System.currentTimeMillis());
            
            LogUtils.d(TAG, "统计数据已保存到缓存: " + key + ", 条目数: " + statistics.size());
        } catch (Exception e) {
            LogUtils.e(TAG, "保存统计数据到缓存失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从缓存获取统计数据
     */
    public Map<String, Object> getStatistics(String key) {
        try {
            // 先尝试从内存缓存获取
            if (memoryCache.containsKey(key)) {
                Object cached = memoryCache.get(key);
                if (cached instanceof Map) {
                    LogUtils.d(TAG, "从内存缓存获取统计数据: " + key);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) cached;
                    return result;
                }
            }
            
            // 从磁盘缓存获取
            String statisticsJson = sharedPreferences.getString(key, null);
            if (statisticsJson != null) {
                LogUtils.d(TAG, "从磁盘缓存获取统计数据: " + key);
                
                // 使用TypeToken解析泛型JSON
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> statistics = gson.fromJson(statisticsJson, type);
                
                // 放入内存缓存
                if (statistics != null) {
                    memoryCache.put(key, statistics);
                    return statistics;
                }
            }
            
            LogUtils.d(TAG, "缓存中不存在统计数据: " + key);
            return null;
        } catch (Exception e) {
            LogUtils.e(TAG, "从缓存获取统计数据失败: " + key + ", 错误: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 保存缓存数据
     * @param key 缓存键
     * @param data 缓存数据
     */
    private void saveCache(String key, Object data) {
        try {
            String json = gson.toJson(data);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, json);
            editor.putLong(KEY_TIMESTAMP_PREFIX + key, System.currentTimeMillis());
            editor.apply();
            
            // 更新内存缓存
            memoryCache.put(key, data);
            
            LogUtils.d(TAG, "缓存数据已保存: " + key);
        } catch (Exception e) {
            LogUtils.e(TAG, "保存缓存数据失败: " + key, e);
        }
    }
    
    /**
     * 获取缓存数据
     * @param key 缓存键
     * @param type 数据类型
     * @param defaultValue 默认值
     * @return 缓存数据
     */
    private <T> T getCache(String key, Type type, T defaultValue) {
        try {
            // 优先从内存缓存获取
            if (memoryCache.containsKey(key)) {
                Object data = memoryCache.get(key);
                if (data != null) {
                    return (T) data;
                }
            }
            
            // 从持久化存储获取
            String json = sharedPreferences.getString(key, null);
            if (json != null) {
                T data = gson.fromJson(json, type);
                
                // 更新内存缓存
                memoryCache.put(key, data);
                
                LogUtils.d(TAG, "从缓存获取数据: " + key);
                return data;
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "获取缓存数据失败: " + key, e);
        }
        
        return defaultValue;
    }
} 