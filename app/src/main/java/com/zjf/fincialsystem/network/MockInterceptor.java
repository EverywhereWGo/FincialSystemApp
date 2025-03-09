package com.zjf.fincialsystem.network;

import android.os.Build;
import android.text.TextUtils;

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zjf.fincialsystem.db.DatabaseManager;
import com.zjf.fincialsystem.model.Budget;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.model.User;
import com.zjf.fincialsystem.utils.DeviceUtils;
import com.zjf.fincialsystem.network.model.AddBudgetRequest;
import com.zjf.fincialsystem.network.model.AddCategoryRequest;
import com.zjf.fincialsystem.network.model.AddTransactionRequest;
import com.zjf.fincialsystem.network.model.LoginRequest;
import com.zjf.fincialsystem.network.model.LoginResponse;
import com.zjf.fincialsystem.network.model.RegisterRequest;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.utils.SecurityUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * 模拟网络请求的拦截器
 * 用于在开发阶段模拟后端API响应
 */
public class MockInterceptor implements Interceptor {

    private static final String TAG = "MockInterceptor";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final Gson gson = new GsonBuilder()
            .setDateFormat("MMM d, yyyy HH:mm:ss")
            .setLenient()
            .create();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String url = request.url().toString();
        String method = request.method();
        String path = request.url().encodedPath();

        LogUtils.d(TAG, "Mock intercepting: " + method + " " + url);

        // 处理预算删除请求
        if (method.equals("DELETE") && path.contains("/api/budgets/")) {
            return createDeleteBudgetResponse(request);
        }
        
        // 根据不同的API路径返回不同的模拟数据
        if (path.contains("/api/login") || url.contains("/api/login")) {
            return createLoginResponse(request);
        } else if (path.contains("/api/register") || url.contains("/api/register")) {
            return createRegisterResponse(request);
        } else if (path.contains("/api/categories") || url.contains("/api/categories")) {
            return createCategoriesResponse(request);
        } else if (path.contains("/api/statistics") || url.contains("/api/statistics")) {
            return createStatisticsResponse(request);
        } else if (path.contains("/api/transactions") || url.contains("/api/transactions")) {
            return createTransactionsResponse(request);
        } else if (path.contains("/api/budgets") || url.contains("/api/budgets")) {
            return createBudgetsResponse(request);
        } else if (path.contains("/api/user") || url.contains("/api/user")) {
            return createUserResponse(request);
        }

