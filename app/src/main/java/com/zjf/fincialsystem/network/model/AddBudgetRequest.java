package com.zjf.fincialsystem.network.model;

import java.util.Date;

/**
 * 添加预算请求
 */
public class AddBudgetRequest {
    private long userId;
    private long categoryId;
    private double amount;
    private String period;
    private Date startDate;
    private Date endDate;
    private int notifyPercent;
    private boolean notifyEnabled;

    public AddBudgetRequest() {
        // 默认构造函数
    }

    public AddBudgetRequest(long categoryId, double amount, String period) {
        this.categoryId = categoryId;
        this.amount = amount;
        this.period = period;
        this.notifyPercent = 80; // 默认80%提醒
        this.notifyEnabled = true; // 默认开启提醒
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

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getNotifyPercent() {
        return notifyPercent;
    }

    public void setNotifyPercent(int notifyPercent) {
        this.notifyPercent = notifyPercent;
    }

    public boolean isNotifyEnabled() {
        return notifyEnabled;
    }

    public void setNotifyEnabled(boolean notifyEnabled) {
        this.notifyEnabled = notifyEnabled;
    }
} 