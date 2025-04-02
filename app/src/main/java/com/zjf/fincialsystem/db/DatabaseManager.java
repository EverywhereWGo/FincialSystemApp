package com.zjf.fincialsystem.db;

import android.content.Context;
import android.database.Cursor;

import com.zjf.fincialsystem.db.dao.CategoryDao;
import com.zjf.fincialsystem.db.dao.TransactionDao;
import com.zjf.fincialsystem.db.dao.UserDao;
import com.zjf.fincialsystem.db.dao.LoginHistoryDao;
import com.zjf.fincialsystem.db.dao.NotificationDao;
import com.zjf.fincialsystem.model.User;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.SecurityUtils;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

/**
 * 数据库管理器
 */
public class DatabaseManager {
    
    private static final String TAG = "DatabaseManager";
    private static DatabaseManager instance;
    
    private FinanceDatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private UserDao userDao;
    private CategoryDao categoryDao;
    private TransactionDao transactionDao;
    private LoginHistoryDao loginHistoryDao;
    private NotificationDao notificationDao;
    
    /**
     * 获取单例实例
     * @return 数据库管理器实例
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化数据库
     * @param context 上下文
     */
    public void init(Context context) {
        LogUtils.i(TAG, "初始化数据库");
        
        // 加载SQLCipher库
        SQLiteDatabase.loadLibs(context);
        
        // 创建数据库Hook，用于自定义加密参数
        SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
            @Override
            public void preKey(SQLiteDatabase database) {
                // 在密钥应用前执行
            }
            
            @Override
            public void postKey(SQLiteDatabase database) {
                // 在密钥应用后执行
                database.execSQL("PRAGMA cipher_compatibility = 3");
                database.execSQL("PRAGMA kdf_iter = 64000");
                database.execSQL("PRAGMA cipher_page_size = 1024");
            }
        };
        
        // 创建数据库帮助类
        dbHelper = new FinanceDatabaseHelper(context);
        
        // 获取加密密钥
        String encryptionKey = SecurityUtils.getDatabaseKey();
        
        try {
            // 打开或创建加密数据库
            database = dbHelper.getWritableDatabase(encryptionKey.toCharArray());
            LogUtils.i(TAG, "数据库初始化成功");
            
            // 初始化DAO
            userDao = new UserDao(database);
            categoryDao = new CategoryDao(database);
            transactionDao = new TransactionDao(database);
            loginHistoryDao = new LoginHistoryDao(database);
            notificationDao = new NotificationDao(database);
            
            // 创建测试用户
            createTestUsers();
            
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化数据库失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 关闭数据库
     */
    public void close() {
        LogUtils.i(TAG, "关闭数据库");
        if (database != null && database.isOpen()) {
            database.close();
        }
        
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
    }
    
    /**
     * 获取用户DAO
     * @return 用户DAO
     */
    public UserDao getUserDao() {
        return userDao;
    }
    
    /**
     * 获取分类DAO
     * @return 分类DAO
     */
    public CategoryDao getCategoryDao() {
        return categoryDao;
    }
    
    /**
     * 获取交易记录DAO
     * @return 交易记录DAO
     */
    public TransactionDao getTransactionDao() {
        return transactionDao;
    }
    
    /**
     * 获取数据库实例
     * @return 数据库实例
     */
    public SQLiteDatabase getDatabase() {
        return database;
    }
    
    /**
     * 获取登录历史 DAO
     */
    public LoginHistoryDao getLoginHistoryDao() {
        if (loginHistoryDao == null) {
            loginHistoryDao = new LoginHistoryDao(database);
        }
        return loginHistoryDao;
    }
    
    /**
     * 获取通知 DAO
     */
    public NotificationDao getNotificationDao() {
        if (notificationDao == null) {
            notificationDao = new NotificationDao(database);
        }
        return notificationDao;
    }
    
    /**
     * 检查数据库状态
     * @return 数据库状态信息
     */
    public String checkDatabaseStatus() {
        StringBuilder status = new StringBuilder();
        
        try {
            status.append("数据库管理器实例: ").append(instance != null ? "已创建" : "未创建").append("\n");
            status.append("数据库助手: ").append(dbHelper != null ? "已创建" : "未创建").append("\n");
            status.append("数据库连接: ").append(database != null ? "已打开" : "未打开").append("\n");
            
            if (database != null) {
                status.append("数据库是否打开: ").append(database.isOpen() ? "是" : "否").append("\n");
                status.append("数据库是否可写: ").append(database.isReadOnly() ? "否(只读)" : "是").append("\n");
                
                // 检查表是否存在
                Cursor cursor = database.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name='transactions'", 
                        null);
                boolean tableExists = cursor != null && cursor.getCount() > 0;
                if (cursor != null) {
                    cursor.close();
                }
                
                status.append("transactions表是否存在: ").append(tableExists ? "是" : "否").append("\n");
            }
            
            status.append("UserDao: ").append(userDao != null ? "已创建" : "未创建").append("\n");
            status.append("CategoryDao: ").append(categoryDao != null ? "已创建" : "未创建").append("\n");
            status.append("TransactionDao: ").append(transactionDao != null ? "已创建" : "未创建").append("\n");
            status.append("NotificationDao: ").append(notificationDao != null ? "已创建" : "未创建").append("\n");
            
        } catch (Exception e) {
            status.append("检查状态时出错: ").append(e.getMessage());
            LogUtils.e(TAG, "检查数据库状态失败: " + e.getMessage(), e);
        }
        
        return status.toString();
    }
    
