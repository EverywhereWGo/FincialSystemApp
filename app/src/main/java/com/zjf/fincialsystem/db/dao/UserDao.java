package com.zjf.fincialsystem.db.dao;

import android.content.ContentValues;

import com.blankj.utilcode.util.LogUtils;
import com.zjf.fincialsystem.model.User;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.db.FinanceDatabaseHelper;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 用户DAO类
 * 提供用户相关的数据库操作
 */
public class UserDao extends BaseDao {
    
    private static final String TABLE_NAME = FinanceDatabaseHelper.TABLE_USERS;
    
    public UserDao(SQLiteDatabase database) {
        super(database);
    }
    
    /**
     * 插入用户
     * @param user 用户对象
     * @return 插入的用户ID，失败返回-1
     */
    public long insert(User user) {
        try {
            ContentValues values = new ContentValues();
            values.put("username", user.getUsername());
            values.put("phone", user.getPhone());
            values.put("email", user.getEmail());
            values.put("password", user.getPassword());
            values.put("role", user.getRole());
            values.put("created_at", user.getCreatedAt());
            values.put("updated_at", user.getUpdatedAt());
            
            if (user.getLastLoginTime() != null) {
                values.put("last_login_time", user.getLastLoginTime());
            }
            
            values.put("failed_attempts", user.getFailedAttempts());
            
            if (user.getLockedUntil() != null) {
                values.put("locked_until", user.getLockedUntil());
            }
            
            return database.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            LogUtils.e("Insert user error: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * 更新用户
     * @param user 用户对象
     * @return 是否成功
     */
    public boolean update(User user) {
        try {
            ContentValues values = new ContentValues();
            
            if (user.getUsername() != null) {
                values.put("username", user.getUsername());
            }
            
            if (user.getPhone() != null) {
                values.put("phone", user.getPhone());
            }
            
            if (user.getEmail() != null) {
                values.put("email", user.getEmail());
            }
            
            if (user.getPassword() != null) {
                values.put("password", user.getPassword());
            }
            
            if (user.getRole() != null) {
                values.put("role", user.getRole());
            }
            
            values.put("updated_at", user.getUpdatedAt());
            
            if (user.getLastLoginTime() != null) {
                values.put("last_login_time", user.getLastLoginTime());
            }
            
            values.put("failed_attempts", user.getFailedAttempts());
            
            if (user.getLockedUntil() != null) {
                values.put("locked_until", user.getLockedUntil());
            } else {
                values.putNull("locked_until");
            }
            
            int rowsAffected = database.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(user.getId())});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Update user error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 删除用户
     * @param userId 用户ID
     * @return 是否成功
     */
    public boolean delete(long userId) {
        try {
            int rowsAffected = database.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(userId)});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Delete user error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 根据ID查询用户
     * @param userId 用户ID
     * @return 用户对象，不存在返回null
     */
    public User queryById(long userId) {
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "id = ?", new String[]{String.valueOf(userId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToUser(cursor);
            }
            return null;
        } catch (Exception e) {
            LogUtils.e("Query user by id error: " + e.getMessage());
            return null;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象，不存在返回null
     */
    public User queryByUsername(String username) {
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "username = ?", new String[]{username}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToUser(cursor);
            }
            return null;
        } catch (Exception e) {
            LogUtils.e("Query user by username error: " + e.getMessage());
            return null;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户对象
     */
    public User queryByPhone(String phone) {
        User user = null;
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "phone = ?", new String[]{phone}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                user = cursorToUser(cursor);
            }
        } catch (Exception e) {
            LogUtils.e("Query user by phone error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return user;
    }
    
    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户对象
     */
    public User queryByEmail(String email) {
        User user = null;
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "email = ?", new String[]{email}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                user = cursorToUser(cursor);
            }
        } catch (Exception e) {
            LogUtils.e("Query user by email error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return user;
    }
    
    /**
     * 查询所有用户
     * @return 用户列表
     */
    public List<User> queryAll() {
        List<User> users = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, null, null, null, null, "id ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    User user = cursorToUser(cursor);
                    users.add(user);
                } while (cursor.moveToNext());
            }
            return users;
        } catch (Exception e) {
            LogUtils.e("Query all users error: " + e.getMessage());
            return users;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 更新用户登录时间
     * @param userId 用户ID
     * @return 是否更新成功
     */
    public boolean updateLoginTime(long userId) {
        try {
            ContentValues values = new ContentValues();
            values.put("last_login_time", System.currentTimeMillis());
            
            int rowsAffected = database.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(userId)});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Update login time error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 增加用户登录失败次数
     * @param userId 用户ID
     * @return 是否更新成功
     */
    public boolean incrementFailedAttempts(long userId) {
        try {
            // 先获取当前失败次数
            User user = queryById(userId);
            if (user == null) {
                return false;
            }
            
            int failedAttempts = user.getFailedAttempts() + 1;
            
            ContentValues values = new ContentValues();
            values.put("failed_attempts", failedAttempts);
            
            // 如果失败次数达到阈值，则锁定账户
            if (failedAttempts >= 5) {
                // 锁定1小时
                long lockedUntil = System.currentTimeMillis() + (60 * 60 * 1000);
                values.put("locked_until", lockedUntil);
            }
            
            int rowsAffected = database.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(userId)});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Increment failed attempts error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 重置用户登录失败次数
     * @param userId 用户ID
     * @return 是否重置成功
     */
    public boolean resetFailedAttempts(long userId) {
        try {
            ContentValues values = new ContentValues();
            values.put("failed_attempts", 0);
            values.put("locked_until", (Long) null);
            
            int rowsAffected = database.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(userId)});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Reset failed attempts error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 将游标数据转换为用户对象
     * @param cursor 游标
     * @return 用户对象
     */
    private User cursorToUser(Cursor cursor) {
        User user = new User();
        
        int idIndex = cursor.getColumnIndex("id");
        if (idIndex != -1) {
            user.setId(cursor.getLong(idIndex));
        }
        
        int usernameIndex = cursor.getColumnIndex("username");
        if (usernameIndex != -1) {
            user.setUsername(cursor.getString(usernameIndex));
        }
        
        int phoneIndex = cursor.getColumnIndex("phone");
        if (phoneIndex != -1) {
            user.setPhone(cursor.getString(phoneIndex));
        }
        
        int emailIndex = cursor.getColumnIndex("email");
        if (emailIndex != -1) {
            user.setEmail(cursor.getString(emailIndex));
        }
        
        int passwordIndex = cursor.getColumnIndex("password");
        if (passwordIndex != -1) {
            user.setPassword(cursor.getString(passwordIndex));
        }
        
        int roleIndex = cursor.getColumnIndex("role");
        if (roleIndex != -1) {
            user.setRole(cursor.getString(roleIndex));
        }
        
        int createdAtIndex = cursor.getColumnIndex("created_at");
        if (createdAtIndex != -1) {
            user.setCreatedAt(cursor.getLong(createdAtIndex));
        }
        
        int updatedAtIndex = cursor.getColumnIndex("updated_at");
        if (updatedAtIndex != -1) {
            user.setUpdatedAt(cursor.getLong(updatedAtIndex));
        }
        
        int lastLoginTimeIndex = cursor.getColumnIndex("last_login_time");
        if (lastLoginTimeIndex != -1 && !cursor.isNull(lastLoginTimeIndex)) {
            user.setLastLoginTime(cursor.getString(lastLoginTimeIndex));
        }
        
        int failedAttemptsIndex = cursor.getColumnIndex("failed_attempts");
        if (failedAttemptsIndex != -1) {
            user.setFailedAttempts(cursor.getInt(failedAttemptsIndex));
        }
        
        int lockedUntilIndex = cursor.getColumnIndex("locked_until");
        if (lockedUntilIndex != -1 && !cursor.isNull(lockedUntilIndex)) {
            user.setLockedUntil(cursor.getString(lockedUntilIndex));
        }
        
        return user;
    }

    /**
     * 根据ID获取用户
     * @param userId 用户ID
     * @return 用户对象，不存在返回null
     */
    public User getUserById(long userId) {
        return queryById(userId);
    }

    /**
     * 更新用户信息
     * @param user 用户对象
     * @return 是否成功
     */
    public boolean updateUser(User user) {
        return update(user);
    }

    /**
     * 插入用户
     * @param user 用户对象
     * @return 插入的用户ID，失败返回-1
     */
    public long insertUser(User user) {
        return insert(user);
    }
} 