package com.zjf.fincialsystem.model;

import java.io.Serializable;

/**
 * 趋势数据模型
 * 用于表示收支趋势的时间序列数据
 */
public class TrendData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private int year;
    private int month;
    private int week;
    private int day;
    private double amount;
    private int count;
    private String label;
    
    public TrendData() {
        // 默认构造函数
    }
    
    public TrendData(int year, int month, int week, int day, double amount, int count) {
        this.year = year;
        this.month = month;
        this.week = week;
        this.day = day;
        this.amount = amount;
        this.count = count;
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
    
    public int getWeek() {
        return week;
    }
    
    public void setWeek(int week) {
        this.week = week;
    }
    
    public int getDay() {
        return day;
    }
    
    public void setDay(int day) {
        this.day = day;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    @Override
    public String toString() {
        return "TrendData{" +
                "year=" + year +
                ", month=" + month +
                ", week=" + week +
                ", day=" + day +
                ", amount=" + amount +
                ", count=" + count +
                ", label='" + label + '\'' +
                '}';
    }
} 