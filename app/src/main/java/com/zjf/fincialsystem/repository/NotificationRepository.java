package com.zjf.fincialsystem.repository;

import android.content.Context;

import com.zjf.fincialsystem.db.DataCacheManager;
import com.zjf.fincialsystem.model.Notification;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.network.api.NotificationApiService;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NetworkUtils;
import com.zjf.fincialsystem.utils.TokenManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 通知数据仓库
 * 负责通知数据的获取和缓存管理
 */
public class NotificationRepository {
    private static final String TAG = "NotificationRepository";

    private final Context context;
    private final NotificationApiService apiService;
    private final DataCacheManager cacheManager;

    public NotificationRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkManager.getInstance().getNotificationApiService();
        this.cacheManager = DataCacheManager.getInstance(context);
    }

    /**
     * 获取通知列表
     *
     * @param callback 回调
     */
    public void getNotifications(final RepositoryCallback<List<Notification>> callback) {
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getNotifications(1, 100, null, null, 0, TokenManager.getInstance().getUserId()).enqueue(new Callback<ApiResponse<Notification>>() {
                @Override
                public void onResponse(Call<ApiResponse<Notification>> call, Response<ApiResponse<Notification>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Notification> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            // 优先使用data字段，如果为空则使用rows字段
                            List<Notification> notifications = apiResponse.getRows();
                            // 返回数据，即使列表为空也正常返回
                            // 保存到缓存
                            cacheManager.saveNotifications(notifications);
                            // 返回数据
                            callback.onSuccess(notifications);
                        } else {
                            callback.onError(apiResponse.getMsg());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Notification>> call, Throwable t) {
                    LogUtils.e(TAG, "获取通知列表失败", t);

                    // 尝试从缓存获取
                    if (cacheManager.isCacheValid("notifications")) {
                        List<Notification> cachedNotifications = cacheManager.getNotifications();
                        if (cachedNotifications != null && !cachedNotifications.isEmpty()) {
                            callback.onSuccess(cachedNotifications);

                            // 标记为从缓存获取
                            callback.isCacheData(true);
                        } else {
                            callback.onError("获取通知数据失败: " + t.getMessage());
                        }
                    } else {
                        callback.onError("获取通知数据失败: " + t.getMessage());
                    }
                }
            });
        } else {
            // 无网络连接，从缓存获取数据
            if (cacheManager.isCacheValid("notifications")) {
                List<Notification> cachedNotifications = cacheManager.getNotifications();
                if (cachedNotifications != null && !cachedNotifications.isEmpty()) {
                    callback.onSuccess(cachedNotifications);
                    // 标记为从缓存获取
                    callback.isCacheData(true);
                } else {
                    callback.onError("无网络连接且无缓存数据");
                }
            } else {
                callback.onError("无网络连接且无缓存数据");
            }
        }
    }

    /**
     * 获取未读通知
     *
     * @param userId   用户ID
     * @param callback 回调
     */
    public void getUnreadNotifications(Long userId, final RepositoryCallback<Notification> callback) {
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getUnreadNotifications(userId).enqueue(new Callback<ApiResponse<Notification>>() {
                @Override
                public void onResponse(Call<ApiResponse<Notification>> call, Response<ApiResponse<Notification>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Notification> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            // 优先使用data字段，如果为空则使用rows字段
                            List<Notification> notifications = apiResponse.getRows();
//                            callback.onSuccess(notifications);
                        } else {
                            callback.onError(apiResponse.getMsg());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Notification>> call, Throwable t) {
                    LogUtils.e(TAG, "获取未读通知失败", t);
                    callback.onError("获取未读通知失败: " + t.getMessage());
                }
            });
        } else {
            callback.onError("无网络连接，无法获取未读通知");
        }
    }

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     * @param callback       回调
     */
    public void markAsRead(long notificationId, final RepositoryCallback<Boolean> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法标记通知为已读");
            return;
        }

        apiService.markAsRead(notificationId).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // 更新缓存
                        updateCachedNotificationReadStatus(notificationId, 1);

                        callback.onSuccess(true);
                    } else {
                        callback.onError(apiResponse.getMsg());
                    }
                } else {
                    callback.onError("标记通知为已读失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                LogUtils.e(TAG, "标记通知为已读失败", t);
                callback.onError("标记通知为已读失败: " + t.getMessage());
            }
        });
    }

    /**
     * 批量标记通知为已读
     *
     * @param userId   用户ID
     * @param callback 回调
     */
    public void markAllAsRead(long userId, ArrayList<Long> ids, final RepositoryCallback<Boolean> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法标记所有通知为已读");
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("ids", ids);
        apiService.batchMarkAsRead(params).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // 更新缓存中所有通知为已读
                        updateAllCachedNotificationsReadStatus(userId, 1);

                        callback.onSuccess(true);
                    } else {
                        callback.onError(apiResponse.getMsg());
                    }
                } else {
                    callback.onError("标记所有通知为已读失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                LogUtils.e(TAG, "标记所有通知为已读失败", t);
                callback.onError("标记所有通知为已读失败: " + t.getMessage());
            }
        });
    }

    /**
     * 删除通知
     *
     * @param notificationId 通知ID
     * @param callback       回调
     */
    public void deleteNotification(long notificationId, final RepositoryCallback<Boolean> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法删除通知");
            return;
        }

        apiService.deleteNotification(String.valueOf(notificationId)).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // 更新缓存
                        removeCachedNotification(notificationId);

                        callback.onSuccess(true);
                    } else {
                        callback.onError(apiResponse.getMsg());
                    }
                } else {
                    callback.onError("删除通知失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                LogUtils.e(TAG, "删除通知失败", t);
                callback.onError("删除通知失败: " + t.getMessage());
            }
        });
    }

    /**
     * 更新缓存中通知的已读状态
     */
    private void updateCachedNotificationReadStatus(long notificationId, Integer isRead) {
        List<Notification> cachedNotifications = cacheManager.getNotifications();
        if (cachedNotifications != null) {
            for (Notification notification : cachedNotifications) {
                if (notification.getId() == notificationId) {
                    notification.setRead(isRead);
                    break;
                }
            }
            cacheManager.saveNotifications(cachedNotifications);
        }
    }

    /**
     * 更新缓存中所有通知的已读状态
     */
    private void updateAllCachedNotificationsReadStatus(long userId, Integer isRead) {
        List<Notification> cachedNotifications = cacheManager.getNotifications();
        if (cachedNotifications != null) {
            for (Notification notification : cachedNotifications) {
                if (notification.getUserId() == userId) {
                    notification.setRead(isRead);
                }
            }
            cacheManager.saveNotifications(cachedNotifications);
        }
    }

    /**
     * 从缓存中移除通知
     */
    private void removeCachedNotification(long notificationId) {
        List<Notification> cachedNotifications = cacheManager.getNotifications();
        if (cachedNotifications != null) {
            List<Notification> updatedNotifications = new ArrayList<>();
            for (Notification notification : cachedNotifications) {
                if (notification.getId() != notificationId) {
                    updatedNotifications.add(notification);
                }
            }
            cacheManager.saveNotifications(updatedNotifications);
        }
    }
}