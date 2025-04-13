package com.zjf.fincialsystem.network.api;

import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.model.AddTransactionRequest;
import com.zjf.fincialsystem.model.Category;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 交易记录相关API接口
 */
public interface TransactionApiService {
    
    /**
     * 获取交易记录列表
     */
    @GET("finance/transaction/list")
    Call<ApiResponse<List<Transaction>>> getTransactions(
            @Query("pageNum") Integer pageNum,
            @Query("pageSize") Integer pageSize,
            @Query("type") Integer type,
            @Query("categoryId") Long categoryId,
            @Query("transactionTime") Long transactionTime,
            @Query("note") String note);
    
    /**
     * 获取交易记录详情
     */
    @GET("finance/transaction/{id}")
    Call<ApiResponse<Transaction>> getTransactionDetail(@Path("id") long id);
    
    /**
     * 按月获取交易记录
     */
    @GET("finance/transaction/month")
    Call<ApiResponse<List<Transaction>>> getTransactionsByMonth(
            @Query("userId") Long userId,
            @Query("startTime") Long startTime,
            @Query("endTime") Long endTime,
            @Query("type") Integer type);
    
    /**
     * 获取年度交易统计
     */
    @GET("finance/transaction/stat/year")
    Call<ApiResponse<List<Map<String, Object>>>> getYearlyStats(
            @Query("userId") Long userId,
            @Query("year") Integer year);
    
    /**
     * 获取月度交易统计
     */
    @GET("finance/transaction/stat/month")
    Call<ApiResponse<List<Map<String, Object>>>> getMonthlyStats(
            @Query("userId") Long userId,
            @Query("startTime") Long startTime,
            @Query("endTime") Long endTime,
            @Query("type") Integer type);
    
    /**
     * 获取月度收支总额
     */
    @GET("finance/transaction/stat/amount")
    Call<ApiResponse<Map<String, Object>>> getMonthAmount(
            @Query("userId") Long userId,
            @Query("startTime") Long startTime,
            @Query("endTime") Long endTime);
    
    /**
     * 添加交易记录
     */
    @POST("finance/transaction")
    Call<ApiResponse<String>> addTransaction(@Body AddTransactionRequest request);
    
    /**
     * 更新交易记录
     */
    @PUT("finance/transaction")
    Call<ApiResponse<String>> updateTransaction(@Body Transaction transaction);
    
    /**
     * 删除交易记录
     */
    @DELETE("finance/transaction/{ids}")
    Call<ApiResponse<String>> deleteTransaction(@Path("ids") String ids);

    /**
     * 获取交易分类
     * @param type 分类类型：0-支出分类，1-收入分类
     * @return API响应
     */
    @GET("finance/category/list")
    Call<ApiResponse<List<Category>>> getCategories(@Query("type") Integer type);
} 