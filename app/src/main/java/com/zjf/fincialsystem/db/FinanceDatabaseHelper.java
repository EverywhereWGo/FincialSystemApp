package com.zjf.fincialsystem.db;

import android.content.Context;

import com.blankj.utilcode.util.LogUtils;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

/**
 * 数据库帮助类
 * 用于创建和升级数据库
 */
public class FinanceDatabaseHelper extends SQLiteOpenHelper {
    
    // 添加表名常量
    public static final String TABLE_USERS = "users";
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String TABLE_BUDGETS = "budgets";
    public static final String TABLE_LOGIN_HISTORY = "login_history";
    public static final String TABLE_NOTIFICATIONS = "notifications";
    
    // 用户表
    private static final String CREATE_TABLE_USERS = 
            "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username VARCHAR(50) NOT NULL," +
                    "phone VARCHAR(20)," +
                    "email VARCHAR(100)," +
                    "password VARCHAR(100) NOT NULL," +
                    "role VARCHAR(20) DEFAULT 'user'," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "last_login_time DATETIME," +
                    "failed_attempts INTEGER DEFAULT 0," +
                    "locked_until DATETIME" +
                    ")";
    
    // 交易记录表
    private static final String CREATE_TABLE_TRANSACTIONS = 
            "CREATE TABLE IF NOT EXISTS " + TABLE_TRANSACTIONS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "amount REAL NOT NULL," +
                    "type INTEGER NOT NULL," +  // 1:收入, 2:支出, 3:转账
                    "category_id INTEGER," +
                    "date DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "description TEXT," +  // 添加描述字段
                    "note TEXT," +
                    "image_path VARCHAR(200)," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +  // 添加创建时间
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +  // 添加更新时间
                    "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id)," +
                    "FOREIGN KEY(category_id) REFERENCES " + TABLE_CATEGORIES + "(id)" +
                    ")";
    
    // 分类表
    private static final String CREATE_TABLE_CATEGORIES = 
            "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name VARCHAR(50) NOT NULL," +
                    "type VARCHAR(20) NOT NULL," +  // income, expense
                    "icon VARCHAR(50)," +
                    "parent_id INTEGER," +
                    "user_id INTEGER," +
                    "is_default INTEGER DEFAULT 0," +
                    "FOREIGN KEY(parent_id) REFERENCES " + TABLE_CATEGORIES + "(id)," +
                    "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id)" +
                    ")";
    
    // 预算表
    private static final String CREATE_TABLE_BUDGETS = 
            "CREATE TABLE IF NOT EXISTS " + TABLE_BUDGETS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "category_id INTEGER," +
                    "amount REAL NOT NULL," +
                    "period VARCHAR(20) NOT NULL," +  // monthly, yearly
                    "start_date DATETIME," +
                    "end_date DATETIME," +
                    "notify_percent INTEGER DEFAULT 80," +
                    "notify_enabled INTEGER DEFAULT 1," +
                    "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id)," +
                    "FOREIGN KEY(category_id) REFERENCES " + TABLE_CATEGORIES + "(id)" +
                    ")";
    
    // 登录历史表
    private static final String CREATE_TABLE_LOGIN_HISTORY = 
            "CREATE TABLE IF NOT EXISTS " + TABLE_LOGIN_HISTORY + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "login_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "ip_address VARCHAR(50)," +
                    "device_model VARCHAR(100)," +
                    "success INTEGER DEFAULT 1," +
                    "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id)" +
                    ")";
    
    // 通知表
    private static final String CREATE_TABLE_NOTIFICATIONS = 
            "CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "title VARCHAR(100) NOT NULL," +
                    "content TEXT NOT NULL," +
                    "type VARCHAR(50)," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "is_read INTEGER DEFAULT 0," +
                    "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id)" +
                    ")";
    
    // 修改数据库版本号
    public static final String DATABASE_NAME = "financial_system.db";
    public static final int DATABASE_VERSION = 2; // 增加版本号
    
    public FinanceDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        LogUtils.d("Creating database tables");
        
