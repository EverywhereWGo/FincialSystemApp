package com.zjf.fincialsystem.network.converter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.zjf.fincialsystem.model.Notification;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.utils.LogUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * 原始JSON响应体转换器工厂
 * 用于处理不符合预期格式的JSON响应
 */
public class RawJsonBodyConverterFactory extends Converter.Factory {
    private static final String TAG = "RawJsonConverter";
    private Gson gson;

    private RawJsonBodyConverterFactory(Gson gson) {
        this.gson = gson;
    }

    public static RawJsonBodyConverterFactory create() {
        return create(new Gson());
    }

    public static RawJsonBodyConverterFactory create(Gson gson) {
        return new RawJsonBodyConverterFactory(gson);
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {
        // 只处理ApiResponse<List<Transaction>>类型的响应
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            
            if (rawType == ApiResponse.class && typeArguments.length == 1) {
                Type innerType = typeArguments[0];
                
                if (innerType instanceof ParameterizedType) {
                    ParameterizedType innerParameterizedType = (ParameterizedType) innerType;
                    Type innerRawType = innerParameterizedType.getRawType();
                    Type[] innerTypeArguments = innerParameterizedType.getActualTypeArguments();
                    
                    if (innerRawType == List.class && innerTypeArguments.length == 1) {
                        Type elementType = innerTypeArguments[0];
                        if (elementType == Transaction.class) {
                            LogUtils.d(TAG, "使用原始JSON响应体转换器处理交易记录列表");
                            return new TransactionListResponseBodyConverter<>(gson, type);
                        }
                        else if (elementType == Notification.class) {
                            LogUtils.d(TAG, "使用原始JSON响应体转换器处理通知列表");
                            return new NotificationListResponseBodyConverter<>(gson, type);
                        }
                    }
                }
            }
            
            // 通用处理ApiResponse<List<T>>类型的响应
            if (rawType == ApiResponse.class) {
                @SuppressWarnings("unchecked")
                Converter<ResponseBody, ?> converter = new Converter<ResponseBody, Object>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public Object convert(ResponseBody value) throws IOException {
                        String json = value.string();
                        try {
                            return gson.fromJson(json, type);
                        } catch (JsonSyntaxException e) {
                            if (e.getMessage() != null && e.getMessage().contains("Expected BEGIN_ARRAY but was BEGIN_OBJECT")) {
                                // 尝试处理rows字段中的对象问题
                                try {
                                    // 使用新的JSON解析方式
                                    JsonElement jsonElement = new Gson().fromJson(json, JsonElement.class);
                                    if (jsonElement.isJsonObject()) {
                                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                                        if (jsonObject.has("rows") && jsonObject.get("rows").isJsonArray()) {
                                            // 手动创建正确的ApiResponse对象
                                            ApiResponse<?> apiResponse = new ApiResponse<>();
                                            
                                            // 设置基本字段
                                            if (jsonObject.has("code")) {
                                                apiResponse.setCode(jsonObject.get("code").getAsInt());
                                            }
                                            
                                            if (jsonObject.has("msg")) {
                                                apiResponse.setMsg(jsonObject.get("msg").getAsString());
                                            }
                                            
                                            if (jsonObject.has("total")) {
                                                apiResponse.setTotal(jsonObject.get("total").getAsLong());
                                            }
                                            
                                            // 处理rows字段，确保它能正确解析
                                            JsonArray rowsArray = jsonObject.getAsJsonArray("rows");
                                            apiResponse.setRows(gson.fromJson(rowsArray, List.class));
                                            
                                            return apiResponse;
                                        }
                                    }
                                } catch (Exception ex) {
                                    LogUtils.e(TAG, "JSON转换失败", ex);
                                    // 如果解析失败，抛出原始异常
                                    throw e;
                                }
                            }
                            throw e;
                        }
                    }
                };
                
                return converter;
            }
        }
        
