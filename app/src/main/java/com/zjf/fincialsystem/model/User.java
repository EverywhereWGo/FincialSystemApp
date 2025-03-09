package com.zjf.fincialsystem.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体类
 */
public class User implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private long id;
    private String username;
    private String password;
    private String name;
    private String email;
    private String phone;
    private String avatar;
    private String role; // 用户角色: admin, user
    private long createdAt;
    private long updatedAt;
    private Date lastLoginTime; // 最后登录时间
    private int failedAttempts; // 登录失败次数
    private Date lockedUntil; // 账号锁定截止时间
    
    public User() {
    }
    
    public User(long id, String username, String password, String name, String email, String phone, String avatar) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.avatar = avatar;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.failedAttempts = 0;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
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
    
    public Date getLastLoginTime() {
        return lastLoginTime;
    }
    
    public void setLastLoginTime(Date lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
    
    public int getFailedAttempts() {
        return failedAttempts;
    }
    
    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }
    
    public Date getLockedUntil() {
        return lockedUntil;
    }
    
    public void setLockedUntil(Date lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
    
    /**
     * 增加登录失败次数
     */
    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }
    
    /**
     * 重置登录失败次数
     */
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }
    
    /**
     * 账户是否被锁定
     * @return 是否锁定
     */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.after(new Date());
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
} 