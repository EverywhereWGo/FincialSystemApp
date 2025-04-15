package com.zjf.fincialsystem.network.api;

import com.zjf.fincialsystem.model.Budget;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.model.AddBudgetRequest;

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
 * 预算相关API接口
 */
public interface BudgetApiService {

    /**
     * 获取预算列表
     */
    @GET("finance/budget/list")
    Call<ApiResponse<Budget>> getBudgets(
            @Query("pageNum") Integer pageNum,
            @Query("pageSize") Integer pageSize,
            @Query("categoryId") Long categoryId,
            @Query("month") Integer month,
            @Query("userId") Long userId);

    /**
     * 获取预算详情
     */
    @GET("finance/budget/{id}")
    Call<ApiResponse<Budget>> getBudgetDetail(@Path("id") long id);

    /**
     * 获取某月的预算
     */
    @GET("finance/budget/month")
    Call<ApiResponse<List<Map<String, Object>>>> getMonthBudgets(
            @Query("userId") Long userId,
            @Query("month") String month);

    /**
     * 获取预算警告
     */
    @GET("finance/budget/warning")
    Call<ApiResponse<List<Map<String, Object>>>> getWarningBudgets(
            @Query("userId") Long userId,
            @Query("month") String month);

    /**
     * 添加预算
     */
    @POST("finance/budget")
    Call<ApiResponse<String>> addBudget(@Body AddBudgetRequest request);

    /**
     * 更新预算
     */
    @PUT("finance/budget")
    Call<ApiResponse<String>> updateBudget(@Body Budget budget);

    /**
     * 删除预算
     */
    @DELETE("finance/budget/{ids}")
    Call<ApiResponse<String>> deleteBudget(@Path("ids") String ids);
} 