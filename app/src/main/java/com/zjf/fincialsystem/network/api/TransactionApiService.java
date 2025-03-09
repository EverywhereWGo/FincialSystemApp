package com.zjf.fincialsystem.network.api;

import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.model.AddTransactionRequest;

import java.util.Date;
import java.util.List;

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
    @GET("api/transactions")
    Call<ApiResponse<List<Transaction>>> getTransactions();
    
    /**
     * 按类型获取交易记录列表
     */
    @GET("api/transactions")
    Call<ApiResponse<List<Transaction>>> getTransactionsByType(@Query("type") int type);
    
    /**
     * 按日期范围获取交易记录
     */
    @GET("api/transactions/date")
    Call<ApiResponse<List<Transaction>>> getTransactionsByDateRange(
            @Query("startDate") long startDateMillis,
            @Query("endDate") long endDateMillis);
    
    /**
     * 获取最近交易记录
     */
    @GET("api/transactions/recent")
    Call<ApiResponse<List<Transaction>>> getRecentTransactions(@Query("limit") int limit);
    
    /**
     * 添加交易记录
     */
    @POST("api/transactions")
    Call<ApiResponse<Transaction>> addTransaction(@Body AddTransactionRequest request);
    
    /**
     * 更新交易记录
     */
    @PUT("api/transactions/{transactionId}")
    Call<ApiResponse<Transaction>> updateTransaction(
            @Path("transactionId") long transactionId,
            @Body Transaction transaction);
    
    /**
     * 删除交易记录
     */
    @DELETE("api/transactions/{transactionId}")
    Call<ApiResponse<Boolean>> deleteTransaction(@Path("transactionId") long transactionId);
    
    /**
     * 获取收入/支出统计
     */
    @GET("api/transactions/statistics")
    Call<ApiResponse<Object>> getStatistics(
            @Query("startDate") long startDateMillis,
            @Query("endDate") long endDateMillis);
} 