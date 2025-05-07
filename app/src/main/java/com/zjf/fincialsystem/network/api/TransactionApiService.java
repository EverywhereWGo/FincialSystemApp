package com.zjf.fincialsystem.network.api;

import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.model.AddTransactionRequest;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.network.model.ImageUploadResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
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
    Call<ApiResponse<Transaction>> getTransactions(
            @Query("userId") Long userId,
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
     * 更新交易记录（使用AddTransactionRequest对象）
     */
    @PUT("finance/transaction")
    Call<ApiResponse<String>> updateTransaction(@Body AddTransactionRequest request);
    
    /**
     * 删除交易记录
     */
    @DELETE("finance/transaction/{ids}")
    Call<ApiResponse<String>> deleteTransaction(@Path("ids") String ids);

    /**
     * 获取交易分类
     * @param type 分类类型：0-支出分类，1-收入分类
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @param name 分类名称（模糊查询）
     * @return API响应
     */
    @GET("finance/category/list")
    Call<ApiResponse<Category>> getCategories(
            @Query("pageNum") Integer pageNum, 
            @Query("pageSize") Integer pageSize,
            @Query("name") String name,
            @Query("type") Integer type);
            
    /**
     * 上传交易相关图片
     * @param file 图片文件
     * @param transactionId 交易ID（可选）
     * @return 上传结果
     */
    @Multipart
    @POST("finance/transaction/image")
    Call<ApiResponse<ImageUploadResponse>> uploadTransactionImage(
            @Part MultipartBody.Part file,
            @Part("transactionId") RequestBody transactionId);
} 