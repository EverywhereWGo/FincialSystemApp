package com.zjf.fincialsystem.network.model;

import java.io.Serializable;

/**
 * 更新用户资料请求类
 * 根据接口文档 2.10 编辑个人资料
 */
public class UpdateProfileRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private long id;          // 用户ID（必填）
    private String nickname;      // 用户昵称（必填）
    private String email;         // 电子邮箱（必填）
    private String phone;         // 手机号码（必填）
    private String avatar;        // 头像路径（选填）
    private int gender;           // 性别（0:未知 1:男 2:女）（选填）
    private String wechat;        // 微信号（选填）
    private String qq;            // QQ号（选填）
    private String signature;     // 个性签名（选填）
    
    public UpdateProfileRequest() {
        // 默认构造函数
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public int getGender() {
        return gender;
    }
    
    public void setGender(int gender) {
        this.gender = gender;
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
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
} 