package com.zjf.fincialsystem.model;

import java.util.Date;

/**
 * 通知实体类
 */
public class Notification {
    
    public static final String TYPE_BUDGET_WARNING = "budget_warning";
    public static final String TYPE_BUDGET_EXCEED = "budget_exceed";
    public static final String TYPE_BILL_REMINDER = "bill_reminder";
    public static final String TYPE_LARGE_EXPENSE = "large_expense";
    public static final String TYPE_INCOME_RECEIVED = "income_received";
    
    private long id;
    private long userId;
    private String title;
    private String content;
    private String type;
    private Date createdAt;
    private boolean isRead;
    
    public Notification() {
        // 默认构造函数
    }
    
    public Notification(long userId, String title, String content, String type) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.createdAt = new Date();
        this.isRead = false;
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
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }
    
    /**
     * 判断是否为预算警告通知
     */
    public boolean isBudgetWarning() {
        return TYPE_BUDGET_WARNING.equals(type);
    }
    
    /**
     * 判断是否为预算超支通知
     */
    public boolean isBudgetExceed() {
        return TYPE_BUDGET_EXCEED.equals(type);
    }
    
    /**
     * 判断是否为账单提醒通知
     */
    public boolean isBillReminder() {
        return TYPE_BILL_REMINDER.equals(type);
    }
    
    /**
     * 判断是否为大额支出通知
     */
    public boolean isLargeExpense() {
        return TYPE_LARGE_EXPENSE.equals(type);
    }
    
    /**
     * 判断是否为收入到账通知
     */
    public boolean isIncomeReceived() {
        return TYPE_INCOME_RECEIVED.equals(type);
    }
} 