package com.zjf.fincialsystem.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 交易记录模型类
 */
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    // 类型常量
    public static final int TYPE_EXPENSE = 1;  // 支出
    public static final int TYPE_INCOME = 2; // 收入
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
    private String paymentMethod; // 支付方式
    private String remark; // 额外备注信息
    private String categoryName; // 分类名称

    // 新增字段
    private String location; // 交易地点
    private String createTime; // 创建时间
    private String createBy; // 创建者
    private int syncState; // 同步状态：0-未同步，1-已同步，2-同步失败

    // 关联对象
    private Category category;

    public Transaction() {
    }

    public Transaction(long id, long userId, long categoryId, int type, double amount, Date date, String description, String note, String imagePath, String paymentMethod, String remark, String categoryName, String location, String createTime, String createBy, int syncState, Category category) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.note = note;
        this.imagePath = imagePath;
        this.paymentMethod = paymentMethod;
        this.remark = remark;
        this.categoryName = categoryName;
        this.location = location;
        this.createTime = createTime;
        this.createBy = createBy;
        this.syncState = syncState;
        this.category = category;
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
                ", note='" + note + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", remark='" + remark + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", location='" + location + '\'' +
                ", createTime='" + createTime + '\'' +
                ", createBy='" + createBy + '\'' +
                ", syncState=" + syncState +
                ", category=" + category +
                '}';
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * 是否为收入
     *
     * @return 是否为收入
     */
    public boolean isIncome() {
        return type == TYPE_INCOME;
    }

    /**
     * 是否为支出
     *
     * @return 是否为支出
     */
    public boolean isExpense() {
        return type == TYPE_EXPENSE;
    }

    /**
     * 是否为转账
     *
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public int getSyncState() {
        return syncState;
    }

    public void setSyncState(int syncState) {
        this.syncState = syncState;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
} 