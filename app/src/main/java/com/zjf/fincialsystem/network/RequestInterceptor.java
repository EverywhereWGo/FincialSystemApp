package com.zjf.fincialsystem.network;

import com.zjf.fincialsystem.utils.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 请求拦截器
 * 用于添加通用请求头，如Token、设备信息等
 */
public class RequestInterceptor implements Interceptor {
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // 构建新的请求，添加通用请求头
        Request.Builder requestBuilder = originalRequest.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "FinanceApp-Android");
        
        // 如果有Token，添加到请求头
        String token = TokenManager.getInstance().getToken();
        if (token != null && !token.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }
        
        Request request = requestBuilder.build();
        return chain.proceed(request);
    }
} 