package com.zjf.fincialsystem.repository;

import android.content.Context;

import com.zjf.fincialsystem.db.DataCacheManager;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.network.api.StatisticsApiService;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NetworkUtils;
import com.zjf.fincialsystem.utils.TokenManager;

import java.util.HashMap;
import java.util.Map;

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
            Call<ApiResponse<Map<String, Object>>> call = NetworkManager.getInstance()
                    .getStatisticsApiService()
                    .getOverview(period);
                
            call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                        Map<String, Object> data = response.body().getData();
                        
                        if (data != null) {
                            // 保存到缓存
                            cacheManager.saveStatistics(cacheKey, data);
                            
                            LogUtils.d(TAG, "成功获取概览数据：" + data);
                            
                            if (callback != null) {
                                callback.onSuccess(data);
                                callback.isCacheData(false);
                            }
                        } else {
                            LogUtils.e(TAG, "API返回的数据为空");
                            if (callback != null) {
                                callback.onError("获取数据失败，服务器返回空数据");
                            }
                        }
                    } else {
                        String errorMessage = "获取概览数据失败：";
                        if (response.body() != null) {
                            errorMessage += response.body().getMessage();
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
                public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
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
            apiService.getIncomeByCategory(startDate, endDate).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Map<String, Object>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            Map<String, Object> data = apiResponse.getData();
                            
                            // 保存到缓存
                            cacheManager.saveStatistics(cacheKey, data);
                            
                            // 返回数据
                            callback.onSuccess(data);
                        } else {
                            callback.onError(apiResponse.getMessage());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
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
            apiService.getExpenseByCategory(startDate, endDate).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Map<String, Object>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            Map<String, Object> data = apiResponse.getData();
                            
                            // 保存到缓存
                            cacheManager.saveStatistics(cacheKey, data);
                            
                            // 返回数据
                            callback.onSuccess(data);
                        } else {
                            callback.onError(apiResponse.getMessage());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
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
            apiService.getTrend(type, period).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Map<String, Object>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            Map<String, Object> data = apiResponse.getData();
                            
                            // 保存到缓存
                            cacheManager.saveStatistics(cacheKey, data);
                            
                            // 返回数据
                            callback.onSuccess(data);
                        } else {
                            callback.onError(apiResponse.getMessage());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
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
            apiService.getBudgetUsage().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Map<String, Object>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            Map<String, Object> data = apiResponse.getData();
                            
                            // 保存到缓存
                            cacheManager.saveStatistics(cacheKey, data);
                            
                            // 返回数据
                            callback.onSuccess(data);
                        } else {
                            callback.onError(apiResponse.getMessage());
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