package com.zjf.fincialsystem.network;

import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zjf.fincialsystem.utils.LogUtils;

import java.util.List;
import java.util.ArrayList;

/**
 * API响应包装类
 * 用于统一处理API返回的数据格式
 * @param <T> 数据类型
 */
public class ApiResponse<T> {
    private static final String TAG = "ApiResponse";
    
    private int code;
    private String msg;
    private T data;
    
    // 分页数据的总记录数
    private long total;
    
    // 分页数据的当前页数据
    private List<T> rows;
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMsg("操作成功");
        response.setData(data);
        return response;
    }
    
    public static <T> ApiResponse<T> error(int code, String msg) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMsg() {
        return msg;
    }
    
    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public long getTotal() {
        return total;
    }
    
    public void setTotal(long total) {
        this.total = total;
    }
    
    public List<T> getRows() {
        return rows;
    }
    
    /**
     * 设置rows数据
     * @param rows 数据列表
     */
    @SuppressWarnings("unchecked")
    public void setRows(List<?> rows) {
        // 解决类型转换问题，使用原始类型处理
        this.rows = (List<T>) rows;
    }
    
    public boolean isSuccess() {
        return code == 200;
    }
    
    /**
     * 安全获取data数据，当data为null时返回默认值
     * @param defaultValue 默认值
     * @return 数据或默认值
     */
    public T getDataSafe(T defaultValue) {
        return data != null ? data : defaultValue;
    }
    
    /**
     * 安全获取rows数据，当rows为null时返回默认空列表
     * @return 数据列表，保证不会为null
     */
    public List<T> getRowsSafe() {
        if (rows == null) {
            return new ArrayList<>();
        }
        
        // 直接返回rows，避免类型转换问题
        return rows;
    }
    
    /**
     * 将rows字段中的对象转换为指定类型
     * 用于处理服务器返回的复杂对象结构
     * @param <R> 目标类型
     * @param classOfR 目标类的Class对象
     * @return 转换后的对象列表
     */
    public <R> List<R> convertRows(Class<R> classOfR) {
        List<R> result = new ArrayList<>();
        if (rows == null) {
            return result;
        }
        
        Gson gson = new Gson();
        try {
            // 首先尝试获取JSON字符串表示
            String jsonString = gson.toJson(rows);
            
            // 检查JSON结构，处理可能是对象而不是数组的情况
            JsonElement jsonElement = gson.fromJson(jsonString, JsonElement.class);
            
            if (jsonElement.isJsonArray()) {
                // 正常的数组处理
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                for (JsonElement element : jsonArray) {
                    try {
                        // 检查元素是否是嵌套对象
                        if (element.isJsonObject()) {
                            JsonObject jsonObject = element.getAsJsonObject();
                            
                            // 尝试直接转换
                            try {
                                R object = gson.fromJson(element, classOfR);
                                result.add(object);
                            } catch (Exception e) {
                                LogUtils.e(TAG, "转换对象失败", e);
                                // 直接转换失败，检查是否包含特定字段
                                if (jsonObject.has("id") && classOfR.getDeclaredField("id") != null) {
                                    // 处理特定的对象结构
                                    try {
                                        R newObject = classOfR.newInstance();
                                        
                                        // 反射设置属性值
                                        for (java.lang.reflect.Field field : classOfR.getDeclaredFields()) {
                                            field.setAccessible(true);
                                            if (jsonObject.has(field.getName())) {
                                                JsonElement fieldElement = jsonObject.get(field.getName());
                                                Object value = gson.fromJson(fieldElement, field.getGenericType());
                                                field.set(newObject, value);
                                            }
                                        }
                                        
                                        result.add(newObject);
                                    } catch (Exception ex) {
                                        LogUtils.e(TAG, "创建对象失败", ex);
                                    }
                                }
                            }
                        } else {
                            // 对于非对象，直接转换
                            R object = gson.fromJson(element, classOfR);
                            result.add(object);
                        }
                    } catch (Exception e) {
                        // 转换失败，跳过此元素
                        LogUtils.e(TAG, "处理数组元素失败", e);
                    }
                }
            } else if (jsonElement.isJsonObject()) {
                // 如果是单个对象而不是数组，将其包装成数组处理
                try {
                    R object = gson.fromJson(jsonElement, classOfR);
                    result.add(object);
                } catch (Exception e) {
                    LogUtils.e(TAG, "处理单个对象失败", e);
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "convertRows异常", e);
        }
        
        return result;
    }
} 