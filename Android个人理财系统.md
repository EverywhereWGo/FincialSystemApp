设计与实现一款基于Android平台的个人理财系统，目标是为用户提供一个集收入、支出、资产管理、预算规划等功能为一体的智能化理财工具。具体的研究目的如下，开发功能全面的个人理财系统，设计并实现一款多功能、易用且高效的Android端个人理财系统，帮助用户便捷地管理日常财务。并且，会格外注重数据安全。在系统设计中，重点解决用户数据的安全性与隐私保护问题，采用加密算法与身份验证等技术，确保用户的财务数据不被泄露或滥用。

#### **1. 用户认证与管理模块**

- **核心功能：**

  - 手机号/邮箱注册（含短信验证码，使用阿里云SDK）
  - 密码强度校验（正则表达式：至少8位，含大小写+数字）
  - 修改密码与个人信息（需旧密码验证）

- **技术实现：**

  ```java
  // 密码强度校验示例
  public static boolean isPasswordValid(String password) {
      String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
      return password.matches(pattern);
  }
  ```

  - 数据库：SQLite + SQLCipher（加密存储用户表）
  - 敏感操作日志记录（记录密码修改时间/IP）

- ##### **.安全功能**

  - **双因素认证（2FA）**

    - 用户登录时，除密码外需输入短信验证码或邮箱验证码

    - 实现方案：

      ```java
      // 发送短信验证码（阿里云SDK示例）
      public void sendSMSCode(String phone) {
          DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", ACCESS_KEY, SECRET_KEY);
          IAcsClient client = new DefaultAcsClient(profile);
          SendSmsRequest request = new SendSmsRequest();
          request.setPhoneNumbers(phone);
          request.setSignName("FinanceApp");
          request.setTemplateCode("SMS_123456");
          request.setTemplateParam("{\"code\":\"123456\"}");
          SendSmsResponse response = client.getAcsResponse(request);
      }
      ```

  - **登录历史记录**

    - 记录用户登录时间、IP地址、设备信息

    - 数据库设计：

      ```sql
      CREATE TABLE login_history (
          id INTEGER PRIMARY KEY,
          user_id INTEGER,
          login_time DATETIME DEFAULT CURRENT_TIMESTAMP,
          ip_address VARCHAR(15),
          device_model VARCHAR(50),
          FOREIGN KEY(user_id) REFERENCES users(id)
      );
      ```

  ##### **2. 安全增强实现**

  - **会话管理**

    - Token有效期控制（JWT实现）

    ```java
    // 生成JWT Token（JJWT库）
    public String generateToken(String userId) {
        return Jwts.builder()
            .setSubject(userId)
            .setExpiration(new Date(System.currentTimeMillis() + 3600_000)) // 1小时过期
            .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
            .compact();
    }
    ```

  - **密码策略升级**

    - 密码错误锁定机制（5次失败后锁定15分钟）

    ```java
    public void handleLoginAttempt(String username) {
        int attempts = getFailedAttempts(username);
        if (attempts >= 5) {
            lockAccount(username);
            throw new AccountLockedException("账号已锁定，请15分钟后重试");
        }
    }
    ```

  ##### **3. 功能扩展**

  - **用户角色分级**

    - 主账户（完全权限）
    - 子账户（仅查看权限）

    ```java
    // 权限检查示例
    public void checkPermission(User user, String requiredRole) {
        if (!user.getRole().equals(requiredRole)) {
            throw new SecurityException("权限不足");
        }
    }
    ```

#### **2. 仪表板模块**

- **核心功能：**

  - 本月总收入/支出统计卡片
  - 消费趋势折线图（MPAndroidChart实现）
  - 快捷入口：记账/预算/报表

- **技术亮点：**

  ```java
  // 折线图数据绑定示例
  LineDataSet dataSet = new LineDataSet(entries, "月度消费趋势");
  dataSet.setColor(Color.BLUE);
  dataSet.setValueTextSize(10f);
  LineData lineData = new LineData(dataSet);
  lineChart.setData(lineData);
  lineChart.invalidate();
  ```

#### **3. 交易记录模块**

- **核心功能：**

  - 手动记账（金额+分类+备注）
  - 分类管理（预设10个常用分类，支持自定义）
  - 附件存储（本地保存发票缩略图）
  - 基于规则引擎的自动分类
  - 支持多级分类(数据库表结构设计)

