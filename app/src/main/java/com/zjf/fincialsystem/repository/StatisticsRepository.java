package com.zjf.fincialsystem.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.zjf.fincialsystem.model.CategoryStatistic;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.network.api.StatisticsApiService;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NetworkUtils;
import com.zjf.fincialsystem.utils.TokenManager;
import com.zjf.fincialsystem.db.DataCacheManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.SimpleTimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 统计数据仓库
 * 负责统计数据的获取和缓存管理
 */
public class StatisticsRepository {
    private static final String TAG = "StatisticsRepository";
    
    private final Context context;
    private final StatisticsApiService apiService;
    private final DataCacheManager cacheManager;
    
    public StatisticsRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkManager.getInstance().getStatisticsApiService();
        this.cacheManager = DataCacheManager.getInstance(context);
    }
    
    /**
     * 获取数值类型属性的安全方法
     * @param data 数据Map
     * @param key 键名
     * @param defaultValue 默认值
     * @return 值
     */
    public double getDoubleValue(Map<String, Object> data, String key, double defaultValue) {
        if (data.containsKey(key)) {
            Object value = data.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取概览统计
     * @param period 统计周期：daily, weekly, monthly, yearly
     * @param callback 回调
     */
    public void getOverview(String period, final RepositoryCallback<Map<String, Object>> callback) {
        // 先检查缓存
        String cacheKey = "overview_" + period;
        Map<String, Object> cachedData = cacheManager.getStatistics(cacheKey);
        
        if (cachedData != null && cacheManager.isCacheValid(cacheKey)) {
            LogUtils.d(TAG, "使用缓存数据：" + cacheKey);
            if (callback != null) {
                callback.onSuccess(cachedData);
                callback.isCacheData(true);
            }
            return;
        }
        
        // 确保我们有网络服务
        if (NetworkManager.getInstance().getStatisticsApiService() == null) {
            LogUtils.e(TAG, "网络服务未初始化，尝试重新初始化");
            NetworkManager.getInstance().init();
        }
        
        // 确保用户已登录
        if (!TokenManager.getInstance().isLoggedIn()) {
            LogUtils.e(TAG, "用户未登录，无法获取概览数据");
            if (callback != null) {
                callback.onError("用户未登录，请重新登录");
            }
            return;
        }
        
        try {
            // 从API获取数据
            long userId = TokenManager.getInstance().getUserId();
            // 使用用户ID和当前年份获取年度统计
            long startTime = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L; // 过去30天
            long endTime = System.currentTimeMillis();
            int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            
            Call<ApiResponse<List<Map<String, Object>>>> call;
            
            if ("monthly".equals(period) || "daily".equals(period)) {
                // 获取月度或日度数据，使用月度交易统计
                call = NetworkManager.getInstance().getTransactionApiService().getMonthlyStats(userId, startTime, endTime, null);
            } else {
                // 获取年度数据
                call = apiService.getYearStatistics(userId, year);
            }
                
            call.enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call, Response<ApiResponse<List<Map<String, Object>>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Map<String, Object>> listData = response.body().getData();
                        
                        if (listData == null || listData.isEmpty()) {
                            LogUtils.d(TAG, "API返回的数据为空，将显示为零");
                            // 创建空结果并返回
                            Map<String, Object> emptyData = new HashMap<>();
                            emptyData.put("income", 0.0);
                            emptyData.put("expense", 0.0);
                            emptyData.put("balance", 0.0);
                            
                            // 保存到缓存
                            cacheManager.saveStatistics(cacheKey, emptyData);
                            
                            if (callback != null) {
                                callback.onSuccess(emptyData);
                                callback.isCacheData(false);
                            }
                            return;
                        }
                        
                        // 汇总数据
                        Map<String, Object> data = new HashMap<>();
                        double totalIncome = 0;
                        double totalExpense = 0;
                        
                        for (Map<String, Object> item : listData) {
                            int type = (int) getDoubleValue(item, "type", 0);
                            double amount = getDoubleValue(item, "amount", 0);
                            
                            if (type == 1) { // 收入
                                totalIncome += amount;
                            } else { // 支出
                                totalExpense += amount;
                            }
                        }
                        
                        data.put("income", totalIncome);
                        data.put("expense", totalExpense);
                        data.put("balance", totalIncome - totalExpense);
                        
                        // 保存到缓存
                        cacheManager.saveStatistics(cacheKey, data);
                        
                        LogUtils.d(TAG, "成功获取概览数据：" + data);
                        
                        if (callback != null) {
                            callback.onSuccess(data);
                            callback.isCacheData(false);
                        }
                    } else {
                        String errorMessage = "获取概览数据失败：";
                        if (response.body() != null) {
                            errorMessage += response.body().getMsg();
                        } else {
                            errorMessage += "网络请求失败，状态码: " + response.code();
                        }
                        
                        LogUtils.e(TAG, errorMessage);
                        if (callback != null) {
                            callback.onError(errorMessage);
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                    String errorMessage = "网络请求失败：" + t.getMessage();
                    LogUtils.e(TAG, errorMessage, t);
                    
                    // 如果有缓存数据，即使过期也返回
                    Map<String, Object> expiredData = cacheManager.getStatistics(cacheKey);
                    if (expiredData != null) {
                        LogUtils.w(TAG, "使用过期的缓存数据");
                        if (callback != null) {
                            callback.onSuccess(expiredData);
                            callback.isCacheData(true);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(errorMessage);
                        }
                    }
                }
            });
        } catch (Exception e) {
            String errorMessage = "请求概览数据时发生错误：" + e.getMessage();
            LogUtils.e(TAG, errorMessage, e);
            
            // 如果有缓存数据，即使过期也返回
            Map<String, Object> expiredData = cacheManager.getStatistics(cacheKey);
            if (expiredData != null) {
                LogUtils.w(TAG, "使用过期的缓存数据");
                if (callback != null) {
                    callback.onSuccess(expiredData);
                    callback.isCacheData(true);
                }
            } else {
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        }
    }
    
    /**
     * 获取收入分类统计
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     * @param callback 回调
     */
    public void getIncomeByCategory(long startDate, long endDate, final RepositoryCallback<Map<String, Object>> callback) {
        final String cacheKey = "income_category_" + startDate + "_" + endDate;
        
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            long userId = TokenManager.getInstance().getUserId();
            apiService.getCategoryStatistics(userId, startDate, endDate, 1).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call, Response<ApiResponse<List<Map<String, Object>>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Map<String, Object>>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            List<Map<String, Object>> listData = apiResponse.getData();
                            
                            // 检查API返回的数据是否为空
                            if (listData == null) {
                                LogUtils.d(TAG, "API返回的分类统计数据为空，返回空列表");
                                listData = new ArrayList<>();
                            }
                            
                            // 将列表数据转换为所需的单个Map格式
                            Map<String, Object> data = new HashMap<>();
                            data.put("categories", listData);
                            
                            // 计算总和
                            double total = 0;
                            for (Map<String, Object> item : listData) {
                                total += getDoubleValue(item, "amount", 0);
                            }
                            data.put("total", total);
                            
                            // 保存到缓存
                            cacheManager.saveStatistics(cacheKey, data);
                            
                            // 返回数据
                            callback.onSuccess(data);
                        } else {
                            callback.onError(apiResponse.getMsg());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                    LogUtils.e(TAG, "获取收入分类统计失败", t);
                    
                    // 网络请求失败，尝试从缓存获取
                    if (cacheManager.isCacheValid("statistics_" + cacheKey)) {
                        Map<String, Object> cachedData = cacheManager.getStatistics(cacheKey);
                        if (cachedData != null && !cachedData.isEmpty()) {
                            callback.onSuccess(cachedData);
                            
                            // 标记为从缓存获取
                            callback.isCacheData(true);
                        } else {
                            callback.onError("获取收入分类统计失败: " + t.getMessage());
                        }
                    } else {
                        callback.onError("获取收入分类统计失败: " + t.getMessage());
                    }
                }
            });
        } else {
            // 无网络连接，从缓存获取数据
            if (cacheManager.isCacheValid("statistics_" + cacheKey)) {
                Map<String, Object> cachedData = cacheManager.getStatistics(cacheKey);
                if (cachedData != null && !cachedData.isEmpty()) {
                    callback.onSuccess(cachedData);
                    
                    // 标记为从缓存获取
                    callback.isCacheData(true);
                } else {
                    callback.onError("无网络连接且无缓存数据");
                }
            } else {
                callback.onError("无网络连接且无缓存数据");
            }
        }
    }
    
    /**
     * 获取支出分类统计
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     * @param callback 回调
     */
    public void getExpenseByCategory(long startDate, long endDate, final RepositoryCallback<Map<String, Object>> callback) {
        final String cacheKey = "expense_category_" + startDate + "_" + endDate;
        
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            long userId = TokenManager.getInstance().getUserId();
            apiService.getCategoryStatistics(userId, startDate, endDate, 0).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call, Response<ApiResponse<List<Map<String, Object>>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Map<String, Object>>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            List<Map<String, Object>> listData = apiResponse.getData();
                            
                            // 检查API返回的数据是否为空
                            if (listData == null) {
                                LogUtils.d(TAG, "API返回的分类统计数据为空，返回空列表");
                                listData = new ArrayList<>();
                            }
                            
                            // 将列表数据转换为所需的单个Map格式
                            Map<String, Object> data = new HashMap<>();
                            data.put("categories", listData);
                            
                            // 计算总和
                            double total = 0;
                            for (Map<String, Object> item : listData) {
                                total += getDoubleValue(item, "amount", 0);
                            }
                            data.put("total", total);
                            
                            // 保存到缓存
                            cacheManager.saveStatistics(cacheKey, data);
                            
                            // 返回数据
                            callback.onSuccess(data);
                        } else {
                            callback.onError(apiResponse.getMsg());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                    LogUtils.e(TAG, "获取支出分类统计失败", t);
                    
                    // 网络请求失败，尝试从缓存获取
                    if (cacheManager.isCacheValid("statistics_" + cacheKey)) {
                        Map<String, Object> cachedData = cacheManager.getStatistics(cacheKey);
                        if (cachedData != null && !cachedData.isEmpty()) {
                            callback.onSuccess(cachedData);
                            
                            // 标记为从缓存获取
                            callback.isCacheData(true);
                        } else {
                            callback.onError("获取支出分类统计失败: " + t.getMessage());
                        }
                    } else {
                        callback.onError("获取支出分类统计失败: " + t.getMessage());
                    }
                }
            });
        } else {
            // 无网络连接，从缓存获取数据
            if (cacheManager.isCacheValid("statistics_" + cacheKey)) {
                Map<String, Object> cachedData = cacheManager.getStatistics(cacheKey);
                if (cachedData != null && !cachedData.isEmpty()) {
                    callback.onSuccess(cachedData);
                    
                    // 标记为从缓存获取
                    callback.isCacheData(true);
                } else {
                    callback.onError("无网络连接且无缓存数据");
                }
            } else {
                callback.onError("无网络连接且无缓存数据");
            }
        }
    }
    
    /**
     * 获取趋势统计
     * @param type 交易类型：0-支出，1-收入
     * @param period 统计周期：daily, weekly, monthly, yearly
     * @param callback 回调
     */
    public void getTrend(int type, String period, final RepositoryCallback<Map<String, Object>> callback) {
        final String cacheKey = "trend_" + type + "_" + period;
        
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            long userId = TokenManager.getInstance().getUserId();
            int months = 12; // 默认获取12个月的数据
            
            if ("weekly".equals(period)) {
                months = 3; // 周趋势显示3个月数据
            } else if ("daily".equals(period)) {
                months = 1; // 日趋势显示1个月数据
            }
            
            LogUtils.d(TAG, "请求趋势数据, userId=" + userId + ", months=" + months);
            
            apiService.getTrend(userId, months).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call, Response<ApiResponse<List<Map<String, Object>>>> response) {
                    LogUtils.d(TAG, "趋势数据响应状态码: " + response.code());
                    
                    if (!response.isSuccessful()) {
                        LogUtils.e(TAG, "获取趋势数据失败，HTTP错误码: " + response.code());
                        callback.onError("网络请求失败，错误码: " + response.code());
                        return;
                    }
                    
                    if (response.body() == null) {
                        LogUtils.e(TAG, "获取趋势数据失败，响应体为空");
                        callback.onError("获取趋势数据失败，响应体为空");
                        return;
                    }
                    
                    ApiResponse<List<Map<String, Object>>> apiResponse = response.body();
                    LogUtils.d(TAG, "趋势数据响应: code=" + apiResponse.getCode() + ", msg=" + apiResponse.getMsg());
                    
                    // 尝试从原始响应体中获取JSON
                    String rawJson = "";
                    try {
                        rawJson = new Gson().toJson(response.body());
                        LogUtils.d(TAG, "趋势数据原始JSON: " + rawJson);
                    } catch (Exception e) {
                        LogUtils.e(TAG, "序列化趋势数据响应失败", e);
                    }
                    
                    if (apiResponse.isSuccess()) {
                        List<Map<String, Object>> allData = apiResponse.getData();
                        
                        // 检查数据是否为空
                        if (allData == null) {
                            LogUtils.w(TAG, "趋势数据为空，创建空列表");
                            allData = new ArrayList<>();
                        }
                        
                        // 过滤特定类型的数据
                        List<Map<String, Object>> filteredData = new ArrayList<>();
                        for (Map<String, Object> item : allData) {
                            // 支持多种数据格式
                            // 格式1: {type: 1, ...}
                            // 格式2: {expense: 1000, income: 2000, ...}
                            if (item.containsKey("type")) {
                                int itemType = (int) getDoubleValue(item, "type", -1);
                                if (itemType == type) {
                                    filteredData.add(item);
                                }
                            } else {
                                // 如果数据中没有type字段，可能是收入/支出混合格式
                                // 创建新的条目，根据请求的类型选择相应的字段
                                Map<String, Object> newItem = new HashMap<>(item);
                                if (type == 0) { // 支出
                                    if (item.containsKey("expense")) {
                                        newItem.put("amount", getDoubleValue(item, "expense", 0));
                                        filteredData.add(newItem);
                                    }
                                } else { // 收入
                                    if (item.containsKey("income")) {
                                        newItem.put("amount", getDoubleValue(item, "income", 0));
                                        filteredData.add(newItem);
                                    }
                                }
                            }
                        }
                        
                        // 如果过滤后没有数据，可能需要转换格式
                        if (filteredData.isEmpty() && !allData.isEmpty()) {
                            LogUtils.w(TAG, "过滤后趋势数据为空，尝试其他格式解析");
                            
                            // 检查是否是整体趋势数据格式
                            boolean hasExpenseOrIncome = false;
                            for (Map<String, Object> item : allData) {
                                if (item.containsKey("expense") || item.containsKey("income")) {
                                    hasExpenseOrIncome = true;
                                    break;
                                }
                            }
                            
                            if (hasExpenseOrIncome) {
                                // 使用收入/支出格式
                                for (Map<String, Object> item : allData) {
                                    Map<String, Object> newItem = new HashMap<>(item);
                                    if (type == 0 && item.containsKey("expense")) { // 支出
                                        newItem.put("amount", getDoubleValue(item, "expense", 0));
                                        newItem.put("type", 0);
                                        filteredData.add(newItem);
                                    } else if (type == 1 && item.containsKey("income")) { // 收入
                                        newItem.put("amount", getDoubleValue(item, "income", 0));
                                        newItem.put("type", 1);
                                        filteredData.add(newItem);
                                    }
                                }
                            }
                        }
                        
                        // 排序数据（按日期/月份）
                        Collections.sort(filteredData, (a, b) -> {
                            // 尝试获取日期字段进行排序
                            String dateA = a.containsKey("month") ? String.valueOf(a.get("month")) : 
                                         (a.containsKey("date") ? String.valueOf(a.get("date")) : "");
                            String dateB = b.containsKey("month") ? String.valueOf(b.get("month")) : 
                                         (b.containsKey("date") ? String.valueOf(b.get("date")) : "");
                            return dateA.compareTo(dateB);
                        });
                        
                        // 创建结果Map
                        Map<String, Object> data = new HashMap<>();
                        data.put("trend", filteredData);
                        
                        // 按前端预期格式添加trendData字段
                        data.put("trendData", filteredData);
                        
                        // 保存到缓存
                        cacheManager.saveStatistics(cacheKey, data);
                        
                        // 返回数据
                        callback.onSuccess(data);
                    } else {
                        callback.onError(apiResponse.getMsg());
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                    LogUtils.e(TAG, "获取趋势统计失败", t);
                    
                    // 网络请求失败，尝试从缓存获取
                    if (cacheManager.isCacheValid("statistics_" + cacheKey)) {
                        Map<String, Object> cachedData = cacheManager.getStatistics(cacheKey);
                        if (cachedData != null && !cachedData.isEmpty()) {
                            callback.onSuccess(cachedData);
                            
                            // 标记为从缓存获取
                            callback.isCacheData(true);
                        } else {
                            callback.onError("获取趋势统计失败: " + t.getMessage());
                        }
                    } else {
                        callback.onError("获取趋势统计失败: " + t.getMessage());
                    }
                }
            });
        } else {
            // 无网络连接，从缓存获取数据
            if (cacheManager.isCacheValid("statistics_" + cacheKey)) {
                Map<String, Object> cachedData = cacheManager.getStatistics(cacheKey);
                if (cachedData != null && !cachedData.isEmpty()) {
                    callback.onSuccess(cachedData);
                    
                    // 标记为从缓存获取
                    callback.isCacheData(true);
                } else {
                    callback.onError("无网络连接且无缓存数据");
                }
            } else {
                callback.onError("无网络连接且无缓存数据");
            }
        }
    }
    
    /**
     * 获取预算使用统计
     * @param callback 回调
     */
    public void getBudgetUsage(final RepositoryCallback<Map<String, Object>> callback) {
        final String cacheKey = "budget_usage";
        
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            long userId = TokenManager.getInstance().getUserId();
            String month = java.time.YearMonth.now().toString().substring(0, 7); // 当前月份，格式为"yyyy-MM"
            
            apiService.getBudgetStatistics(userId, month).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Map<String, Object>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            Map<String, Object> data = apiResponse.getData();
                            
                            // 检查数据是否为空
                            if (data == null) {
                                LogUtils.d(TAG, "API返回的预算使用统计数据为空，返回空Map");
                                data = new HashMap<>();
                            }
                            
                            // 保存到缓存
                            cacheManager.saveStatistics(cacheKey, data);
                            
                            // 返回数据
                            callback.onSuccess(data);
                        } else {
                            callback.onError(apiResponse.getMsg());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                    LogUtils.e(TAG, "获取预算使用统计失败", t);
                    
                    // 网络请求失败，尝试从缓存获取
                    if (cacheManager.isCacheValid("statistics_" + cacheKey)) {
                        Map<String, Object> cachedData = cacheManager.getStatistics(cacheKey);
                        if (cachedData != null && !cachedData.isEmpty()) {
                            callback.onSuccess(cachedData);
                            
                            // 标记为从缓存获取
                            callback.isCacheData(true);
                        } else {
                            callback.onError("获取预算使用统计失败: " + t.getMessage());
                        }
                    } else {
                        callback.onError("获取预算使用统计失败: " + t.getMessage());
                    }
                }
            });
        } else {
            // 无网络连接，从缓存获取数据
            if (cacheManager.isCacheValid("statistics_" + cacheKey)) {
                Map<String, Object> cachedData = cacheManager.getStatistics(cacheKey);
                if (cachedData != null && !cachedData.isEmpty()) {
                    callback.onSuccess(cachedData);
                    
                    // 标记为从缓存获取
                    callback.isCacheData(true);
                } else {
                    callback.onError("无网络连接且无缓存数据");
                }
            } else {
                callback.onError("无网络连接且无缓存数据");
            }
        }
    }
} 