package com.zjf.fincialsystem.network.api;

import com.zjf.fincialsystem.model.Budget;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.model.AddBudgetRequest;

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
 * 预算相关API接口
 */
public interface BudgetApiService {
    
    /**
     * 获取预算列表
     * @param period 预算周期（月度/年度）
     */
    @GET("api/budgets")
    Call<ApiResponse<List<Budget>>> getBudgets(@Query("period") String period);
    
    /**
     * 获取当前预算
     */
    @GET("api/budgets/current")
    Call<ApiResponse<List<Budget>>> getCurrentBudgets();
    
    /**
     * 添加预算
     */
    @POST("api/budgets")
    Call<ApiResponse<Budget>> addBudget(@Body AddBudgetRequest request);
    
    /**
     * 更新预算
     */
    @PUT("api/budgets/{budgetId}")
    Call<ApiResponse<Budget>> updateBudget(@Path("budgetId") long budgetId, @Body Budget budget);
    
    /**
     * 删除预算
     */
    @DELETE("api/budgets/{budgetId}")
    Call<ApiResponse<Boolean>> deleteBudget(@Path("budgetId") long budgetId);
} 