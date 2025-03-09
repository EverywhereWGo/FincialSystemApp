package com.zjf.fincialsystem.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 预算模型类
 */
public class Budget implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public static final String PERIOD_MONTHLY = "monthly";
    public static final String PERIOD_YEARLY = "yearly";
    
    private long id;
    private long userId;
    private long categoryId;
    private double amount;
    private String period;
    private Date startDate;
    private Date endDate;
    private int notifyPercent;
    private boolean notifyEnabled;
    
    // 非数据库字段，用于UI显示
    private Category category;
    private double usedAmount;
    private double remainingAmount;
    private double usedPercent;
    
    public Budget() {
        // 默认构造函数
    }
    
    public Budget(long userId, long categoryId, double amount, String period) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.period = period;
        this.notifyPercent = 80;
        this.notifyEnabled = true;
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
    
    public Category getCategory() {
        return category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }
    
    public double getUsedAmount() {
        return usedAmount;
    }
    
    public void setUsedAmount(double usedAmount) {
        this.usedAmount = usedAmount;
        calculateRemainingAndPercent();
    }
    
    public double getRemainingAmount() {
        return remainingAmount;
    }
    
    public double getUsedPercent() {
        return usedPercent;
    }
    
    /**
     * 计算剩余金额和使用百分比
     */
    private void calculateRemainingAndPercent() {
        this.remainingAmount = this.amount - this.usedAmount;
        this.usedPercent = (this.usedAmount / this.amount) * 100;
    }
    
    /**
     * 判断是否为月度预算
     */
    public boolean isMonthly() {
        return PERIOD_MONTHLY.equals(period);
    }
    
    /**
     * 判断是否为年度预算
     */
    public boolean isYearly() {
        return PERIOD_YEARLY.equals(period);
    }
    
    /**
     * 判断是否超出预算
     */
    public boolean isOverBudget() {
        return usedAmount > amount;
    }
    
    /**
     * 判断是否接近预算上限
     */
    public boolean isNearLimit() {
        return usedPercent >= notifyPercent && usedPercent < 100;
    }
    
    /**
     * 判断是否需要发送通知
     */
    public boolean shouldNotify() {
        return notifyEnabled && (isOverBudget() || isNearLimit());
    }
    
    /**
     * 获取预算使用百分比
     * @return 使用百分比
     */
    public int getUsedPercentage() {
        if (amount <= 0) {
            return 0;
        }
        return (int) (usedAmount * 100 / amount);
    }
} 