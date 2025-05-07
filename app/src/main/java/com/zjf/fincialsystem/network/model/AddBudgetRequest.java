package com.zjf.fincialsystem.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * 添加预算请求参数
 */
public class AddBudgetRequest {
    @SerializedName("userId")
    private long userId;
    
    @SerializedName("categoryId")
    private long categoryId;
    
    @SerializedName("amount")
    private double amount;
    
    @SerializedName("year")
    private int year;
    
    @SerializedName("month")
    private int month;
    
    @SerializedName("warningThreshold")
    private int warningThreshold;
    
    @SerializedName("notifyEnable")
    private boolean notifyEnable;
    
    @SerializedName("remark")
    private String remark;
    
    public AddBudgetRequest() {
        // 默认构造函数
    }
    
    public AddBudgetRequest(long userId, long categoryId, double amount, int year, int month, int warningThreshold, boolean notifyEnable, String remark) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.year = year;
        this.month = month;
        this.warningThreshold = warningThreshold;
        this.notifyEnable = notifyEnable;
        this.remark = remark;
    }
    
    public long getUserId() {
        return userId;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    public long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public int getYear() {
        return year;
    }
    
    public void setYear(int year) {
        this.year = year;
    }
    
    public int getMonth() {
        return month;
    }
    
    public void setMonth(int month) {
        this.month = month;
    }
    
    public int getWarningThreshold() {
        return warningThreshold;
    }
    
    public void setWarningThreshold(int warningThreshold) {
        this.warningThreshold = warningThreshold;
    }
    
    public boolean isNotifyEnable() {
        return notifyEnable;
    }
    
    public void setNotifyEnable(boolean notifyEnable) {
        this.notifyEnable = notifyEnable;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
}