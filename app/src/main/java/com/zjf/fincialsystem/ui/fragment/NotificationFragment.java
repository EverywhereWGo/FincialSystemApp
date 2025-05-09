package com.zjf.fincialsystem.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.FragmentNotificationBinding;
import com.zjf.fincialsystem.model.Notification;
import com.zjf.fincialsystem.db.dao.NotificationDao;
import com.zjf.fincialsystem.db.DatabaseManager;
import com.zjf.fincialsystem.ui.adapter.NotificationAdapter;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.StatusBarUtils;
import com.zjf.fincialsystem.utils.TokenManager;
import com.zjf.fincialsystem.repository.NotificationRepository;
import com.zjf.fincialsystem.repository.RepositoryCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 通知页面
 */
public class NotificationFragment extends Fragment {

    private static final String TAG = "NotificationFragment";
    private FragmentNotificationBinding binding;
    private NotificationAdapter adapter;
    private List<Notification> notifications = new ArrayList<>();
    private List<Notification> allNotifications = new ArrayList<>(); // 存储所有通知
    private NotificationDao notificationDao;
    private NotificationRepository notificationRepository;
    private boolean showAllNotifications = false; // 是否显示所有通知（包括已读）

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 使用ViewBinding
        binding = FragmentNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化DAO
        notificationDao = DatabaseManager.getInstance().getNotificationDao();
        
        // 初始化Repository
        notificationRepository = new NotificationRepository(requireContext());
        
        // 设置沉浸式状态栏
        setupStatusBar();
        
        // 初始化视图
        initViews();
        
        // 创建测试通知数据
        createTestNotifications();
        
