package com.zjf.fincialsystem.repository;

import android.content.Context;

import com.zjf.fincialsystem.db.DataCacheManager;
import com.zjf.fincialsystem.model.Budget;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.network.api.BudgetApiService;
import com.zjf.fincialsystem.network.model.AddBudgetRequest;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 预算数据仓库
 * 负责预算数据的获取和缓存管理
 */
public class BudgetRepository {
    private static final String TAG = "BudgetRepository";
    
    private final Context context;
    private final BudgetApiService apiService;
    private final DataCacheManager cacheManager;
    
    public BudgetRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkManager.getInstance().getBudgetApiService();
        this.cacheManager = DataCacheManager.getInstance(context);
    }
    
    /**
     * 获取预算列表
     * @param period 预算周期（月度/年度）
     * @param callback 回调
     */
    public void getBudgets(String period, final RepositoryCallback<List<Budget>> callback) {
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getBudgets(period).enqueue(new Callback<ApiResponse<List<Budget>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Budget>>> call, Response<ApiResponse<List<Budget>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Budget>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            List<Budget> budgets = apiResponse.getData();
                            
                            // 保存到缓存
                            cacheManager.saveBudgets(budgets);
                            
                            // 返回数据
                            callback.onSuccess(budgets);
                        } else {
                            callback.onError(apiResponse.getMessage());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Budget>>> call, Throwable t) {
                    LogUtils.e(TAG, "获取预算列表失败", t);
                    
                    // 网络请求失败，尝试从缓存获取
                    if (cacheManager.isCacheValid("budgets")) {
                        List<Budget> cachedBudgets = cacheManager.getBudgets();
                        if (cachedBudgets != null && !cachedBudgets.isEmpty()) {
                            // 过滤预算周期
                            if (period != null && !period.isEmpty()) {
                                List<Budget> filteredBudgets = new ArrayList<>();
                                for (Budget budget : cachedBudgets) {
                                    if (budget.getPeriod().equals(period)) {
                                        filteredBudgets.add(budget);
                                    }
                                }
                                callback.onSuccess(filteredBudgets);
                            } else {
                                callback.onSuccess(cachedBudgets);
                            }
                            
                            // 标记为从缓存获取
                            callback.isCacheData(true);
                        } else {
                            callback.onError("获取预算数据失败: " + t.getMessage());
                        }
                    } else {
                        callback.onError("获取预算数据失败: " + t.getMessage());
                    }
                }
            });
        } else {
            // 无网络连接，从缓存获取数据
            if (cacheManager.isCacheValid("budgets")) {
                List<Budget> cachedBudgets = cacheManager.getBudgets();
                if (cachedBudgets != null && !cachedBudgets.isEmpty()) {
                    // 过滤预算周期
                    if (period != null && !period.isEmpty()) {
                        List<Budget> filteredBudgets = new ArrayList<>();
                        for (Budget budget : cachedBudgets) {
                            if (budget.getPeriod().equals(period)) {
                                filteredBudgets.add(budget);
                            }
                        }
                        callback.onSuccess(filteredBudgets);
                    } else {
                        callback.onSuccess(cachedBudgets);
                    }
                    
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
     * 获取当前预算
     * @param callback 回调
     */
    public void getCurrentBudgets(final RepositoryCallback<List<Budget>> callback) {
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            apiService.getCurrentBudgets().enqueue(new Callback<ApiResponse<List<Budget>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Budget>>> call, Response<ApiResponse<List<Budget>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<Budget>> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            List<Budget> budgets = apiResponse.getData();
                            
                            // 返回数据
                            callback.onSuccess(budgets);
                        } else {
                            callback.onError(apiResponse.getMessage());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Budget>>> call, Throwable t) {
                    LogUtils.e(TAG, "获取当前预算失败", t);
                    
                    // 网络请求失败，尝试从缓存获取月度预算
                    if (cacheManager.isCacheValid("budgets")) {
                        List<Budget> cachedBudgets = cacheManager.getBudgets();
                        if (cachedBudgets != null && !cachedBudgets.isEmpty()) {
                            // 过滤月度预算
                            List<Budget> monthlyBudgets = new ArrayList<>();
                            for (Budget budget : cachedBudgets) {
                                if (budget.isMonthly()) {
                                    monthlyBudgets.add(budget);
                                }
                            }
                            
                            if (!monthlyBudgets.isEmpty()) {
                                callback.onSuccess(monthlyBudgets);
                                
                                // 标记为从缓存获取
                                callback.isCacheData(true);
                            } else {
                                callback.onError("获取当前预算失败: " + t.getMessage());
                            }
                        } else {
                            callback.onError("获取当前预算失败: " + t.getMessage());
                        }
                    } else {
                        callback.onError("获取当前预算失败: " + t.getMessage());
                    }
                }
            });
        } else {
            // 无网络连接，从缓存获取数据
            if (cacheManager.isCacheValid("budgets")) {
                List<Budget> cachedBudgets = cacheManager.getBudgets();
                if (cachedBudgets != null && !cachedBudgets.isEmpty()) {
                    // 过滤月度预算
                    List<Budget> monthlyBudgets = new ArrayList<>();
                    for (Budget budget : cachedBudgets) {
                        if (budget.isMonthly()) {
                            monthlyBudgets.add(budget);
                        }
                    }
                    
                    if (!monthlyBudgets.isEmpty()) {
                        callback.onSuccess(monthlyBudgets);
                        
                        // 标记为从缓存获取
                        callback.isCacheData(true);
                    } else {
                        callback.onError("无网络连接且无有效缓存数据");
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
     * 添加预算
     * @param request 添加预算请求参数
     * @param callback 回调
     */
    public void addBudget(AddBudgetRequest request, final RepositoryCallback<Budget> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法添加预算");
            return;
        }
        
        apiService.addBudget(request).enqueue(new Callback<ApiResponse<Budget>>() {
            @Override
            public void onResponse(Call<ApiResponse<Budget>> call, Response<ApiResponse<Budget>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Budget> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Budget budget = apiResponse.getData();
                        
                        // 更新缓存
                        List<Budget> cachedBudgets = cacheManager.getBudgets();
                        cachedBudgets.add(budget);
                        cacheManager.saveBudgets(cachedBudgets);
                        
                        // 返回数据
                        callback.onSuccess(budget);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Budget>> call, Throwable t) {
                LogUtils.e(TAG, "添加预算失败", t);
                callback.onError("添加预算失败: " + t.getMessage());
            }
        });
    }
    
    /**
     * 更新预算
     * @param budgetId 预算ID
     * @param budget 要更新的预算
     * @param callback 回调
     */
    public void updateBudget(long budgetId, Budget budget, final RepositoryCallback<Budget> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法更新预算");
            return;
        }
        
        apiService.updateBudget(budgetId, budget).enqueue(new Callback<ApiResponse<Budget>>() {
            @Override
            public void onResponse(Call<ApiResponse<Budget>> call, Response<ApiResponse<Budget>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Budget> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Budget updatedBudget = apiResponse.getData();
                        
                        // 更新缓存
                        List<Budget> cachedBudgets = cacheManager.getBudgets();
                        for (int i = 0; i < cachedBudgets.size(); i++) {
                            if (cachedBudgets.get(i).getId() == updatedBudget.getId()) {
                                cachedBudgets.set(i, updatedBudget);
                                break;
                            }
                        }
                        cacheManager.saveBudgets(cachedBudgets);
                        
                        // 返回数据
                        callback.onSuccess(updatedBudget);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Budget>> call, Throwable t) {
                LogUtils.e(TAG, "更新预算失败", t);
                callback.onError("更新预算失败: " + t.getMessage());
            }
        });
    }
    
    /**
     * 删除预算
     * @param budgetId 要删除的预算ID
     * @param callback 回调
     */
    public void deleteBudget(long budgetId, final RepositoryCallback<Boolean> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法删除预算");
            return;
        }
        
        apiService.deleteBudget(budgetId).enqueue(new Callback<ApiResponse<Boolean>>() {
            @Override
            public void onResponse(Call<ApiResponse<Boolean>> call, Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Boolean> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData()) {
                        // 更新缓存
                        List<Budget> cachedBudgets = cacheManager.getBudgets();
                        for (int i = 0; i < cachedBudgets.size(); i++) {
                            if (cachedBudgets.get(i).getId() == budgetId) {
                                cachedBudgets.remove(i);
                                break;
                            }
                        }
                        cacheManager.saveBudgets(cachedBudgets);
                        
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
                LogUtils.e(TAG, "删除预算失败", t);
                callback.onError("删除预算失败: " + t.getMessage());
            }
        });
    }
} 