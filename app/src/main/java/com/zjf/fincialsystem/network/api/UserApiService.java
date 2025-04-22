package com.zjf.fincialsystem.network.api;

import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.model.LoginRequest;
import com.zjf.fincialsystem.network.model.LoginResponse;
import com.zjf.fincialsystem.network.model.RegisterRequest;
import com.zjf.fincialsystem.network.model.UpdateProfileRequest;
import com.zjf.fincialsystem.model.User;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.Query;

import java.util.Map;

/**
 * 用户相关API接口
 */
public interface UserApiService {
    
    /**
     * 财务系统用户登录
     */
    @POST("finance/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);
    
    /**
     * 财务系统用户注册
     */
    @POST("finance/auth/register")
    Call<ApiResponse<String>> register(@Body RegisterRequest request);
    
    /**
     * 获取财务系统用户信息
     */
    @GET("finance/user/{id}")
    Call<ApiResponse<User>> getUserInfo(@Path("id") long userId);
    
    /**
     * 修改密码
     */
    @PUT("finance/user/resetPwd")
    Call<ApiResponse<Boolean>> changePassword(@Body Map<String, Object> passwordData);

    /**
     * 更新用户资料
     */
    @PUT("finance/user/profile")
    Call<ApiResponse<Boolean>> updateUserProfile(@Body UpdateProfileRequest request);
    
    /**
     * 上传用户头像
     * 根据接口文档：'/finance/user/avatar'
     * 参数: avatarfile - 文件, userId - 用户ID
     */
    @Multipart
    @POST("finance/user/avatar")
    Call<ApiResponse<Map<String, String>>> uploadAvatar(
            @Part MultipartBody.Part avatarfile,
            @Part("userId") RequestBody userId
    );
    
    /**
     * 财务系统退出登录
     */
    @POST("finance/auth/logout")
    Call<ApiResponse<String>> logout(@Body Map<String, Object> logoutRequest);
    
    /**
     * 注销账户
     * 按照接口文档使用finance/auth/unregister接口
     */
    @POST("finance/auth/unregister")
    Call<ApiResponse<Boolean>> deleteAccount(@Body Map<String, Object> accountData);
} 