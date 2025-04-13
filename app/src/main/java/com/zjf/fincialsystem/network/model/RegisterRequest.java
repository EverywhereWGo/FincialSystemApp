package com.zjf.fincialsystem.network.model;

/**
 * 注册请求参数
 */
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    
    public RegisterRequest(String username, String password, String nickname, String email, String phone) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
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
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
} 