- **技术实现：**

  - 数据库表设计：

    ```sql
    CREATE TABLE records (
        id INTEGER PRIMARY KEY,
        amount REAL,
        category VARCHAR(20),
        date DATETIME,
        note TEXT,
        image_path VARCHAR(100)
    );
    ```

  - 图片存储：使用`FileProvider`保存到应用私有目录

#### **4. 预算管理模块**

- **核心功能：**

  - 按月设置分类预算（如餐饮3000元）
  - 进度条显示消费占比（圆形进度条控件）
  - 超支80%时发送Notification提醒

- **关键代码：**

  ```java
  // 预算进度计算
  float progress = (currentSpending / budgetAmount) * 100;
  ProgressBar budgetProgress = findViewById(R.id.progress_bar);
  budgetProgress.setProgress((int) progress);
  ```

#### **5. 报表与统计模块**

- **核心功能：**

  - 月度收支对比柱状图
  - 分类消费占比饼图
  - 导出Excel报表（Apache POI实现）

- **技术实现：**

  ```java
  // 使用Apache POI生成Excel
  HSSFWorkbook workbook = new HSSFWorkbook();
  HSSFSheet sheet = workbook.createSheet("消费记录");
  HSSFRow headerRow = sheet.createRow(0);
  headerRow.createCell(0).setCellValue("日期");
  headerRow.createCell(1).setCellValue("金额");
  ```

#### **6. 通知与提醒模块**

- **核心功能：**

  - 预算超支提醒（Notification）
  - 定时账单提醒（AlarmManager）
  - 通知中心历史记录查看

- **实现方案：**

  ```java
  // 发送Notification示例
  NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_warning)
      .setContentTitle("预算警告")
      .setContentText("餐饮预算已使用80%");
  NotificationManagerCompat.from(this).notify(1, builder.build());
  ```

------

### **二、安全设计实施方案**

#### **1. 数据加密策略**

| 数据类型     | 加密方式              | 实现方案                                        |
| :----------- | :-------------------- | :---------------------------------------------- |
| 用户密码     | SHA-256加盐哈希       | `MessageDigest` + 随机盐值                      |
| 本地数据库   | SQLCipher AES-256加密 | 初始化时设置密码`SQLiteDatabase.openDatabase()` |
| 网络传输数据 | HTTPS + 参数AES加密   | OkHttp拦截器自动处理                            |

#### **2. 关键安全代码示例**

```java
// 数据库加密初始化
SQLiteDatabase.loadLibs(context);
File databaseFile = context.getDatabasePath("finance.db");
SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
    databaseFile, 
    "user123!@#".toCharArray(), 
    null,
    null
);

// 敏感信息脱敏显示
public static String maskCardNumber(String cardNumber) {
    return cardNumber.replaceAll("(?<=\\d{4})\\d(?=\\d{4})", "*");
}
```

------

### **三、答辩重点与亮点设计**

#### **1. 技术难点解析**

- **数据库加密与性能平衡：** 对比加密前后查询性能（演示1000条数据查询耗时）
- **混合图表交互：** MPAndroidChart多图表联动刷新机制

#### **2. 创新点展示**

- **动态预算提醒系统：** 演示设置3000元餐饮预算后，消费达到2400元触发提醒
- **安全设计对比：** 展示加密数据库文件与明文存储的差异（使用SQLiteBrowser工具）

#### **3. 演示流程设计**

1. **用户注册**：展示密码强度校验与短信验证
2. **记录消费**：添加一笔餐饮消费并上传发票截图
3. **预算管理**：设置预算后触发通知提醒
4. **报表导出**：生成Excel文件并通过邮件发送

------

### **四、开发计划与测试方案**

#### **1. 开发阶段（共8周）**

| 阶段    | 内容                  | 产出物                    |
| :------ | :-------------------- | :------------------------ |
| 第1-2周 | 核心框架搭建+用户模块 | 可运行的基础登录/注册界面 |
| 第3-4周 | 交易记录+预算管理实现 | 完成记账与预算设置功能    |
| 第5-6周 | 图表统计+报表导出     | 生成可视化报表            |
| 第7周   | 安全加固+性能优化     | 通过渗透测试报告          |
| 第8周   | 验收测试+答辩准备     | 完整演示视频+答辩PPT      |

#### **2. 测试方案**

- **安全测试：** 使用ADB导出数据库文件验证加密有效性
- **边界测试：** 输入超大金额（如1亿元）测试系统容错
- **兼容性测试：** 在Android 8.0-13设备上运行验证