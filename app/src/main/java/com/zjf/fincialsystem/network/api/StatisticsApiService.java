package com.zjf.fincialsystem.network.api;

import com.zjf.fincialsystem.network.ApiResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * 统计数据相关API接口
 */
public interface StatisticsApiService {
    
    /**
     * 获取分类支出统计
     */
    @GET("finance/statistic/category")
    Call<ApiResponse<List<Map<String, Object>>>> getCategoryStatistics(
            @Query("userId") Long userId,
            @Query("startTime") Long startTime,
            @Query("endTime") Long endTime,
            @Query("type") Integer type);
    
    /**
     * 获取年度收支统计
     */
    @GET("finance/statistic/year")
    Call<ApiResponse<List<Map<String, Object>>>> getYearStatistics(
            @Query("userId") Long userId,
            @Query("year") Integer year);
    
    /**
     * 获取收支趋势
     */
    @GET("finance/statistic/trend")
    Call<ApiResponse<List<Map<String, Object>>>> getTrend(
            @Query("userId") Long userId,
            @Query("months") Integer months);
    
    /**
     * 获取预算执行情况
     */
    @GET("finance/statistic/budget")
    Call<ApiResponse<Map<String, Object>>> getBudgetStatistics(
            @Query("userId") Long userId,
            @Query("month") String month);
    
    /**
     * 获取顺序记录
     */
    @GET("finance/statistic/topTransactions")
    Call<ApiResponse<List<Map<String, Object>>>> getTopTransactions(
            @Query("userId") Long userId,
            @Query("type") Integer type,
            @Query("startTime") Long startTime,
            @Query("endTime") Long endTime,
            @Query("limit") Integer limit);
} 