        try {
            // 创建表（注意顺序：先创建被引用的表）
            // 1. 用户表（被多个表引用）
            db.execSQL(CREATE_TABLE_USERS);
            LogUtils.d("Created users table");
            
            // 2. 分类表（自引用和被交易表引用）
            db.execSQL(CREATE_TABLE_CATEGORIES);
            LogUtils.d("Created categories table");
            
            // 3. 交易表（引用用户表和分类表）
            db.execSQL(CREATE_TABLE_TRANSACTIONS);
            LogUtils.d("Created transactions table");
            
            // 4. 预算表（引用用户表和分类表）
            db.execSQL(CREATE_TABLE_BUDGETS);
            LogUtils.d("Created budgets table");
            
            // 5. 登录历史表（引用用户表）
            db.execSQL(CREATE_TABLE_LOGIN_HISTORY);
            LogUtils.d("Created login_history table");
            
            // 6. 通知表（引用用户表）
            db.execSQL(CREATE_TABLE_NOTIFICATIONS);
            LogUtils.d("Created notifications table");
            
            // 插入默认分类数据
            insertDefaultCategories(db);
            LogUtils.d("Inserted default categories");
        } catch (Exception e) {
            LogUtils.e("Error creating database tables: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogUtils.d("Upgrading database from version " + oldVersion + " to " + newVersion);
        
        if (oldVersion < 2) {
            // 在版本1到版本2的升级中，添加description字段、created_at和updated_at字段
            try {
                // 检查transactions表中是否已经有description列
                boolean hasDescCol = false;
                boolean hasCreatedAtCol = false;
                boolean hasUpdatedAtCol = false;
                
                android.database.Cursor pragmaCursor = db.rawQuery("PRAGMA table_info(" + TABLE_TRANSACTIONS + ")", null);
                if (pragmaCursor != null) {
                    int nameIndex = pragmaCursor.getColumnIndex("name");
                    while (pragmaCursor.moveToNext()) {
                        String colName = pragmaCursor.getString(nameIndex);
                        if ("description".equals(colName)) {
                            hasDescCol = true;
                        } else if ("created_at".equals(colName)) {
                            hasCreatedAtCol = true;
                        } else if ("updated_at".equals(colName)) {
                            hasUpdatedAtCol = true;
                        }
                    }
                    pragmaCursor.close();
                }
                
                // 添加缺失的列
                if (!hasDescCol) {
                    db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN description TEXT");
                    LogUtils.d("Added description column to transactions table");
                }
                
                if (!hasCreatedAtCol) {
                    db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP");
                    LogUtils.d("Added created_at column to transactions table");
                }
                
                if (!hasUpdatedAtCol) {
                    db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP");
                    LogUtils.d("Added updated_at column to transactions table");
                }
                
                // 还需要修改type列的类型，但SQLite不直接支持ALTER COLUMN，需要创建新表并迁移数据
                // 对于简单的演示应用，可以考虑重新创建表
                
            } catch (Exception e) {
                LogUtils.e("Error upgrading database: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 插入默认分类数据
     */
    private void insertDefaultCategories(SQLiteDatabase db) {
        // 支出分类
        String[] expenseCategories = {"餐饮", "购物", "住房", "交通", "医疗", "教育", "娱乐", "旅行", "通讯", "其他"};
        String[] expenseIcons = {"ic_food", "ic_shopping", "ic_house", "ic_transport", "ic_medical", "ic_education", "ic_entertainment", "ic_travel", "ic_communication", "ic_other"};
        
        // 收入分类
        String[] incomeCategories = {"工资", "奖金", "投资", "兼职", "礼金", "其他"};
        String[] incomeIcons = {"ic_salary", "ic_bonus", "ic_investment", "ic_part_time", "ic_gift", "ic_other"};
        
        // 插入支出分类
        for (int i = 0; i < expenseCategories.length; i++) {
            db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (name, type, icon, is_default) VALUES (?, ?, ?, ?)",
                    new Object[]{expenseCategories[i], "expense", expenseIcons[i], 1});
        }
        
        // 插入收入分类
        for (int i = 0; i < incomeCategories.length; i++) {
            db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (name, type, icon, is_default) VALUES (?, ?, ?, ?)",
                    new Object[]{incomeCategories[i], "income", incomeIcons[i], 1});
        }
    }
} 