package com.zjf.fincialsystem.utils;

import com.blankj.utilcode.util.LogUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 日期工具类
 * 提供日期格式化和解析方法
 */
public class DateUtils {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("yyyy年MM月", Locale.getDefault());
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd", Locale.getDefault());
    
    /**
     * 获取当前日期
     * @return 当前日期
     */
    public static Date getCurrentDate() {
        return new Date();
    }
    
    /**
     * 获取指定年月的第一天
     * @param year 年
     * @param month 月
     * @return 第一天的日期
     */
    public static Date getFirstDayOfMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    
    /**
     * 获取指定年月的最后一天
     * @param year 年
     * @param month 月
     * @return 最后一天的日期
     */
    public static Date getLastDayOfMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }
    
    /**
     * 获取当前月份的第一天
     * @return 当前月份的第一天
     */
    public static Date getFirstDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        return getFirstDayOfMonth(year, month);
    }
    
    /**
     * 获取当前月份的最后一天
     * @return 当前月份的最后一天
     */
    public static Date getLastDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        return getLastDayOfMonth(year, month);
    }
    
    /**
     * 获取当前月份的天数
     * @return 当前月份的天数
     */
    public static int getDaysInMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * 获取指定日期是当月的第几天
     * @param date 日期
     * @return 当月的第几天
     */
    public static int getDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * 格式化日期为字符串（yyyy-MM-dd）
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_FORMAT.format(date);
    }
    
    /**
     * 格式化日期为字符串（yyyy-MM-dd HH:mm:ss）
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_TIME_FORMAT.format(date);
    }
    
    /**
     * 格式化日期为月份字符串（yyyy年MM月）
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatMonth(Date date) {
        if (date == null) {
            return "";
        }
        return MONTH_FORMAT.format(date);
    }
    
    /**
     * 格式化日期为天字符串（dd）
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDay(Date date) {
        if (date == null) {
            return "";
        }
        return DAY_FORMAT.format(date);
    }
    
    /**
     * 获取友好的时间跨度
     * @param date 日期
     * @return 友好的时间跨度
     */
    public static String getFriendlyTimeSpan(Date date) {
        if (date == null) {
            return "";
        }
        
        Calendar calendar = Calendar.getInstance();
        Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.setTime(date);
        
        // 今天
        if (calendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR)
                && calendar.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR)) {
            return "今天";
        }
        
        // 昨天
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        if (calendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR)
                && calendar.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR)) {
            return "昨天";
        }
        
        // 前天
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        if (calendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR)
                && calendar.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR)) {
            return "前天";
        }
        
        // 一周内
        calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        if (date.after(calendar.getTime())) {
            String[] weekdays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            return weekdays[targetCalendar.get(Calendar.DAY_OF_WEEK) - 1];
        }
        
        // 其他日期
        return formatDate(date);
    }
    
    /**
     * 解析日期（yyyy-MM-dd）
     * @param dateStr 日期字符串
     * @return 日期对象
     */
    public static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 解析日期时间（yyyy-MM-dd HH:mm:ss）
     * @param dateTimeStr 日期时间字符串
     * @return 日期对象
     */
    public static Date parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return DATE_TIME_FORMAT.parse(dateTimeStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取当前年份的第一天
     * @return 当前年份的第一天
     */
    public static Date getFirstDayOfYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    
    /**
     * 获取当前年份的最后一天
     * @return 当前年份的最后一天
     */
    public static Date getLastDayOfYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, 11); // 12月
        calendar.set(Calendar.DAY_OF_MONTH, 31); // 31日
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }
    
    /**
     * 计算两个日期之间的天数
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 天数
     */
    public static int daysBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);
        startCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);
        endCalendar.set(Calendar.HOUR_OF_DAY, 0);
        endCalendar.set(Calendar.MINUTE, 0);
        endCalendar.set(Calendar.SECOND, 0);
        endCalendar.set(Calendar.MILLISECOND, 0);
        
        long startTime = startCalendar.getTimeInMillis();
        long endTime = endCalendar.getTimeInMillis();
        long days = (endTime - startTime) / (1000 * 60 * 60 * 24);
        
        return (int) days;
    }
} 