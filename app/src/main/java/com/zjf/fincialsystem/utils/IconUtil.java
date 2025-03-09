package com.zjf.fincialsystem.utils;

import android.util.SparseArray;

import com.zjf.fincialsystem.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图标工具类
 * 用于管理各种分类图标
 */
public class IconUtil {
    
    private static final Map<String, Integer> ICON_MAP = new HashMap<>();
    private static final List<IconItem> ICON_LIST = new ArrayList<>();
    
    static {
        // 初始化图标映射
        ICON_MAP.put("ic_food", R.drawable.ic_note); // 替换为实际图标
        ICON_MAP.put("ic_shopping", R.drawable.ic_description);
        ICON_MAP.put("ic_transport", R.drawable.ic_history);
        ICON_MAP.put("ic_home", R.drawable.ic_home);
        ICON_MAP.put("ic_entertainment", R.drawable.ic_edit);
        ICON_MAP.put("ic_salary", R.drawable.ic_money);
        ICON_MAP.put("ic_bonus", R.drawable.ic_description);
        ICON_MAP.put("ic_investment", R.drawable.ic_money);
        ICON_MAP.put("ic_parttime", R.drawable.ic_badge);
        
        // 初始化图标列表
        for (Map.Entry<String, Integer> entry : ICON_MAP.entrySet()) {
            ICON_LIST.add(new IconItem(entry.getKey(), entry.getValue()));
        }
    }
    
    /**
     * 根据图标名称获取图标资源ID
     */
    public static int getIconResourceId(String iconName) {
        if (iconName == null || iconName.isEmpty()) {
            return 0;
        }
        
        Integer resourceId = ICON_MAP.get(iconName);
        return resourceId != null ? resourceId : 0;
    }
    
    /**
     * 获取所有可用图标
     */
    public static List<IconItem> getAllIcons() {
        return ICON_LIST;
    }
    
    /**
     * 根据资源ID获取图标名称
     */
    public static String getIconNameByResourceId(int resourceId) {
        for (Map.Entry<String, Integer> entry : ICON_MAP.entrySet()) {
            if (entry.getValue() == resourceId) {
                return entry.getKey();
            }
        }
        return "";
    }
    
    /**
     * 根据颜色代码获取相应颜色资源ID
     * @param colorCode 颜色代码，例如 #FFFFFF
     * @param defaultColorResId 默认颜色资源ID
     * @return 颜色资源ID
     */
    public static int getColorResourceId(String colorCode, int defaultColorResId) {
        // 这里可以根据颜色代码返回对应的颜色资源ID
        // 由于颜色可能是十六进制代码，无法直接映射到资源ID，这里简单返回默认值
        return defaultColorResId;
    }
    
    /**
     * 图标项，包含图标名称和资源ID
     */
    public static class IconItem {
        private String name;
        private int resourceId;
        
        public IconItem(String name, int resourceId) {
            this.name = name;
            this.resourceId = resourceId;
        }
        
        public String getName() {
            return name;
        }
        
        public int getResourceId() {
            return resourceId;
        }
    }
} 