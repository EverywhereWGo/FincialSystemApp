package com.zjf.fincialsystem.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 数字工具类
 * 提供数字格式化等功能
 */
public class NumberUtils {
    
    private static final NumberFormat AMOUNT_FORMATTER = new DecimalFormat("#,##0.00");
    
    /**
     * 格式化金额
     * @param amount 金额
     * @return 格式化后的金额字符串，例如：1,234.56
     */
    public static String formatAmount(double amount) {
        return AMOUNT_FORMATTER.format(amount);
    }
    
    /**
     * 格式化金额并添加货币符号
     * @param amount 金额
     * @return 格式化后的金额字符串，例如：¥1,234.56
     */
    public static String formatAmountWithCurrency(double amount) {
        return String.format(Locale.getDefault(), "¥%s", formatAmount(amount));
    }
    
    /**
     * 将Object转换为double
     * @param value 对象值
     * @param defaultValue 默认值
     * @return double值
     */
    public static double toDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                String str = (String) value;
                if (str.isEmpty()) {
                    return defaultValue;
                }
                // 处理逗号分隔的数字格式 (例如: "1,234.56")
                str = str.replace(",", "");
                return Double.parseDouble(str);
            } else if (value instanceof Boolean) {
                return ((Boolean) value) ? 1.0 : 0.0;
            } else {
                LogUtils.w("NumberUtils", "未知类型转换为double: " + value.getClass().getName());
                return Double.parseDouble(value.toString());
            }
        } catch (Exception e) {
            LogUtils.e("NumberUtils", "转换double失败: " + value, e);
            return defaultValue;
        }
    }
} 