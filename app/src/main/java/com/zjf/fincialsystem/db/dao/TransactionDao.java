package com.zjf.fincialsystem.db.dao;

import android.content.ContentValues;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.db.FinanceDatabaseHelper;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 交易记录DAO类
 * 提供交易记录相关的数据库操作
 */
public class TransactionDao extends BaseDao {
    
    private static final String TABLE_NAME = FinanceDatabaseHelper.TABLE_TRANSACTIONS;
    private static final String TAG = "TransactionDao";
    
    public TransactionDao(SQLiteDatabase database) {
        super(database);
    }
    
    /**
     * 插入交易记录
     * @param transaction 交易记录对象
     * @return 插入的交易记录ID，失败返回-1
     */
    public long insert(Transaction transaction) {
        try {
            LogUtils.d(TAG, "开始插入交易记录: " + transaction.toString());
            
            // 检查数据库连接
            if (database == null) {
                LogUtils.e(TAG, "数据库连接为空");
                return -1;
            }
            
            if (!database.isOpen()) {
                LogUtils.e(TAG, "数据库连接已关闭");
                return -1;
            }
            
            LogUtils.w(TAG, "注意：当前使用的是本地数据库存储，而不是调用API！应当改为调用API接口");
            
            ContentValues values = new ContentValues();
            values.put("user_id", transaction.getUserId());
            values.put("amount", transaction.getAmount());
            values.put("type", transaction.getType());
            values.put("category_id", transaction.getCategoryId());
            values.put("date", DateUtils.formatDateTime(transaction.getDate()));
            values.put("description", transaction.getDescription());
            values.put("note", transaction.getNote());
            values.put("image_path", transaction.getImagePath());

            
            LogUtils.d(TAG, "准备插入数据，ContentValues: " + values.toString());
            
            long result = database.insert(TABLE_NAME, null, values);
            
            if (result > 0) {
                LogUtils.d(TAG, "交易记录插入成功，ID: " + result);
            } else {
                LogUtils.e(TAG, "交易记录插入失败，返回值: " + result);
            }
            
            return result;
        } catch (Exception e) {
            LogUtils.e(TAG, "插入交易记录出错: " + e.getMessage(), e);
            return -1;
        }
    }
    
