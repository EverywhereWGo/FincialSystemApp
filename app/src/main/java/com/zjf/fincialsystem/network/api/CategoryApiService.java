package com.zjf.fincialsystem.network.api;

import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.model.AddCategoryRequest;

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
 * 分类相关API接口
 */
public interface CategoryApiService {
    
    /**
     * 获取分类列表
     * @param type 分类类型（收入/支出/全部）
     */
    @GET("api/categories")
    Call<ApiResponse<List<Category>>> getCategories(@Query("type") int type);
    
    /**
     * 获取分类列表（按类型）
     * @param type 分类类型（收入/支出/全部）
     */
    @GET("api/categories/by-type")
    Call<ApiResponse<List<Category>>> getCategoriesByType(@Query("type") Integer type);
    
    /**
     * 添加分类
     */
    @POST("api/categories")
    Call<ApiResponse<Category>> addCategory(@Body AddCategoryRequest request);
    
    /**
     * 更新分类
     */
    @PUT("api/categories/{categoryId}")
    Call<ApiResponse<Category>> updateCategory(@Path("categoryId") long categoryId, @Body Category category);
    
    /**
     * 删除分类
     */
    @DELETE("api/categories/{categoryId}")
    Call<ApiResponse<Boolean>> deleteCategory(@Path("categoryId") long categoryId);
} 