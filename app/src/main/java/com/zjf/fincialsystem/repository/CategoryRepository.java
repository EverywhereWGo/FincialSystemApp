package com.zjf.fincialsystem.repository;

import android.content.Context;

import com.zjf.fincialsystem.db.DataCacheManager;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.network.api.CategoryApiService;
import com.zjf.fincialsystem.network.model.AddCategoryRequest;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 分类数据仓库
 * 负责分类数据的获取和缓存管理
 */
public class CategoryRepository {
    private static final String TAG = "CategoryRepository";
    
    private final Context context;
    private final CategoryApiService apiService;
    private final DataCacheManager cacheManager;
    
    public CategoryRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkManager.getInstance().getCategoryApiService();
        this.cacheManager = DataCacheManager.getInstance(context);
    }
    
    /**
     * 获取分类列表
     * @param type 分类类型：0-支出，1-收入，null-全部
     * @param callback 回调
     */
    public void getCategories(Integer type, final RepositoryCallback<List<Category>> callback) {
        try {
            // 检查网络状态
            if (NetworkUtils.isNetworkAvailable(context)) {
                // 有网络连接，从网络获取数据
                Call<ApiResponse<List<Category>>> call;
                if (type != null) {
                    call = apiService.getCategories(type);
                } else {
                    // 如果类型为空，获取所有分类
                    call = apiService.getCategoriesByType(null);
                }
                
                call.enqueue(new Callback<ApiResponse<List<Category>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<List<Category>> apiResponse = response.body();
                            if (apiResponse.isSuccess()) {
                                List<Category> categories = apiResponse.getData();
                                
                                // 保存到缓存
                                cacheManager.saveCategories(categories);
                                
                                // 返回数据
                                callback.onSuccess(categories);
                            } else {
                                callback.onError(apiResponse.getMessage());
                            }
                        } else {
                            callback.onError("网络请求失败");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                        LogUtils.e(TAG, "获取分类列表失败", t);
                        
                        // 网络请求失败，尝试从缓存获取
                        if (cacheManager.isCacheValid("categories")) {
                            List<Category> cachedCategories = cacheManager.getCategories();
                            if (cachedCategories != null && !cachedCategories.isEmpty()) {
                                // 过滤分类类型
                                if (type != null) {
                                    List<Category> filteredCategories = new ArrayList<>();
                                    for (Category category : cachedCategories) {
                                        if (category.getType() == type) {
                                            filteredCategories.add(category);
                                        }
                                    }
                                    callback.onSuccess(filteredCategories);
                                } else {
                                    callback.onSuccess(cachedCategories);
                                }
                                
                                // 标记为从缓存获取
                                callback.isCacheData(true);
                            } else {
                                callback.onError("获取分类数据失败: " + t.getMessage());
                            }
                        } else {
                            callback.onError("获取分类数据失败: " + t.getMessage());
                        }
                    }
                });
            } else {
                // 无网络连接，从缓存获取数据
                if (cacheManager.isCacheValid("categories")) {
                    List<Category> cachedCategories = cacheManager.getCategories();
                    if (cachedCategories != null && !cachedCategories.isEmpty()) {
                        // 过滤分类类型
                        if (type != null) {
                            List<Category> filteredCategories = new ArrayList<>();
                            for (Category category : cachedCategories) {
                                if (category.getType() == type) {
                                    filteredCategories.add(category);
                                }
                            }
                            callback.onSuccess(filteredCategories);
                        } else {
                            callback.onSuccess(cachedCategories);
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
        } catch (Exception e) {
            LogUtils.e(TAG, "获取分类列表失败", e);
            callback.onError("获取分类列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 添加分类
     * @param request 添加分类请求参数
     * @param callback 回调
     */
    public void addCategory(AddCategoryRequest request, final RepositoryCallback<Category> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法添加分类");
            return;
        }
        
        apiService.addCategory(request).enqueue(new Callback<ApiResponse<Category>>() {
            @Override
            public void onResponse(Call<ApiResponse<Category>> call, Response<ApiResponse<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Category> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Category category = apiResponse.getData();
                        
                        // 更新缓存
                        List<Category> cachedCategories = cacheManager.getCategories();
                        cachedCategories.add(category);
                        cacheManager.saveCategories(cachedCategories);
                        
                        // 返回数据
                        callback.onSuccess(category);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Category>> call, Throwable t) {
                LogUtils.e(TAG, "添加分类失败", t);
                callback.onError("添加分类失败: " + t.getMessage());
            }
        });
    }
    
    /**
     * 更新分类
     * @param category 要更新的分类
     * @param callback 回调
     */
    public void updateCategory(Category category, final RepositoryCallback<Category> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法更新分类");
            return;
        }
        
        apiService.updateCategory(category.getId(), category).enqueue(new Callback<ApiResponse<Category>>() {
            @Override
            public void onResponse(Call<ApiResponse<Category>> call, Response<ApiResponse<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Category> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Category updatedCategory = apiResponse.getData();
                        
                        // 更新缓存
                        List<Category> cachedCategories = cacheManager.getCategories();
                        for (int i = 0; i < cachedCategories.size(); i++) {
                            if (cachedCategories.get(i).getId() == updatedCategory.getId()) {
                                cachedCategories.set(i, updatedCategory);
                                break;
                            }
                        }
                        cacheManager.saveCategories(cachedCategories);
                        
                        // 返回数据
                        callback.onSuccess(updatedCategory);
                    } else {
                        callback.onError(apiResponse.getMessage());
                    }
                } else {
                    callback.onError("网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Category>> call, Throwable t) {
                LogUtils.e(TAG, "更新分类失败", t);
                callback.onError("更新分类失败: " + t.getMessage());
            }
        });
    }
    
    /**
     * 删除分类
     * @param categoryId 要删除的分类ID
     * @param callback 回调
     */
    public void deleteCategory(long categoryId, final RepositoryCallback<Boolean> callback) {
        // 检查网络状态
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onError("无网络连接，无法删除分类");
            return;
        }
        
        apiService.deleteCategory(categoryId).enqueue(new Callback<ApiResponse<Boolean>>() {
            @Override
            public void onResponse(Call<ApiResponse<Boolean>> call, Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Boolean> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData()) {
                        // 更新缓存
                        List<Category> cachedCategories = cacheManager.getCategories();
                        for (int i = 0; i < cachedCategories.size(); i++) {
                            if (cachedCategories.get(i).getId() == categoryId) {
                                cachedCategories.remove(i);
                                break;
                            }
                        }
                        cacheManager.saveCategories(cachedCategories);
                        
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
                LogUtils.e(TAG, "删除分类失败", t);
                callback.onError("删除分类失败: " + t.getMessage());
            }
        });
    }
} 