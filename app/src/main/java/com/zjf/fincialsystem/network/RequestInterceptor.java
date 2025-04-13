package com.zjf.fincialsystem.network;

import com.blankj.utilcode.util.LogUtils;
import com.zjf.fincialsystem.utils.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 网络请求拦截器
 * 添加请求头信息，如Token等
 */
public class RequestInterceptor implements Interceptor {

    private static final String TAG = "RequestInterceptor";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // 构建新的请求
        Request.Builder requestBuilder = originalRequest.newBuilder();
        
        // 添加通用头
        requestBuilder.header("Content-Type", "application/json");
        requestBuilder.header("Accept", "application/json");
        
        // 如果有Token，添加到请求头
        String token = TokenManager.getInstance().getToken();
        if (token != null && !token.isEmpty()) {
            // 添加Bearer前缀，符合接口文档要求
            requestBuilder.header("Authorization", "Bearer " + token);
            LogUtils.d(TAG, "添加认证Token到请求头: Bearer " + token.substring(0, Math.min(15, token.length())) + "...");
        } else {
            LogUtils.w(TAG, "请求没有认证Token，可能需要登录");
        }
        
        // 添加设备信息
        requestBuilder.header("User-Agent", System.getProperty("http.agent"));
        
        Request newRequest = requestBuilder.build();
        LogUtils.d(TAG, "发送请求到: " + newRequest.url() + " 方法: " + newRequest.method());
        
        // 执行请求
        Response response = chain.proceed(newRequest);
        
        // 记录响应状态
        LogUtils.d(TAG, "收到响应，状态码: " + response.code() + "，URL: " + response.request().url());
        
        // 检查是否需要重新登录
        if (response.code() == 401) {
            LogUtils.w(TAG, "收到401未授权响应，可能需要重新登录或Token无效");
        }
        
        return response;
    }
} 