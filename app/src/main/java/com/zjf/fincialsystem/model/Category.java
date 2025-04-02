package com.zjf.fincialsystem.model;

import java.io.Serializable;

/**
 * 分类模型类
 */
public class Category implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // 类型常量
    public static final int TYPE_INCOME = 1;  // 收入
    public static final int TYPE_EXPENSE = 2; // 支出
    
    private long id;
    private String name;
    private int type;
    private String icon;
    private String color;
    private long userId;
    private Long parentId; // 父分类ID，支持多级分类
    private boolean isDefault; // 是否默认分类
    private long createdAt;
    private long updatedAt;
    
    public Category() {
    }
    
    public Category(long id, String name, int type, String icon, String color, long userId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.color = color;
        this.userId = userId;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public long getUserId() {
        return userId;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
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
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    /**
     * 是否为收入分类
     * @return 是否为收入分类
     */
    public boolean isIncome() {
        return type == TYPE_INCOME;
    }
    
    /**
     * 是否为支出分类
     * @return 是否为支出分类
     */
    public boolean isExpense() {
        return type == TYPE_EXPENSE;
    }
    
    /**
     * 获取图标名称
     * 用于在界面中显示对应的drawable资源
     * @return 图标资源名称
     */
    public String getIconName() {
        if (icon == null || icon.isEmpty()) {
            // 返回默认图标名称
            return type == TYPE_INCOME ? "ic_income" : "ic_expense";
        }
        return icon;
    }
    
    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", icon='" + icon + '\'' +
                ", color='" + color + '\'' +
                ", userId=" + userId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 