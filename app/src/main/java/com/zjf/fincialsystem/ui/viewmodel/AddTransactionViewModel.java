package com.zjf.fincialsystem.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.network.model.AddTransactionRequest;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.repository.TransactionRepository;
import com.zjf.fincialsystem.utils.LogUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 添加交易记录的ViewModel
 */
public class AddTransactionViewModel extends ViewModel {

    private static final String TAG = "AddTransactionViewModel";

    private final TransactionRepository repository;
    private final Executor executor;

    private MutableLiveData<List<Category>> expenseCategories;
    private MutableLiveData<List<Category>> incomeCategories;

    public AddTransactionViewModel(TransactionRepository repository) {
        this.repository = repository;
        this.executor = Executors.newSingleThreadExecutor();
        
        // 初始化LiveData
        expenseCategories = new MutableLiveData<>();
        incomeCategories = new MutableLiveData<>();
        
        // 加载分类数据
        loadCategories();
    }

    /**
     * 加载分类数据
     */
    private void loadCategories() {
        LogUtils.d(TAG, "开始加载分类数据");
        // 使用executor在后台线程中执行网络请求
        executor.execute(() -> {
            repository.getAllCategories(new RepositoryCallback<Map<Integer, List<Category>>>() {
                @Override
                public void onSuccess(Map<Integer, List<Category>> result) {
                    // 处理支出分类
                    List<Category> expenseList = result.get(0);
                    if (expenseList != null && !expenseList.isEmpty()) {
                        LogUtils.d(TAG, "成功获取支出分类，数量: " + expenseList.size());
                        expenseCategories.postValue(expenseList);
                    } else {
                        LogUtils.w(TAG, "支出分类为空");
                    }
                    
                    // 处理收入分类
                    List<Category> incomeList = result.get(1);
                    if (incomeList != null && !incomeList.isEmpty()) {
                        LogUtils.d(TAG, "成功获取收入分类，数量: " + incomeList.size());
                        incomeCategories.postValue(incomeList);
                    } else {
                        LogUtils.w(TAG, "收入分类为空");
                    }
                }
                
                @Override
                public void onError(String errorMsg) {
                    LogUtils.e(TAG, "加载分类数据失败: " + errorMsg);
                }
                
                @Override
                public void isCacheData(boolean isCache) {
                    LogUtils.d(TAG, "分类数据来源: " + (isCache ? "缓存" : "API或默认数据"));
                }
            });
        });
    }

    /**
     * 获取支出分类
     */
    public LiveData<List<Category>> getExpenseCategories() {
        if (expenseCategories.getValue() == null) {
            LogUtils.d(TAG, "支出分类尚未加载，重新请求");
            loadCategories();
        }
        return expenseCategories;
    }

    /**
     * 获取收入分类
     */
    public LiveData<List<Category>> getIncomeCategories() {
        if (incomeCategories.getValue() == null) {
            LogUtils.d(TAG, "收入分类尚未加载，重新请求");
            loadCategories();
        }
        return incomeCategories;
    }

    /**
     * 添加交易记录
     * @param type 交易类型：0-支出，1-收入
     * @param amount 金额
     * @param categoryId 分类ID
     * @param description
     * @param date 日期
     * @return 添加的交易记录LiveData
     */
    public LiveData<Transaction> addTransaction(int type, double amount, long categoryId, 
                                               String description, Date date) {
        LogUtils.d(TAG, "ViewModel开始添加交易记录：type=" + type + ", amount=" + amount + ", categoryId=" + categoryId);
        MutableLiveData<Transaction> result = new MutableLiveData<>();
        
        // 在后台线程中添加交易记录
        executor.execute(() -> {
            // 如果是支出，金额取负值
            double actualAmount = type == 0 ? -amount : amount;
            
            // 创建请求对象
            AddTransactionRequest request = new AddTransactionRequest();
            request.setCategoryId(categoryId);
            request.setType(type);
            request.setAmount(actualAmount);
            request.setDate(date.getTime());
            request.setDescription(description);
            request.setUserId(1); // 使用当前登录用户ID
            
            LogUtils.d(TAG, "准备调用API保存交易记录");
            
            // 调用Repository的addTransaction方法
            repository.addTransaction(request, new RepositoryCallback<Transaction>() {
                @Override
                public void onSuccess(Transaction resultTransaction) {
                    LogUtils.d(TAG, "交易记录保存成功，返回Transaction: " + (resultTransaction != null ? resultTransaction.toString() : "null"));
                    result.postValue(resultTransaction);
                }
                
                @Override
                public void onError(String errorMsg) {
                    LogUtils.e(TAG, "交易记录保存失败: " + errorMsg);
                    // 设置null来表示失败
                    result.postValue(null);
                    
                    // 如果需要，您可以在此处对错误进行进一步处理
                    // 例如，区分不同类型的错误并提供不同的用户体验
                    if (errorMsg.contains("无网络连接")) {
                        LogUtils.e(TAG, "网络连接错误，请检查网络设置");
                    } else if (errorMsg.contains("超时")) {
                        LogUtils.e(TAG, "网络请求超时，服务器可能暂时不可用");
                    } else if (errorMsg.contains("401") || errorMsg.contains("认证失败")) {
                        LogUtils.e(TAG, "认证失败，可能需要重新登录");
                    }
                }
                
                @Override
                public void isCacheData(boolean isCache) {
                    // 不需要处理
                }
            });
        });
        
        return result;
    }
}