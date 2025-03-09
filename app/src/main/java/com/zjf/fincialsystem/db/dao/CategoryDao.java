package com.zjf.fincialsystem.db.dao;

import android.content.ContentValues;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.db.FinanceDatabaseHelper;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类DAO类
 * 提供分类相关的数据库操作
 */
public class CategoryDao extends BaseDao {
    
    private static final String TABLE_NAME = FinanceDatabaseHelper.TABLE_CATEGORIES;
    
    public CategoryDao(SQLiteDatabase database) {
        super(database);
    }
    
    /**
     * 插入分类
     * @param category 分类对象
     * @return 插入的分类ID，失败返回-1
     */
    public long insert(Category category) {
        try {
            ContentValues values = new ContentValues();
            values.put("name", category.getName());
            values.put("type", category.getType());
            values.put("icon", category.getIcon());
            
            if (category.getParentId() != null) {
                values.put("parent_id", category.getParentId());
            }
            
            if (category.getUserId() > 0) {
                values.put("user_id", category.getUserId());
            }
            
            values.put("is_default", category.isDefault() ? 1 : 0);
            
            return database.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            LogUtils.e("Insert category error: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * 更新分类
     * @param category 分类对象
     * @return 是否成功
     */
    public boolean update(Category category) {
        try {
            ContentValues values = new ContentValues();
            values.put("name", category.getName());
            values.put("type", category.getType());
            values.put("icon", category.getIcon());
            
            if (category.getParentId() != null) {
                values.put("parent_id", category.getParentId());
            } else {
                values.putNull("parent_id");
            }
            
            if (category.getUserId() > 0) {
                values.put("user_id", category.getUserId());
            } else {
                values.putNull("user_id");
            }
            
            values.put("is_default", category.isDefault() ? 1 : 0);
            
            int rowsAffected = database.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(category.getId())});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Update category error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 删除分类
     * @param categoryId 分类ID
     * @return 是否成功
     */
    public boolean delete(long categoryId) {
        try {
            int rowsAffected = database.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(categoryId)});
            return rowsAffected > 0;
        } catch (Exception e) {
            LogUtils.e("Delete category error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 根据ID查询分类
     * @param categoryId 分类ID
     * @return 分类对象，不存在返回null
     */
    public Category queryById(long categoryId) {
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "id = ?", new String[]{String.valueOf(categoryId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToCategory(cursor);
            }
            return null;
        } catch (Exception e) {
            LogUtils.e("Query category by id error: " + e.getMessage());
            return null;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询所有分类
     * @return 分类列表
     */
    public List<Category> queryAll() {
        List<Category> categories = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, null, null, null, null, "type ASC, name ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Category category = cursorToCategory(cursor);
                    categories.add(category);
                } while (cursor.moveToNext());
            }
            return categories;
        } catch (Exception e) {
            LogUtils.e("Query all categories error: " + e.getMessage());
            return categories;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询指定类型的分类
     * @param type 分类类型（收入、支出）
     * @return 分类列表
     */
    public List<Category> queryByType(String type) {
        List<Category> categories = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "type = ?", new String[]{type}, null, null, "name ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Category category = cursorToCategory(cursor);
                    categories.add(category);
                } while (cursor.moveToNext());
            }
            return categories;
        } catch (Exception e) {
            LogUtils.e("Query categories by type error: " + e.getMessage());
            return categories;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户自定义的分类
     * @param userId 用户ID
     * @return 分类列表
     */
    public List<Category> queryByUserId(long userId) {
        List<Category> categories = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "user_id = ?", new String[]{String.valueOf(userId)}, null, null, "type ASC, name ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Category category = cursorToCategory(cursor);
                    categories.add(category);
                } while (cursor.moveToNext());
            }
            return categories;
        } catch (Exception e) {
            LogUtils.e("Query categories by user id error: " + e.getMessage());
            return categories;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户可用的所有分类（系统默认分类 + 用户自定义分类）
     * @param userId 用户ID
     * @return 分类列表
     */
    public List<Category> queryAvailableCategories(long userId) {
        List<Category> categories = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "is_default = 1 OR user_id = ?", new String[]{String.valueOf(userId)}, null, null, "type ASC, name ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Category category = cursorToCategory(cursor);
                    categories.add(category);
                } while (cursor.moveToNext());
            }
            return categories;
        } catch (Exception e) {
            LogUtils.e("Query available categories error: " + e.getMessage());
            return categories;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询用户可用的指定类型的分类（系统默认分类 + 用户自定义分类）
     * @param userId 用户ID
     * @param type 分类类型（收入、支出）
     * @return 分类列表
     */
    public List<Category> queryAvailableCategoriesByType(long userId, String type) {
        List<Category> categories = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "(is_default = 1 OR user_id = ?) AND type = ?",
                    new String[]{String.valueOf(userId), type}, null, null, "name ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Category category = cursorToCategory(cursor);
                    categories.add(category);
                } while (cursor.moveToNext());
            }
            return categories;
        } catch (Exception e) {
            LogUtils.e("Query available categories by type error: " + e.getMessage());
            return categories;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 查询子分类
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    public List<Category> queryByParentId(long parentId) {
        List<Category> categories = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, null, "parent_id = ?", new String[]{String.valueOf(parentId)}, null, null, "name ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Category category = cursorToCategory(cursor);
                    categories.add(category);
                } while (cursor.moveToNext());
            }
            return categories;
        } catch (Exception e) {
            LogUtils.e("Query categories by parent id error: " + e.getMessage());
            return categories;
        } finally {
            closeCursor(cursor);
        }
    }
    
    /**
     * 将游标数据转换为分类对象
     * @param cursor 游标
     * @return 分类对象
     */
    private Category cursorToCategory(Cursor cursor) {
        Category category = new Category();
        category.setId(cursor.getLong(cursor.getColumnIndex("id")));
        category.setName(cursor.getString(cursor.getColumnIndex("name")));
        category.setType(cursor.getInt(cursor.getColumnIndex("type")));
        category.setIcon(cursor.getString(cursor.getColumnIndex("icon")));
        
        int parentIdIndex = cursor.getColumnIndex("parent_id");
        if (!cursor.isNull(parentIdIndex)) {
            category.setParentId(cursor.getLong(parentIdIndex));
        }
        
        int userIdIndex = cursor.getColumnIndex("user_id");
        if (!cursor.isNull(userIdIndex)) {
            category.setUserId(cursor.getLong(userIdIndex));
        }
        
        category.setDefault(cursor.getInt(cursor.getColumnIndex("is_default")) == 1);
        
        return category;
    }
} 