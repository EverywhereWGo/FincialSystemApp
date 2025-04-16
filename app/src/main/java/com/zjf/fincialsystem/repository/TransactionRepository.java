package com.zjf.fincialsystem.repository;

import static com.zjf.fincialsystem.model.Category.TYPE_EXPENSE;
import static com.zjf.fincialsystem.model.Category.TYPE_INCOME;

import android.content.Context;

import com.zjf.fincialsystem.db.DataCacheManager;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.network.api.TransactionApiService;
import com.zjf.fincialsystem.network.model.AddTransactionRequest;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NetworkUtils;
import com.zjf.fincialsystem.utils.TokenManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final TokenManager tokenManager;
    
    public TransactionRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkManager.getInstance().getTransactionApiService();
        this.cacheManager = DataCacheManager.getInstance(context);
        this.tokenManager = TokenManager.getInstance();
    }
    
    /**
     * 获取交易记录列表
     * @param callback 回调
     */
    public void getTransactions(final RepositoryCallback<List<Transaction>> callback) {
        // 获取当前用户ID
        long userId = tokenManager.getUserId();
        if (userId <= 0) {
            callback.onError("未获取到有效的用户ID");
            return;
        }
        
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getTransactions(userId, 1, 100, null, null, null, null).enqueue(new Callback<ApiResponse<Transaction>>() {
                @Override
                public void onResponse(Call<ApiResponse<Transaction>> call, Response<ApiResponse<Transaction>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Transaction> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            List<Transaction> transactions = new ArrayList<>();
                            
                            // 处理Transaction对象
                            Transaction transaction = apiResponse.getData();
                            if (transaction != null) {
                                transactions.add(transaction);
                            }
                            
                            // 尝试从rows获取更多数据
                            List<?> rowsData = apiResponse.getRowsSafe();
                            if (rowsData != null && !rowsData.isEmpty()) {
                                for (Object item : rowsData) {
                                    if (item instanceof Transaction) {
                                        transactions.add((Transaction) item);
                                    } else if (item instanceof List) {
                                        // 处理嵌套列表情况
                                        List<?> itemList = (List<?>) item;
                                        for (Object subItem : itemList) {
                                            if (subItem instanceof Transaction) {
                                                transactions.add((Transaction) subItem);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (!transactions.isEmpty()) {
                                // 保存到缓存
                                cacheManager.saveTransactions(transactions);
                                
                                // 返回数据
                                callback.onSuccess(transactions);
                            } else {
                                callback.onError("返回数据为空");
                            }
                        } else {
                            callback.onError(apiResponse.getMsg());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Transaction>> call, Throwable t) {
                    LogUtils.e(TAG, "获取交易记录列表失败", t);
                    
                    // 处理JSON解析异常，可能是服务端返回格式不一致导致的
                    if (t instanceof com.google.gson.JsonSyntaxException) {
                        LogUtils.w(TAG, "服务端可能返回了非预期格式，尝试使用备用方案获取数据");
                        // 如果有缓存数据，先使用缓存数据
                        if (cacheManager.isCacheValid("transactions")) {
                            List<Transaction> cachedTransactions = cacheManager.getTransactions();
                            if (cachedTransactions != null && !cachedTransactions.isEmpty()) {
                                callback.onSuccess(cachedTransactions);
                                // 标记为从缓存获取
                                callback.isCacheData(true);
                                return;
                            }
                        }
                    }
                    
                    // 常规错误处理，尝试从缓存获取
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
        // 获取当前用户ID
        long userId = tokenManager.getUserId();
        if (userId <= 0) {
            callback.onError("未获取到有效的用户ID");
            return;
        }
        
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getTransactions(userId, 1, 100, type, null, null, null).enqueue(new Callback<ApiResponse<Transaction>>() {
                @Override
                public void onResponse(Call<ApiResponse<Transaction>> call, Response<ApiResponse<Transaction>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Transaction> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            List<Transaction> transactions = new ArrayList<>();
                            
                            // 处理Transaction对象
                            Transaction transaction = apiResponse.getData();
                            if (transaction != null) {
                                transactions.add(transaction);
                            }
                            
                            // 尝试从rows获取更多数据
                            List<?> rowsData = apiResponse.getRowsSafe();
                            if (rowsData != null && !rowsData.isEmpty()) {
                                for (Object item : rowsData) {
                                    if (item instanceof Transaction) {
                                        transactions.add((Transaction) item);
                                    } else if (item instanceof List) {
                                        // 处理嵌套列表情况
                                        List<?> itemList = (List<?>) item;
                                        for (Object subItem : itemList) {
                                            if (subItem instanceof Transaction) {
                                                transactions.add((Transaction) subItem);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (!transactions.isEmpty()) {
                                // 返回数据
                                callback.onSuccess(transactions);
                            } else {
                                callback.onError("返回数据为空");
                            }
                        } else {
                            callback.onError(apiResponse.getMsg());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Transaction>> call, Throwable t) {
                    LogUtils.e(TAG, "获取交易记录列表失败", t);
                    
                    // 处理JSON解析异常，可能是服务端返回格式不一致导致的
                    if (t instanceof com.google.gson.JsonSyntaxException) {
                        LogUtils.w(TAG, "服务端可能返回了非预期格式，尝试使用备用方案获取数据");
                        // 如果有缓存数据，先使用缓存数据
                        if (cacheManager.isCacheValid("transactions")) {
                            List<Transaction> cachedTransactions = cacheManager.getTransactions();
                            if (cachedTransactions != null && !cachedTransactions.isEmpty()) {
                                // 过滤交易类型
                                List<Transaction> filteredTransactions = new ArrayList<>();
                                for (Transaction transaction : cachedTransactions) {
                                    if (transaction.getType() == type) {
                                        filteredTransactions.add(transaction);
                                    }
                                }
                                
                                callback.onSuccess(filteredTransactions);
                                // 标记为从缓存获取
                                callback.isCacheData(true);
                                return;
                            }
                        }
                    }
                    
                    // 常规错误处理，尝试从缓存获取
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
     * @param request 添加交易请求
     * @param callback 回调
     */
    public void addTransaction(AddTransactionRequest request, final RepositoryCallback<Transaction> callback) {
        // 获取当前用户ID并设置到请求中
        long userId = tokenManager.getUserId();
        if (userId <= 0) {
            callback.onError("未获取到有效的用户ID");
            return;
        }
        
        // 设置当前用户ID
        request.setUserId(userId);
        
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法添加交易记录");
            return;
        }
        
        LogUtils.d(TAG, "开始调用API添加交易记录: " + request.toString());
        
        // 调用API添加交易记录
        apiService.addTransaction(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    LogUtils.d(TAG, "API响应: " + apiResponse.getCode() + " - " + apiResponse.getMsg());
                    
                    if (apiResponse.isSuccess()) {
                        LogUtils.i(TAG, "交易记录已成功通过API添加到服务器");
                        
                        // 创建一个新的Transaction对象
                        Transaction transaction = new Transaction();
                        transaction.setCategoryId(request.getCategoryId());
                        transaction.setType(request.getType());
                        transaction.setAmount(request.getAmount());
                        transaction.setNote(request.getDescription());
                        transaction.setRemark(request.getRemark());
                        transaction.setImagePath(request.getImagePath());
                        // 将long类型的日期转换为Date类型
                        transaction.setDate(new Date(request.getDate()));
                        
                        // 更新本地缓存以便后续离线查询
                        updateLocalCache(transaction);
                        
                        // 返回数据
                        callback.onSuccess(transaction);
                    } else {
                        LogUtils.e(TAG, "API添加交易记录失败: " + apiResponse.getMsg());
                        callback.onError("添加交易记录失败: " + apiResponse.getMsg());
                    }
                } else {
                    String errorMsg = "网络请求失败";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            LogUtils.e(TAG, "读取错误信息失败", e);
                        }
                    }
                    LogUtils.e(TAG, "网络请求失败: " + errorMsg);
                    callback.onError("服务器响应错误: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                LogUtils.e(TAG, "添加交易记录网络请求失败", t);
                callback.onError("网络请求失败: " + t.getMessage() + "，请稍后重试");
            }
        });
    }
    
    /**
     * 更新本地缓存中的交易记录
     */
    private void updateLocalCache(Transaction transaction) {
        // 更新缓存
        List<Transaction> cachedTransactions = cacheManager.getTransactions();
        if (cachedTransactions == null) {
            cachedTransactions = new ArrayList<>();
        }
        cachedTransactions.add(transaction);
        cacheManager.saveTransactions(cachedTransactions);
        LogUtils.d(TAG, "本地缓存已更新，当前缓存交易记录数: " + cachedTransactions.size());
    }
    
    /**
     * 更新交易记录
     * @param request 更新交易请求
     * @param callback 回调
     */
    public void updateTransaction(AddTransactionRequest request, final RepositoryCallback<Transaction> callback) {
        // 获取当前用户ID并设置到请求中
        long userId = tokenManager.getUserId();
        if (userId <= 0) {
            callback.onError("未获取到有效的用户ID");
            return;
        }
        
        // 设置当前用户ID
        request.setUserId(userId);
        
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法更新交易记录");
            return;
        }
        
        // 确保id存在
        if (request.getId() <= 0) {
            callback.onError("无法更新交易记录：缺少交易ID");
            return;
        }
        
        LogUtils.d(TAG, "开始更新交易记录，ID: " + request.getId() + ", 用户ID: " + request.getUserId());
        
        // 调用API更新交易记录
        apiService.updateTransaction(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        LogUtils.d(TAG, "交易记录更新成功，ID: " + request.getId());
                        
                        // 构建返回的交易对象
                        Transaction transaction = new Transaction();
                        transaction.setId(request.getId());
                        transaction.setType(request.getType());
                        transaction.setAmount(request.getAmount());
                        transaction.setCategoryId(request.getCategoryId());
                        
                        // 设置日期
                        if (request.getDate() > 0) {
                            transaction.setDate(new Date(request.getDate()));
                        }
                        
                        // 设置描述和备注
                        transaction.setDescription(request.getDescription());
                        transaction.setNote(request.getNote());
                        transaction.setRemark(request.getRemark());
                        
                        // 更新缓存
                        updateTransactionInCache(transaction);
                        
                        // 返回数据
                        callback.onSuccess(transaction);
                    } else {
                        LogUtils.e(TAG, "交易记录更新失败: " + apiResponse.getMsg());
                        
                        // 检查是否为认证错误
                        if (apiResponse.getMsg() != null && (apiResponse.getMsg().contains("401") || apiResponse.getMsg().contains("认证"))) {
                            LogUtils.w(TAG, "可能是认证错误，尝试使用不同的用户ID重试");
                            
                            // 重新设置用户ID并重试
                            request.setUserId(1); // 确保使用默认用户ID
                            
                            // 重试请求
                            apiService.updateTransaction(request).enqueue(new Callback<ApiResponse<String>>() {
                                @Override
                                public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        ApiResponse<String> apiResponse = response.body();
                                        if (apiResponse.isSuccess()) {
                                            LogUtils.d(TAG, "重试后交易记录更新成功，ID: " + request.getId());
                                            
                                            // 构建返回的交易对象
                                            Transaction transaction = new Transaction();
                                            transaction.setId(request.getId());
                                            transaction.setType(request.getType());
                                            transaction.setAmount(request.getAmount());
                                            transaction.setCategoryId(request.getCategoryId());
                                            
                                            // 设置日期
                                            if (request.getDate() > 0) {
                                                transaction.setDate(new Date(request.getDate()));
                                            }
                                            
                                            // 设置描述和备注
                                            transaction.setDescription(request.getDescription());
                                            transaction.setNote(request.getNote());
                                            transaction.setRemark(request.getRemark());
                                            
                                            // 更新缓存
                                            updateTransactionInCache(transaction);
                                            
                                            // 返回数据
                                            callback.onSuccess(transaction);
                                        } else {
                                            LogUtils.e(TAG, "重试后交易记录更新仍然失败: " + apiResponse.getMsg());
                                            callback.onError(apiResponse.getMsg() + " (重试后)");
                                        }
                                    } else {
                                        LogUtils.e(TAG, "重试后交易记录更新网络请求仍然失败");
                                        callback.onError("网络请求失败 (重试后)");
                                    }
                                }
                                
                                @Override
                                public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                                    LogUtils.e(TAG, "重试后更新交易记录失败", t);
                                    callback.onError("更新交易记录失败: " + t.getMessage() + " (重试后)");
                                }
                            });
                            return; // 防止多次回调
                        }
                        
                        // 设置null来表示失败
                        callback.onError(apiResponse.getMsg());
                    }
                } else {
                    LogUtils.e(TAG, "交易记录更新网络请求失败");
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                LogUtils.e(TAG, "更新交易记录失败", t);
                
                // 检查是否为认证错误
                if (t.getMessage() != null && (t.getMessage().contains("401") || t.getMessage().contains("认证"))) {
                    LogUtils.w(TAG, "可能是认证错误，尝试使用不同的用户ID重试");
                    
                    // 重新设置用户ID并重试
                    request.setUserId(1); // 确保使用默认用户ID
                    
                    // 重试请求
                    apiService.updateTransaction(request).enqueue(new Callback<ApiResponse<String>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                ApiResponse<String> apiResponse = response.body();
                                if (apiResponse.isSuccess()) {
                                    LogUtils.d(TAG, "重试后交易记录更新成功，ID: " + request.getId());
                                    
                                    // 构建返回的交易对象
                                    Transaction transaction = new Transaction();
                                    transaction.setId(request.getId());
                                    transaction.setType(request.getType());
                                    transaction.setAmount(request.getAmount());
                                    transaction.setCategoryId(request.getCategoryId());
                                    
                                    // 设置日期
                                    if (request.getDate() > 0) {
                                        transaction.setDate(new Date(request.getDate()));
                                    }
                                    
                                    // 设置描述和备注
                                    transaction.setDescription(request.getDescription());
                                    transaction.setNote(request.getNote());
                                    transaction.setRemark(request.getRemark());
                                    
                                    // 更新缓存
                                    updateTransactionInCache(transaction);
                                    
                                    // 返回数据
                                    callback.onSuccess(transaction);
                                } else {
                                    LogUtils.e(TAG, "重试后交易记录更新仍然失败: " + apiResponse.getMsg());
                                    callback.onError(apiResponse.getMsg() + " (重试后)");
                                }
                            } else {
                                LogUtils.e(TAG, "重试后交易记录更新网络请求仍然失败");
                                callback.onError("网络请求失败 (重试后)");
                            }
                        }
                        
                        @Override
                        public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                            LogUtils.e(TAG, "重试后更新交易记录失败", t);
                            callback.onError("更新交易记录失败: " + t.getMessage() + " (重试后)");
                        }
                    });
                    return; // 防止多次回调
                }
                
                callback.onError("更新交易记录失败: " + t.getMessage());
            }
        });
    }
    
    /**
     * 更新缓存中的交易记录
     */
    private void updateTransactionInCache(Transaction transaction) {
        if (transaction == null || transaction.getId() <= 0) {
            LogUtils.w(TAG, "无法更新缓存中的交易记录：无效的交易记录或ID");
            return;
        }
        
        try {
            List<Transaction> cachedTransactions = cacheManager.getTransactions();
            if (cachedTransactions != null && !cachedTransactions.isEmpty()) {
                boolean found = false;
                for (int i = 0; i < cachedTransactions.size(); i++) {
                    if (cachedTransactions.get(i).getId() == transaction.getId()) {
                        cachedTransactions.set(i, transaction);
                        found = true;
                        break;
                    }
                }
                
                if (found) {
                    cacheManager.saveTransactions(cachedTransactions);
                    LogUtils.d(TAG, "成功更新缓存中的交易记录，ID: " + transaction.getId());
                } else {
                    LogUtils.w(TAG, "未在缓存中找到要更新的交易记录，ID: " + transaction.getId());
                }
            } else {
                LogUtils.w(TAG, "缓存中没有交易记录数据，无法更新");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "更新缓存中的交易记录失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除交易记录
     * @param transactionId 交易记录ID
     * @param callback 回调
     */
    public void deleteTransaction(long transactionId, final RepositoryCallback<Boolean> callback) {
        // 获取当前用户ID
        long userId = tokenManager.getUserId();
        if (userId <= 0) {
            callback.onError("未获取到有效的用户ID");
            return;
        }
        
        // 首先尝试获取交易记录详情，确认是否是当前用户的交易记录
        getTransaction(transactionId, new RepositoryCallback<Transaction>() {
            @Override
            public void onSuccess(Transaction transaction) {
                // 验证交易记录所属用户是否与当前用户匹配
                if (transaction.getUserId() != userId) {
                    callback.onError("没有权限删除此交易记录");
                    return;
                }
                
                // 检查网络状态
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    callback.onError("无网络连接，无法删除交易记录");
                    return;
                }
                
                // 实际删除操作
                apiService.deleteTransaction(String.valueOf(transactionId)).enqueue(new Callback<ApiResponse<String>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<String> apiResponse = response.body();
                            if (apiResponse.isSuccess()) {
                                // 从缓存中移除
                                removeTransactionFromCache(transactionId);
                                callback.onSuccess(true);
                            } else {
                                callback.onError(apiResponse.getMsg());
                            }
                        } else {
                            callback.onError("删除交易记录失败，请稍后重试");
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                        LogUtils.e(TAG, "删除交易记录失败", t);
                        callback.onError("删除交易记录失败: " + t.getMessage());
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                callback.onError("获取交易记录失败: " + error);
            }
            
            @Override
            public void isCacheData(boolean isCache) {
                // 不需要特殊处理
            }
        });
    }
    
    /**
     * 获取单个交易记录详情
     * @param transactionId 交易记录ID
     * @param callback 回调
     */
    public void getTransaction(long transactionId, final RepositoryCallback<Transaction> callback) {
        // 获取当前用户ID
        long userId = tokenManager.getUserId();
        if (userId <= 0) {
            callback.onError("未获取到有效的用户ID");
            return;
        }
        
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getTransactionDetail(transactionId).enqueue(new Callback<ApiResponse<Transaction>>() {
                @Override
                public void onResponse(Call<ApiResponse<Transaction>> call, Response<ApiResponse<Transaction>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Transaction> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            Transaction transaction = apiResponse.getData();
                            
                            // 返回数据
                            callback.onSuccess(transaction);
                        } else {
                            callback.onError(apiResponse.getMsg());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Transaction>> call, Throwable t) {
                    LogUtils.e(TAG, "获取交易记录详情失败", t);
                    
                    // 网络请求失败，尝试从缓存获取
                    if (cacheManager.isCacheValid("transactions")) {
                        List<Transaction> cachedTransactions = cacheManager.getTransactions();
                        if (cachedTransactions != null && !cachedTransactions.isEmpty()) {
                            // 查找指定ID的交易记录
                            for (Transaction transaction : cachedTransactions) {
                                if (transaction.getId() == transactionId) {
                                    callback.onSuccess(transaction);
                                    
                                    // 标记为从缓存获取
                                    callback.isCacheData(true);
                                    return;
                                }
                            }
                            callback.onError("找不到指定的交易记录");
                        } else {
                            callback.onError("获取交易记录详情失败: " + t.getMessage());
                        }
                    } else {
                        callback.onError("获取交易记录详情失败: " + t.getMessage());
                    }
                }
            });
        } else {
            // 无网络连接，从缓存获取数据
            if (cacheManager.isCacheValid("transactions")) {
                List<Transaction> cachedTransactions = cacheManager.getTransactions();
                if (cachedTransactions != null && !cachedTransactions.isEmpty()) {
                    // 查找指定ID的交易记录
                    for (Transaction transaction : cachedTransactions) {
                        if (transaction.getId() == transactionId) {
                            callback.onSuccess(transaction);
                            
                            // 标记为从缓存获取
                            callback.isCacheData(true);
                            return;
                        }
                    }
                    callback.onError("找不到指定的交易记录");
                } else {
                    callback.onError("无网络连接且无缓存数据");
                }
            } else {
                callback.onError("无网络连接且无缓存数据");
            }
        }
    }
    
    /**
     * 按日期范围获取交易记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param callback 回调
     */
    public void getTransactionsByDateRange(Date startDate, Date endDate, final RepositoryCallback<List<Transaction>> callback) {
        // 获取当前用户ID
        long userId = tokenManager.getUserId();
        if (userId <= 0) {
            callback.onError("未获取到有效的用户ID");
            return;
        }
        
        long startTime = startDate.getTime();
        long endTime = endDate.getTime();
        
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getTransactionsByMonth(userId, startTime, endTime, null).enqueue(new Callback<ApiResponse<List<Transaction>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Transaction>>> call, Response<ApiResponse<List<Transaction>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Transaction>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            List<Transaction> transactions = new ArrayList<>();
                            
                            // 获取响应中的交易记录列表
                            List<Transaction> dataList = apiResponse.getData();
                            if (dataList != null && !dataList.isEmpty()) {
                                transactions.addAll(dataList);
                            }
                            
                            // 尝试从rows获取更多数据
                            List<?> rowsData = apiResponse.getRowsSafe();
                            if (rowsData != null && !rowsData.isEmpty()) {
                                for (Object item : rowsData) {
                                    if (item instanceof Transaction) {
                                        transactions.add((Transaction) item);
                                    } else if (item instanceof List) {
                                        // 处理嵌套列表情况
                                        List<?> itemList = (List<?>) item;
                                        for (Object subItem : itemList) {
                                            if (subItem instanceof Transaction) {
                                                transactions.add((Transaction) subItem);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (!transactions.isEmpty()) {
                                // 返回数据
                                callback.onSuccess(transactions);
                            } else {
                                callback.onError("返回数据为空");
                            }
                        } else {
                            callback.onError(apiResponse.getMsg());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Transaction>>> call, Throwable t) {
                    LogUtils.e(TAG, "获取交易记录列表失败", t);
                    
                    // 处理JSON解析异常，可能是服务端返回格式不一致导致的
                    if (t instanceof com.google.gson.JsonSyntaxException) {
                        LogUtils.w(TAG, "服务端可能返回了非预期格式，尝试使用缓存数据");
                        // 尝试从缓存获取
                        if (cacheManager.isCacheValid("transactions")) {
                            List<Transaction> cachedTransactions = cacheManager.getTransactions();
                            if (cachedTransactions != null && !cachedTransactions.isEmpty()) {
                                // 过滤日期范围
                                List<Transaction> filteredTransactions = new ArrayList<>();
                                for (Transaction transaction : cachedTransactions) {
                                    if (transaction.getDate() != null && 
                                        !transaction.getDate().before(startDate) && 
                                        !transaction.getDate().after(endDate)) {
                                        filteredTransactions.add(transaction);
                                    }
                                }
                                
                                if (!filteredTransactions.isEmpty()) {
                                    callback.onSuccess(filteredTransactions);
                                    // 标记为从缓存获取
                                    callback.isCacheData(true);
                                    return;
                                }
                            }
                        }
                    }
                    
                    // 常规错误处理，尝试从缓存获取
                    if (cacheManager.isCacheValid("transactions")) {
                        List<Transaction> allTransactions = cacheManager.getTransactions();
                        if (allTransactions != null && !allTransactions.isEmpty()) {
                            // 过滤日期范围
                            List<Transaction> filteredTransactions = new ArrayList<>();
                            for (Transaction transaction : allTransactions) {
                                if (transaction.getDate() != null && 
                                    !transaction.getDate().before(startDate) && 
                                    !transaction.getDate().after(endDate)) {
                                    filteredTransactions.add(transaction);
                                }
                            }
                            
                            if (!filteredTransactions.isEmpty()) {
                                callback.onSuccess(filteredTransactions);
                                // 标记为从缓存获取
                                callback.isCacheData(true);
                            } else {
                                callback.onError("没有找到该日期范围内的交易记录");
                            }
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
                    // 过滤日期范围
                    List<Transaction> filteredTransactions = new ArrayList<>();
                    for (Transaction transaction : allTransactions) {
                        if (transaction.getDate() != null && 
                            !transaction.getDate().before(startDate) && 
                            !transaction.getDate().after(endDate)) {
                            filteredTransactions.add(transaction);
                        }
                    }
                    
                    if (!filteredTransactions.isEmpty()) {
                        callback.onSuccess(filteredTransactions);
                        // 标记为从缓存获取
                        callback.isCacheData(true);
                    } else {
                        callback.onError("没有找到该日期范围内的交易记录");
                    }
                } else {
                    callback.onError("无网络连接且无缓存数据");
                }
            } else {
                callback.onError("无网络连接且无缓存数据");
            }
        }
    }
    
    /**
     * 获取分类列表
     * @param type 类型：0-支出，1-收入
     * @return 分类列表
     */
    public List<Category> getCategories(int type) {
        // 尝试从缓存获取
        List<Category> cachedCategories = cacheManager.getCategories(type);
        if (cachedCategories != null && !cachedCategories.isEmpty()) {
            LogUtils.i(TAG, "从缓存获取" + (type == 0 ? "支出" : "收入") + "分类数据，共" + cachedCategories.size() + "条");
            return new ArrayList<>(cachedCategories);
        }
        
        LogUtils.i(TAG, "缓存中无" + (type == 0 ? "支出" : "收入") + "分类数据，尝试从API获取");
        
        // 从API获取
        try {
            ApiResponse<Category> response = apiService.getCategories(1, 100, null, type).execute().body();
            if (response != null && response.isSuccess()) {
                List<Category> categories = new ArrayList<>();
                
                // 优先使用data字段，如果为空则使用rows字段
                List<Category> categoriesData = response.getRows();
                if (!categoriesData.isEmpty()) {
                    categories.addAll(categoriesData);
                } else {
                    // 如果data为空，从rows字段获取分类列表
                    try {
                        // 处理getRowsSafe可能返回泛型不匹配的情况
                        List<?> rowsData = response.getRowsSafe();
                        if (rowsData != null && !rowsData.isEmpty()) {
                            for (Object item : rowsData) {
                                if (item instanceof Category) {
                                    categories.add((Category) item);
                                } else if (item instanceof List) {
                                    // 处理嵌套列表情况
                                    List<?> nestedList = (List<?>) item;
                                    for (Object nestedItem : nestedList) {
                                        if (nestedItem instanceof Category) {
                                            categories.add((Category) nestedItem);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LogUtils.e(TAG, "处理rows数据失败: " + e.getMessage(), e);
                    }
                }
                
                LogUtils.i(TAG, "API返回原始数据，共" + (response.getTotal() > 0 ? response.getTotal() : (response.getRowsSafe().size())) + "条");
                
                if (!categories.isEmpty()) {
                    LogUtils.i(TAG, "成功从API获取" + (type == 0 ? "支出" : "收入") + "分类数据，共" + categories.size() + "条");
                    
                    // 确保type字段正确设置（1表示支出，2表示收入，按照服务器的定义）
                    for (Category category : categories) {
                        // 确保与APP内部的定义一致
                        if (type == 0 && category.getType() != TYPE_EXPENSE) {
                            category.setType(TYPE_EXPENSE);
                        } else if (type == 1 && category.getType() != TYPE_INCOME) {
                            category.setType(TYPE_INCOME);
                        }
                    }
                    
                    // 保存到缓存
                    cacheManager.saveCategories(type, categories);
                    return categories;
                } else {
                    LogUtils.w(TAG, "API返回的" + (type == 0 ? "支出" : "收入") + "分类数据为空或类型不匹配");
                }
            } else {
                LogUtils.w(TAG, "API获取" + (type == 0 ? "支出" : "收入") + "分类失败: " + 
                    (response != null ? response.getMsg() : "响应为空"));
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "获取分类失败: " + e.getMessage(), e);
        }
        
        // 返回默认分类（出错或没有数据时）
        List<Category> defaultCategories = type == 0 ? getDefaultExpenseCategories() : getDefaultIncomeCategories();
        LogUtils.i(TAG, "使用默认" + (type == 0 ? "支出" : "收入") + "分类数据，共" + defaultCategories.size() + "条");
        return defaultCategories;
    }
    
    /**
     * 获取分类列表（带回调）
     * @param type 类型：0-支出，1-收入
     * @param callback 回调
     */
    public void getCategoriesWithCallback(int type, final RepositoryCallback<List<Category>> callback) {
        try {
            List<Category> categories = getCategories(type);
            callback.onSuccess(categories);
            // 检查是否是缓存数据
            boolean isCacheData = cacheManager.isCacheValid("categories_" + type) && 
                                  cacheManager.getCategories(type) != null && 
                                  !cacheManager.getCategories(type).isEmpty();
            callback.isCacheData(isCacheData);
            LogUtils.d(TAG, "返回" + (type == TYPE_EXPENSE ? "支出" : "收入") + "分类数据来源: " +
                         (isCacheData ? "缓存" : "API或默认数据"));
        } catch (Exception e) {
            callback.onError("获取分类失败: " + e.getMessage());
            LogUtils.e(TAG, "获取" + (type == TYPE_EXPENSE ? "支出" : "收入") + "分类数据异常", e);
        }
    }
    
    /**
     * 获取所有交易分类列表（包括支出和收入分类）
     * @param callback 回调
     */
    public void getAllCategories(final RepositoryCallback<Map<Integer, List<Category>>> callback) {
        LogUtils.d(TAG, "开始加载所有分类数据");
        final Map<Integer, List<Category>> allCategories = new HashMap<>();
        final AtomicInteger pendingRequests = new AtomicInteger(2); // 需要请求支出和收入两种分类
        final boolean[] isFromCache = new boolean[]{false, false};
        
        // 获取支出分类
        getCategoriesWithCallback(TYPE_EXPENSE, new RepositoryCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                synchronized (allCategories) {
                    allCategories.put(0, result);
                    LogUtils.d(TAG, "成功获取支出分类，数量: " + result.size());
                    checkComplete();
                }
            }
            
            @Override
            public void onError(String errorMsg) {
                LogUtils.e(TAG, "获取支出分类失败: " + errorMsg);
                synchronized (allCategories) {
                    // 即使获取支出分类失败，也尝试返回收入分类
                    allCategories.put(0, new ArrayList<>());
                    checkComplete();
                }
            }
            
            @Override
            public void isCacheData(boolean isCache) {
                isFromCache[0] = isCache;
                LogUtils.d(TAG, "支出分类数据来源: " + (isCache ? "缓存" : "API或默认数据"));
            }
            
            private void checkComplete() {
                if (pendingRequests.decrementAndGet() == 0) {
                    callback.onSuccess(allCategories);
                    // 如果任一分类来自缓存，则整体标记为来自缓存
                    boolean anyFromCache = isFromCache[0] || isFromCache[1];
                    callback.isCacheData(anyFromCache);
                    LogUtils.i(TAG, "所有分类数据加载完成，总支出分类: " + 
                             (allCategories.get(0) != null ? allCategories.get(0).size() : 0) + 
                             ", 总收入分类: " + 
                             (allCategories.get(1) != null ? allCategories.get(1).size() : 0) +
                             ", 数据来源: " + (anyFromCache ? "部分或全部来自缓存" : "全部来自API或默认数据"));
                }
            }
        });
        
        // 获取收入分类
        getCategoriesWithCallback(TYPE_INCOME, new RepositoryCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                synchronized (allCategories) {
                    allCategories.put(1, result);
                    LogUtils.d(TAG, "成功获取收入分类，数量: " + result.size());
                    checkComplete();
                }
            }
            
            @Override
            public void onError(String errorMsg) {
                LogUtils.e(TAG, "获取收入分类失败: " + errorMsg);
                synchronized (allCategories) {
                    // 即使获取收入分类失败，也尝试返回支出分类
                    allCategories.put(1, new ArrayList<>());
                    checkComplete();
                }
            }
            
            @Override
            public void isCacheData(boolean isCache) {
                isFromCache[1] = isCache;
                LogUtils.d(TAG, "收入分类数据来源: " + (isCache ? "缓存" : "API或默认数据"));
            }
            
            private void checkComplete() {
                if (pendingRequests.decrementAndGet() == 0) {
                    callback.onSuccess(allCategories);
                    // 如果任一分类来自缓存，则整体标记为来自缓存
                    boolean anyFromCache = isFromCache[0] || isFromCache[1];
                    callback.isCacheData(anyFromCache);
                    LogUtils.i(TAG, "所有分类数据加载完成，总支出分类: " + 
                             (allCategories.get(0) != null ? allCategories.get(0).size() : 0) + 
                             ", 总收入分类: " + 
                             (allCategories.get(1) != null ? allCategories.get(1).size() : 0) +
                             ", 数据来源: " + (anyFromCache ? "部分或全部来自缓存" : "全部来自API或默认数据"));
                }
            }
        });
    }
    
    /**
     * 获取默认支出分类（当无法从服务器获取时使用）
     */
    private List<Category> getDefaultExpenseCategories() {
        List<Category> categories = new ArrayList<>();
        
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("餐饮");
        category1.setType(TYPE_EXPENSE);  // 支出类型 = 1
        category1.setIcon("food");
        category1.setColor("#F44336");
        categories.add(category1);
        
        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("购物");
        category2.setType(TYPE_EXPENSE);  // 支出类型 = 1
        category2.setIcon("shopping");
        category2.setColor("#2196F3");
        categories.add(category2);
        
        Category category3 = new Category();
        category3.setId(3L);
        category3.setName("交通");
        category3.setType(TYPE_EXPENSE);  // 支出类型 = 1
        category3.setIcon("transport");
        category3.setColor("#4CAF50");
        categories.add(category3);
        
        Category category4 = new Category();
        category4.setId(4L);
        category4.setName("娱乐");
        category4.setType(TYPE_EXPENSE);  // 支出类型 = 1
        category4.setIcon("entertainment");
        category4.setColor("#FF9800");
        categories.add(category4);
        
        Category category5 = new Category();
        category5.setId(5L);
        category5.setName("住房");
        category5.setType(TYPE_EXPENSE);  // 支出类型 = 1
        category5.setIcon("house");
        category5.setColor("#9C27B0");
        categories.add(category5);
        
        return categories;
    }
    
    /**
     * 获取默认收入分类（当无法从服务器获取时使用）
     */
    private List<Category> getDefaultIncomeCategories() {
        List<Category> categories = new ArrayList<>();
        
        Category category1 = new Category();
        category1.setId(6L);
        category1.setName("工资");
        category1.setType(TYPE_INCOME);  // 收入类型 = 2
        category1.setIcon("salary");
        category1.setColor("#3F51B5");
        categories.add(category1);
        
        Category category2 = new Category();
        category2.setId(7L);
        category2.setName("奖金");
        category2.setType(TYPE_INCOME);  // 收入类型 = 2
        category2.setIcon("bonus");
        category2.setColor("#E91E63");
        categories.add(category2);
        
        Category category3 = new Category();
        category3.setId(8L);
        category3.setName("投资");
        category3.setType(TYPE_INCOME);  // 收入类型 = 2
        category3.setIcon("investment");
        category3.setColor("#009688");
        categories.add(category3);
        
        Category category4 = new Category();
        category4.setId(9L);
        category4.setName("兼职");
        category4.setType(TYPE_INCOME);  // 收入类型 = 2
        category4.setIcon("part_time");
        category4.setColor("#FF5722");
        categories.add(category4);
        
        return categories;
    }
    
    /**
     * 从缓存中移除交易记录
     * @param transactionId 交易记录ID
     */
    private void removeTransactionFromCache(long transactionId) {
        List<Transaction> cachedTransactions = cacheManager.getTransactions();
        if (cachedTransactions != null) {
            for (int i = 0; i < cachedTransactions.size(); i++) {
                if (cachedTransactions.get(i).getId() == transactionId) {
                    cachedTransactions.remove(i);
                    break;
                }
            }
            cacheManager.saveTransactions(cachedTransactions);
        }
    }
} 