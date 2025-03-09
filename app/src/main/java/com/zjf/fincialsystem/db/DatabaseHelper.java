package com.zjf.fincialsystem.db;

import android.content.Context;

import com.zjf.fincialsystem.utils.LogUtils;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

/**
 * 数据库帮助类
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String TAG = "DatabaseHelper";
    
    // 数据库名称和版本
    private static final String DATABASE_NAME = "financial_system.db";
    private static final int DATABASE_VERSION = 2;
    
    // 用户表
    public static final String TABLE_USER = "user";
    public static final String USER_ID = "id";
    public static final String USER_USERNAME = "username";
    public static final String USER_PASSWORD = "password";
    public static final String USER_NAME = "name";
    public static final String USER_EMAIL = "email";
    public static final String USER_PHONE = "phone";
    public static final String USER_AVATAR = "avatar";
    public static final String USER_CREATED_AT = "created_at";
    public static final String USER_UPDATED_AT = "updated_at";
    public static final String USER_ROLE = "role";
    public static final String USER_LAST_LOGIN_TIME = "last_login_time";
    public static final String USER_FAILED_ATTEMPTS = "failed_attempts";
    public static final String USER_LOCKED_UNTIL = "locked_until";
    
    // 分类表
    public static final String TABLE_CATEGORY = "category";
    public static final String CATEGORY_ID = "id";
    public static final String CATEGORY_NAME = "name";
    public static final String CATEGORY_TYPE = "type";
    public static final String CATEGORY_ICON = "icon";
    public static final String CATEGORY_COLOR = "color";
    public static final String CATEGORY_USER_ID = "user_id";
    public static final String CATEGORY_CREATED_AT = "created_at";
    public static final String CATEGORY_UPDATED_AT = "updated_at";
    
    // 交易记录表 - 改为"transactions"避免使用SQLite关键字
    public static final String TABLE_TRANSACTION = "transactions";
    public static final String TRANSACTION_ID = "id";
    public static final String TRANSACTION_USER_ID = "user_id";
    public static final String TRANSACTION_CATEGORY_ID = "category_id";
    public static final String TRANSACTION_TYPE = "type";
    public static final String TRANSACTION_AMOUNT = "amount";
    public static final String TRANSACTION_DATE = "date";
    public static final String TRANSACTION_DESCRIPTION = "description";
    public static final String TRANSACTION_CREATED_AT = "created_at";
    public static final String TRANSACTION_UPDATED_AT = "updated_at";
    
    // 创建用户表的SQL语句
    private static final String CREATE_TABLE_USER = "CREATE TABLE " + TABLE_USER + " ("
            + USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + USER_USERNAME + " TEXT NOT NULL UNIQUE, "
            + USER_PASSWORD + " TEXT NOT NULL, "
            + USER_NAME + " TEXT, "
            + USER_EMAIL + " TEXT, "
            + USER_PHONE + " TEXT, "
            + USER_AVATAR + " TEXT, "
            + USER_ROLE + " TEXT DEFAULT 'user', "
            + USER_LAST_LOGIN_TIME + " DATETIME, "
            + USER_FAILED_ATTEMPTS + " INTEGER DEFAULT 0, "
            + USER_LOCKED_UNTIL + " DATETIME, "
            + USER_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + USER_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
            + ")";
    
    // 创建分类表的SQL语句
    private static final String CREATE_TABLE_CATEGORY = "CREATE TABLE " + TABLE_CATEGORY + " ("
            + CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + CATEGORY_NAME + " TEXT NOT NULL, "
            + CATEGORY_TYPE + " INTEGER NOT NULL, "
            + CATEGORY_ICON + " TEXT, "
            + CATEGORY_COLOR + " TEXT, "
            + CATEGORY_USER_ID + " INTEGER, "
            + CATEGORY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + CATEGORY_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (" + CATEGORY_USER_ID + ") REFERENCES " + TABLE_USER + "(" + USER_ID + ")"
            + ")";
    
    // 创建交易记录表的SQL语句
    private static final String CREATE_TABLE_TRANSACTION = "CREATE TABLE " + TABLE_TRANSACTION + " ("
            + TRANSACTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TRANSACTION_USER_ID + " INTEGER NOT NULL, "
            + TRANSACTION_CATEGORY_ID + " INTEGER NOT NULL, "
            + TRANSACTION_TYPE + " INTEGER NOT NULL, "
            + TRANSACTION_AMOUNT + " REAL NOT NULL, "
            + TRANSACTION_DATE + " DATETIME NOT NULL, "
            + TRANSACTION_DESCRIPTION + " TEXT, "
            + TRANSACTION_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + TRANSACTION_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (" + TRANSACTION_USER_ID + ") REFERENCES " + TABLE_USER + "(" + USER_ID + "), "
            + "FOREIGN KEY (" + TRANSACTION_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORY + "(" + CATEGORY_ID + ")"
            + ")";
    
    /**
     * 构造函数
     * @param context 上下文
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        LogUtils.i(TAG, "创建数据库表");
        db.execSQL(CREATE_TABLE_USER);
        db.execSQL(CREATE_TABLE_CATEGORY);
        db.execSQL(CREATE_TABLE_TRANSACTION);
        
        // 初始化默认数据
        initDefaultData(db);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogUtils.i(TAG, "升级数据库：" + oldVersion + " -> " + newVersion);
        
        // 如果需要升级数据库，可以在这里添加升级逻辑
        if (oldVersion < 2) {
            // 版本1升级到版本2的逻辑
        }
    }
    
    /**
     * 初始化默认数据
     * @param db 数据库
     */
    private void initDefaultData(SQLiteDatabase db) {
        LogUtils.i(TAG, "初始化默认数据");
        
        // 添加默认用户
        db.execSQL("INSERT INTO " + TABLE_USER + " (" 
                + USER_USERNAME + ", " 
                + USER_PASSWORD + ", "
                + USER_ROLE + ", " 
                + USER_NAME + ") VALUES ('admin', 'admin123', 'admin', '管理员')");
        
        // 添加默认收入分类
        db.execSQL("INSERT INTO " + TABLE_CATEGORY + " (" 
                + CATEGORY_NAME + ", " 
                + CATEGORY_TYPE + ", " 
                + CATEGORY_ICON + ", " 
                + CATEGORY_COLOR + ", " 
                + CATEGORY_USER_ID + ") VALUES ('工资', 1, 'ic_salary', '#4CAF50', 1)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORY + " (" 
                + CATEGORY_NAME + ", " 
                + CATEGORY_TYPE + ", " 
                + CATEGORY_ICON + ", " 
                + CATEGORY_COLOR + ", " 
                + CATEGORY_USER_ID + ") VALUES ('奖金', 1, 'ic_bonus', '#8BC34A', 1)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORY + " (" 
                + CATEGORY_NAME + ", " 
                + CATEGORY_TYPE + ", " 
                + CATEGORY_ICON + ", " 
                + CATEGORY_COLOR + ", " 
                + CATEGORY_USER_ID + ") VALUES ('投资收益', 1, 'ic_investment', '#009688', 1)");
        
        // 添加默认支出分类
        db.execSQL("INSERT INTO " + TABLE_CATEGORY + " (" 
                + CATEGORY_NAME + ", " 
                + CATEGORY_TYPE + ", " 
                + CATEGORY_ICON + ", " 
                + CATEGORY_COLOR + ", " 
                + CATEGORY_USER_ID + ") VALUES ('餐饮', 2, 'ic_food', '#F44336', 1)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORY + " (" 
                + CATEGORY_NAME + ", " 
                + CATEGORY_TYPE + ", " 
                + CATEGORY_ICON + ", " 
                + CATEGORY_COLOR + ", " 
                + CATEGORY_USER_ID + ") VALUES ('购物', 2, 'ic_shopping', '#E91E63', 1)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORY + " (" 
                + CATEGORY_NAME + ", " 
                + CATEGORY_TYPE + ", " 
                + CATEGORY_ICON + ", " 
                + CATEGORY_COLOR + ", " 
                + CATEGORY_USER_ID + ") VALUES ('交通', 2, 'ic_transport', '#9C27B0', 1)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORY + " (" 
                + CATEGORY_NAME + ", " 
                + CATEGORY_TYPE + ", " 
                + CATEGORY_ICON + ", " 
                + CATEGORY_COLOR + ", " 
                + CATEGORY_USER_ID + ") VALUES ('住房', 2, 'ic_house', '#673AB7', 1)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORY + " (" 
                + CATEGORY_NAME + ", " 
                + CATEGORY_TYPE + ", " 
                + CATEGORY_ICON + ", " 
                + CATEGORY_COLOR + ", " 
                + CATEGORY_USER_ID + ") VALUES ('娱乐', 2, 'ic_entertainment', '#3F51B5', 1)");
    }
} 