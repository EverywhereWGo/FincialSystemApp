package com.zjf.fincialsystem.network.model;

/**
 * 登录请求参数
 */
public class LoginRequest {
    private String username;
    private String password;
    private String deviceInfo;
    
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public LoginRequest(String username, String password, String deviceInfo) {
        this.username = username;
        this.password = password;
        this.deviceInfo = deviceInfo;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getDeviceInfo() {
        return deviceInfo;
    }
    
    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
} 