package com.zjf.fincialsystem.repository;

import android.content.Context;

import com.zjf.fincialsystem.db.DataCacheManager;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.network.api.TransactionApiService;
import com.zjf.fincialsystem.network.model.AddTransactionRequest;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 交易记录数据仓库
 * 负责交易记录数据的获取和缓存管理
 */
public class TransactionRepository {
    private static final String TAG = "TransactionRepository";
    
    private final Context context;
    private final TransactionApiService apiService;
    private final DataCacheManager cacheManager;
    
    public TransactionRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkManager.getInstance().getTransactionApiService();
        this.cacheManager = DataCacheManager.getInstance(context);
    }
    
    /**
     * 获取交易记录列表
     * @param callback 回调
     */
    public void getTransactions(final RepositoryCallback<List<Transaction>> callback) {
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getTransactions().enqueue(new Callback<ApiResponse<List<Transaction>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Transaction>>> call, Response<ApiResponse<List<Transaction>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Transaction>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            List<Transaction> transactions = apiResponse.getData();
                            
                            // 保存到缓存
                            cacheManager.saveTransactions(transactions);
                            
                            // 返回数据
                            callback.onSuccess(transactions);
                        } else {
                            callback.onError(apiResponse.getMessage());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Transaction>>> call, Throwable t) {
                    LogUtils.e(TAG, "获取交易记录列表失败", t);
                    
                    // 网络请求失败，尝试从缓存获取
                    if (cacheManager.isCacheValid("transactions")) {
                        List<Transaction> cachedTransactions = cacheManager.getTransactions();
                        if (cachedTransactions != null && !cachedTransactions.isEmpty()) {
                            callback.onSuccess(cachedTransactions);
                            
                            // 标记为从缓存获取
                            callback.isCacheData(true);
                        } else {
                            callback.onError("获取交易记录数据失败: " + t.getMessage());
                        }
                    } else {
                        callback.onError("获取交易记录数据失败: " + t.getMessage());
                    }
                }
            });
        } else {
            // 无网络连接，从缓存获取数据
            if (cacheManager.isCacheValid("transactions")) {
                List<Transaction> cachedTransactions = cacheManager.getTransactions();
                if (cachedTransactions != null && !cachedTransactions.isEmpty()) {
                    callback.onSuccess(cachedTransactions);
                    
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
     * 按类型获取交易记录列表
     * @param type 交易类型：0-支出，1-收入
     * @param callback 回调
     */
    public void getTransactionsByType(int type, final RepositoryCallback<List<Transaction>> callback) {
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getTransactionsByType(type).enqueue(new Callback<ApiResponse<List<Transaction>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Transaction>>> call, Response<ApiResponse<List<Transaction>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Transaction>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            List<Transaction> transactions = apiResponse.getData();
                            
                            // 返回数据
                            callback.onSuccess(transactions);
                        } else {
                            callback.onError(apiResponse.getMessage());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Transaction>>> call, Throwable t) {
                    LogUtils.e(TAG, "获取交易记录列表失败", t);
                    
                    // 网络请求失败，尝试从缓存获取
                    if (cacheManager.isCacheValid("transactions")) {
                        List<Transaction> allTransactions = cacheManager.getTransactions();
                        if (allTransactions != null && !allTransactions.isEmpty()) {
                            // 过滤交易类型
                            List<Transaction> filteredTransactions = new ArrayList<>();
                            for (Transaction transaction : allTransactions) {
                                if (transaction.getType() == type) {
                                    filteredTransactions.add(transaction);
                                }
                            }
                            
                            callback.onSuccess(filteredTransactions);
                            
                            // 标记为从缓存获取
                            callback.isCacheData(true);
                        } else {
                            callback.onError("获取交易记录数据失败: " + t.getMessage());
                        }
                    } else {
                        callback.onError("获取交易记录数据失败: " + t.getMessage());
                    }
                }
            });
        } else {
            // 无网络连接，从缓存获取数据
            if (cacheManager.isCacheValid("transactions")) {
                List<Transaction> allTransactions = cacheManager.getTransactions();
                if (allTransactions != null && !allTransactions.isEmpty()) {
                    // 过滤交易类型
                    List<Transaction> filteredTransactions = new ArrayList<>();
                    for (Transaction transaction : allTransactions) {
                        if (transaction.getType() == type) {
                            filteredTransactions.add(transaction);
                        }
                    }
                    
                    callback.onSuccess(filteredTransactions);
                    
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
     * 添加交易记录
     * @param request 添加交易记录请求参数
     * @param callback 回调
     */
    public void addTransaction(AddTransactionRequest request, final RepositoryCallback<Transaction> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法添加交易记录");
            return;
        }
        
        apiService.addTransaction(request).enqueue(new Callback<ApiResponse<Transaction>>() {
            @Override
            public void onResponse(Call<ApiResponse<Transaction>> call, Response<ApiResponse<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Transaction> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Transaction transaction = apiResponse.getData();
                        
                        // 更新缓存
                        List<Transaction> cachedTransactions = cacheManager.getTransactions();
                        cachedTransactions.add(transaction);
                        cacheManager.saveTransactions(cachedTransactions);
                        
                        // 返回数据
                        callback.onSuccess(transaction);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Transaction>> call, Throwable t) {
                LogUtils.e(TAG, "添加交易记录失败", t);
                callback.onError("添加交易记录失败: " + t.getMessage());
            }
        });
    }
    
    /**
     * 更新交易记录
     * @param transaction 要更新的交易记录
     * @param callback 回调
     */
    public void updateTransaction(Transaction transaction, final RepositoryCallback<Transaction> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法更新交易记录");
            return;
        }
        
        apiService.updateTransaction(transaction.getId(), transaction).enqueue(new Callback<ApiResponse<Transaction>>() {
            @Override
            public void onResponse(Call<ApiResponse<Transaction>> call, Response<ApiResponse<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Transaction> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Transaction updatedTransaction = apiResponse.getData();
                        
                        // 更新缓存
                        List<Transaction> cachedTransactions = cacheManager.getTransactions();
                        for (int i = 0; i < cachedTransactions.size(); i++) {
                            if (cachedTransactions.get(i).getId() == updatedTransaction.getId()) {
                                cachedTransactions.set(i, updatedTransaction);
                                break;
                            }
                        }
                        cacheManager.saveTransactions(cachedTransactions);
                        
                        // 返回数据
                        callback.onSuccess(updatedTransaction);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Transaction>> call, Throwable t) {
                LogUtils.e(TAG, "更新交易记录失败", t);
                callback.onError("更新交易记录失败: " + t.getMessage());
            }
        });
    }
    
    /**
     * 删除交易记录
     * @param transactionId 要删除的交易记录ID
     * @param callback 回调
     */
    public void deleteTransaction(long transactionId, final RepositoryCallback<Boolean> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法删除交易记录");
            return;
        }
        
        apiService.deleteTransaction(transactionId).enqueue(new Callback<ApiResponse<Boolean>>() {
            @Override
            public void onResponse(Call<ApiResponse<Boolean>> call, Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Boolean> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData()) {
                        // 更新缓存
                        List<Transaction> cachedTransactions = cacheManager.getTransactions();
                        for (int i = 0; i < cachedTransactions.size(); i++) {
                            if (cachedTransactions.get(i).getId() == transactionId) {
                                cachedTransactions.remove(i);
                                break;
                            }
                        }
                        cacheManager.saveTransactions(cachedTransactions);
                        
                        // 返回数据
                        callback.onSuccess(true);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Boolean>> call, Throwable t) {
                LogUtils.e(TAG, "删除交易记录失败", t);
                callback.onError("删除交易记录失败: " + t.getMessage());
            }
        });
    }
} 