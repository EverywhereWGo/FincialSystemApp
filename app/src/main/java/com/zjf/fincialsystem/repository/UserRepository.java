package com.zjf.fincialsystem.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.zjf.fincialsystem.model.User;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.network.api.UserApiService;
import com.zjf.fincialsystem.network.model.LoginRequest;
import com.zjf.fincialsystem.network.model.LoginResponse;
import com.zjf.fincialsystem.network.model.RegisterRequest;
import com.zjf.fincialsystem.utils.DeviceUtils;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NetworkUtils;
import com.zjf.fincialsystem.utils.TokenManager;
import com.zjf.fincialsystem.utils.SharedPreferencesUtils;
import com.zjf.fincialsystem.utils.Constants;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.JsonSyntaxException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户数据仓库
 * 负责用户数据的获取和本地存储
 */
public class UserRepository {
    private static final String TAG = "UserRepository";
    
    private final Context context;
    private final UserApiService apiService;
    private final TokenManager tokenManager;
    
    public UserRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkManager.getInstance().getUserApiService();
        this.tokenManager = TokenManager.getInstance();
    }
    
    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     * @param callback 回调
     */
    public void login(String username, String password, final RepositoryCallback<LoginResponse> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法登录");
            return;
        }
        
        // 创建登录请求
        LoginRequest request = new LoginRequest(username, password);
        request.setDeviceInfo(DeviceUtils.getDeviceInfo());
        
        // 发起登录请求
        apiService.login(request).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LoginResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        LoginResponse loginResponse = apiResponse.getData();
                        
                        // 保存Token和用户信息到TokenManager
                        tokenManager.saveToken(loginResponse.getToken(), loginResponse.getExpiryTime());
                        
                        // 额外保存到SharedPreferences
                        SharedPreferencesUtils.setStringPreference(
                                context, 
                                Constants.PREF_NAME, 
                                Constants.PREF_KEY_TOKEN, 
                                loginResponse.getToken()
                        );
                        
                        SharedPreferencesUtils.setLongPreference(
                                context,
                                Constants.PREF_NAME,
                                Constants.PREF_KEY_TOKEN_EXPIRY,
                                loginResponse.getExpiryTime()
                        );
                        
                        LogUtils.d(TAG, "登录成功，保存Token: " + loginResponse.getToken() +
                                ", 过期时间: " + new Date(loginResponse.getExpiryTime()));
                        
                        // 返回数据
                        callback.onSuccess(loginResponse);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("登录失败，请稍后重试");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                String errorMessage;
                if (t instanceof JsonSyntaxException) {
                    errorMessage = "数据格式错误，请联系管理员";
                    LogUtils.e(TAG, "登录数据解析失败: " + t.getMessage(), t);
                } else if (!NetworkUtils.isNetworkAvailable(context)) {
                    errorMessage = "网络不可用，请检查网络连接";
                } else {
                    errorMessage = "登录失败: " + t.getMessage();
                }
                LogUtils.e(TAG, "登录失败", t);
                callback.onError(errorMessage);
            }
        });
    }
    
    /**
     * 注册
     * @param request 注册请求参数
     * @param callback 回调
     */
    public void register(RegisterRequest request, final RepositoryCallback<String> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法注册");
            return;
        }
        
        // 发起注册请求
        apiService.register(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        String message = apiResponse.getData();
                        callback.onSuccess(message);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("注册失败，请稍后重试");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                LogUtils.e(TAG, "注册失败", t);
                callback.onError("注册失败: " + t.getMessage());
            }
        });
    }
    
    /**
     * 获取用户信息
     * @param userId 用户ID
     * @param callback 回调
     */
    @SuppressWarnings("unchecked")
    public void getUserInfo(long userId, final RepositoryCallback<User> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法获取用户信息");
            return;
        }
        
        // 检查Token是否有效
        if (!tokenManager.isLoggedIn()) {
            callback.onError("未登录或登录已过期");
            return;
        }
        
        // 发起获取用户信息请求
        apiService.getUserInfo(userId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        try {
                            // 获取用户信息（实际项目中需要根据返回的JSON数据解析成User对象）
                            User user = (User) apiResponse.getData();
                            callback.onSuccess(user);
                        } catch (Exception e) {
                            LogUtils.e(TAG, "解析用户信息失败", e);
                            callback.onError("获取用户信息失败: " + e.getMessage());
                        }
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("获取用户信息失败，请稍后重试");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                LogUtils.e(TAG, "获取用户信息失败", t);
                callback.onError("获取用户信息失败: " + t.getMessage());
            }
        });
    }
    
    /**
     * 退出登录
     */
    public void logout() {
        // 清除Token
        tokenManager.clearToken();
    }
    
    /**
     * 检查是否已登录
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        return tokenManager.isLoggedIn();
    }
    
    /**
     * 注销账户
     * @param userId 用户ID
     * @param callback 回调
     */
    public void deleteAccount(long userId, final RepositoryCallback<Boolean> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法注销账户");
            return;
        }
        
        // 检查Token是否有效
        if (!tokenManager.isLoggedIn()) {
            callback.onError("未登录或登录已过期");
            return;
        }
        
        // 实际项目中应调用真实API，这里模拟一个成功的响应
        // 模拟网络延迟
        new android.os.Handler().postDelayed(() -> {
            try {
                // 模拟成功响应
                callback.onSuccess(true);
                
                // 注销成功后清除token
                tokenManager.clearToken();
            } catch (Exception e) {
                LogUtils.e(TAG, "注销账户失败", e);
                callback.onError("注销账户失败: " + e.getMessage());
            }
        }, 1500); // 1.5秒延迟模拟网络请求
        
        /* 实际API调用应该类似这样：
        apiService.deleteAccount(userId).enqueue(new Callback<ApiResponse<Boolean>>() {
            @Override
            public void onResponse(Call<ApiResponse<Boolean>> call, Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Boolean> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // 清除Token
                        tokenManager.clearToken();
                        callback.onSuccess(true);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("注销账户失败，请稍后重试");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Boolean>> call, Throwable t) {
                LogUtils.e(TAG, "注销账户失败", t);
                callback.onError("注销账户失败: " + t.getMessage());
            }
        });
        */
    }

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @param callback 回调
     */
    public void changePassword(long userId, String oldPassword, String newPassword, final RepositoryCallback<Boolean> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法修改密码");
            return;
        }
        
        // 检查Token是否有效
        if (!tokenManager.isLoggedIn()) {
            callback.onError("未登录或登录已过期");
            return;
        }
        
        // 准备请求参数
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("userId", String.valueOf(userId));
        passwordData.put("oldPassword", oldPassword);
        passwordData.put("newPassword", newPassword);
        
        // 实际项目中应调用真实API，这里模拟一个成功的响应
        // 模拟网络延迟
        new android.os.Handler().postDelayed(() -> {
            try {
                // 模拟密码验证 (实际中应该由服务器验证)
                boolean verified = true; // 模拟验证成功
                
                if (verified) {
                    // 模拟成功响应
                    callback.onSuccess(true);
                } else {
                    callback.onError("原密码不正确");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "修改密码失败", e);
                callback.onError("修改密码失败: " + e.getMessage());
            }
        }, 1500); // 1.5秒延迟模拟网络请求
        
        /* 实际API调用应该类似这样：
        apiService.changePassword(passwordData).enqueue(new Callback<ApiResponse<Boolean>>() {
            @Override
            public void onResponse(Call<ApiResponse<Boolean>> call, Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Boolean> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(true);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("修改密码失败，请稍后重试");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Boolean>> call, Throwable t) {
                LogUtils.e(TAG, "修改密码失败", t);
                callback.onError("修改密码失败: " + t.getMessage());
            }
        });
        */
    }

    /**
     * 更新用户资料
     * @param user 用户对象
     * @param callback 回调
     */
    public void updateUserProfile(User user, final RepositoryCallback<Boolean> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法更新用户资料");
            return;
        }
        
        // 检查Token是否有效
        if (!tokenManager.isLoggedIn()) {
            callback.onError("未登录或登录已过期");
            return;
        }
        
        // 实际项目中应调用真实API，这里模拟一个成功的响应
        // 模拟网络延迟
        new android.os.Handler().postDelayed(() -> {
            try {
                // 模拟成功响应
                callback.onSuccess(true);
            } catch (Exception e) {
                LogUtils.e(TAG, "更新用户资料失败", e);
                callback.onError("更新用户资料失败: " + e.getMessage());
            }
        }, 1500); // 1.5秒延迟模拟网络请求
        
        /* 实际API调用应该类似这样：
        apiService.updateUserProfile(user).enqueue(new Callback<ApiResponse<Boolean>>() {
            @Override
            public void onResponse(Call<ApiResponse<Boolean>> call, Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Boolean> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(true);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("更新用户资料失败，请稍后重试");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Boolean>> call, Throwable t) {
                LogUtils.e(TAG, "更新用户资料失败", t);
                callback.onError("更新用户资料失败: " + t.getMessage());
            }
        });
        */
    }
} 