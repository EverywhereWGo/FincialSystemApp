package com.zjf.fincialsystem.model;

import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * 用户实体类
 */
public class User implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private long id;
    private String username;
    private String password;
    private String nickname;
    private String name;
    private String email;
    private String phone;
    private String role;
    private long createdAt;
    private long updatedAt;
    private String lastLoginTime;
    private int failedAttempts;
    private String lockedUntil;
    private String wechat;
    private String qq;
    
    public User() {
        // 默认构造函数
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
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
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
    
    public String getLastLoginTime() {
        return lastLoginTime;
    }
    
    public void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
    
    public Date getLastLoginTimeAsDate() {
        if (lastLoginTime == null) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.parse(lastLoginTime);
        } catch (Exception e) {
            return null;
        }
    }
    
    public void setLastLoginTime(Date lastLoginTime) {
        if (lastLoginTime != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            this.lastLoginTime = format.format(lastLoginTime);
        } else {
            this.lastLoginTime = null;
        }
    }
    
    public int getFailedAttempts() {
        return failedAttempts;
    }
    
    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }
    
    public String getLockedUntil() {
        return lockedUntil;
    }
    
    public void setLockedUntil(String lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
    
    public Date getLockedUntilAsDate() {
        if (lockedUntil == null) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.parse(lockedUntil);
        } catch (Exception e) {
            return null;
        }
    }
    
    public void setLockedUntil(Date lockedUntil) {
        if (lockedUntil != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            this.lockedUntil = format.format(lockedUntil);
        } else {
            this.lockedUntil = null;
        }
    }
    
    public String getWechat() {
        return wechat;
    }
    
    public void setWechat(String wechat) {
        this.wechat = wechat;
    }
    
    public String getQq() {
        return qq;
    }
    
    public void setQq(String qq) {
        this.qq = qq;
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
        try {
            if (lockedUntil == null) {
                return false;
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date lockDate = format.parse(lockedUntil);
            return lockDate != null && lockDate.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", lastLoginTime=" + lastLoginTime +
                '}';
    }
} 