    /**
     * 更新交易记录
     * @param transaction 交易记录对象
     * @return 是否成功
     */
    public boolean update(Transaction transaction) {
        try {
            ContentValues values = new ContentValues();
            values.put("user_id", transaction.getUserId());
            values.put("amount", transaction.getAmount());
            values.put("type", transaction.getType());
            values.put("category_id", transaction.getCategoryId());
            values.put("date", DateUtils.formatDateTime(transaction.getDate()));
            values.put("note", transaction.getNote());
            values.put("image_path", transaction.getImagePath());
            
            int rowsAffected = database.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(transaction.getId())});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Update transaction error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 删除交易记录
     * @param transactionId 交易记录ID
     * @return 是否成功
     */
    public boolean delete(long transactionId) {
        try {
            int rowsAffected = database.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(transactionId)});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Delete transaction error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 根据ID查询交易记录
     * @param transactionId 交易记录ID
     * @return 交易记录对象，不存在返回null
     */
    public Transaction queryById(long transactionId) {
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "id = ?", new String[]{String.valueOf(transactionId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToTransaction(cursor);
            }
            return null;
        } catch (Exception e) {
            LogUtils.e("Query transaction by id error: " + e.getMessage());
            return null;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户的所有交易记录
     * @param userId 用户ID
     * @return 交易记录列表
     */
    public List<Transaction> queryByUserId(long userId) {
        List<Transaction> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "user_id = ?", new String[]{String.valueOf(userId)}, null, null, "date DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Transaction transaction = cursorToTransaction(cursor);
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
            return transactions;
        } catch (Exception e) {
            LogUtils.e("Query transactions by user id error: " + e.getMessage());
            return transactions;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户在指定日期范围内的交易记录
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 交易记录列表
     */
    public List<Transaction> queryByDateRange(long userId, Date startDate, Date endDate) {
        List<Transaction> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            String startDateStr = DateUtils.formatDateTime(startDate);
            String endDateStr = DateUtils.formatDateTime(endDate);
            
            cursor = database.query(TABLE_NAME, null, "user_id = ? AND date BETWEEN ? AND ?",
                    new String[]{String.valueOf(userId), startDateStr, endDateStr}, null, null, "date DESC");
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Transaction transaction = cursorToTransaction(cursor);
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
            return transactions;
        } catch (Exception e) {
            LogUtils.e("Query transactions by date range error: " + e.getMessage());
            return transactions;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户在指定月份的交易记录
     * @param userId 用户ID
     * @param year 年
     * @param month 月（1-12）
     * @return 交易记录列表
     */
    public List<Transaction> queryByMonth(long userId, int year, int month) {
        Date startDate = DateUtils.getFirstDayOfMonth(year, month);
        Date endDate = DateUtils.getLastDayOfMonth(year, month);
        return queryByDateRange(userId, startDate, endDate);
    }
    
    /**
     * 查询用户在当前月份的交易记录
     * @param userId 用户ID
     * @return 交易记录列表
     */
    public List<Transaction> queryByCurrentMonth(long userId) {
        Date startDate = DateUtils.getFirstDayOfMonth();
        Date endDate = DateUtils.getLastDayOfMonth();
        return queryByDateRange(userId, startDate, endDate);
    }
    
    /**
     * 查询用户在指定分类的交易记录
     * @param userId 用户ID
     * @param categoryId 分类ID
     * @return 交易记录列表
     */
    public List<Transaction> queryByCategory(long userId, long categoryId) {
        List<Transaction> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "user_id = ? AND category_id = ?",
                    new String[]{String.valueOf(userId), String.valueOf(categoryId)}, null, null, "date DESC");
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Transaction transaction = cursorToTransaction(cursor);
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
            return transactions;
        } catch (Exception e) {
            LogUtils.e("Query transactions by category error: " + e.getMessage());
            return transactions;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户在指定类型的交易记录
     * @param userId 用户ID
     * @param type 交易类型（收入、支出、转账）
     * @return 交易记录列表
     */
    public List<Transaction> queryByType(long userId, int type) {
        List<Transaction> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "user_id = ? AND type = ?",
                    new String[]{String.valueOf(userId), String.valueOf(type)}, null, null, "date DESC");
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Transaction transaction = cursorToTransaction(cursor);
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
            return transactions;
        } catch (Exception e) {
            LogUtils.e("Query transactions by type error: " + e.getMessage());
            return transactions;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 统计用户在指定日期范围内的收入总额
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 收入总额
     */
    public double sumIncomeByDateRange(long userId, Date startDate, Date endDate) {
        Cursor cursor = null;
        try {
            String startDateStr = DateUtils.formatDateTime(startDate);
            String endDateStr = DateUtils.formatDateTime(endDate);
            
            cursor = database.rawQuery("SELECT SUM(amount) FROM " + TABLE_NAME +
                    " WHERE user_id = ? AND type = ? AND date BETWEEN ? AND ?",
                    new String[]{String.valueOf(userId), String.valueOf(Transaction.TYPE_INCOME), startDateStr, endDateStr});
            
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
            return 0;
        } catch (Exception e) {
            LogUtils.e("Sum income by date range error: " + e.getMessage());
            return 0;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 统计用户在指定日期范围内的支出总额
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 支出总额
     */
    public double sumExpenseByDateRange(long userId, Date startDate, Date endDate) {
        Cursor cursor = null;
        try {
            String startDateStr = DateUtils.formatDateTime(startDate);
            String endDateStr = DateUtils.formatDateTime(endDate);
            
            cursor = database.rawQuery("SELECT SUM(amount) FROM " + TABLE_NAME +
                    " WHERE user_id = ? AND type = ? AND date BETWEEN ? AND ?",
                    new String[]{String.valueOf(userId), String.valueOf(Transaction.TYPE_EXPENSE), startDateStr, endDateStr});
            
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
            return 0;
        } catch (Exception e) {
            LogUtils.e("Sum expense by date range error: " + e.getMessage());
            return 0;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 统计用户在指定日期范围内的分类支出总额
     * @param userId 用户ID
     * @param categoryId 分类ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分类支出总额
     */
    public double sumExpenseByCategoryAndDateRange(long userId, long categoryId, Date startDate, Date endDate) {
        Cursor cursor = null;
        try {
            String startDateStr = DateUtils.formatDateTime(startDate);
            String endDateStr = DateUtils.formatDateTime(endDate);
            
            cursor = database.rawQuery("SELECT SUM(amount) FROM " + TABLE_NAME +
                    " WHERE user_id = ? AND type = ? AND category_id = ? AND date BETWEEN ? AND ?",
                    new String[]{String.valueOf(userId), String.valueOf(Transaction.TYPE_EXPENSE), String.valueOf(categoryId), startDateStr, endDateStr});
            
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
            return 0;
        } catch (Exception e) {
            LogUtils.e("Sum expense by category and date range error: " + e.getMessage());
            return 0;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询收入总额
     *
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 收入总额
     */
    public double queryIncomeSum(long userId, Date startDate, Date endDate) {
        String startDateStr = DateUtils.formatDate(startDate);
        String endDateStr = DateUtils.formatDate(endDate);
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT SUM(amount) FROM " + TABLE_NAME +
                    " WHERE user_id = ? AND type = ? AND date BETWEEN ? AND ?",
                    new String[]{String.valueOf(userId), String.valueOf(Transaction.TYPE_INCOME), startDateStr, endDateStr});
            
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
        } catch (Exception e) {
            LogUtils.e("查询收入总额失败：" + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * 查询支出总额
     *
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 支出总额
     */
    public double queryExpenseSum(long userId, Date startDate, Date endDate) {
        String startDateStr = DateUtils.formatDate(startDate);
        String endDateStr = DateUtils.formatDate(endDate);
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT SUM(amount) FROM " + TABLE_NAME +
                    " WHERE user_id = ? AND type = ? AND date BETWEEN ? AND ?",
                    new String[]{String.valueOf(userId), String.valueOf(Transaction.TYPE_EXPENSE), startDateStr, endDateStr});
            
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
        } catch (Exception e) {
            LogUtils.e("查询支出总额失败：" + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }
    
    /**
     * 查询每日支出
     *
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日支出列表（日期，金额）
     */
    public List<Object[]> queryDailyExpense(long userId, Date startDate, Date endDate) {
        String startDateStr = DateUtils.formatDate(startDate);
        String endDateStr = DateUtils.formatDate(endDate);
        Cursor cursor = null;
        List<Object[]> result = new ArrayList<>();
        
        try {
            LogUtils.d(TAG, "查询每日支出 - userId: " + userId + ", 开始日期: " + startDateStr + ", 结束日期: " + endDateStr);
            
            cursor = database.rawQuery(
                    "SELECT date, SUM(amount) FROM " + TABLE_NAME +
                            " WHERE user_id = ? AND type = ? AND date BETWEEN ? AND ? " +
                            "GROUP BY date ORDER BY date ASC",
                    new String[]{String.valueOf(userId), String.valueOf(Transaction.TYPE_EXPENSE), startDateStr, endDateStr});
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String dateStr = cursor.getString(0);
                    double amount = cursor.getDouble(1);
                    Date date = DateUtils.parseDate(dateStr);
                    
                    LogUtils.d(TAG, "每日支出数据 - 日期: " + dateStr + ", 金额: " + amount);
                    
                    // 确保date非空
                    if (date != null) {
                        Object[] item = new Object[]{date, amount};
                        result.add(item);
                    } else {
                        LogUtils.e(TAG, "解析日期失败: " + dateStr);
                    }
                } while (cursor.moveToNext());
            }
            
            LogUtils.d(TAG, "查询结果: 共 " + result.size() + " 条数据");
        } catch (Exception e) {
            LogUtils.e(TAG, "查询每日支出失败：" + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }
    
    /**
     * 查询用户最近的交易记录
     *
     * @param userId 用户ID
     * @param limit 限制条数
     * @return 交易记录列表
     */
    public List<Transaction> queryRecentByUserId(long userId, int limit) {
        Cursor cursor = null;
        List<Transaction> transactions = new ArrayList<>();
        
        try {
            cursor = database.query(
                    TABLE_NAME,
                    null,
                    "user_id = ?",
                    new String[]{String.valueOf(userId)},
                    null,
                    null,
                    "date DESC",
                    String.valueOf(limit)
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Transaction transaction = cursorToTransaction(cursor);
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            LogUtils.e("查询最近交易记录失败：" + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return transactions;
    }
    
    /**
     * 将游标转换为交易对象
     *
     * @param cursor 游标
     * @return 交易对象
     */
    private Transaction cursorToTransaction(Cursor cursor) {
        Transaction transaction = new Transaction();
        transaction.setId(cursor.getLong(cursor.getColumnIndex("id")));
        transaction.setUserId(cursor.getLong(cursor.getColumnIndex("user_id")));
        transaction.setCategoryId(cursor.getLong(cursor.getColumnIndex("category_id")));
        transaction.setType(cursor.getInt(cursor.getColumnIndex("type")));
        transaction.setAmount(cursor.getDouble(cursor.getColumnIndex("amount")));
        
        String dateStr = cursor.getString(cursor.getColumnIndex("date"));
        if (dateStr != null) {
            transaction.setDate(DateUtils.parseDate(dateStr));
        }
        
        transaction.setDescription(cursor.getString(cursor.getColumnIndex("description")));
        transaction.setNote(cursor.getString(cursor.getColumnIndex("note")));
        transaction.setImagePath(cursor.getString(cursor.getColumnIndex("image_path")));

        
        return transaction;
    }
} 