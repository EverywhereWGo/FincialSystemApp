package com.zjf.fincialsystem.db.dao;

import android.content.ContentValues;

import com.blankj.utilcode.util.LogUtils;
import com.zjf.fincialsystem.model.Budget;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.db.FinanceDatabaseHelper;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 预算DAO类
 * 提供预算相关的数据库操作
 */
public class BudgetDao extends BaseDao {
    
    private static final String TABLE_NAME = FinanceDatabaseHelper.TABLE_BUDGETS;
    
    public BudgetDao(SQLiteDatabase database) {
        super(database);
    }
    
    /**
     * 插入预算
     * @param budget 预算对象
     * @return 插入的预算ID，失败返回-1
     */
    public long insert(Budget budget) {
        try {
            ContentValues values = new ContentValues();
            values.put("user_id", budget.getUserId());
            values.put("category_id", budget.getCategoryId());
            values.put("amount", budget.getAmount());
            values.put("period", budget.getPeriod());
            
            if (budget.getStartDate() != null) {
                values.put("start_date", DateUtils.formatDateTime(budget.getStartDate()));
            }
            
            if (budget.getEndDate() != null) {
                values.put("end_date", DateUtils.formatDateTime(budget.getEndDate()));
            }
            
            values.put("notify_percent", budget.getNotifyPercent());
            values.put("notify_enabled", budget.isNotifyEnabled() ? 1 : 0);
            
            return database.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            LogUtils.e("Insert budget error: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * 更新预算
     * @param budget 预算对象
     * @return 是否成功
     */
    public boolean update(Budget budget) {
        try {
            ContentValues values = new ContentValues();
            values.put("user_id", budget.getUserId());
            values.put("category_id", budget.getCategoryId());
            values.put("amount", budget.getAmount());
            values.put("period", budget.getPeriod());
            
            if (budget.getStartDate() != null) {
                values.put("start_date", DateUtils.formatDateTime(budget.getStartDate()));
            } else {
                values.putNull("start_date");
            }
            
            if (budget.getEndDate() != null) {
                values.put("end_date", DateUtils.formatDateTime(budget.getEndDate()));
            } else {
                values.putNull("end_date");
            }
            
            values.put("notify_percent", budget.getNotifyPercent());
            values.put("notify_enabled", budget.isNotifyEnabled() ? 1 : 0);
            
            int rowsAffected = database.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(budget.getId())});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Update budget error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 删除预算
     * @param budgetId 预算ID
     * @return 是否成功
     */
    public boolean delete(long budgetId) {
        try {
            int rowsAffected = database.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(budgetId)});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Delete budget error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 根据ID查询预算
     * @param budgetId 预算ID
     * @return 预算对象，不存在返回null
     */
    public Budget queryById(long budgetId) {
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "id = ?", new String[]{String.valueOf(budgetId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToBudget(cursor);
            }
            return null;
        } catch (Exception e) {
            LogUtils.e("Query budget by id error: " + e.getMessage());
            return null;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户的所有预算
     * @param userId 用户ID
     * @return 预算列表
     */
    public List<Budget> queryByUserId(long userId) {
        List<Budget> budgets = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "user_id = ?", new String[]{String.valueOf(userId)}, null, null, "category_id ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Budget budget = cursorToBudget(cursor);
                    budgets.add(budget);
                } while (cursor.moveToNext());
            }
            return budgets;
        } catch (Exception e) {
            LogUtils.e("Query budgets by user id error: " + e.getMessage());
            return budgets;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户在指定分类的预算
     * @param userId 用户ID
     * @param categoryId 分类ID
     * @return 预算对象，不存在返回null
     */
    public Budget queryByUserIdAndCategoryId(long userId, long categoryId) {
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "user_id = ? AND category_id = ?",
                    new String[]{String.valueOf(userId), String.valueOf(categoryId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToBudget(cursor);
            }
            return null;
        } catch (Exception e) {
            LogUtils.e("Query budget by user id and category id error: " + e.getMessage());
            return null;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户在指定周期的预算
     * @param userId 用户ID
     * @param period 周期（月度、年度）
     * @return 预算列表
     */
    public List<Budget> queryByUserIdAndPeriod(long userId, String period) {
        List<Budget> budgets = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "user_id = ? AND period = ?",
                    new String[]{String.valueOf(userId), period}, null, null, "category_id ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Budget budget = cursorToBudget(cursor);
                    budgets.add(budget);
                } while (cursor.moveToNext());
            }
            return budgets;
        } catch (Exception e) {
            LogUtils.e("Query budgets by user id and period error: " + e.getMessage());
            return budgets;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户在指定日期范围内的预算
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 预算列表
     */
    public List<Budget> queryByDateRange(long userId, Date startDate, Date endDate) {
        List<Budget> budgets = new ArrayList<>();
        Cursor cursor = null;
        try {
            String startDateStr = DateUtils.formatDateTime(startDate);
            String endDateStr = DateUtils.formatDateTime(endDate);
            
            cursor = database.query(TABLE_NAME, null,
                    "user_id = ? AND ((start_date IS NULL AND end_date IS NULL) OR " +
                            "(start_date <= ? AND (end_date IS NULL OR end_date >= ?)))",
                    new String[]{String.valueOf(userId), endDateStr, startDateStr}, null, null, "category_id ASC");
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Budget budget = cursorToBudget(cursor);
                    budgets.add(budget);
                } while (cursor.moveToNext());
            }
            return budgets;
        } catch (Exception e) {
            LogUtils.e("Query budgets by date range error: " + e.getMessage());
            return budgets;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户当前有效的预算
     * @param userId 用户ID
     * @return 预算列表
     */
    public List<Budget> queryCurrentBudgets(long userId) {
        Date now = new Date();
        return queryByDateRange(userId, now, now);
    }
    
    /**
     * 将游标数据转换为预算对象
     * @param cursor 游标
     * @return 预算对象
     */
    private Budget cursorToBudget(Cursor cursor) {
        Budget budget = new Budget();
        budget.setId(cursor.getLong(cursor.getColumnIndex("id")));
        budget.setUserId(cursor.getLong(cursor.getColumnIndex("user_id")));
        budget.setCategoryId(cursor.getLong(cursor.getColumnIndex("category_id")));
        budget.setAmount(cursor.getDouble(cursor.getColumnIndex("amount")));
        budget.setPeriod(cursor.getString(cursor.getColumnIndex("period")));
        
        int startDateIndex = cursor.getColumnIndex("start_date");
        if (!cursor.isNull(startDateIndex)) {
            budget.setStartDate(DateUtils.parseDateTime(cursor.getString(startDateIndex)));
        }
        
        int endDateIndex = cursor.getColumnIndex("end_date");
        if (!cursor.isNull(endDateIndex)) {
            budget.setEndDate(DateUtils.parseDateTime(cursor.getString(endDateIndex)));
        }
        
        budget.setNotifyPercent(cursor.getInt(cursor.getColumnIndex("notify_percent")));
        budget.setNotifyEnabled(cursor.getInt(cursor.getColumnIndex("notify_enabled")) == 1);
        
        return budget;
    }
} 