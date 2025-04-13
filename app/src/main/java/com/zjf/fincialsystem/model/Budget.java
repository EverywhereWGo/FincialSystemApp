package com.zjf.fincialsystem.model;

import com.google.gson.annotations.SerializedName;
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
    
    @SerializedName("categoryName")
    private String categoryName;
    
    private double amount;
    
    // 后端返回格式中的字段
    @SerializedName("year")
    private int year;
    
    @SerializedName("month")
    private int month;
    
    @SerializedName("warningThreshold")
    private double warningThreshold;
    
    @SerializedName("warned")
    private boolean warned;
    
    @SerializedName("delFlag")
    private String delFlag;
    
    @SerializedName("usedAmount")
    private Double usedAmount;
    
    @SerializedName("usedPercentage")
    private Integer usedPercentage;
    
    @SerializedName("createBy")
    private String createBy;
    
    @SerializedName("createTime")
    private String createTime;
    
    @SerializedName("updateBy")
    private String updateBy;
    
    @SerializedName("updateTime")
    private String updateTime;
    
    @SerializedName("remark")
    private String remark;
    
    private String period;
    private Date startDate;
    private Date endDate;
    private int notifyPercent;
    private boolean notifyEnabled;
    
    // 非数据库字段，用于UI显示
    private Category category;
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
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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
    
    public double getWarningThreshold() {
        return warningThreshold;
    }
    
    public void setWarningThreshold(double warningThreshold) {
        this.warningThreshold = warningThreshold;
    }
    
    public boolean isWarned() {
        return warned;
    }
    
    public void setWarned(boolean warned) {
        this.warned = warned;
    }
    
    public String getDelFlag() {
        return delFlag;
    }
    
    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }
    
    public String getCreateBy() {
        return createBy;
    }
    
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
    
    public String getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    
    public String getUpdateBy() {
        return updateBy;
    }
    
    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
    
    public String getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    /**
     * 获取已使用金额，兼容后端返回的null情况
     */
    public double getUsedAmount() {
        return usedAmount != null ? usedAmount : 0.0;
    }
    
    /**
     * 设置已使用金额
     */
    public void setUsedAmount(Double usedAmount) {
        this.usedAmount = usedAmount;
        calculateRemainingAndPercent();
    }
    
    /**
     * 获取预算使用百分比
     * @return 使用百分比
     */
    public int getUsedPercentage() {
        return usedPercentage != null ? usedPercentage : calculateUsedPercentage();
    }
    
    /**
     * 设置预算使用百分比
     */
    public void setUsedPercentage(Integer usedPercentage) {
        this.usedPercentage = usedPercentage;
    }
    
    /**
     * 计算预算使用百分比
     */
    private int calculateUsedPercentage() {
        if (amount <= 0) {
            return 0;
        }
        return (int) ((getUsedAmount() * 100) / amount);
    }
    
    /**
     * 计算剩余金额和使用百分比
     */
    private void calculateRemainingAndPercent() {
        this.remainingAmount = this.amount - this.getUsedAmount();
        this.usedPercent = (this.getUsedAmount() / this.amount) * 100;
    }
    
    /**
     * 判断是否为月度预算
     */
    public boolean isMonthly() {
        return month > 0 || PERIOD_MONTHLY.equals(period);
    }
    
    /**
     * 判断是否为年度预算
     */
    public boolean isYearly() {
        return year > 0 && month == 0 || PERIOD_YEARLY.equals(period);
    }
    
    /**
     * 判断是否超出预算
     */
    public boolean isOverBudget() {
        return getUsedAmount() > amount;
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
} 