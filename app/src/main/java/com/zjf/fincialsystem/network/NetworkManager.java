package com.zjf.fincialsystem.network;

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zjf.fincialsystem.BuildConfig;
import com.zjf.fincialsystem.network.api.BudgetApiService;
import com.zjf.fincialsystem.network.api.CategoryApiService;
import com.zjf.fincialsystem.network.api.StatisticsApiService;
import com.zjf.fincialsystem.network.api.TransactionApiService;
import com.zjf.fincialsystem.network.api.UserApiService;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 网络请求管理类
 * 使用单例模式
 */
public class NetworkManager {
    
    private static final String BASE_URL = "https://api.example.com/"; // 实际项目中替换为真实API地址
    private static final long CONNECT_TIMEOUT = 15L;
    private static final long READ_TIMEOUT = 15L;
    private static final long WRITE_TIMEOUT = 15L;
    
    private static volatile NetworkManager instance;
    private Retrofit retrofit;
    private OkHttpClient okHttpClient;
    
    // 服务接口实例缓存
    private UserApiService userApiService;
    private CategoryApiService categoryApiService;
    private TransactionApiService transactionApiService;
    private BudgetApiService budgetApiService;
    private StatisticsApiService statisticsApiService;
    
    private NetworkManager() {
        // 创建日期格式适配器，支持"Mar 9, 2025 21:47:00"格式
        Gson gson = new GsonBuilder()
                .setDateFormat("MMM d, yyyy HH:mm:ss")
                .setLenient() // 增加宽松解析
                .create();
        
        // 创建OkHttpClient
        okHttpClient = createOkHttpClient();
        
        // 创建Retrofit实例
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson)) // 使用自定义Gson配置
                .build();
    }
    
    /**
     * 获取单例实例
     */
    public static NetworkManager getInstance() {
        if (instance == null) {
            synchronized (NetworkManager.class) {
                if (instance == null) {
                    instance = new NetworkManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化网络请求
     */
    public void init() {
        try {
            // 初始化各个API服务
            userApiService = retrofit.create(UserApiService.class);
            categoryApiService = retrofit.create(CategoryApiService.class);
            transactionApiService = retrofit.create(TransactionApiService.class);
            budgetApiService = retrofit.create(BudgetApiService.class);
            statisticsApiService = retrofit.create(StatisticsApiService.class);
            
            LogUtils.i("网络管理器初始化成功");
        } catch (Exception e) {
            LogUtils.e("网络管理器初始化失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 创建OkHttpClient实例
     */
    private OkHttpClient createOkHttpClient() {
        // 创建日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // 创建请求拦截器，添加通用请求头
        RequestInterceptor requestInterceptor = new RequestInterceptor();
        
        // 构建OkHttpClient
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(requestInterceptor);
        
        // 在开发模式下添加模拟数据拦截器
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new MockInterceptor());
            LogUtils.d("Added MockInterceptor for debug mode");
        }
        
        return builder.build();
    }
    
    /**
     * 获取API服务接口
     */
    public <T> T getService(Class<T> serviceClass) {
        if (retrofit == null) {
            init();
        }
        return retrofit.create(serviceClass);
    }
    
    /**
     * 获取OkHttpClient实例
     */
    public OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = createOkHttpClient();
        }
        return okHttpClient;
    }
    
    /**
     * 获取用户API服务
     */
    public UserApiService getUserApiService() {
        if (userApiService == null) {
            userApiService = getService(UserApiService.class);
        }
        return userApiService;
    }
    
    /**
     * 获取分类API服务
     */
    public CategoryApiService getCategoryApiService() {
        if (categoryApiService == null) {
            categoryApiService = getService(CategoryApiService.class);
        }
        return categoryApiService;
    }
    
    /**
     * 获取交易记录API服务
     */
    public TransactionApiService getTransactionApiService() {
        if (transactionApiService == null) {
            transactionApiService = getService(TransactionApiService.class);
        }
        return transactionApiService;
    }
    
    /**
     * 获取预算API服务
     */
    public BudgetApiService getBudgetApiService() {
        if (budgetApiService == null) {
            budgetApiService = getService(BudgetApiService.class);
        }
        return budgetApiService;
    }
    
    /**
     * 获取统计API服务
     */
    public StatisticsApiService getStatisticsApiService() {
        if (statisticsApiService == null) {
            statisticsApiService = getService(StatisticsApiService.class);
        }
        return statisticsApiService;
    }
    
    /**
     * 获取Retrofit实例
     */
    public Retrofit getRetrofit() {
        return retrofit;
    }
} 