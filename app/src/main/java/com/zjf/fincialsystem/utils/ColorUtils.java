package com.zjf.fincialsystem.utils;

import android.graphics.Color;

/**
 * 颜色工具类
 */
public class ColorUtils {
    
    /**
     * 解析颜色字符串
     * @param colorStr 颜色字符串，格式为：#RRGGBB 或 #AARRGGBB
     * @return 颜色值
     */
    public static int parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return Color.BLACK;
        }
        
        try {
            return Color.parseColor(colorStr);
        } catch (Exception e) {
            LogUtils.e("解析颜色失败：" + colorStr, e);
            return Color.BLACK;
        }
    }
    
    /**
     * 获取随机颜色
     * @return 随机颜色
     */
    public static int getRandomColor() {
        int r = (int) (Math.random() * 255);
        int g = (int) (Math.random() * 255);
        int b = (int) (Math.random() * 255);
        return Color.rgb(r, g, b);
    }
    
    /**
     * 获取随机颜色字符串
     * @return 随机颜色字符串，格式为：#RRGGBB
     */
    public static String getRandomColorString() {
        int color = getRandomColor();
        return String.format("#%06X", (0xFFFFFF & color));
    }
    
    /**
     * 获取颜色的亮度
     * @param color 颜色值
     * @return 亮度值，范围为0-255
     */
    public static int getBrightness(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return (int) Math.sqrt(r * r * 0.241 + g * g * 0.691 + b * b * 0.068);
    }
    
    /**
     * 判断颜色是否为深色
     * @param color 颜色值
     * @return 是否为深色
     */
    public static boolean isDarkColor(int color) {
        return getBrightness(color) < 128;
    }
    
    /**
     * 获取颜色的反色
     * @param color 颜色值
     * @return 反色值
     */
    public static int getContrastColor(int color) {
        return isDarkColor(color) ? Color.WHITE : Color.BLACK;
    }
    
    /**
     * 调整颜色的亮度
     * @param color 颜色值
     * @param factor 亮度因子，大于1变亮，小于1变暗
     * @return 调整后的颜色值
     */
    public static int adjustBrightness(int color, float factor) {
        int r = Math.min(255, Math.max(0, (int) (Color.red(color) * factor)));
        int g = Math.min(255, Math.max(0, (int) (Color.green(color) * factor)));
        int b = Math.min(255, Math.max(0, (int) (Color.blue(color) * factor)));
        return Color.rgb(r, g, b);
    }
    
    /**
     * 获取颜色的透明度
     * @param color 颜色值
     * @param alpha 透明度，范围为0-255
     * @return 调整透明度后的颜色值
     */
    public static int getColorWithAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
} 