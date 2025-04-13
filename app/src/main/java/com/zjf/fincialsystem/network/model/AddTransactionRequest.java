package com.zjf.fincialsystem.network.model;

import java.util.Date;

/**
 * 添加交易记录的请求参数
 */
public class AddTransactionRequest {
    private long userId;
    private long categoryId;
    private int type;
    private double amount;
    private long date;
    private String description;
    private String note;
    private String imagePath;

    public AddTransactionRequest() {
        // 默认构造函数
    }

    public AddTransactionRequest(long categoryId, int type, double amount, long date, String description) {
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.description = description;
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
} 