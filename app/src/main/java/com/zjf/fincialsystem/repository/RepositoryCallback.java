package com.zjf.fincialsystem.repository;

/**
 * 数据仓库回调接口
 * @param <T> 数据类型
 */
public interface RepositoryCallback<T> {
    
    /**
     * 成功回调
     * @param result 结果数据
     */
    void onSuccess(T result);
    
    /**
     * 错误回调
     * @param error 错误信息
     */
    void onError(String error);
    
    /**
     * 标记数据是否来自缓存
     * @param isCache 是否来自缓存
     */
    default void isCacheData(boolean isCache) {
        // 默认空实现
    }
} 