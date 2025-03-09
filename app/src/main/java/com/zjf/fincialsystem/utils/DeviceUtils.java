package com.zjf.fincialsystem.utils;

import android.os.Build;

/**
 * 设备工具类，提供设备相关信息获取功能
 */
public class DeviceUtils {

    /**
     * 获取设备信息
     *
     * @return 设备信息字符串
     */
    public static String getDeviceInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("设备: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL);
        sb.append(", Android ").append(Build.VERSION.RELEASE);
        sb.append(" (SDK ").append(Build.VERSION.SDK_INT).append(")");
        return sb.toString();
    }

    /**
     * 获取设备型号
     *
     * @return 设备型号
     */
    public static String getModel() {
        return Build.MODEL;
    }

    /**
     * 获取设备制造商
     *
     * @return 设备制造商
     */
    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * 获取设备Android版本
     *
     * @return Android版本
     */
    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * 获取设备SDK版本
     *
     * @return SDK版本
     */
    public static int getSDKVersion() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 获取设备品牌
     *
     * @return 设备品牌
     */
    public static String getBrand() {
        return Build.BRAND;
    }
} 