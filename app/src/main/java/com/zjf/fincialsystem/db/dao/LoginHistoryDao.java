package com.zjf.fincialsystem.db.dao;

import android.util.Log;

import com.zjf.fincialsystem.model.LoginHistory;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.db.FinanceDatabaseHelper;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 登录历史数据访问对象
 */
public class LoginHistoryDao {
    private SQLiteDatabase database;
    private static final String TABLE_NAME = FinanceDatabaseHelper.TABLE_LOGIN_HISTORY;

    public LoginHistoryDao(SQLiteDatabase database) {
        this.database = database;
    }

    /**
     * 创建登录历史记录
     *
     * @param loginHistory 登录历史对象
     * @return 插入的ID
     */
    public long insert(LoginHistory loginHistory) {
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("user_id", loginHistory.getUserId());
            values.put("login_time", DateUtils.formatDateTime(loginHistory.getLoginTime()));
            values.put("ip_address", loginHistory.getIpAddress());
            values.put("device_info", loginHistory.getDeviceInfo());
            values.put("success", loginHistory.isSuccess() ? 1 : 0);

            return database.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 获取用户的登录历史记录
     *
     * @param userId 用户ID
     * @return 登录历史记录列表
     */
    public List<LoginHistory> getLoginHistoryByUserId(long userId) {
        List<LoginHistory> loginHistoryList = new ArrayList<>();
        try {
            Cursor cursor = database.query(
                    TABLE_NAME,
                    null,
                    "user_id = ?",
                    new String[]{String.valueOf(userId)},
                    null,
                    null,
                    "login_time DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    LoginHistory loginHistory = cursorToLoginHistory(cursor);
                    loginHistoryList.add(loginHistory);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loginHistoryList;
    }

    /**
     * 获取最近的登录历史记录
     *
     * @param limit 限制数量
     * @return 登录历史记录列表
     */
    public List<LoginHistory> getRecentLoginHistory(int limit) {
        List<LoginHistory> loginHistoryList = new ArrayList<>();
        try {
            Cursor cursor = database.query(
                    TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "login_time DESC",
                    String.valueOf(limit)
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    LoginHistory loginHistory = cursorToLoginHistory(cursor);
                    loginHistoryList.add(loginHistory);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loginHistoryList;
    }

    /**
     * 删除用户的登录历史记录
     *
     * @param userId 用户ID
     * @return 删除的行数
     */
    public int deleteByUserId(long userId) {
        try {
            return database.delete(
                    TABLE_NAME,
                    "user_id = ?",
                    new String[]{String.valueOf(userId)}
            );
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 查询指定ID的登录历史
     *
     * @param loginHistoryId 登录历史ID
     * @return 登录历史对象
     */
    public LoginHistory queryById(long loginHistoryId) {
        LoginHistory loginHistory = null;
        Cursor cursor = null;

        try {
            cursor = database.query(TABLE_NAME, null, "id = ?",
                    new String[]{String.valueOf(loginHistoryId)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                loginHistory = cursorToLoginHistory(cursor);
            }
        } catch (Exception e) {
            Log.e("LoginHistoryDao", "Query by ID failed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return loginHistory;
    }

    /**
     * 查询指定用户的最后一次成功登录
     *
     * @param userId 用户ID
     * @return 登录历史对象
     */
    public LoginHistory queryLastSuccessLogin(long userId) {
        LoginHistory loginHistory = null;
        Cursor cursor = null;

        try {
            cursor = database.query(TABLE_NAME, null, "user_id = ? AND success = 1",
                    new String[]{String.valueOf(userId)}, null, null, "login_time DESC", "1");

            if (cursor != null && cursor.moveToFirst()) {
                loginHistory = cursorToLoginHistory(cursor);
            }
        } catch (Exception e) {
            Log.e("LoginHistoryDao", "Query last success login failed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return loginHistory;
    }

    /**
     * 查询指定用户的登录失败次数
     *
     * @param userId 用户ID
     * @return 失败次数
     */
    public int queryFailedCount(long userId) {
        Cursor cursor = null;
        int count = 0;

        try {
            cursor = database.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME +
                    " WHERE user_id = ? AND success = 0", new String[]{String.valueOf(userId)});

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e("LoginHistoryDao", "Query failed count failed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return count;
    }

    /**
     * 将游标数据转换为登录历史对象
     *
     * @param cursor 游标
     * @return 登录历史对象
     */
    private LoginHistory cursorToLoginHistory(Cursor cursor) {
        LoginHistory loginHistory = new LoginHistory();
        loginHistory.setId(cursor.getLong(cursor.getColumnIndex("id")));
        loginHistory.setUserId(cursor.getLong(cursor.getColumnIndex("user_id")));
        loginHistory.setLoginTime(DateUtils.parseDateTime(cursor.getString(cursor.getColumnIndex("login_time"))));
        loginHistory.setIpAddress(cursor.getString(cursor.getColumnIndex("ip_address")));
        loginHistory.setDeviceInfo(cursor.getString(cursor.getColumnIndex("device_info")));
        loginHistory.setSuccess(cursor.getInt(cursor.getColumnIndex("success")) == 1);
        return loginHistory;
    }
} 