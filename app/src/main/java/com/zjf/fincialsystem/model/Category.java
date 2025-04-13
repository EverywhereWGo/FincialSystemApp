package com.zjf.fincialsystem.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * 分类模型类
 * 匹配服务器端 FinCategory
 */
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    // 类型常量 - 根据服务器定义调整
    public static final int TYPE_EXPENSE = 0;  // 支出
    public static final int TYPE_INCOME = 1;   // 收入

    /**
     * 分类ID
     */
    @SerializedName("id")
    private Long id;

    /**
     * 分类名称
     */
    @SerializedName("name")
    private String name;

    /**
     * 分类类型: 1-支出，2-收入 (匹配服务器定义)
     */
    @SerializedName("type")
    private Integer type;

    /**
     * 分类图标
     */
    @SerializedName("icon")
    private String icon;

    /**
     * 分类颜色
     */
    @SerializedName("color")
    private String color;

    /**
     * 用户ID
     */
    @SerializedName(value = "userId", alternate = {"user_id"})
    private Long userId;

    /**
     * 父分类ID
     */
    @Expose(serialize = false, deserialize = false)
    private Long parentId;

    /**
     * 是否为默认分类
     */
    @Expose(serialize = false, deserialize = false)
    private Boolean isDefault;

    /**
     * 显示顺序
     */
    @SerializedName(value = "displayOrder", alternate = {"display_order"})
    private Integer displayOrder;

    /**
     * 删除标志
     */
    @SerializedName(value = "delFlag", alternate = {"del_flag"})
    private String delFlag;

    /**
     * 创建者
     */
    @SerializedName(value = "createBy", alternate = {"create_by"})
    private String createBy;

    /**
     * 创建时间
     */
    @SerializedName(value = "createTime", alternate = {"create_time"})
    private String createTime;

    /**
     * 更新者
     */
    @SerializedName(value = "updateBy", alternate = "update_by")
    private String updateBy;

    /**
     * 更新时间
     */
    @SerializedName(value = "updateTime", alternate = {"update_time"})
    private String updateTime;

    /**
     * 备注
     */
    @SerializedName("remark")
    private String remark;

    public Category() {
    }

    public Category(Long id, String name, Integer type, String icon, String color, Long userId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.color = color;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Boolean isDefault() {
        return isDefault != null && isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
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
     * 是否为收入分类
     *
     * @return 是否为收入分类
     */
    public boolean isIncome() {
        return type != null && type == TYPE_INCOME;
    }

    /**
     * 是否为支出分类
     *
     * @return 是否为支出分类
     */
    public boolean isExpense() {
        return type != null && type == TYPE_EXPENSE;
    }

    /**
     * 获取图标名称
     * 用于在界面中显示对应的drawable资源
     *
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
                ", parentId=" + parentId +
                ", isDefault=" + isDefault +
                ", displayOrder=" + displayOrder +
                ", delFlag='" + delFlag + '\'' +
                ", createBy='" + createBy + '\'' +
                ", createTime='" + createTime + '\'' +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime='" + updateTime + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
} 