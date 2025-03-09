package com.zjf.fincialsystem.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 交易记录模型类
 */
public class Transaction implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // 类型常量
    public static final int TYPE_INCOME = 1;  // 收入
    public static final int TYPE_EXPENSE = 2; // 支出
    public static final int TYPE_TRANSFER = 3; // 转账
    
    private long id;
    private long userId;
    private long categoryId;
    private int type;
    private double amount;
    private Date date;
    private String description;
    private String note; // 备注
    private String imagePath; // 图片路径
    private long createdAt;
    private long updatedAt;
    
    // 关联对象
    private Category category;
    
    public Transaction() {
    }
    
    public Transaction(long id, long userId, long categoryId, int type, double amount, Date date, String description) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
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
    
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }
    
    /**
     * 是否为收入
     * @return 是否为收入
     */
    public boolean isIncome() {
        return type == TYPE_INCOME;
    }
    
    /**
     * 是否为支出
     * @return 是否为支出
     */
    public boolean isExpense() {
        return type == TYPE_EXPENSE;
    }
    
    /**
     * 是否为转账
     * @return 是否为转账
     */
    public boolean isTransfer() {
        return type == TYPE_TRANSFER;
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
    
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", type=" + type +
                ", amount=" + amount +
                ", date=" + date +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 