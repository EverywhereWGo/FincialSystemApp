package com.zjf.fincialsystem.model;

import java.io.Serializable;

/**
 * 分类统计数据模型
 * 用于表示按分类统计的收支情况
 */
public class CategoryStatistic implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private long categoryId;
    private String categoryName;
    private String categoryColor;
    private double amount;
    private double percentage;
    private int count;
    
    public CategoryStatistic() {
        // 默认构造函数
    }
    
    public CategoryStatistic(long categoryId, String categoryName, String categoryColor, double amount, int count) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
        this.amount = amount;
        this.count = count;
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
    
    public String getCategoryColor() {
        return categoryColor;
    }
    
    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public double getPercentage() {
        return percentage;
    }
    
    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    @Override
    public String toString() {
        return "CategoryStatistic{" +
                "categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", categoryColor='" + categoryColor + '\'' +
                ", amount=" + amount +
                ", percentage=" + percentage +
                ", count=" + count +
                '}';
    }
}
