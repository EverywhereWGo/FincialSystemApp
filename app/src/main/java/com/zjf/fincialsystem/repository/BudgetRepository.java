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
import com.zjf.fincialsystem.utils.TokenManager;

import java.util.ArrayList;
import java.util.Calendar;
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
     * @param selectedDate 指定的日期，如果为null则使用当前日期
     * @param callback 回调
     */
    public void getBudgets(String period, Calendar selectedDate, final RepositoryCallback<List<Budget>> callback) {
        // 检查网络状态
        if (NetworkUtils.isNetworkAvailable(context)) {
            // 有网络连接，从网络获取数据
            // 获取月份作为整数参数 (如果是monthly，使用选定月份数字，否则传null)
            Integer monthParam = null;
            if (Budget.PERIOD_MONTHLY.equals(period)) {
                // 使用选定的日期，如果没有则使用当前日期
                Calendar cal = selectedDate != null ? selectedDate : Calendar.getInstance();
                // 获取月份(1-12)
                monthParam = cal.get(Calendar.MONTH) + 1;
                LogUtils.d(TAG, "查询预算数据，月份: " + monthParam + "，年份: " + cal.get(Calendar.YEAR));
            }
            apiService.getBudgets(1, 100, null, monthParam, TokenManager.getInstance().getUserId()).enqueue(new Callback<ApiResponse<Budget>>() {
                @Override
                public void onResponse(Call<ApiResponse<Budget>> call, Response<ApiResponse<Budget>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<Budget> apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            // 首先尝试从data获取数据
                            List<Budget> budgets = apiResponse.getRows();
                            
                            // 保存到缓存
                            cacheManager.saveBudgets(budgets);
                            
                            // 返回数据
                            callback.onSuccess(budgets);
                        } else {
                            callback.onError(apiResponse.getMsg());
                        }
                    } else {
                        callback.onError("网络请求失败");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Budget>> call, Throwable t) {
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
        // 直接调用getBudgets方法，传入null表示使用当前日期
        getBudgets(Budget.PERIOD_MONTHLY, null, callback);
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
        
        apiService.addBudget(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // 模拟创建一个Budget对象返回
                        Budget budget = new Budget();
                        budget.setCategoryId(request.getCategoryId());
                        budget.setAmount(request.getAmount());
                        
                        // 更新缓存
                        List<Budget> cachedBudgets = cacheManager.getBudgets();
                        cachedBudgets.add(budget);
                        cacheManager.saveBudgets(cachedBudgets);
                        
                        // 返回数据
                        callback.onSuccess(budget);
                    } else {
                        callback.onError(apiResponse.getMsg());
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
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
        
        apiService.updateBudget(budget).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // 更新缓存
                        List<Budget> cachedBudgets = cacheManager.getBudgets();
                        for (int i = 0; i < cachedBudgets.size(); i++) {
                            if (cachedBudgets.get(i).getId() == budgetId) {
                                cachedBudgets.set(i, budget);
                                break;
                            }
                        }
                        cacheManager.saveBudgets(cachedBudgets);
                        
                        // 返回数据
                        callback.onSuccess(budget);
                    } else {
                        callback.onError(apiResponse.getMsg());
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
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
        
        apiService.deleteBudget(String.valueOf(budgetId)).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
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
                        callback.onError(apiResponse.getMsg());
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                LogUtils.e(TAG, "删除预算失败", t);
                callback.onError("删除预算失败: " + t.getMessage());
            }
        });
    }
} 