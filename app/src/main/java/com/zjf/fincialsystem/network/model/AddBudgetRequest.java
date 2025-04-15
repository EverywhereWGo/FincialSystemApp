package com.zjf.fincialsystem.network.model;

/**
 * 添加预算请求
 */
public class AddBudgetRequest {
    private long userId;
    private long categoryId;
    private String categoryName;
    private double amount;
    private Integer year;
    private Integer month;
    private int warningThreshold;
    private Boolean warned;


    public AddBudgetRequest() {
        // 默认构造函数
    }

    @Override
    public String toString() {
        return "AddBudgetRequest{" +
                "userId=" + userId +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", amount=" + amount +
                ", year=" + year +
                ", month=" + month +
                ", warningThreshold=" + warningThreshold +
                ", warned=" + warned +
                '}';
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public int getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(int warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public Boolean getWarned() {
        return warned;
    }

    public void setWarned(Boolean warned) {
        this.warned = warned;
    }
}