        // 对于不模拟的请求，继续正常请求
        LogUtils.d(TAG, "Not mocking: " + url);
        return chain.proceed(request);
    }

    /**
     * 创建登录响应
     */
    private Response createLoginResponse(Request request) {
        try {
            // 解析请求体
            String requestBody = request.body() != null ? bodyToString(request.body()) : "";
            LoginRequest loginRequest = gson.fromJson(requestBody, LoginRequest.class);
            
            // 创建模拟用户
            User user = new User();
            user.setId(1);
            user.setUsername(loginRequest.getUsername());
            user.setName("管理员");
            user.setEmail("admin@example.com");
            user.setPhone("13800138000");
            user.setRole("admin");
            user.setLastLoginTime(new Date());
            
            // 创建登录响应
            LoginResponseData responseData = new LoginResponseData();
            responseData.setUser(user);
            
            // 生成随机token
            String token = UUID.randomUUID().toString();
            responseData.setToken(token);
            
            // 设置过期时间为当前时间 + 30天
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 30);
            long expiryTime = calendar.getTimeInMillis();
            
            LogUtils.d("MockInterceptor", "设置令牌过期时间: " + new Date(expiryTime));
            responseData.setExpiryTime(expiryTime);
            
            // 设置设备信息
            responseData.setDeviceInfo(loginRequest.getDeviceInfo());
            
            // 创建成功的API响应
            ApiResponse<LoginResponseData> apiResponse = new ApiResponse<>();
            apiResponse.setCode(200);
            apiResponse.setMessage("操作成功");
            apiResponse.setData(responseData);
            
            String responseJson = gson.toJson(apiResponse);
            LogUtils.d("Mock login response: " + responseJson);
            
            // 构建响应
            return new Response.Builder()
                    .code(200)
                    .message("OK")
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .body(ResponseBody.create(JSON, responseJson))
                    .addHeader("content-type", "application/json")
                    .build();
        } catch (Exception e) {
            LogUtils.e(TAG, "创建登录响应时出错", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }

    /**
     * 创建注册响应
     */
    private Response createRegisterResponse(Request request) {
        try {
            // 模拟注册成功响应
            ApiResponse<String> response = ApiResponse.success("注册成功");
            String jsonResponse = gson.toJson(response);
            
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(JSON, jsonResponse))
                    .build();
        } catch (Exception e) {
            LogUtils.e(TAG, "Error creating mock register response", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }

    /**
     * 创建分类列表响应
     */
    private Response createCategoriesResponse(Request request) {
        try {
            // 获取请求参数
            String queryType = request.url().queryParameter("type");
            int type = TextUtils.isEmpty(queryType) ? 0 : Integer.parseInt(queryType);
            
            List<Category> categories = new ArrayList<>();
            
            // 添加支出分类
            if (type == 0 || type == Category.TYPE_EXPENSE) {
                categories.add(createCategory(1, "餐饮", Category.TYPE_EXPENSE, "ic_food", "#FF5722"));
                categories.add(createCategory(2, "购物", Category.TYPE_EXPENSE, "ic_shopping", "#4CAF50"));
                categories.add(createCategory(3, "交通", Category.TYPE_EXPENSE, "ic_transport", "#2196F3"));
                categories.add(createCategory(4, "住房", Category.TYPE_EXPENSE, "ic_home", "#9C27B0"));
                categories.add(createCategory(5, "娱乐", Category.TYPE_EXPENSE, "ic_entertainment", "#FFC107"));
            }
            
            // 添加收入分类
            if (type == 0 || type == Category.TYPE_INCOME) {
                categories.add(createCategory(6, "工资", Category.TYPE_INCOME, "ic_salary", "#3F51B5"));
                categories.add(createCategory(7, "奖金", Category.TYPE_INCOME, "ic_bonus", "#E91E63"));
                categories.add(createCategory(8, "理财", Category.TYPE_INCOME, "ic_investment", "#009688"));
                categories.add(createCategory(9, "兼职", Category.TYPE_INCOME, "ic_parttime", "#795548"));
            }
            
            ApiResponse<List<Category>> response = ApiResponse.success(categories);
            String jsonResponse = gson.toJson(response);
            
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(JSON, jsonResponse))
                    .build();
        } catch (Exception e) {
            LogUtils.e(TAG, "Error creating mock categories response", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }

    /**
     * 创建统计数据响应
     */
    private Response createStatisticsResponse(Request request) {
        try {
            String url = request.url().toString();
            
            if (url.contains("/overview")) {
                return createOverviewResponse(request);
            } else if (url.contains("/income-by-category")) {
                return createIncomeByCategoryResponse(request);
            } else if (url.contains("/expense-by-category")) {
                return createExpenseByCategoryResponse(request);
            } else if (url.contains("/trend")) {
                return createTrendResponse(request);
            } else if (url.contains("/budget-usage")) {
                return createBudgetUsageResponse(request);
            }
            
            // 默认返回概览统计
            return createOverviewResponse(request);
        } catch (Exception e) {
            LogUtils.e(TAG, "Error creating mock statistics response", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }

    /**
     * 创建概览统计响应
     */
    private Response createOverviewResponse(Request request) {
        try {
            // 获取请求参数
            String period = request.url().queryParameter("period");
            if (period == null) {
                period = "monthly"; // 默认月度统计
            }
            
            // 创建概览数据
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            
            // 直接使用基本类型Double.valueOf()以避免装箱问题
            data.put("totalIncome", Double.valueOf(15800.0));
            data.put("totalExpense", Double.valueOf(9650.0));
            data.put("totalBalance", Double.valueOf(6150.0));
            data.put("periodType", period);
            
            if (period.equals("monthly")) {
                data.put("periodName", "2023年11月");
            } else if (period.equals("yearly")) {
                data.put("periodName", "2023年");
            } else if (period.equals("weekly")) {
                data.put("periodName", "2023年第45周");
            } else {
                data.put("periodName", "2023年11月15日");
            }
            
            ApiResponse<java.util.Map<String, Object>> response = ApiResponse.success(data);
            
            // 使用toJson前先打印数据结构，帮助调试
            LogUtils.d(TAG, "概览统计数据: " + data);
            
            String jsonResponse = gson.toJson(response);
            
            LogUtils.d(TAG, "概览统计JSON: " + jsonResponse);
            
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(JSON, jsonResponse))
                    .build();
        } catch (Exception e) {
            LogUtils.e(TAG, "Error creating mock overview response", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }

    /**
     * 创建收入分类统计响应
     */
    private Response createIncomeByCategoryResponse(Request request) {
        try {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            
            List<java.util.Map<String, Object>> categoryStats = new ArrayList<>();
            
            java.util.Map<String, Object> stat1 = new java.util.HashMap<>();
            stat1.put("categoryId", 6);
            stat1.put("categoryName", "工资");
            stat1.put("categoryIcon", "ic_salary");
            stat1.put("categoryColor", "#3F51B5");
            stat1.put("amount", 12500.0);
            stat1.put("percentage", 79.11);
            categoryStats.add(stat1);
            
            java.util.Map<String, Object> stat2 = new java.util.HashMap<>();
            stat2.put("categoryId", 7);
            stat2.put("categoryName", "奖金");
            stat2.put("categoryIcon", "ic_bonus");
            stat2.put("categoryColor", "#E91E63");
            stat2.put("amount", 2000.0);
            stat2.put("percentage", 12.66);
            categoryStats.add(stat2);
            
            java.util.Map<String, Object> stat3 = new java.util.HashMap<>();
            stat3.put("categoryId", 8);
            stat3.put("categoryName", "理财");
            stat3.put("categoryIcon", "ic_investment");
            stat3.put("categoryColor", "#009688");
            stat3.put("amount", 1300.0);
            stat3.put("percentage", 8.23);
            categoryStats.add(stat3);
            
            data.put("totalAmount", 15800.0);
            data.put("categoryStats", categoryStats);
            
            ApiResponse<java.util.Map<String, Object>> response = ApiResponse.success(data);
            String jsonResponse = gson.toJson(response);
            
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(JSON, jsonResponse))
                    .build();
        } catch (Exception e) {
            LogUtils.e(TAG, "Error creating mock income-by-category response", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }

    /**
     * 创建支出分类统计响应
     */
    private Response createExpenseByCategoryResponse(Request request) {
        try {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            
            List<java.util.Map<String, Object>> categoryStats = new ArrayList<>();
            
            java.util.Map<String, Object> stat1 = new java.util.HashMap<>();
            stat1.put("categoryId", 2);
            stat1.put("categoryName", "购物");
            stat1.put("categoryIcon", "ic_shopping");
            stat1.put("categoryColor", "#4CAF50");
            stat1.put("amount", 3650.0);
            stat1.put("percentage", 37.82);
            categoryStats.add(stat1);
            
            java.util.Map<String, Object> stat2 = new java.util.HashMap<>();
            stat2.put("categoryId", 4);
            stat2.put("categoryName", "住房");
            stat2.put("categoryIcon", "ic_home");
            stat2.put("categoryColor", "#9C27B0");
            stat2.put("amount", 2800.0);
            stat2.put("percentage", 29.02);
            categoryStats.add(stat2);
            
            java.util.Map<String, Object> stat3 = new java.util.HashMap<>();
            stat3.put("categoryId", 1);
            stat3.put("categoryName", "餐饮");
            stat3.put("categoryIcon", "ic_food");
            stat3.put("categoryColor", "#FF5722");
            stat3.put("amount", 1850.0);
            stat3.put("percentage", 19.17);
            categoryStats.add(stat3);
            
            java.util.Map<String, Object> stat4 = new java.util.HashMap<>();
            stat4.put("categoryId", 3);
            stat4.put("categoryName", "交通");
            stat4.put("categoryIcon", "ic_transport");
            stat4.put("categoryColor", "#2196F3");
            stat4.put("amount", 850.0);
            stat4.put("percentage", 8.81);
            categoryStats.add(stat4);
            
            java.util.Map<String, Object> stat5 = new java.util.HashMap<>();
            stat5.put("categoryId", 5);
            stat5.put("categoryName", "娱乐");
            stat5.put("categoryIcon", "ic_entertainment");
            stat5.put("categoryColor", "#FFC107");
            stat5.put("amount", 500.0);
            stat5.put("percentage", 5.18);
            categoryStats.add(stat5);
            
            data.put("totalAmount", 9650.0);
            data.put("categoryStats", categoryStats);
            
            ApiResponse<java.util.Map<String, Object>> response = ApiResponse.success(data);
            String jsonResponse = gson.toJson(response);
            
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(JSON, jsonResponse))
                    .build();
        } catch (Exception e) {
            LogUtils.e(TAG, "Error creating mock expense-by-category response", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }

    /**
     * 创建趋势统计响应
     */
    private Response createTrendResponse(Request request) {
        try {
            // 获取请求参数
            String queryType = request.url().queryParameter("type");
            String period = request.url().queryParameter("period");
            
            int type = TextUtils.isEmpty(queryType) ? 0 : Integer.parseInt(queryType);
            if (period == null) {
                period = "monthly"; // 默认月度统计
            }
            
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            List<java.util.Map<String, Object>> trendData = new ArrayList<>();
            
            if (period.equals("monthly")) {
                // 月度趋势，按天统计
                for (int i = 1; i <= 30; i++) {
                    java.util.Map<String, Object> item = new java.util.HashMap<>();
                    item.put("date", "2023-11-" + (i < 10 ? "0" + i : i));
                    item.put("amount", type == 0 ? 200 + Math.random() * 300 : 500 + Math.random() * 200);
                    trendData.add(item);
                }
            } else if (period.equals("yearly")) {
                // 年度趋势，按月统计
                String[] months = {"一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"};
                for (int i = 0; i < 12; i++) {
                    java.util.Map<String, Object> item = new java.util.HashMap<>();
                    item.put("date", months[i]);
                    item.put("amount", type == 0 ? 7000 + Math.random() * 3000 : 12000 + Math.random() * 4000);
                    trendData.add(item);
                }
            }
            
            data.put("type", type);
            data.put("period", period);
            data.put("trendData", trendData);
            
            ApiResponse<java.util.Map<String, Object>> response = ApiResponse.success(data);
            String jsonResponse = gson.toJson(response);
            
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(JSON, jsonResponse))
                    .build();
        } catch (Exception e) {
            LogUtils.e(TAG, "Error creating mock trend response", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }

    /**
     * 创建预算使用统计响应
     */
    private Response createBudgetUsageResponse(Request request) {
        try {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            
            List<java.util.Map<String, Object>> budgetStats = new ArrayList<>();
            
            java.util.Map<String, Object> stat1 = new java.util.HashMap<>();
            stat1.put("budgetId", 1);
            stat1.put("categoryId", 1);
            stat1.put("categoryName", "餐饮");
            stat1.put("categoryIcon", "ic_food");
            stat1.put("categoryColor", "#FF5722");
            stat1.put("budgetAmount", 3000.0);
            stat1.put("usedAmount", 1850.0);
            stat1.put("remainAmount", 1150.0);
            stat1.put("usagePercentage", 61.67);
            budgetStats.add(stat1);
            
            java.util.Map<String, Object> stat2 = new java.util.HashMap<>();
            stat2.put("budgetId", 2);
            stat2.put("categoryId", 2);
            stat2.put("categoryName", "购物");
            stat2.put("categoryIcon", "ic_shopping");
            stat2.put("categoryColor", "#4CAF50");
            stat2.put("budgetAmount", 2000.0);
            stat2.put("usedAmount", 3650.0);
            stat2.put("remainAmount", -1650.0);
            stat2.put("usagePercentage", 182.5);
            budgetStats.add(stat2);
            
            java.util.Map<String, Object> stat3 = new java.util.HashMap<>();
            stat3.put("budgetId", 3);
            stat3.put("categoryId", 3);
            stat3.put("categoryName", "交通");
            stat3.put("categoryIcon", "ic_transport");
            stat3.put("categoryColor", "#2196F3");
            stat3.put("budgetAmount", 1500.0);
            stat3.put("usedAmount", 850.0);
            stat3.put("remainAmount", 650.0);
            stat3.put("usagePercentage", 56.67);
            budgetStats.add(stat3);
            
            java.util.Map<String, Object> stat4 = new java.util.HashMap<>();
            stat4.put("budgetId", 4);
            stat4.put("categoryId", 5);
            stat4.put("categoryName", "娱乐");
            stat4.put("categoryIcon", "ic_entertainment");
            stat4.put("categoryColor", "#FFC107");
            stat4.put("budgetAmount", 1000.0);
            stat4.put("usedAmount", 500.0);
            stat4.put("remainAmount", 500.0);
            stat4.put("usagePercentage", 50.0);
            budgetStats.add(stat4);
            
            data.put("totalBudget", 7500.0);
            data.put("totalUsed", 6850.0);
            data.put("totalRemain", 650.0);
            data.put("overallPercentage", 91.33);
            data.put("budgetStats", budgetStats);
            
            ApiResponse<java.util.Map<String, Object>> response = ApiResponse.success(data);
            String jsonResponse = gson.toJson(response);
            
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(JSON, jsonResponse))
                    .build();
        } catch (Exception e) {
            LogUtils.e(TAG, "Error creating mock budget-usage response", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }

    /**
     * 创建交易记录响应
     */
    private Response createTransactionsResponse(Request request) {
        try {
            List<Transaction> transactions = new ArrayList<>();
            
            // 添加一些模拟交易数据
            transactions.add(createTransaction(1, 1, 1, Transaction.TYPE_EXPENSE, 35.5, "午餐", new Date()));
            transactions.add(createTransaction(2, 1, 2, Transaction.TYPE_EXPENSE, 128.0, "超市购物", new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)));
            transactions.add(createTransaction(3, 1, 6, Transaction.TYPE_INCOME, 12500.0, "工资", new Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000)));
            
            // 为交易记录设置分类信息
            for (Transaction transaction : transactions) {
                Category category = new Category();
                category.setId(transaction.getCategoryId());
                if (transaction.getCategoryId() == 1) {
                    category.setName("餐饮");
                    category.setType(Category.TYPE_EXPENSE);
                    category.setIcon("ic_food");
                    category.setColor("#FF5722");
                } else if (transaction.getCategoryId() == 2) {
                    category.setName("购物");
                    category.setType(Category.TYPE_EXPENSE);
                    category.setIcon("ic_shopping");
                    category.setColor("#4CAF50");
                } else if (transaction.getCategoryId() == 6) {
                    category.setName("工资");
                    category.setType(Category.TYPE_INCOME);
                    category.setIcon("ic_salary");
                    category.setColor("#3F51B5");
                }
                transaction.setCategory(category);
            }
            
            ApiResponse<List<Transaction>> response = ApiResponse.success(transactions);
            String jsonResponse = gson.toJson(response);
            
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(JSON, jsonResponse))
                    .build();
        } catch (Exception e) {
            LogUtils.e(TAG, "Error creating mock transactions response", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }

    /**
     * 创建预算响应
     */
    private Response createBudgetsResponse(Request request) {
        try {
            // 获取请求参数
            String period = request.url().queryParameter("period");
            if (period == null) {
                period = "monthly"; // 默认返回月度预算
            }
            
            List<Budget> budgets = new ArrayList<>();
            boolean isCurrent = request.url().toString().contains("/current");
            
            if (period.equals("monthly") || period.isEmpty()) {
                // 月度预算
                Budget budget1 = new Budget();
                budget1.setId(1);
                budget1.setUserId(1);
                budget1.setCategoryId(1); // 餐饮分类
                budget1.setAmount(3000);
                budget1.setPeriod("monthly");
                // 设置预算周期为当前月
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                budget1.setStartDate(calendar.getTime());
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                budget1.setEndDate(calendar.getTime());
                budget1.setNotifyPercent(80);
                budget1.setNotifyEnabled(true);
                // 设置已使用金额为2400
                budget1.setUsedAmount(2400);
                budgets.add(budget1);
                
                Budget budget2 = new Budget();
                budget2.setId(2);
                budget2.setUserId(1);
                budget2.setCategoryId(2); // 购物分类
                budget2.setAmount(2000);
                budget2.setPeriod("monthly");
                budget2.setStartDate(budget1.getStartDate());
                budget2.setEndDate(budget1.getEndDate());
                budget2.setNotifyPercent(80);
                budget2.setNotifyEnabled(true);
                // 设置已使用金额为1200
                budget2.setUsedAmount(1200);
                budgets.add(budget2);
                
                Budget budget3 = new Budget();
                budget3.setId(3);
                budget3.setUserId(1);
                budget3.setCategoryId(3); // 交通分类
                budget3.setAmount(800);
                budget3.setPeriod("monthly");
                budget3.setStartDate(budget1.getStartDate());
                budget3.setEndDate(budget1.getEndDate());
                budget3.setNotifyPercent(90);
                budget3.setNotifyEnabled(true);
                // 设置已使用金额为850
                budget3.setUsedAmount(850);
                budgets.add(budget3);
                
                Budget budget4 = new Budget();
                budget4.setId(4);
                budget4.setUserId(1);
                budget4.setCategoryId(5); // 娱乐分类
                budget4.setAmount(1000);
                budget4.setPeriod("monthly");
                budget4.setStartDate(budget1.getStartDate());
                budget4.setEndDate(budget1.getEndDate());
                budget4.setNotifyPercent(80);
                budget4.setNotifyEnabled(true);
                // 设置已使用金额为320
                budget4.setUsedAmount(320);
                budgets.add(budget4);
                
            } else if (period.equals("yearly")) {
                // 年度预算
                Budget budget1 = new Budget();
                budget1.setId(5);
                budget1.setUserId(1);
                budget1.setCategoryId(1); // 餐饮分类
                budget1.setAmount(36000);
                budget1.setPeriod("yearly");
                // 设置预算周期为当前年
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                budget1.setStartDate(calendar.getTime());
                calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR));
                budget1.setEndDate(calendar.getTime());
                budget1.setNotifyPercent(80);
                budget1.setNotifyEnabled(true);
                // 设置已使用金额为25000
                budget1.setUsedAmount(25000);
                budgets.add(budget1);
                
                Budget budget2 = new Budget();
                budget2.setId(6);
                budget2.setUserId(1);
                budget2.setCategoryId(2); // 购物分类
                budget2.setAmount(24000);
                budget2.setPeriod("yearly");
                budget2.setStartDate(budget1.getStartDate());
                budget2.setEndDate(budget1.getEndDate());
                budget2.setNotifyPercent(80);
                budget2.setNotifyEnabled(true);
                // 设置已使用金额为18000
                budget2.setUsedAmount(18000);
                budgets.add(budget2);
            }
            
            // 如果是获取当前预算，只返回当前有效的预算
            if (isCurrent) {
                // 当前只返回月度预算
                budgets = budgets.stream()
                        .filter(budget -> "monthly".equals(budget.getPeriod()))
                        .collect(Collectors.toList());
            }
            
            // 设置分类信息
            for (Budget budget : budgets) {
                Category category = createCategoryForBudget(budget.getCategoryId());
                budget.setCategory(category);
            }
            
            ApiResponse<List<Budget>> response = ApiResponse.success(budgets);
            String jsonResponse = gson.toJson(response);
            
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(JSON, jsonResponse))
                    .build();
        } catch (Exception e) {
            LogUtils.e(TAG, "Error creating mock budgets response", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }

    /**
     * 创建用户信息响应
     */
    private Response createUserResponse(Request request) {
        // 实现用户信息模拟响应
        // 这里略去具体实现...
        User user = new User();
        user.setId(1);
        user.setUsername("admin");
        user.setName("管理员");
        user.setEmail("admin@example.com");
        user.setPhone("13800138000");
        
        ApiResponse<User> response = ApiResponse.success(user);
        String jsonResponse = gson.toJson(response);
        
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(JSON, jsonResponse))
                .build();
    }

    /**
     * 创建错误响应
     */
    private Response createErrorResponse(Request request, int code, String message) {
        ApiResponse<Object> response = ApiResponse.error(code, message);
        String jsonResponse = gson.toJson(response);
        
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(message)
                .body(ResponseBody.create(JSON, jsonResponse))
                .build();
    }

    /**
     * 创建分类对象
     */
    private Category createCategory(long id, String name, int type, String icon, String color) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        category.setIcon(icon);
        category.setColor(color);
        category.setUserId(1); // 默认用户ID
        category.setDefault(true);
        category.setCreatedAt(System.currentTimeMillis());
        category.setUpdatedAt(System.currentTimeMillis());
        return category;
    }

    /**
     * 创建交易记录对象
     */
    private Transaction createTransaction(long id, long userId, long categoryId, int type, double amount, String description, Date date) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setUserId(userId);
        transaction.setCategoryId(categoryId);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setDate(date);
        transaction.setCreatedAt(System.currentTimeMillis());
        transaction.setUpdatedAt(System.currentTimeMillis());
        return transaction;
    }

    /**
     * 登录响应数据类
     */
    private static class LoginResponseData {
        private String token;
        private User user;
        private long expiryTime;
        private String deviceInfo;
        
        public String getToken() {
            return token;
        }
        
        public void setToken(String token) {
            this.token = token;
        }
        
        public User getUser() {
            return user;
        }
        
        public void setUser(User user) {
            this.user = user;
        }
        
        public long getExpiryTime() {
            return expiryTime;
        }
        
        public void setExpiryTime(long expiryTime) {
            this.expiryTime = expiryTime;
        }
        
        public String getDeviceInfo() {
            return deviceInfo;
        }
        
        public void setDeviceInfo(String deviceInfo) {
            this.deviceInfo = deviceInfo;
        }
    }

    /**
     * 为预算创建分类对象
     */
    private Category createCategoryForBudget(long categoryId) {
        Category category = new Category();
        category.setId(categoryId);
        
        if (categoryId == 1) {
            category.setName("餐饮");
            category.setType(Category.TYPE_EXPENSE);
            category.setIcon("ic_food");
            category.setColor("#FF5722");
        } else if (categoryId == 2) {
            category.setName("购物");
            category.setType(Category.TYPE_EXPENSE);
            category.setIcon("ic_shopping");
            category.setColor("#4CAF50");
        } else if (categoryId == 3) {
            category.setName("交通");
            category.setType(Category.TYPE_EXPENSE);
            category.setIcon("ic_transport");
            category.setColor("#2196F3");
        } else if (categoryId == 4) {
            category.setName("住房");
            category.setType(Category.TYPE_EXPENSE);
            category.setIcon("ic_home");
            category.setColor("#9C27B0");
        } else if (categoryId == 5) {
            category.setName("娱乐");
            category.setType(Category.TYPE_EXPENSE);
            category.setIcon("ic_entertainment");
            category.setColor("#FFC107");
        }
        
        return category;
    }

    /**
     * 将RequestBody转换为字符串
     */
    private String bodyToString(RequestBody body) {
        try {
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            return buffer.readUtf8();
        } catch (IOException e) {
            LogUtils.e(TAG, "将请求体转换为字符串失败", e);
            return "";
        }
    }

    /**
     * 创建删除预算响应
     */
    private Response createDeleteBudgetResponse(Request request) {
        try {
            // 创建正确的删除响应，返回布尔类型的data字段
            ApiResponse<Boolean> response = ApiResponse.success(true);
            String jsonResponse = gson.toJson(response);
            
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(JSON, jsonResponse))
                    .build();
        } catch (Exception e) {
            LogUtils.e(TAG, "Error creating mock delete budget response", e);
            return createErrorResponse(request, 500, "服务器内部错误");
        }
    }
} 