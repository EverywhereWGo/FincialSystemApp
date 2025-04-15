package com.zjf.fincialsystem.db.dao;

import android.content.ContentValues;

import com.blankj.utilcode.util.LogUtils;
import com.zjf.fincialsystem.model.Notification;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.db.FinanceDatabaseHelper;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知DAO类
 * 提供通知相关的数据库操作
 */
public class NotificationDao extends BaseDao {
    
    private static final String TABLE_NAME = FinanceDatabaseHelper.TABLE_NOTIFICATIONS;
    
    public NotificationDao(SQLiteDatabase database) {
        super(database);
    }
    
    /**
     * 插入通知
     * @param notification 通知对象
     * @return 插入的通知ID，失败返回-1
     */
    public long insert(Notification notification) {
        try {
            ContentValues values = new ContentValues();
            values.put("user_id", notification.getUserId());
            values.put("title", notification.getTitle());
            values.put("content", notification.getContent());
            values.put("type", notification.getType());
            values.put("created_at", DateUtils.formatDateTime(notification.getCreateTime()));
            values.put("is_read", notification.isRead() == 1 ? 1 : 0);
            
            return database.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            LogUtils.e("Insert notification error: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * 更新通知
     * @param notification 通知对象
     * @return 是否成功
     */
    public boolean update(Notification notification) {
        try {
            ContentValues values = new ContentValues();
            values.put("user_id", notification.getUserId());
            values.put("title", notification.getTitle());
            values.put("content", notification.getContent());
            values.put("type", notification.getType());
            values.put("created_at", DateUtils.formatDateTime(notification.getCreateTime()));
            values.put("is_read", notification.isRead() == 1 ? 1 : 0);
            
            int rowsAffected = database.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(notification.getId())});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Update notification error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 删除通知
     * @param notificationId 通知ID
     * @return 是否成功
     */
    public boolean delete(long notificationId) {
        try {
            int rowsAffected = database.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(notificationId)});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Delete notification error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 清空用户的通知
     * @param userId 用户ID
     * @return 是否成功
     */
    public boolean deleteByUserId(long userId) {
        try {
            int rowsAffected = database.delete(TABLE_NAME, "user_id = ?", new String[]{String.valueOf(userId)});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Delete notifications by user id error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 根据ID查询通知
     * @param notificationId 通知ID
     * @return 通知对象，不存在返回null
     */
    public Notification queryById(long notificationId) {
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "id = ?", new String[]{String.valueOf(notificationId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToNotification(cursor);
            }
            return null;
        } catch (Exception e) {
            LogUtils.e("Query notification by id error: " + e.getMessage());
            return null;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户的所有通知
     * @param userId 用户ID
     * @return 通知列表
     */
    public List<Notification> queryByUserId(long userId) {
        List<Notification> notifications = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "user_id = ?", new String[]{String.valueOf(userId)}, null, null, "created_at DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Notification notification = cursorToNotification(cursor);
                    notifications.add(notification);
                } while (cursor.moveToNext());
            }
            return notifications;
        } catch (Exception e) {
            LogUtils.e("Query notifications by user id error: " + e.getMessage());
            return notifications;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户的未读通知
     * @param userId 用户ID
     * @return 通知列表
     */
    public List<Notification> queryUnreadByUserId(long userId) {
        List<Notification> notifications = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "user_id = ? AND is_read = 0", new String[]{String.valueOf(userId)}, null, null, "created_at DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Notification notification = cursorToNotification(cursor);
                    notifications.add(notification);
                } while (cursor.moveToNext());
            }
            return notifications;
        } catch (Exception e) {
            LogUtils.e("Query unread notifications by user id error: " + e.getMessage());
            return notifications;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户的最近通知
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 通知列表
     */
    public List<Notification> queryRecentByUserId(long userId, int limit) {
        List<Notification> notifications = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "user_id = ?", new String[]{String.valueOf(userId)}, null, null, "created_at DESC", String.valueOf(limit));
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Notification notification = cursorToNotification(cursor);
                    notifications.add(notification);
                } while (cursor.moveToNext());
            }
            return notifications;
        } catch (Exception e) {
            LogUtils.e("Query recent notifications by user id error: " + e.getMessage());
            return notifications;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户的未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    public int queryUnreadCount(long userId) {
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE user_id = ? AND is_read = 0",
                    new String[]{String.valueOf(userId)});
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } catch (Exception e) {
            LogUtils.e("Query unread count error: " + e.getMessage());
            return 0;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 标记通知为已读
     * @param notificationId 通知ID
     * @return 是否成功
     */
    public boolean markAsRead(long notificationId) {
        try {
            ContentValues values = new ContentValues();
            values.put("is_read", 1);
            
            int rowsAffected = database.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(notificationId)});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Mark notification as read error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 标记用户的所有通知为已读
     * @param userId 用户ID
     * @return 是否成功
     */
    public boolean markAllAsRead(long userId) {
        try {
            ContentValues values = new ContentValues();
            values.put("is_read", 1);
            
            int rowsAffected = database.update(TABLE_NAME, values, "user_id = ? AND is_read = 0", new String[]{String.valueOf(userId)});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Mark all notifications as read error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 将游标数据转换为通知对象
     * @param cursor 游标
     * @return 通知对象
     */
    private Notification cursorToNotification(Cursor cursor) {
        Notification notification = new Notification();
        notification.setId(cursor.getLong(cursor.getColumnIndex("id")));
        notification.setUserId(cursor.getLong(cursor.getColumnIndex("user_id")));
        notification.setTitle(cursor.getString(cursor.getColumnIndex("title")));
        notification.setContent(cursor.getString(cursor.getColumnIndex("content")));
        notification.setType(cursor.getString(cursor.getColumnIndex("type")));
        notification.setCreateTime(DateUtils.parseDateTime(cursor.getString(cursor.getColumnIndex("created_at"))));
        notification.setRead(cursor.getInt(cursor.getColumnIndex("is_read")) == 1 ? 1 : 0);
        return notification;
    }
} 