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
     */
    @GET("finance/category/list")
    Call<ApiResponse<List<Category>>> getCategories(@Query("pageNum") Integer pageNum, 
                                                   @Query("pageSize") Integer pageSize,
                                                   @Query("name") String name,
                                                   @Query("type") Integer type);
    
    /**
     * 获取分类详情
     */
    @GET("finance/category/{id}")
    Call<ApiResponse<Category>> getCategoryById(@Path("id") long id);
    
    /**
     * 获取指定类型的分类
     */
    @GET("finance/category/type/{type}")
    Call<ApiResponse<List<Category>>> getCategoriesByType(@Path("type") Integer type);
    
    /**
     * 添加分类
     */
    @POST("finance/category")
    Call<ApiResponse<String>> addCategory(@Body AddCategoryRequest request);
    
    /**
     * 更新分类
     */
    @PUT("finance/category")
    Call<ApiResponse<String>> updateCategory(@Body Category category);
    
    /**
     * 删除分类
     */
    @DELETE("finance/category/{ids}")
    Call<ApiResponse<String>> deleteCategory(@Path("ids") String ids);
} 