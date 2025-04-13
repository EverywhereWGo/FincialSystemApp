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
            apiService.getTransactions(1, 100, null, null, null, null).enqueue(new Callback<ApiResponse<List<Transaction>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Transaction>>> call, Response<ApiResponse<List<Transaction>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Transaction>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            // 优先使用data字段，如果为空则使用convertRows方法
                            List<Transaction> transactions = apiResponse.getDataSafe(new ArrayList<>());
                            
                            // 如果data为空，使用convertRows方法转换rows数据为Transaction列表
                            if (transactions.isEmpty()) {
                                // 修改为显式转换处理嵌套列表的情况
                                List<Transaction> convertedTransactions = new ArrayList<>();
                                // 使用convertRows方法处理嵌套列表的情况
                                List<Transaction> rowsData = apiResponse.convertRows(Transaction.class);
                                if (rowsData != null && !rowsData.isEmpty()) {
                                    convertedTransactions.addAll(rowsData);
                                }
                                transactions = convertedTransactions;
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
                public void onFailure(Call<ApiResponse<List<Transaction>>> call, Throwable t) {
                    LogUtils.e(TAG, "获取交易记录列表失败", t);
                    
                    // 处理JSON解析异常，可能是服务端返回格式不一致导致的
                    if (t instanceof com.google.gson.JsonSyntaxException && 
                            t.getMessage() != null && 
                            t.getMessage().contains("Expected BEGIN_ARRAY but was BEGIN_OBJECT")) {
                        LogUtils.w(TAG, "服务端可能返回了对象而非数组格式，尝试使用备用方案获取数据");
                        // 这里可能需要使用RawResponseBodyConverter处理
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
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getTransactions(1, 100, type, null, null, null).enqueue(new Callback<ApiResponse<List<Transaction>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Transaction>>> call, Response<ApiResponse<List<Transaction>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Transaction>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            // 优先使用data字段，如果为空则使用convertRows方法
                            List<Transaction> transactions = apiResponse.getDataSafe(new ArrayList<>());
                            
                            // 如果data为空，使用convertRows方法转换rows数据为Transaction列表
                            if (transactions.isEmpty()) {
                                // 修改为显式转换处理嵌套列表的情况
                                List<Transaction> convertedTransactions = new ArrayList<>();
                                // 使用convertRows方法处理嵌套列表的情况
                                List<Transaction> rowsData = apiResponse.convertRows(Transaction.class);
                                if (rowsData != null && !rowsData.isEmpty()) {
                                    convertedTransactions.addAll(rowsData);
                                }
                                transactions = convertedTransactions;
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
                    if (t instanceof com.google.gson.JsonSyntaxException && 
                            t.getMessage() != null && 
                            t.getMessage().contains("Expected BEGIN_ARRAY but was BEGIN_OBJECT")) {
                        LogUtils.w(TAG, "服务端可能返回了对象而非数组格式，尝试使用备用方案获取数据");
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
     * @param request 添加交易记录请求参数
     * @param callback 回调
     */
    public void addTransaction(AddTransactionRequest request, final RepositoryCallback<Transaction> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            LogUtils.e(TAG, "无网络连接，无法添加交易记录到服务器");
            callback.onError("无网络连接，请检查您的网络设置后重试");
            return;
        }
        
        LogUtils.d(TAG, "开始调用API添加交易记录: " + request.toString());
        
        // 有网络连接，调用API
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
                        transaction.setDescription(request.getDescription());
                        transaction.setNote(request.getNote());
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
     * @param transaction 要更新的交易记录
     * @param callback 回调
     */
    public void updateTransaction(Transaction transaction, final RepositoryCallback<Transaction> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法更新交易记录");
            return;
        }
        
        apiService.updateTransaction(transaction).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // 更新缓存
                        List<Transaction> cachedTransactions = cacheManager.getTransactions();
                        for (int i = 0; i < cachedTransactions.size(); i++) {
                            if (cachedTransactions.get(i).getId() == transaction.getId()) {
                                cachedTransactions.set(i, transaction);
                                break;
                            }
                        }
                        cacheManager.saveTransactions(cachedTransactions);
                        
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
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
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
        
        apiService.deleteTransaction(String.valueOf(transactionId)).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
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
                        callback.onError(apiResponse.getMsg());
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                LogUtils.e(TAG, "删除交易记录失败", t);
                callback.onError("删除交易记录失败: " + t.getMessage());
            }
        });
    }
    
    /**
     * 获取单个交易记录详情
     * @param transactionId 交易记录ID
     * @param callback 回调
     */
    public void getTransaction(long transactionId, final RepositoryCallback<Transaction> callback) {
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
     * 按日期范围获取交易记录列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param callback 回调
     */
    public void getTransactionsByDateRange(Date startDate, Date endDate, final RepositoryCallback<List<Transaction>> callback) {
        // 检查参数
        if (startDate == null || endDate == null) {
            callback.onError("日期参数不能为空");
            return;
        }
        
        // 转换日期为Long类型的时间戳
        final long startTimestamp = startDate.getTime();
        final long endTimestamp = endDate.getTime();
        
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getTransactions(1, 100, null, startTimestamp, endTimestamp, null).enqueue(new Callback<ApiResponse<List<Transaction>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Transaction>>> call, Response<ApiResponse<List<Transaction>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Transaction>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            // 使用安全方法获取数据，优先从data字段获取
                            List<Transaction> transactions = apiResponse.getData();
                            
                            // 如果data为空，尝试从rows字段安全获取数据，并处理类型转换
                            if (transactions == null || transactions.isEmpty()) {
                                // 获取rows数据，并进行类型转换
                                List<?> rowsData = apiResponse.getRowsSafe();
                                if (rowsData != null && !rowsData.isEmpty()) {
                                    transactions = new ArrayList<>();
                                    // 尝试将rowsData转换为List<Transaction>
                                    for (Object item : rowsData) {
                                        if (item instanceof Transaction) {
                                            transactions.add((Transaction) item);
                                        } else if (item instanceof List) {
                                            // 如果item是List，尝试获取List中的Transaction对象
                                            List<?> itemList = (List<?>) item;
                                            for (Object subItem : itemList) {
                                                if (subItem instanceof Transaction) {
                                                    transactions.add((Transaction) subItem);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (transactions != null && !transactions.isEmpty()) {
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
                        LogUtils.w(TAG, "服务端可能返回了错误格式，尝试使用缓存数据");
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
            ApiResponse<List<Category>> response = apiService.getCategories(type).execute().body();
            if (response != null && response.isSuccess()) {
                List<Category> categories = new ArrayList<>();
                
                // 尝试从data字段获取数据
                List<Category> dataList = response.getData();
                if (dataList != null && !dataList.isEmpty()) {
                    categories.addAll(dataList);
                } else {
                    // 如果data为空，使用新的convertRows方法将rows字段的数据转换为Category对象
                    List<Category> rowsCategories = response.convertRows(Category.class);
                    if (rowsCategories != null && !rowsCategories.isEmpty()) {
                        categories.addAll(rowsCategories);
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
} 