        // 加载通知数据
        loadNotifications();
    }
    
    /**
     * 设置沉浸式状态栏
     */
    private void setupStatusBar() {
        if (getActivity() != null) {
            StatusBarUtils.setImmersiveStatusBar(getActivity(), true);
        }
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        try {
            // 设置RecyclerView
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new NotificationAdapter(notifications);
            binding.recyclerView.setAdapter(adapter);
            
            // 设置下拉刷新
            binding.swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);
            binding.swipeRefreshLayout.setColorSchemeResources(
                    R.color.colorPrimary,
                    R.color.colorAccent,
                    R.color.colorPrimaryDark
            );
            
            // 设置历史通知开关
            binding.switchShowHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
                showAllNotifications = isChecked;
                updateNotificationsList();
                LogUtils.d(TAG, "显示历史通知开关状态: " + (isChecked ? "开启" : "关闭"));
            });
            
            // 设置清除所有通知按钮
            binding.btnClearAll.setOnClickListener(v -> clearAllNotifications());
            
            // 设置空通知提示
            updateEmptyState();
            
            // 设置通知点击回调
            adapter.setOnNotificationClickListener(notification -> {
                // 标记为已读
                markAsRead(notification);
                // 处理通知点击事件，如根据通知类型跳转到不同页面
                handleNotificationClick(notification);
            });
            
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化视图失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建测试通知
     */
    private void createTestNotifications() {
        try {
            // 获取当前用户ID
            long userId = TokenManager.getInstance().getUserId();
            if (userId <= 0) {
                LogUtils.e(TAG, "用户ID无效: " + userId);
                return;
            }
            
            // 确保测试通知数据存在
            List<Notification> existingNotifications = notificationDao.queryByUserId(userId);
            
            // 强制创建测试数据用于演示，无论是否已存在
            LogUtils.d(TAG, "开始创建测试通知数据");
            
            // 清除旧的测试数据
            if (existingNotifications != null && !existingNotifications.isEmpty()) {
                for (Notification notification : existingNotifications) {
                    notificationDao.delete(notification.getId());
                }
                LogUtils.d(TAG, "已清除 " + existingNotifications.size() + " 条旧通知数据");
            }
            
            // 预算警告通知 - 未读
            Notification budgetWarning = new Notification(
                    userId,
                    "预算警告提醒",
                    "您的餐饮预算已使用80%，请注意合理消费。",
                    Notification.TYPE_BUDGET_WARNING
            );
            budgetWarning.setCreateTime(new Date(System.currentTimeMillis() - 3600000)); // 1小时前
            budgetWarning.setRead(0); // 未读
            notificationDao.insert(budgetWarning);
            
            // 预算超支通知 - 未读
            Notification budgetExceed = new Notification(
                    userId,
                    "预算超支提醒",
                    "您的购物预算已超支15%，建议控制支出。",
                    Notification.TYPE_BUDGET_EXCEED
            );
            budgetExceed.setCreateTime(new Date(System.currentTimeMillis() - 7200000)); // 2小时前
            budgetExceed.setRead(0); // 未读
            notificationDao.insert(budgetExceed);
            
            // 账单提醒通知 - 未读
            Notification billReminder = new Notification(
                    userId,
                    "账单到期提醒",
                    "您的水电费将于3天后到期，请及时缴纳。",
                    Notification.TYPE_BILL_REMINDER
            );
            billReminder.setCreateTime(new Date(System.currentTimeMillis() - 86400000)); // 1天前
            billReminder.setRead(0); // 未读
            notificationDao.insert(billReminder);
            
            // 大额支出通知 - 已读
            Notification largeExpense = new Notification(
                    userId,
                    "大额支出提醒",
                    "检测到一笔¥1,999的大额支出，请确认是否为您本人操作。",
                    Notification.TYPE_LARGE_EXPENSE
            );
            largeExpense.setCreateTime(new Date(System.currentTimeMillis() - 172800000)); // 2天前
            largeExpense.setRead(1); // 已读
            notificationDao.insert(largeExpense);
            
            // 收入到账通知 - 已读
            Notification incomeReceived = new Notification(
                    userId,
                    "收入到账提醒",
                    "您有一笔¥5,000的收入已到账，来源：工资。",
                    Notification.TYPE_INCOME_RECEIVED
            );
            incomeReceived.setCreateTime(new Date(System.currentTimeMillis() - 259200000)); // 3天前
            incomeReceived.setRead(1); // 已读
            notificationDao.insert(incomeReceived);
            
            // 另一条已读通知
            Notification otherNotification = new Notification(
                    userId,
                    "新功能上线通知",
                    "我们的APP已更新，新增了预算管理功能，快来体验吧！",
                    Notification.TYPE_BUDGET_WARNING
            );
            otherNotification.setCreateTime(new Date(System.currentTimeMillis() - 432000000)); // 5天前
            otherNotification.setRead(1); // 已读
            notificationDao.insert(otherNotification);
            
            LogUtils.d(TAG, "已创建6条测试通知数据，其中3条未读，3条已读");
            
        } catch (Exception e) {
            LogUtils.e(TAG, "创建测试通知失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 加载通知数据
     */
    private void loadNotifications() {
        try {
            // 显示加载中
            binding.swipeRefreshLayout.setRefreshing(true);
            
            // 获取当前用户ID
            long userId = TokenManager.getInstance().getUserId();
            if (userId <= 0) {
                LogUtils.e(TAG, "用户ID无效: " + userId);
                binding.swipeRefreshLayout.setRefreshing(false);
                return;
            }
            
            LogUtils.d(TAG, "开始从网络加载通知数据，用户ID: " + userId);
            
            // 从API获取通知列表
            notificationRepository.getNotifications(new RepositoryCallback<List<Notification>>() {
                @Override
                public void onSuccess(List<Notification> result) {
                    // 存储所有通知
                    allNotifications.clear();
                    allNotifications.addAll(result);
                    
                    LogUtils.d(TAG, "网络请求成功，获取到 " + result.size() + " 条通知");
                    
                    // 记录已读和未读数量（用于调试）
                    int readCount = 0;
                    int unreadCount = 0;
                    for (Notification notification : result) {
                        if (notification.isRead() != null && notification.isRead() == 1) {
                            readCount++;
                        } else {
                            unreadCount++;
                        }
                    }
                    LogUtils.d(TAG, "其中已读通知: " + readCount + " 条，未读通知: " + unreadCount + " 条");
                    
                    // 更新列表显示
                    updateNotificationsList();
                    
                    // 完成刷新
                    binding.swipeRefreshLayout.setRefreshing(false);
                    
                    LogUtils.d(TAG, "成功加载 " + result.size() + " 条通知，其中未读 " 
                            + countUnreadNotifications() + " 条");
                }

                @Override
                public void onError(String errorMsg) {
                    LogUtils.e(TAG, "网络加载通知失败: " + errorMsg);
                    Toast.makeText(getContext(), "加载通知失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                    
                    // 网络加载失败，尝试从本地数据库加载
                    LogUtils.d(TAG, "尝试从本地数据库加载通知数据");
                    loadNotificationsFromDatabase(userId);
                    
                    binding.swipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void isCacheData(boolean isCache) {
                    if (isCache) {
                        LogUtils.d(TAG, "使用缓存数据");
                    }
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "加载通知时出错: " + e.getMessage(), e);
            
            // 异常情况下尝试从本地数据库加载
            long userId = TokenManager.getInstance().getUserId();
            if (userId > 0) {
                loadNotificationsFromDatabase(userId);
            }
            
            binding.swipeRefreshLayout.setRefreshing(false);
        }
    }
    
    /**
     * 从本地数据库加载通知
     */
    private void loadNotificationsFromDatabase(long userId) {
        try {
            List<Notification> dbNotifications = notificationDao.queryByUserId(userId);
            if (dbNotifications != null && !dbNotifications.isEmpty()) {
                LogUtils.d(TAG, "从本地数据库加载到 " + dbNotifications.size() + " 条通知");
                
                // 记录已读和未读数量（用于调试）
                int readCount = 0;
                int unreadCount = 0;
                for (Notification notification : dbNotifications) {
                    if (notification.isRead() != null && notification.isRead() == 1) {
                        readCount++;
                    } else {
                        unreadCount++;
                    }
                }
                LogUtils.d(TAG, "其中已读通知: " + readCount + " 条，未读通知: " + unreadCount + " 条");
                
                // 存储所有通知
                allNotifications.clear();
                allNotifications.addAll(dbNotifications);
                
                // 更新列表显示
                updateNotificationsList();
            } else {
                LogUtils.d(TAG, "本地数据库中没有通知数据");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "从本地数据库加载通知失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新通知列表显示
     * 根据开关状态过滤通知
     */
    private void updateNotificationsList() {
        notifications.clear();
        
        if (showAllNotifications) {
            // 显示所有通知
            notifications.addAll(allNotifications);
            LogUtils.d(TAG, "显示所有通知: " + notifications.size() + " 条");
        } else {
            // 只显示未读通知
            for (Notification notification : allNotifications) {
                if (notification.isRead() == null || notification.isRead() == 0) {
                    notifications.add(notification);
                }
            }
            LogUtils.d(TAG, "只显示未读通知: " + notifications.size() + " 条");
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }
    
    /**
     * 统计未读通知数量
     */
    private int countUnreadNotifications() {
        int count = 0;
        for (Notification notification : allNotifications) {
            if (notification.isRead() == null || notification.isRead() == 0) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 标记通知为已读
     */
    private void markAsRead(Notification notification) {
        if (notification == null || notification.isRead() != null && notification.isRead() == 1) {
            return;
        }
        
        try {
            notificationRepository.markAsRead(notification.getId(), new RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    LogUtils.d(TAG, "成功标记通知为已读: ID = " + notification.getId());
                    
                    // 更新本地通知状态
                    notification.setRead(1);
                    
                    // 如果当前是只显示未读，需要更新界面
                    if (!showAllNotifications) {
                        updateNotificationsList();
                    } else {
                        // 更新已读状态的UI显示
                        int position = notifications.indexOf(notification);
                        if (position >= 0) {
                            adapter.notifyItemChanged(position);
                        }
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    LogUtils.e(TAG, "标记通知为已读失败: " + errorMsg);
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "标记通知为已读时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理通知点击事件
     */
    private void handleNotificationClick(Notification notification) {
        if (notification == null || getActivity() == null) {
            return;
        }
        
        try {
            // 根据通知类型处理
            if (notification.isBudgetWarning() || notification.isBudgetExceed()) {
                // 跳转到预算页面
                // 切换到预算Fragment
                if (getActivity() != null) {
                    getActivity().findViewById(R.id.nav_budget).performClick();
                }
            } else if (notification.isBillReminder()) {
                // 跳转到账单页面
                // 暂未实现
                Toast.makeText(getContext(), "账单提醒功能正在开发中", Toast.LENGTH_SHORT).show();
            } else if (notification.isLargeExpense()) {
                // 跳转到交易详情页面
                // 暂未实现
                Toast.makeText(getContext(), "大额支出通知功能正在开发中", Toast.LENGTH_SHORT).show();
            } else if (notification.isIncomeReceived()) {
                // 跳转到收入详情页面
                // 暂未实现
                Toast.makeText(getContext(), "收入通知功能正在开发中", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            LogUtils.e(TAG, "处理通知点击事件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清除所有通知
     */
    private void clearAllNotifications() {
        if (notifications.isEmpty()) {
            return;
        }
        
        try {
            // 根据当前是否显示历史通知设置不同的对话框标题和内容
            String title = showAllNotifications ? 
                    getString(R.string.confirm_delete) : 
                    "确认操作";
                    
            String message = showAllNotifications ? 
                    "是否将所有通知标记为已读？此操作将影响所有通知状态。" : 
                    "是否将所有未读通知标记为已读？";
                    
            String positiveButton = getString(R.string.confirm);
            
            new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> {
                    // 获取要清除的通知ID列表
                    ArrayList<Long> notificationIds = new ArrayList<>();
                    for (Notification notification : notifications) {
                        notificationIds.add(notification.getId());
                    }
                    
                    // 获取当前用户ID
                    long userId = TokenManager.getInstance().getUserId();
                    
                    // 调用API批量删除通知
                    notificationRepository.markAllAsRead(userId, notificationIds, new RepositoryCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            LogUtils.d(TAG, "成功标记所有通知为已读");
                            
                            // 更新本地数据
                            for (Notification notification : allNotifications) {
                                if (notificationIds.contains(notification.getId())) {
                                    notification.setRead(1);
                                }
                            }
                            
                            // 更新界面
                            updateNotificationsList();
                            
                            Toast.makeText(getContext(), "已将所有通知标记为已读", Toast.LENGTH_SHORT).show();
                        }
                        
                        @Override
                        public void onError(String errorMsg) {
                            LogUtils.e(TAG, "批量标记通知为已读失败: " + errorMsg);
                            Toast.makeText(getContext(), "操作失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        } catch (Exception e) {
            LogUtils.e(TAG, "处理清除所有通知操作时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新空状态显示
     */
    private void updateEmptyState() {
        if (binding != null) {
            boolean isEmpty = notifications == null || notifications.isEmpty();
            
            // 更新视图可见性
            binding.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            
            // 只有当列表有内容且当前显示的是当前模式下的所有通知时才显示清除按钮
            boolean showClearButton = !isEmpty && 
                    (showAllNotifications || countUnreadNotifications() > 0);
            
            binding.btnClearAll.setVisibility(showClearButton ? View.VISIBLE : View.GONE);
            
            // 更新清除按钮文字
            if (showClearButton) {
                String buttonText = showAllNotifications ? 
                        getString(R.string.clear_all_notifications) : 
                        getString(R.string.mark_all_as_read);
                binding.btnClearAll.setText(buttonText);
            }
            
            if (isEmpty) {
                String message = showAllNotifications ? 
                        getString(R.string.no_all_notifications) : 
                        getString(R.string.no_unread_notifications);
                
                // 更新空状态文本
                TextView emptyText = binding.emptyView.findViewById(R.id.tv_empty_message);
                if (emptyText != null) {
                    emptyText.setText(message);
                }
                
                LogUtils.d(TAG, "显示空状态视图 - " + message);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 