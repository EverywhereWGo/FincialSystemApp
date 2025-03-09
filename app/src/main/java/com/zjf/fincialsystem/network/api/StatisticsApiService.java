package com.zjf.fincialsystem.network.api;

import com.zjf.fincialsystem.network.ApiResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * 统计数据相关API接口
 */
public interface StatisticsApiService {
    
    /**
     * 获取收入支出概览
     * @param period 统计周期：daily, weekly, monthly, yearly
     */
    @GET("api/statistics/overview")
    Call<ApiResponse<Map<String, Object>>> getOverview(@Query("period") String period);
    
    /**
     * 获取收入分类统计
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    @GET("api/statistics/income-by-category")
    Call<ApiResponse<Map<String, Object>>> getIncomeByCategory(
            @Query("startDate") long startDate,
            @Query("endDate") long endDate);
    
    /**
     * 获取支出分类统计
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    @GET("api/statistics/expense-by-category")
    Call<ApiResponse<Map<String, Object>>> getExpenseByCategory(
            @Query("startDate") long startDate,
            @Query("endDate") long endDate);
    
    /**
     * 获取日期趋势统计
     * @param type 交易类型：0-支出，1-收入
     * @param period 统计周期：daily, weekly, monthly, yearly
     */
    @GET("api/statistics/trend")
    Call<ApiResponse<Map<String, Object>>> getTrend(
            @Query("type") int type,
            @Query("period") String period);
    
    /**
     * 获取预算使用统计
     */
    @GET("api/statistics/budget-usage")
    Call<ApiResponse<Map<String, Object>>> getBudgetUsage();
} 