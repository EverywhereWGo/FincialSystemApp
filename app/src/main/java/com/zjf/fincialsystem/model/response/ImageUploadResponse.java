package com.zjf.fincialsystem.model.response;

import com.google.gson.annotations.SerializedName;

/**
 * 图片上传响应实体类
 */
public class ImageUploadResponse {
    
    @SerializedName("imageUrl")
    private String imageUrl;
    
    @SerializedName("msg")
    private String message;
    
    public ImageUploadResponse() {
    }
    
    public ImageUploadResponse(String imageUrl, String message) {
        this.imageUrl = imageUrl;
        this.message = message;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "ImageUploadResponse{" +
                "imageUrl='" + imageUrl + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
} 