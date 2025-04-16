package com.zjf.fincialsystem.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.zjf.fincialsystem.model.CategoryStatistic;
import com.zjf.fincialsystem.model.TrendData;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.SimpleTimeZone;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 统计数据仓库
 * 负责统计数据的获取和缓存管理
 */
public class StatisticsRepository {
    private static final String TAG = "StatisticsRepository";
    
    /**
     * 统计周期类型枚举
     */
    public enum PeriodType {
        DAILY,      // 日统计
        WEEKLY,     // 周统计
        MONTHLY,    // 月统计
        YEARLY      // 年统计
    }
    
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
     * @param period 统计周期：daily, weekly, monthly, yearly，也可以是带有年月信息的字符串，如 "monthly_2024-05"
     * @param callback 回调
     */
    public void getOverview(String period, final RepositoryCallback<Map<String, Object>> callback) {
        // 解析period参数，检查是否包含特定年月
        String periodType = period;
        long customStartTime = 0;
        long customEndTime = 0;
        
        // 检查是否是带有特定年月的请求格式 (如 "monthly_2024-05")
        if (period.contains("_")) {
            String[] parts = period.split("_");
            periodType = parts[0]; // 提取周期类型 (如 "monthly")
            
            if (parts.length > 1 && parts[1].matches("\\d{4}-\\d{2}")) {
                String yearMonth = parts[1]; // 提取年月 (如 "2024-05")
                
                try {
                    // 解析年月
                    String[] dateParts = yearMonth.split("-");
                    int year = Integer.parseInt(dateParts[0]);
                    int month = Integer.parseInt(dateParts[1]) - 1; // Calendar月份从0开始
                    
                    // 创建指定月份的起始和结束时间
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(year, month, 1, 0, 0, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    customStartTime = calendar.getTimeInMillis();
                    
                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    calendar.set(Calendar.MILLISECOND, 999);
                    customEndTime = calendar.getTimeInMillis();
                    
                    LogUtils.d(TAG, "解析自定义时间范围: " + yearMonth + ", 开始: " + customStartTime + ", 结束: " + customEndTime);
                } catch (Exception e) {
                    LogUtils.e(TAG, "解析年月失败: " + yearMonth, e);
                }
            }
        }
        
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
            
            long startTime, endTime;
            
            // 如果有自定义时间范围，则使用它
            if (customStartTime > 0 && customEndTime > 0) {
                startTime = customStartTime;
                endTime = customEndTime;
                LogUtils.d(TAG, "使用自定义时间范围: " + startTime + " - " + endTime);
            } else {
                // 否则根据periodType计算时间范围
                long now = System.currentTimeMillis();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(now);
                
                // 设置时间范围
                if ("daily".equals(periodType)) {
                    // 日统计 - 今天的数据
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    startTime = calendar.getTimeInMillis();
                    endTime = now;
                } else if ("weekly".equals(periodType)) {
                    // 周统计 - 本周数据
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    startTime = calendar.getTimeInMillis();
                    endTime = now;
                } else if ("yearly".equals(periodType)) {
                    // 年统计 - 使用年度统计接口
                    int year = calendar.get(Calendar.YEAR);
                    
                    // 调用年度统计接口
                    Call<ApiResponse<List<Map<String, Object>>>> call = apiService.getYearStatistics(userId, year);
                    
                    call.enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call, Response<ApiResponse<List<Map<String, Object>>>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<Map<String, Object>> listData = response.body().getData();
                                
                                if (listData == null || listData.isEmpty()) {
                                    LogUtils.d(TAG, "API返回的数据为空，将显示为零");
                                    // 创建空结果并返回
                                    Map<String, Object> emptyData = new HashMap<>();
                                    emptyData.put("totalIncome", 0.0);
                                    emptyData.put("totalExpense", 0.0);
                                    emptyData.put("totalBalance", 0.0);
                                    
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
                                    // 年度统计接口返回的是按月汇总的数据
                                    double income = getDoubleValue(item, "income", 0);
                                    double expense = getDoubleValue(item, "expense", 0);
                                    
                                    totalIncome += income;
                                    totalExpense += expense;
                                }
                                
                                double totalBalance = totalIncome - totalExpense;
                                
                                data.put("totalIncome", totalIncome);
                                data.put("totalExpense", totalExpense);
                                data.put("totalBalance", totalBalance);
                                
                                // 保存到缓存
                                cacheManager.saveStatistics(cacheKey, data);
                                
                                LogUtils.d(TAG, "成功获取年度概览数据：" + data);
                                
                                if (callback != null) {
                                    callback.onSuccess(data);
                                    callback.isCacheData(false);
                                }
                            } else {
                                String errorMessage = "获取年度概览数据失败：";
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
                    
                    return; // 异步调用，此处返回
                } else {
                    // 月统计(默认) - 本月数据
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    startTime = calendar.getTimeInMillis();
                    endTime = now;
                }
            }
            
            LogUtils.d(TAG, "请求概览数据，时间范围: " + new Date(startTime) + " - " + new Date(endTime));
            
            // 使用月度收支总额接口获取概览数据
            Call<ApiResponse<Map<String, Object>>> call = NetworkManager.getInstance().getTransactionApiService()
                .getMonthAmount(userId, startTime, endTime);
            
            call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> responseData = response.body().getData();
                        
                        if (responseData == null || responseData.isEmpty()) {
                            LogUtils.d(TAG, "API返回的数据为空，将显示为零");
                            // 创建空结果并返回
                            Map<String, Object> emptyData = new HashMap<>();
                            emptyData.put("totalIncome", 0.0);
                            emptyData.put("totalExpense", 0.0);
                            emptyData.put("totalBalance", 0.0);
                            
                            // 保存到缓存
                            cacheManager.saveStatistics(cacheKey, emptyData);
                            
                            if (callback != null) {
                                callback.onSuccess(emptyData);
                                callback.isCacheData(false);
                            }
                            return;
                        }
                        
                        // 转换API返回的数据格式
                        Map<String, Object> data = new HashMap<>();
                        double income = getDoubleValue(responseData, "income", 0);
                        double expense = getDoubleValue(responseData, "expense", 0);
                        double balance = getDoubleValue(responseData, "balance", 0);
                        
                        data.put("totalIncome", income);
                        data.put("totalExpense", expense);
                        data.put("totalBalance", balance);
                        
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
            
            // 调试日志
            LogUtils.d(TAG, "请求支出分类数据: userId=" + userId + ", startTime=" + startDate + ", endTime=" + endDate + ", type=1");
            
            apiService.getCategoryStatistics(userId, startDate, endDate, 2).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call, Response<ApiResponse<List<Map<String, Object>>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Map<String, Object>>> apiResponse = response.body();
                        
                        // 调试日志
                        LogUtils.d(TAG, "支出分类响应: code=" + apiResponse.getCode() + ", msg=" + apiResponse.getMsg());
                        
                        if (apiResponse.isSuccess()) {
                            List<Map<String, Object>> listData = apiResponse.getData();
                            
                            // 检查API返回的数据是否为空
                            if (listData == null) {
                                LogUtils.d(TAG, "API返回的分类统计数据为空，返回空列表");
                                listData = new ArrayList<>();
                            } else {
                                LogUtils.d(TAG, "API返回的分类统计数据: " + listData.size() + "条");
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
     * 获取特定月份的支出分类统计
     * @param period 带有年月信息的周期标识符，如 "monthly_2024-05"
     * @param startDate 开始日期时间戳（作为备用）
     * @param endDate 结束日期时间戳（作为备用）
     * @param callback 回调
     */
    public void getExpenseByCategoryForMonth(String period, long startDate, long endDate, final RepositoryCallback<Map<String, Object>> callback) {
        // 使用period作为缓存键，确保不同月份数据不冲突
        final String cacheKey = "expense_category_" + period;
        
        // 解析period中的年月信息
        String yearMonth = null;
        int year = 0;
        int month = 0;
        
        // 检查是否是带有特定年月的请求格式 (如 "monthly_2024-05")
        if (period.contains("_")) {
            String[] parts = period.split("_");
            
            if (parts.length > 1 && parts[1].matches("\\d{4}-\\d{2}")) {
                yearMonth = parts[1]; // 提取年月 (如 "2024-05")
                LogUtils.d(TAG, "支出分类统计使用特定年月: " + yearMonth);
                
                try {
                    // 解析年月
                    String[] dateParts = yearMonth.split("-");
                    year = Integer.parseInt(dateParts[0]);
                    month = Integer.parseInt(dateParts[1]);
                } catch (Exception e) {
                    LogUtils.e(TAG, "解析年月失败: " + yearMonth, e);
                }
            }
        }
        
        // 先检查缓存
        Map<String, Object> cachedData = cacheManager.getStatistics(cacheKey);
        
        if (cachedData != null && cacheManager.isCacheValid(cacheKey)) {
            LogUtils.d(TAG, "使用缓存数据：" + cacheKey);
            if (callback != null) {
                callback.onSuccess(cachedData);
                callback.isCacheData(true);
            }
            return;
        }
        
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            long userId = TokenManager.getInstance().getUserId();
            
            // 调试日志
            if (year > 0 && month > 0) {
                LogUtils.d(TAG, "请求特定月份支出分类数据: userId=" + userId + ", year=" + year + ", month=" + month + ", type=2");
            } else {
                LogUtils.d(TAG, "请求支出分类数据: userId=" + userId + ", startTime=" + startDate + ", endTime=" + endDate + ", type=2");
            }
            
            // 构建API请求参数
            Call<ApiResponse<List<Map<String, Object>>>> call;
            
            // 如果有特定年月，创建自定义请求
            if (year > 0 && month > 0) {
                // 如果后端有按年月查询的专用API，可以使用下面的写法
                // call = apiService.getCategoryStatisticsByMonth(userId, year, month, 2);
                
                // 如果没有专用API，则构造时间范围请求
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month - 1, 1, 0, 0, 0); // 月份从0开始
                calendar.set(Calendar.MILLISECOND, 0);
                long monthStartTime = calendar.getTimeInMillis();
                
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                long monthEndTime = calendar.getTimeInMillis();
                
                LogUtils.d(TAG, "使用计算的时间范围: " + new Date(monthStartTime) + " - " + new Date(monthEndTime));
                
                // 使用标准API，传入计算出的时间范围
                call = apiService.getCategoryStatistics(userId, monthStartTime, monthEndTime, 2);
            } else {
                // 使用传入的开始和结束时间
                call = apiService.getCategoryStatistics(userId, startDate, endDate, 2);
            }
            
            call.enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call, Response<ApiResponse<List<Map<String, Object>>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Map<String, Object>>> apiResponse = response.body();
                        
                        // 调试日志
                        LogUtils.d(TAG, "支出分类响应: code=" + apiResponse.getCode() + ", msg=" + apiResponse.getMsg());
                        
                        if (apiResponse.isSuccess()) {
                            List<Map<String, Object>> listData = apiResponse.getData();
                            
                            // 检查API返回的数据是否为空
                            if (listData == null) {
                                LogUtils.d(TAG, "API返回的分类统计数据为空，返回空列表");
                                listData = new ArrayList<>();
                            } else {
                                LogUtils.d(TAG, "API返回的分类统计数据: " + listData.size() + "条");
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
                        Map<String, Object> expiredCachedData = cacheManager.getStatistics(cacheKey);
                        if (expiredCachedData != null && !expiredCachedData.isEmpty()) {
                            callback.onSuccess(expiredCachedData);
                            
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
                Map<String, Object> offlineCachedData = cacheManager.getStatistics(cacheKey);
                if (offlineCachedData != null && !offlineCachedData.isEmpty()) {
                    callback.onSuccess(offlineCachedData);
                    
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
     * @param period 统计周期：daily, weekly, monthly, yearly，也可以是带有年月信息的字符串，如 "monthly_2024-05"
     * @param callback 回调
     */
    public void getTrend(int type, String period, final RepositoryCallback<Map<String, Object>> callback) {
        final String cacheKey = "trend_" + type + "_" + period;
        
        // 解析period参数，检查是否有特定月份
        String periodType = period;
        final String yearMonth;
        
        // 检查是否是带有特定年月的请求格式 (如 "monthly_2024-05")
        if (period.contains("_")) {
            String[] parts = period.split("_");
            periodType = parts[0]; // 提取周期类型 (如 "monthly")
            
            if (parts.length > 1 && parts[1].matches("\\d{4}-\\d{2}")) {
                yearMonth = parts[1]; // 提取年月 (如 "2024-05")
                LogUtils.d(TAG, "趋势统计使用特定年月: " + yearMonth);
            } else {
                yearMonth = null;
            }
        } else {
            yearMonth = null;
        }
        
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            long userId = TokenManager.getInstance().getUserId();
            int months = 12; // 默认获取12个月的数据
            
            if ("weekly".equals(periodType)) {
                months = 3; // 周趋势显示3个月数据
            } else if ("daily".equals(periodType)) {
                months = 1; // 日趋势显示1个月数据
            }
            
            LogUtils.d(TAG, "请求趋势数据, userId=" + userId + ", months=" + months + (yearMonth != null ? ", 特定年月=" + yearMonth : ""));
            
            // 构建API请求参数 - 这里我们始终使用标准trend API，根据接口文档，没有monthTrend API
            Call<ApiResponse<List<Map<String, Object>>>> call;
            
            // 调用标准API接口，不再根据年月使用不同的API
            call = apiService.getTrend(userId, months);
            
            call.enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
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
                        
                        // 如果有特定年月，过滤数据只保留该月的数据
                        if (yearMonth != null) {
                            LogUtils.d(TAG, "过滤特定年月(" + yearMonth + ")的趋势数据");
                            List<Map<String, Object>> filteredByMonth = new ArrayList<>();
                            
                            for (Map<String, Object> item : allData) {
                                // 检查数据是否匹配请求的年月
                                boolean matches = false;
                                
                                // 检查month字段
                                if (item.containsKey("month") && item.get("month") != null) {
                                    String monthValue = String.valueOf(item.get("month"));
                                    if (monthValue.contains(yearMonth) || yearMonth.contains(monthValue)) {
                                        matches = true;
                                    }
                                }
                                
                                // 检查date字段
                                if (!matches && item.containsKey("date") && item.get("date") != null) {
                                    String dateValue = String.valueOf(item.get("date"));
                                    if (dateValue.startsWith(yearMonth)) {
                                        matches = true;
                                    }
                                }
                                
                                if (matches) {
                                    filteredByMonth.add(item);
                                }
                            }
                            
                            // 使用过滤后的数据
                            if (!filteredByMonth.isEmpty()) {
                                LogUtils.d(TAG, "成功过滤出特定月份数据: " + filteredByMonth.size() + "条");
                                allData = filteredByMonth;
                            } else {
                                LogUtils.w(TAG, "特定月份没有数据，保留所有数据");
                            }
                        }
                        
                        // 直接使用原始数据，不做过滤，因为API已经按要求返回了正确格式的数据
                        List<Map<String, Object>> filteredData = new ArrayList<>();
                        
                        // 如果API返回的数据中直接包含expense和income字段，则按类型提取
                        for (Map<String, Object> item : allData) {
                            // 创建新的条目，以确保不修改原始数据
                            Map<String, Object> newItem = new HashMap<>(item);
                            
                            // 根据请求的类型获取相应的数据
                            if (type == 0 || type == 1) { // 支出
                                if (item.containsKey("expense")) {
                                    newItem.put("amount", getDoubleValue(item, "expense", 0));
                                    newItem.put("type", 1); // 设置为支出类型
                                    filteredData.add(newItem);
                                }
                            } else if (type == 2) { // 收入
                                if (item.containsKey("income")) {
                                    newItem.put("amount", getDoubleValue(item, "income", 0));
                                    newItem.put("type", 2); // 设置为收入类型
                                    filteredData.add(newItem);
                                }
                            }
                        }
                        
                        // 如果过滤后没有数据，可能需要采用不同的解析方式
                        if (filteredData.isEmpty() && !allData.isEmpty()) {
                            LogUtils.w(TAG, "过滤后趋势数据为空，尝试其他格式解析");
                            
                            // 直接使用原始数据 - 假设API返回的数据格式已经符合要求
                            filteredData = allData;
                            
                            // 确保每个条目都有amount字段（如果需要）
                            for (Map<String, Object> item : filteredData) {
                                if (!item.containsKey("amount")) {
                                    // 根据数据中可能存在的字段来设置amount
                                    if (type == 0 || type == 1) { // 支出
                                        if (item.containsKey("expense")) {
                                            item.put("amount", getDoubleValue(item, "expense", 0));
                                        }
                                    } else if (type == 2) { // 收入
                                        if (item.containsKey("income")) {
                                            item.put("amount", getDoubleValue(item, "income", 0));
                                        }
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

    /**
     * 获取趋势数据
     * @param periodType 周期类型
     * @param statisticType 统计类型：0-支出，1-收入
     * @return Observable<List<TrendData>>
     */
    public Observable<List<TrendData>> getTrendData(PeriodType periodType, int statisticType) {
        return Observable.create(emitter -> {
            // 转换周期类型为API参数
            String periodString;
            switch (periodType) {
                case YEARLY:
                    periodString = "yearly";
                    break;
                case MONTHLY:
                    periodString = "monthly";
                    break;
                case WEEKLY:
                    periodString = "weekly";
                    break;
                case DAILY:
                default:
                    periodString = "daily";
                    break;
            }
            
            // 调用原有的getTrend方法获取数据
            try {
                getTrend(statisticType, periodString, new RepositoryCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        List<TrendData> result = new ArrayList<>();
                        List<TrendData> parsedDataList = new ArrayList<>();
                        
                        try {
                            // 处理API返回的趋势数据
                            if (data.containsKey("trendData") && data.get("trendData") instanceof List) {
                                List<?> trendDataList = (List<?>) data.get("trendData");
                                
                                // 解析原始数据
                                for (Object item : trendDataList) {
                                    if (item instanceof Map) {
                                        Map<?, ?> itemMap = (Map<?, ?>) item;
                                        TrendData trendData = new TrendData();
                                        
                                        // 设置年份
                                        if (itemMap.containsKey("year")) {
                                            Object yearObj = itemMap.get("year");
                                            if (yearObj instanceof Number) {
                                                trendData.setYear(((Number) yearObj).intValue());
                                            }
                                        }
                                        
                                        // 设置月份
                                        if (itemMap.containsKey("month")) {
                                            Object monthObj = itemMap.get("month");
                                            if (monthObj instanceof Number) {
                                                trendData.setMonth(((Number) monthObj).intValue());
                                            } else if (monthObj instanceof String) {
                                                try {
                                                    String monthStr = (String) monthObj;
                                                    // 处理 "2025-04" 这种格式的月份
                                                    if (monthStr.contains("-")) {
                                                        // 分割字符串，取第二部分作为月份
                                                        String[] parts = monthStr.split("-");
                                                        if (parts.length > 1) {
                                                            // 如果是 "2025-04" 这种格式，把年份也保存下来
                                                            try {
                                                                trendData.setYear(Integer.parseInt(parts[0]));
                                                            } catch (NumberFormatException e) {
                                                                LogUtils.e(TAG, "年份解析错误: " + parts[0], e);
                                                            }
                                                            // 设置月份，注意可能存在前导0
                                                            trendData.setMonth(Integer.parseInt(parts[1]));
                                                        }
                                                    } else {
                                                        // 尝试直接解析为整数
                                                        trendData.setMonth(Integer.parseInt(monthStr));
                                                    }
                                                } catch (NumberFormatException e) {
                                                    LogUtils.e(TAG, "月份解析错误: " + monthObj, e);
                                                    // 解析失败时设置默认值
                                                    trendData.setMonth(0);
                                                }
                                            }
                                        }
                                        
                                        // 设置周
                                        if (itemMap.containsKey("week")) {
                                            Object weekObj = itemMap.get("week");
                                            if (weekObj instanceof Number) {
                                                trendData.setWeek(((Number) weekObj).intValue());
                                            }
                                        }
                                        
                                        // 设置日
                                        if (itemMap.containsKey("day")) {
                                            Object dayObj = itemMap.get("day");
                                            if (dayObj instanceof Number) {
                                                trendData.setDay(((Number) dayObj).intValue());
                                            }
                                        }
                                        
                                        // 设置金额
                                        if (itemMap.containsKey("amount")) {
                                            Object amountObj = itemMap.get("amount");
                                            if (amountObj instanceof Number) {
                                                trendData.setAmount(((Number) amountObj).doubleValue());
                                            }
                                        } else if (statisticType == 0 && itemMap.containsKey("expense")) {
                                            // 支出类型
                                            Object expenseObj = itemMap.get("expense");
                                            if (expenseObj instanceof Number) {
                                                trendData.setAmount(((Number) expenseObj).doubleValue());
                                            }
                                        } else if (statisticType == 1 && itemMap.containsKey("income")) {
                                            // 收入类型
                                            Object incomeObj = itemMap.get("income");
                                            if (incomeObj instanceof Number) {
                                                trendData.setAmount(((Number) incomeObj).doubleValue());
                                            }
                                        }
                                        
                                        // 设置数量
                                        if (itemMap.containsKey("count")) {
                                            Object countObj = itemMap.get("count");
                                            if (countObj instanceof Number) {
                                                trendData.setCount(((Number) countObj).intValue());
                                            }
                                        }
                                        
                                        // 设置标签
                                        if (itemMap.containsKey("label")) {
                                            Object labelObj = itemMap.get("label");
                                            if (labelObj instanceof String) {
                                                trendData.setLabel((String) labelObj);
                                            }
                                        }
                                        
                                        parsedDataList.add(trendData);
                                    }
                                }
                            }
                            
                            // 如果有数据，在前后各添加3个月
                            if (!parsedDataList.isEmpty()) {
                                // 按年月排序
                                Collections.sort(parsedDataList, (a, b) -> {
                                    int yearCompare = Integer.compare(a.getYear(), b.getYear());
                                    if (yearCompare != 0) return yearCompare;
                                    return Integer.compare(a.getMonth(), b.getMonth());
                                });
                                
                                // 获取第一个月和最后一个月
                                TrendData firstData = parsedDataList.get(0);
                                TrendData lastData = parsedDataList.get(parsedDataList.size() - 1);
                                
                                // 添加前3个月的空数据
                                for (int i = 3; i > 0; i--) {
                                    TrendData emptyData = new TrendData();
                                    
                                    // 设置前i个月的年月
                                    int targetMonth = firstData.getMonth() - i;
                                    int targetYear = firstData.getYear();
                                    
                                    // 处理月份溢出
                                    while (targetMonth <= 0) {
                                        targetMonth += 12;
                                        targetYear--;
                                    }
                                    
                                    emptyData.setYear(targetYear);
                                    emptyData.setMonth(targetMonth);
                                    emptyData.setAmount(0.0);
                                    emptyData.setCount(0);
                                    result.add(emptyData);
                                }
                                
                                // 添加原有数据
                                result.addAll(parsedDataList);
                                
                                // 添加后3个月的空数据
                                for (int i = 1; i <= 3; i++) {
                                    TrendData emptyData = new TrendData();
                                    
                                    // 设置后i个月的年月
                                    int targetMonth = lastData.getMonth() + i;
                                    int targetYear = lastData.getYear();
                                    
                                    // 处理月份溢出
                                    while (targetMonth > 12) {
                                        targetMonth -= 12;
                                        targetYear++;
                                    }
                                    
                                    emptyData.setYear(targetYear);
                                    emptyData.setMonth(targetMonth);
                                    emptyData.setAmount(0.0);
                                    emptyData.setCount(0);
                                    result.add(emptyData);
                                }
                            } else {
                                // 无数据时，添加当前月份及前后3个月
                                Calendar calendar = Calendar.getInstance();
                                int currentYear = calendar.get(Calendar.YEAR);
                                int currentMonth = calendar.get(Calendar.MONTH) + 1; // Calendar月份从0开始
                                
                                // 添加当前月份前3个月
                                for (int i = 3; i > 0; i--) {
                                    TrendData emptyData = new TrendData();
                                    
                                    int targetMonth = currentMonth - i;
                                    int targetYear = currentYear;
                                    
                                    // 处理月份溢出
                                    while (targetMonth <= 0) {
                                        targetMonth += 12;
                                        targetYear--;
                                    }
                                    
                                    emptyData.setYear(targetYear);
                                    emptyData.setMonth(targetMonth);
                                    emptyData.setAmount(0.0);
                                    emptyData.setCount(0);
                                    result.add(emptyData);
                                }
                                
                                // 添加当前月份
                                TrendData currentData = new TrendData();
                                currentData.setYear(currentYear);
                                currentData.setMonth(currentMonth);
                                currentData.setAmount(0.0);
                                currentData.setCount(0);
                                result.add(currentData);
                                
                                // 添加当前月份后3个月
                                for (int i = 1; i <= 3; i++) {
                                    TrendData emptyData = new TrendData();
                                    
                                    int targetMonth = currentMonth + i;
                                    int targetYear = currentYear;
                                    
                                    // 处理月份溢出
                                    while (targetMonth > 12) {
                                        targetMonth -= 12;
                                        targetYear++;
                                    }
                                    
                                    emptyData.setYear(targetYear);
                                    emptyData.setMonth(targetMonth);
                                    emptyData.setAmount(0.0);
                                    emptyData.setCount(0);
                                    result.add(emptyData);
                                }
                            }
                            
                            // 发送结果
                            emitter.onNext(result);
                            emitter.onComplete();
                        } catch (Exception e) {
                            LogUtils.e(TAG, "处理趋势数据失败", e);
                            emitter.onError(e);
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        LogUtils.e(TAG, "获取趋势数据失败: " + error);
                        emitter.onError(new Exception(error));
                    }
                });
            } catch (Exception e) {
                LogUtils.e(TAG, "创建趋势数据Observable失败", e);
                emitter.onError(e);
            }
        });
    }
} 