    /**
     * 创建测试用户
     * 在首次运行或数据库重置后调用，确保始终有一个可用的管理员账户
     */
    public void createTestUsers() {
        LogUtils.i(TAG, "检查并创建测试用户");
        try {
            if (userDao == null) {
                LogUtils.e(TAG, "UserDao未初始化，无法创建测试用户");
                return;
            }
            
            // 检查是否已存在admin用户
            User adminUser = userDao.queryByUsername("admin");
            if (adminUser == null) {
                LogUtils.i(TAG, "未找到admin用户，创建新管理员账户");
                adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setPassword(SecurityUtils.hashPassword("admin123"));
                adminUser.setName("系统管理员");
                adminUser.setEmail("admin@example.com");
                adminUser.setRole("admin");
                adminUser.setCreatedAt(System.currentTimeMillis());
                adminUser.setUpdatedAt(System.currentTimeMillis());
                
                long userId = userDao.insert(adminUser);
                if (userId > 0) {
                    LogUtils.i(TAG, "成功创建管理员账户，ID: " + userId);
                } else {
                    LogUtils.e(TAG, "创建管理员账户失败");
                }
            } else {
                LogUtils.i(TAG, "已存在admin用户，ID: " + adminUser.getId());
            }
            
            // 检查并创建测试普通用户
            User testUser = userDao.queryByUsername("test");
            if (testUser == null) {
                LogUtils.i(TAG, "未找到test用户，创建新测试账户");
                testUser = new User();
                testUser.setUsername("test");
                testUser.setPassword(SecurityUtils.hashPassword("test123"));
                testUser.setName("测试用户");
                testUser.setEmail("test@example.com");
                testUser.setPhone("13800138000");
                testUser.setRole("user");
                testUser.setCreatedAt(System.currentTimeMillis());
                testUser.setUpdatedAt(System.currentTimeMillis());
                
                long userId = userDao.insert(testUser);
                if (userId > 0) {
                    LogUtils.i(TAG, "成功创建测试用户账户，ID: " + userId);
                } else {
                    LogUtils.e(TAG, "创建测试用户账户失败");
                }
            } else {
                LogUtils.i(TAG, "已存在test用户，ID: " + testUser.getId());
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "创建测试用户时出错: " + e.getMessage(), e);
        }
    }
} 