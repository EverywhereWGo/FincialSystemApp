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
} 