package com.zjf.fincialsystem.network.api;

import com.zjf.fincialsystem.model.Notification;
import com.zjf.fincialsystem.network.ApiResponse;

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
 * 通知相关API接口
 */
public interface NotificationApiService {
    
    /**
     * 获取通知列表
     */
    @GET("finance/notification/list")
    Call<ApiResponse<List<Notification>>> getNotifications(
            @Query("pageNum") Integer pageNum,
            @Query("pageSize") Integer pageSize,
            @Query("title") String title,
            @Query("type") Integer type,
            @Query("isRead") Integer isRead);
    
    /**
     * 获取通知详情
     */
    @GET("finance/notification/{id}")
    Call<ApiResponse<Notification>> getNotificationDetail(@Path("id") long id);
    
    /**
     * 获取未读通知
     */
    @GET("finance/notification/unread")
    Call<ApiResponse<List<Notification>>> getUnreadNotifications(@Query("userId") Long userId);
    
    /**
     * 添加通知
     */
    @POST("finance/notification")
    Call<ApiResponse<String>> addNotification(@Body Notification notification);
    
    /**
     * 更新通知
     */
    @PUT("finance/notification")
    Call<ApiResponse<String>> updateNotification(@Body Notification notification);
    
    /**
     * 删除通知
     */
    @DELETE("finance/notification/{ids}")
    Call<ApiResponse<String>> deleteNotification(@Path("ids") String ids);
    
    /**
     * 标记通知为已读
     */
    @PUT("finance/notification/read/{id}")
    Call<ApiResponse<String>> markAsRead(@Path("id") long id);
    
    /**
     * 批量标记通知为已读
     */
    @PUT("finance/notification/batchRead")
    Call<ApiResponse<String>> batchMarkAsRead(@Body Map<String, Object> params);
} 