        // 不符合条件的类型返回null，让下一个转换器处理
        return null;
    }

    /**
     * 通知列表响应体转换器
     * 专门处理服务端返回的通知列表数据格式不一致的情况
     */
    private static class NotificationListResponseBodyConverter<T> implements Converter<ResponseBody, T> {
        private final Gson gson;
        private final Type type;

        NotificationListResponseBodyConverter(Gson gson, Type type) {
            this.gson = gson;
            this.type = type;
        }

        @Override
        public T convert(ResponseBody value) throws IOException {
            String json = value.string();
            LogUtils.d(TAG, "原始通知响应JSON: " + json);
            
            try {
                // 先尝试常规转换
                return gson.fromJson(json, type);
            } catch (Exception e) {
                LogUtils.e(TAG, "通知转换失败，尝试特殊处理: " + e.getMessage(), e);
                
                try {
                    // 解析JSON对象
                    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                    
                    // 创建一个标准的ApiResponse对象
                    ApiResponse<List<Notification>> response = new ApiResponse<>();
                    
                    // 提取code和msg字段
                    if (jsonObject.has("code")) {
                        response.setCode(jsonObject.get("code").getAsInt());
                    }
                    
                    if (jsonObject.has("msg")) {
                        response.setMsg(jsonObject.get("msg").getAsString());
                    }
                    
                    // 初始化为空列表，而不是null
                    List<Notification> notificationList = new ArrayList<>();
                    
                    // 检查rows字段
                    if (jsonObject.has("rows") && !jsonObject.get("rows").isJsonNull()) {
                        JsonElement rowsElement = jsonObject.get("rows");
                        LogUtils.d(TAG, "处理通知rows字段, 类型: " + rowsElement.getClass().getSimpleName() + ", isArray: " + rowsElement.isJsonArray() + ", isObject: " + rowsElement.isJsonObject());
                        
                        if (rowsElement.isJsonArray()) {
                            JsonArray rowsArray = rowsElement.getAsJsonArray();
                            LogUtils.d(TAG, "通知rows是JsonArray，元素数: " + rowsArray.size());
                            
                            for (int i = 0; i < rowsArray.size(); i++) {
                                JsonElement element = rowsArray.get(i);
                                if (element.isJsonObject()) {
                                    try {
                                        Notification notification = gson.fromJson(element, Notification.class);
                                        if (notification != null) {
                                            notificationList.add(notification);
                                        }
                                    } catch (Exception ex) {
                                        LogUtils.e(TAG, "解析通知对象失败: " + ex.getMessage());
                                    }
                                }
                            }
                        }
                    }
                    
                    // 设置响应数据
                    response.setData(notificationList);
                    
                    // 如果total字段存在，则设置
                    if (jsonObject.has("total")) {
                        response.setTotal(jsonObject.get("total").getAsLong());
                    } else {
                        // 没有total字段，使用列表大小作为总数
                        response.setTotal(notificationList.size());
                    }
                    
                    LogUtils.d(TAG, "特殊处理成功，解析到" + notificationList.size() + "条通知记录");
                    return (T) response;
                } catch (Exception ex) {
                    LogUtils.e(TAG, "特殊处理通知也失败: " + ex.getMessage(), ex);
                    
                    // 返回一个空的响应对象，避免应用崩溃
                    ApiResponse<List<Notification>> emptyResponse = new ApiResponse<>();
                    emptyResponse.setCode(500);
                    emptyResponse.setMsg("通知数据解析失败: " + ex.getMessage());
                    emptyResponse.setData(new ArrayList<>());
                    emptyResponse.setTotal(0);
                    return (T) emptyResponse;
                }
            } finally {
                value.close();
            }
        }
    }

    /**
     * 交易记录列表响应体转换器
     * 专门处理服务端返回的交易记录列表数据格式不一致的情况
     */
    private static class TransactionListResponseBodyConverter<T> implements Converter<ResponseBody, T> {
        private final Gson gson;
        private final Type type;

        TransactionListResponseBodyConverter(Gson gson, Type type) {
            this.gson = gson;
            this.type = type;
        }

        @Override
        public T convert(ResponseBody value) throws IOException {
            String json = value.string();
            LogUtils.d(TAG, "原始交易响应JSON: " + json);
            
            try {
                // 先尝试常规转换
                return gson.fromJson(json, type);
            } catch (Exception e) {
                LogUtils.e(TAG, "交易列表转换失败，尝试特殊处理: " + e.getMessage(), e);
                
                try {
                    // 手动解析JSON响应
                    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                    
                    // 创建ApiResponse对象
                    ApiResponse<List<Transaction>> response = new ApiResponse<>();
                    
                    // 设置基本字段
                    if (jsonObject.has("code")) {
                        response.setCode(jsonObject.get("code").getAsInt());
                    }
                    
                    if (jsonObject.has("msg")) {
                        response.setMsg(jsonObject.get("msg").getAsString());
                    }
                    
                    // 初始化为空列表
                    List<Transaction> transactions = new ArrayList<>();
                    
                    // 尝试处理data字段
                    if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                        JsonElement dataElement = jsonObject.get("data");
                        
                        if (dataElement.isJsonArray()) {
                            // data字段是数组，直接解析
                            JsonArray dataArray = dataElement.getAsJsonArray();
                            for (int i = 0; i < dataArray.size(); i++) {
                                try {
                                    Transaction transaction = gson.fromJson(dataArray.get(i), Transaction.class);
                                    if (transaction != null) {
                                        // 处理可能的日期转换
                                        if (dataArray.get(i).isJsonObject()) {
                                            JsonObject transObj = dataArray.get(i).getAsJsonObject();
                                            if (transObj.has("transactionTime") && !transObj.get("transactionTime").isJsonNull()) {
                                                try {
                                                    long timestamp = transObj.get("transactionTime").getAsLong();
                                                    transaction.setDate(new Date(timestamp));
                                                } catch (Exception dateEx) {
                                                    LogUtils.e(TAG, "交易日期转换失败: " + dateEx.getMessage());
                                                }
                                            }
                                        }
                                        transactions.add(transaction);
                                    }
                                } catch (Exception ex) {
                                    LogUtils.e(TAG, "解析交易对象失败: " + ex.getMessage());
                                }
                            }
                            // 设置到data字段
                            response.setData(transactions);
                        }
                    }
                    
                    // 如果data为空，则尝试处理rows字段
                    if ((transactions == null || transactions.isEmpty()) && jsonObject.has("rows") && !jsonObject.get("rows").isJsonNull()) {
                        JsonElement rowsElement = jsonObject.get("rows");
                        
                        if (rowsElement.isJsonArray()) {
                            // rows字段是数组，直接解析
                            JsonArray rowsArray = rowsElement.getAsJsonArray();
                            transactions = new ArrayList<>();
                            
                            for (int i = 0; i < rowsArray.size(); i++) {
                                try {
                                    Transaction transaction = gson.fromJson(rowsArray.get(i), Transaction.class);
                                    if (transaction != null) {
                                        // 处理可能的日期转换
                                        if (rowsArray.get(i).isJsonObject()) {
                                            JsonObject transObj = rowsArray.get(i).getAsJsonObject();
                                            if (transObj.has("transactionTime") && !transObj.get("transactionTime").isJsonNull()) {
                                                try {
                                                    long timestamp = transObj.get("transactionTime").getAsLong();
                                                    transaction.setDate(new Date(timestamp));
                                                } catch (Exception dateEx) {
                                                    LogUtils.e(TAG, "交易日期转换失败: " + dateEx.getMessage());
                                                }
                                            }
                                        }
                                        transactions.add(transaction);
                                    }
                                } catch (Exception ex) {
                                    LogUtils.e(TAG, "解析交易对象失败: " + ex.getMessage());
                                }
                            }
                            
                            // 设置到rows字段
                            response.setRows(transactions);
                        }
                    }
                    
                    // 设置total字段
                    if (jsonObject.has("total")) {
                        response.setTotal(jsonObject.get("total").getAsLong());
                    } else {
                        // 如果没有total字段，使用列表大小
                        response.setTotal((transactions != null) ? transactions.size() : 0);
                    }
                    
                    LogUtils.d(TAG, "特殊处理成功，解析到" + ((transactions != null) ? transactions.size() : 0) + "条交易记录");
                    return (T) response;
                } catch (Exception ex) {
                    LogUtils.e(TAG, "特殊处理交易记录失败: " + ex.getMessage(), ex);
                    
                    // 返回一个空的响应对象，避免应用崩溃
                    ApiResponse<List<Transaction>> emptyResponse = new ApiResponse<>();
                    emptyResponse.setCode(500);
                    emptyResponse.setMsg("交易数据解析失败: " + ex.getMessage());
                    emptyResponse.setData(new ArrayList<>());
                    emptyResponse.setTotal(0);
                    return (T) emptyResponse;
                }
            } finally {
                value.close();
            }
        }
    }
} 