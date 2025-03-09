package com.zjf.fincialsystem.network.api;

import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.model.LoginRequest;
import com.zjf.fincialsystem.network.model.LoginResponse;
import com.zjf.fincialsystem.network.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * 用户相关API接口
 */
public interface UserApiService {
    
    /**
     * 用户登录
     */
    @POST("api/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);
    
    /**
     * 用户注册
     */
    @POST("api/register")
    Call<ApiResponse<String>> register(@Body RegisterRequest request);
    
    /**
     * 获取用户信息
     */
    @GET("api/user/{userId}")
    Call<ApiResponse<Object>> getUserInfo(@Path("userId") long userId);
} 