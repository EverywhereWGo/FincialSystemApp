package com.zjf.fincialsystem.db.dao;

import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

/**
 * 数据访问对象基类
 */
public abstract class BaseDao {
    
    protected SQLiteDatabase database;
    
    public BaseDao(SQLiteDatabase database) {
        this.database = database;
    }
    
    /**
     * 开始事务
     */
    protected void beginTransaction() {
        try {
            database.beginTransaction();
        } catch (Exception e) {
            Log.e("BaseDao", "Begin transaction failed", e);
        }
    }
    
    /**
     * 设置事务成功
     */
    protected void setTransactionSuccessful() {
        try {
            database.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("BaseDao", "Set transaction successful failed", e);
        }
    }
    
    /**
     * 结束事务
     */
    protected void endTransaction() {
        try {
            database.endTransaction();
        } catch (Exception e) {
            Log.e("BaseDao", "End transaction failed", e);
        }
    }
    
    /**
     * 执行事务
     * @param runnable 事务执行内容
     * @return 是否成功
     */
    protected boolean executeTransaction(Runnable runnable) {
        try {
            database.beginTransaction();
            runnable.run();
            database.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e("BaseDao", "Execute transaction failed", e);
            return false;
        } finally {
            try {
                database.endTransaction();
            } catch (Exception e) {
                Log.e("BaseDao", "End transaction failed", e);
            }
        }
    }
    
    /**
     * 关闭游标
     * @param cursor 游标
     */
    protected void closeCursor(net.sqlcipher.Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            try {
                cursor.close();
            } catch (Exception e) {
                Log.e("BaseDao", "Close cursor failed", e);
            }
        }
    }
} 