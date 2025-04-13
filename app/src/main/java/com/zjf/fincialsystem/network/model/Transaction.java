package com.zjf.fincialsystem.network.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

/**
 * 交易记录网络模型
 */
public class Transaction implements Serializable {
    @SerializedName("id")
    private long id;  // 交易ID

    @SerializedName("userId")
    private long userId;  // 用户ID

    @SerializedName("categoryId")
    private long categoryId;  // 分类ID

    @SerializedName("categoryName")
    private String categoryName;  // 分类名称

    @SerializedName("amount")
    private double amount;  // 金额

    @SerializedName("type")
    private int type;  // 类型 1:支出 2:收入

    @SerializedName("transactionTime")
    private long transactionTime;  // 交易时间

    @SerializedName("note")
    private String note;  // 备注

    @SerializedName("imagePath")
    private String imagePath;  // 图片路径

    @SerializedName("location")
    private String location;  // 位置

    @SerializedName("createTime")
    private String createTime;  // 创建时间

    @SerializedName("updateTime")
    private String updateTime;  // 更新时间

    @SerializedName("createBy")
    private String createBy;  // 创建人

    @SerializedName("updateBy")
    private String updateBy;  // 更新人

    @SerializedName("remark")
    private String remark;  // 备注

    @SerializedName("syncState")
    private int syncState;  // 同步状态 0:已同步 1:未同步

    @SerializedName("delFlag")
    private String delFlag;  // 删除标志 0:正常 1:已删除

    // 构造方法
    public Transaction() {
    }

    public Transaction(long id, long userId, long categoryId, String categoryName, double amount, int type, long transactionTime, String note) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
        this.type = type;
        this.transactionTime = transactionTime;
        this.note = note;
    }
    
    // 将网络模型转换为本地模型
    public com.zjf.fincialsystem.model.Transaction toModel() {
        com.zjf.fincialsystem.model.Transaction transaction = new com.zjf.fincialsystem.model.Transaction();
        transaction.setId(this.id);
        transaction.setUserId(this.userId);
        transaction.setCategoryId(this.categoryId);
        transaction.setType(this.type);
        transaction.setAmount(this.amount);
        transaction.setDate(new Date(this.transactionTime));
        transaction.setDescription(this.note);
        transaction.setImagePath(this.imagePath);
        return transaction;
    }

    // Getter 和 Setter 方法
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(long transactionTime) {
        this.transactionTime = transactionTime;
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

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getSyncState() {
        return syncState;
    }

    public void setSyncState(int syncState) {
        this.syncState = syncState;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", amount=" + amount +
                ", type=" + type +
                ", transactionTime=" + transactionTime +
                ", note='" + note + '\'' +
                '}';
    }
} 