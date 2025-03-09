package com.zjf.fincialsystem.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 颜色工具类
 * 用于管理各种分类颜色
 */
public class ColorUtil {
    
    private static final List<String> COLORS = new ArrayList<>();
    
    static {
        // 初始化常用颜色列表
        COLORS.add("#F44336"); // 红色
        COLORS.add("#E91E63"); // 粉红色
        COLORS.add("#9C27B0"); // 紫色
        COLORS.add("#673AB7"); // 深紫色
        COLORS.add("#3F51B5"); // 靛蓝色
        COLORS.add("#2196F3"); // 蓝色
        COLORS.add("#03A9F4"); // 浅蓝色
        COLORS.add("#00BCD4"); // 青色
        COLORS.add("#009688"); // 水鸭色
        COLORS.add("#4CAF50"); // 绿色
        COLORS.add("#8BC34A"); // 浅绿色
        COLORS.add("#CDDC39"); // 酸橙色
        COLORS.add("#FFEB3B"); // 黄色
        COLORS.add("#FFC107"); // 琥珀色
        COLORS.add("#FF9800"); // 橙色
        COLORS.add("#FF5722"); // 深橙色
        COLORS.add("#795548"); // 棕色
        COLORS.add("#9E9E9E"); // 灰色
        COLORS.add("#607D8B"); // 蓝灰色
    }
    
    /**
     * 获取所有可用颜色
     */
    public static List<String> getAllColors() {
        return COLORS;
    }
    
    /**
     * 获取默认颜色
     */
    public static String getDefaultColor() {
        return COLORS.get(0);
    }
    
    /**
     * 根据类型获取默认颜色
     * @param type 类型（收入/支出）
     */
    public static String getDefaultColorByType(int type) {
        // 支出默认使用红色，收入默认使用绿色
        return type == 2 ? "#F44336" : "#4CAF50";
    }
    
    /**
     * 检查颜色值是否有效
     */
    public static boolean isValidColor(String color) {
        if (color == null || color.isEmpty()) {
            return false;
        }
        
        // 简单检查是否为十六进制颜色值
        return color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");
    }
} 