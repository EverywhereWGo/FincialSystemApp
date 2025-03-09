package com.zjf.fincialsystem.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录历史记录模型类
 */
public class LoginHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private long userId;
    private Date loginTime;
    private String ipAddress;
    private String deviceInfo;
    private boolean success;

    public LoginHistory() {
    }

    public LoginHistory(long userId, Date loginTime, String ipAddress, String deviceInfo, boolean success) {
        this.userId = userId;
        this.loginTime = loginTime;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.success = success;
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

    public Date getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "LoginHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", loginTime=" + loginTime +
                ", ipAddress='" + ipAddress + '\'' +
                ", deviceInfo='" + deviceInfo + '\'' +
                ", success=" + success +
                '}';
    }
} 