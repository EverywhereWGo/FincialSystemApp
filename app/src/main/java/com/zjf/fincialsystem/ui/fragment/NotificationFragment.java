package com.zjf.fincialsystem.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private NotificationDao notificationDao;
    private NotificationRepository notificationRepository;

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
            
            // 查询是否已有通知数据
            List<Notification> existingNotifications = notificationDao.queryByUserId(userId);
            if (existingNotifications != null && !existingNotifications.isEmpty()) {
                LogUtils.d(TAG, "已有" + existingNotifications.size() + "条通知数据，不再创建测试数据");
                return;
            }
            
            LogUtils.d(TAG, "开始创建测试通知数据");
            
            // 预算警告通知
            Notification budgetWarning = new Notification(
                    userId,
                    "预算警告提醒",
                    "您的餐饮预算已使用80%，请注意合理消费。",
                    Notification.TYPE_BUDGET_WARNING
            );
            budgetWarning.setCreatedAt(new Date(System.currentTimeMillis() - 3600000)); // 1小时前
            notificationDao.insert(budgetWarning);
            
            // 预算超支通知
            Notification budgetExceed = new Notification(
                    userId,
                    "预算超支提醒",
                    "您的购物预算已超支15%，建议控制支出。",
                    Notification.TYPE_BUDGET_EXCEED
            );
            budgetExceed.setCreatedAt(new Date(System.currentTimeMillis() - 7200000)); // 2小时前
            notificationDao.insert(budgetExceed);
            
            // 账单提醒通知
            Notification billReminder = new Notification(
                    userId,
                    "账单到期提醒",
                    "您的水电费将于3天后到期，请及时缴纳。",
                    Notification.TYPE_BILL_REMINDER
            );
            billReminder.setCreatedAt(new Date(System.currentTimeMillis() - 86400000)); // 1天前
            notificationDao.insert(billReminder);
            
            // 大额支出通知
            Notification largeExpense = new Notification(
                    userId,
                    "大额支出提醒",
                    "检测到一笔¥1,999的大额支出，请确认是否为您本人操作。",
                    Notification.TYPE_LARGE_EXPENSE
            );
            largeExpense.setCreatedAt(new Date(System.currentTimeMillis() - 172800000)); // 2天前
            largeExpense.setRead(true); // 已读
            notificationDao.insert(largeExpense);
            
            // 收入到账通知
            Notification incomeReceived = new Notification(
                    userId,
                    "收入到账提醒",
                    "您有一笔¥5,000的收入已到账，来源：工资。",
                    Notification.TYPE_INCOME_RECEIVED
            );
            incomeReceived.setCreatedAt(new Date(System.currentTimeMillis() - 259200000)); // 3天前
            incomeReceived.setRead(true); // 已读
            notificationDao.insert(incomeReceived);
            
            LogUtils.d(TAG, "已创建5条测试通知数据");
            
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
            
            // 从API获取通知列表
            notificationRepository.getNotifications(new RepositoryCallback<List<Notification>>() {
                @Override
                public void onSuccess(List<Notification> result) {
                    // 更新列表数据
                    notifications.clear();
                    notifications.addAll(result);
                    adapter.notifyDataSetChanged();
                    
                    // 更新空列表状态
                    updateEmptyState();
                    
                    // 完成刷新
                    binding.swipeRefreshLayout.setRefreshing(false);
                    
                    LogUtils.d(TAG, "成功加载 " + result.size() + " 条通知");
                }
                
                @Override
                public void onError(String errorMsg) {
                    LogUtils.e(TAG, "加载通知失败: " + errorMsg);
                    Toast.makeText(requireContext(), "加载通知失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                    
                    // 尝试从本地数据库加载
                    loadNotificationsFromDatabase(userId);
                    
                    // 完成刷新
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
                
                @Override
                public void isCacheData(boolean isCache) {
                    if (isCache) {
                        LogUtils.d(TAG, "显示的是缓存通知数据");
                        // 可以在UI上显示缓存标记
                    }
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "加载通知数据失败: " + e.getMessage(), e);
            binding.swipeRefreshLayout.setRefreshing(false);
            
            // 发生异常，尝试从本地数据库加载
            long userId = TokenManager.getInstance().getUserId();
            if (userId > 0) {
                loadNotificationsFromDatabase(userId);
            }
        }
    }
    
    /**
     * 从本地数据库加载通知数据
     */
    private void loadNotificationsFromDatabase(long userId) {
        try {
            // 从本地数据库加载通知数据
            List<Notification> dbNotifications = notificationDao.queryByUserId(userId);
            
            if (dbNotifications != null && !dbNotifications.isEmpty()) {
                LogUtils.d(TAG, "从本地数据库加载 " + dbNotifications.size() + " 条通知");
                notifications.clear();
                notifications.addAll(dbNotifications);
                adapter.notifyDataSetChanged();
            } else {
                LogUtils.w(TAG, "本地数据库中没有通知数据");
                
                // 如果本地数据库也没有数据，则创建并使用测试数据
                createTestNotifications();
                dbNotifications = notificationDao.queryByUserId(userId);
                if (dbNotifications != null && !dbNotifications.isEmpty()) {
                    notifications.clear();
                    notifications.addAll(dbNotifications);
                    adapter.notifyDataSetChanged();
                }
            }
            
            // 更新空列表状态
            updateEmptyState();
        } catch (Exception e) {
            LogUtils.e(TAG, "从本地数据库加载通知失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 标记通知为已读
     */
    private void markAsRead(Notification notification) {
        if (notification == null) {
            return;
        }
        
        // 如果已经是已读状态，不需要操作
        if (notification.isRead()) {
            return;
        }
        
        // 先在UI上标记为已读
        notification.setRead(true);
        adapter.notifyDataSetChanged();
        
        // 调用API标记为已读
        notificationRepository.markAsRead(notification.getId(), new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                LogUtils.d(TAG, "成功标记通知为已读: " + notification.getId());
                
                // 更新本地数据库
                notificationDao.markAsRead(notification.getId());
            }
            
            @Override
            public void onError(String errorMsg) {
                LogUtils.e(TAG, "标记通知为已读失败: " + errorMsg);
                
                // 即使API调用失败，也更新本地数据库
                notificationDao.markAsRead(notification.getId());
            }
        });
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
            Toast.makeText(requireContext(), "没有通知可清除", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(requireContext())
            .setTitle("清除所有通知")
            .setMessage("确定要清除所有通知吗？此操作不可撤销。")
            .setPositiveButton("确定", (dialog, which) -> {
                // 获取当前用户ID
                long userId = TokenManager.getInstance().getUserId();
                if (userId <= 0) {
                    Toast.makeText(requireContext(), "用户未登录，无法清除通知", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 显示加载中
                binding.swipeRefreshLayout.setRefreshing(true);
                
                // 调用API批量标记为已读
                notificationRepository.markAllAsRead(userId, new RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        LogUtils.d(TAG, "成功标记所有通知为已读");
                        
                        // 在本地数据库中标记所有通知为已读
                        notificationDao.markAllAsRead(userId);
                        
                        // 清空当前通知列表
                        notifications.clear();
                        adapter.notifyDataSetChanged();
                        
                        // 更新空列表状态
                        updateEmptyState();
                        
                        // 完成刷新
                        binding.swipeRefreshLayout.setRefreshing(false);
                        
                        Toast.makeText(requireContext(), "已清除所有通知", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onError(String errorMsg) {
                        LogUtils.e(TAG, "标记所有通知为已读失败: " + errorMsg);
                        Toast.makeText(requireContext(), "清除通知失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                        
                        // 即使API调用失败，也在本地清除
                        notificationDao.markAllAsRead(userId);
                        
                        // 清空当前通知列表
                        notifications.clear();
                        adapter.notifyDataSetChanged();
                        
                        // 更新空列表状态
                        updateEmptyState();
                        
                        // 完成刷新
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                });
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 更新空状态显示
     */
    private void updateEmptyState() {
        if (binding != null) {
            if (notifications == null || notifications.isEmpty()) {
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.recyclerView.setVisibility(View.GONE);
                binding.btnClearAll.setVisibility(View.GONE);
            } else {
                binding.emptyView.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.btnClearAll.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 