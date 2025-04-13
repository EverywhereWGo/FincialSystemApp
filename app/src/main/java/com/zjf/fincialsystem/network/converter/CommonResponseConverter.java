package com.zjf.fincialsystem.network.converter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zjf.fincialsystem.model.Budget;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.network.ApiResponse;
import com.zjf.fincialsystem.utils.LogUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * 通用API响应转换器工厂
 * 用于处理标准格式的API响应转换
 */
public class CommonResponseConverter extends Converter.Factory {
    private static final String TAG = "CommonConverter";
    private final Gson gson;

    private CommonResponseConverter(Gson gson) {
        this.gson = gson;
    }

    public static CommonResponseConverter create() {
        return create(new Gson());
    }

    public static CommonResponseConverter create(Gson gson) {
        return new CommonResponseConverter(gson);
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {
        // 处理ApiResponse<List<T>>类型的响应
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
                        
                        // 根据不同的类型创建对应的转换器
                        if (elementType == Budget.class) {
                            LogUtils.d(TAG, "使用通用响应转换器处理预算列表");
                            return new ListResponseBodyConverter<>(gson, type);
                        } else if (elementType == Category.class) {
                            LogUtils.d(TAG, "使用通用响应转换器处理分类列表");
                            return new ListResponseBodyConverter<>(gson, type);
                        }
                        // 可以根据需要添加更多的类型处理
                    }
                }
            }
        }
        
        // 不符合条件的类型返回null，让下一个转换器处理
        return null;
    }

    /**
     * 列表响应体转换器
     * 用于处理服务端返回的列表数据格式不一致的情况
     */
    private static class ListResponseBodyConverter<T> implements Converter<ResponseBody, T> {
        private final Gson gson;
        private final Type type;

        ListResponseBodyConverter(Gson gson, Type type) {
            this.gson = gson;
            this.type = type;
        }

        @Override
        public T convert(ResponseBody value) throws IOException {
            String json = value.string();
            LogUtils.d(TAG, "原始响应JSON: " + json);
            
            try {
                // 先尝试常规转换
                return gson.fromJson(json, type);
            } catch (Exception e) {
                LogUtils.e(TAG, "常规转换失败，尝试特殊处理: " + e.getMessage(), e);
                
                try {
                    // 解析JSON对象
                    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                    
                    // 创建一个标准的ApiResponse对象
                    ApiResponse<List<?>> response = new ApiResponse<>();
                    
                    // 提取code和msg字段
                    if (jsonObject.has("code")) {
                        response.setCode(jsonObject.get("code").getAsInt());
                    }
                    
                    if (jsonObject.has("msg")) {
                        response.setMsg(jsonObject.get("msg").getAsString());
                    }
                    
                    // 提取total字段
                    if (jsonObject.has("total")) {
                        response.setTotal(jsonObject.get("total").getAsLong());
                    }
                    
                    // 初始化为空列表，而不是null
                    List<Object> itemList = new ArrayList<>();
                    
                    // 处理rows字段
                    if (jsonObject.has("rows") && !jsonObject.get("rows").isJsonNull()) {
                        JsonElement rowsElement = jsonObject.get("rows");
                        LogUtils.d(TAG, "处理rows字段, 类型: " + rowsElement.getClass().getSimpleName());
                        
                        if (rowsElement.isJsonArray()) {
                            JsonArray rowsArray = rowsElement.getAsJsonArray();
                            
                            // 手动处理数组中的每个元素
                            for (JsonElement element : rowsArray) {
                                if (element.isJsonObject()) {
                                    try {
                                        // 动态确定目标类型
                                        Class<?> targetClass = getTargetClass(type);
                                        if (targetClass != null) {
                                            Object item = gson.fromJson(element, targetClass);
                                            if (item != null) {
                                                itemList.add(item);
                                            }
                                        }
                                    } catch (Exception ex) {
                                        LogUtils.e(TAG, "解析对象失败: " + ex.getMessage());
                                    }
                                }
                            }
                        } else if (rowsElement.isJsonObject()) {
                            // 如果rows是单个对象而不是数组，直接解析该对象
                            try {
                                // 动态确定目标类型
                                Class<?> targetClass = getTargetClass(type);
                                if (targetClass != null) {
                                    Object item = gson.fromJson(rowsElement, targetClass);
                                    if (item != null) {
                                        itemList.add(item);
                                    }
                                }
                            } catch (Exception ex) {
                                LogUtils.e(TAG, "解析单个对象失败: " + ex.getMessage());
                            }
                        }
                    }
                    
                    // 如果rows解析失败，检查data字段
                    if (itemList.isEmpty() && jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                        JsonElement dataElement = jsonObject.get("data");
                        
                        if (dataElement.isJsonArray()) {
                            JsonArray dataArray = dataElement.getAsJsonArray();
                            
                            // 手动处理数组中的每个元素
                            for (JsonElement element : dataArray) {
                                if (element.isJsonObject()) {
                                    try {
                                        // 动态确定目标类型
                                        Class<?> targetClass = getTargetClass(type);
                                        if (targetClass != null) {
                                            Object item = gson.fromJson(element, targetClass);
                                            if (item != null) {
                                                itemList.add(item);
                                            }
                                        }
                                    } catch (Exception ex) {
                                        LogUtils.e(TAG, "解析data数组对象失败: " + ex.getMessage());
                                    }
                                }
                            }
                        } else if (dataElement.isJsonObject()) {
                            // 如果data字段是单个对象，尝试直接解析
                            try {
                                // 动态确定目标类型
                                Class<?> targetClass = getTargetClass(type);
                                if (targetClass != null) {
                                    Object item = gson.fromJson(dataElement, targetClass);
                                    if (item != null) {
                                        itemList.add(item);
                                    }
                                }
                            } catch (Exception ex) {
                                LogUtils.e(TAG, "解析data单个对象失败: " + ex.getMessage());
                            }
                        }
                    }
                    
                    // 设置处理后的数据
                    response.setRows(itemList);
                    
                    // 返回结果
                    return (T) response;
                } catch (Exception ex) {
                    LogUtils.e(TAG, "特殊处理失败: " + ex.getMessage(), ex);
                    throw new IOException("Failed to process response: " + ex.getMessage(), ex);
                }
            }
        }
        
        /**
         * 从类型参数中获取目标类的类型
         */
        private Class<?> getTargetClass(Type type) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type[] typeArgs = parameterizedType.getActualTypeArguments();
                if (typeArgs.length > 0) {
                    Type innerType = typeArgs[0];
                    if (innerType instanceof ParameterizedType) {
                        ParameterizedType innerParameterizedType = (ParameterizedType) innerType;
                        Type[] innerTypeArgs = innerParameterizedType.getActualTypeArguments();
                        if (innerTypeArgs.length > 0) {
                            Type elementType = innerTypeArgs[0];
                            if (elementType instanceof Class) {
                                return (Class<?>) elementType;
                            }
                        }
                    }
                }
            }
            return null;
        }
    }
} 