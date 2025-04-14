package com.zjf.fincialsystem.network;

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.zjf.fincialsystem.BuildConfig;
import com.zjf.fincialsystem.network.api.BudgetApiService;
import com.zjf.fincialsystem.network.api.CategoryApiService;
import com.zjf.fincialsystem.network.api.NotificationApiService;
import com.zjf.fincialsystem.network.api.StatisticsApiService;
import com.zjf.fincialsystem.network.api.TransactionApiService;
import com.zjf.fincialsystem.network.api.UserApiService;
import com.zjf.fincialsystem.network.converter.CommonResponseConverter;
import com.zjf.fincialsystem.network.converter.RawJsonBodyConverterFactory;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 网络请求管理类
 * 使用单例模式
 */
public class NetworkManager {
    
    private static final String TAG = "NetworkManager";
    private static final String BASE_URL = "http://172.15.32.34:80/dev-api/"; // 开发环境URL
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
    private NotificationApiService notificationApiService;
    
    private NetworkManager() {
        // 创建日期格式适配器，支持多种日期格式
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .setLenient() // 增加宽松解析
                .registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                    @Override
                    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        try {
                            String dateStr = json.getAsString();
                            // 尝试多种可能的日期格式
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            return format.parse(dateStr);
                        } catch (ParseException e1) {
                            try {
                                // 尝试第二种格式
                                SimpleDateFormat format2 = new SimpleDateFormat("MMM d, yyyy HH:mm:ss");
                                return format2.parse(json.getAsString());
                            } catch (ParseException e2) {
                                LogUtils.e(TAG, "日期解析失败: " + json.getAsString(), e2);
                                // 解析失败返回当前时间
                                return new Date();
                            }
                        }
                    }
                })
                .create();
        
        // 创建OkHttpClient
        okHttpClient = createOkHttpClient();
        
        // 创建Retrofit实例
        retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .client(okHttpClient)
                .addConverterFactory(CommonResponseConverter.create()) // 添加通用响应转换器，优先级最高
                .addConverterFactory(GsonConverterFactory.create(gson)) // 使用自定义Gson配置
                .build();
        
        // 创建API服务
        transactionApiService = retrofit.create(TransactionApiService.class);
        
        LogUtils.i(TAG, "NetworkManager初始化完成，BASE_URL: " + BASE_URL);
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
            budgetApiService = retrofit.create(BudgetApiService.class);
            statisticsApiService = retrofit.create(StatisticsApiService.class);
            notificationApiService = retrofit.create(NotificationApiService.class);
            
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
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
                LogUtils.d(TAG, "API请求日志: " + message));
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
        
        // 添加SSL配置，信任所有证书（仅用于开发测试，生产环境应使用正式证书）
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            
            LogUtils.d("已配置SSL信任所有证书（仅用于开发测试）");
        } catch (Exception e) {
            LogUtils.e("SSL配置失败: " + e.getMessage(), e);
        }
        
        // 在开发模式下添加模拟数据拦截器
        // 不再使用模拟数据，改为真实接口
        /*if (BuildConfig.DEBUG) {
            builder.addInterceptor(new MockInterceptor());
            LogUtils.d("Added MockInterceptor for debug mode");
        }*/
        
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
     * 获取通知API服务
     */
    public NotificationApiService getNotificationApiService() {
        if (notificationApiService == null) {
            notificationApiService = getService(NotificationApiService.class);
        }
        return notificationApiService;
    }
    
    /**
     * 获取Retrofit实例
     */
    public Retrofit getRetrofit() {
        return retrofit;
    }
    
    /**
     * 获取基础URL
     * @return 基础URL
     */
    public String getBaseUrl() {
        // 检查IP地址是否有效
        if (BASE_URL.startsWith("http")) {
            return BASE_URL;
        } else {
            // 如果BASE_URL不是以http开头，添加协议前缀
            return "http://172.15.32.34:80/dev-api/";
        }
    }
} 