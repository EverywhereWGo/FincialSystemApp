package com.zjf.fincialsystem.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import android.widget.Button;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.FragmentProfileBinding;
import com.zjf.fincialsystem.model.User;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.repository.UserRepository;
import com.zjf.fincialsystem.ui.activity.ImageViewActivity;
import com.zjf.fincialsystem.ui.activity.LoginActivity;
import com.zjf.fincialsystem.ui.activity.EditProfileActivity;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.TokenManager;
import com.zjf.fincialsystem.utils.SecurityUtils;

/**
 * 个人资料Fragment
 */
public class ProfileFragment extends Fragment {
    
    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private UserRepository userRepository;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化仓库
        userRepository = new UserRepository(requireContext());
        
        // 初始化视图
        initViews();
        
        // 加载数据
        loadData();
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        try {
            // 设置标题
            binding.tvTitle.setText(R.string.profile);
            
            // 设置点击事件
            setupClickListeners();
            
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化视图失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 设置各种点击事件
     */
    private void setupClickListeners() {
        // 头像点击事件
        binding.ivAvatar.setOnClickListener(v -> {
            try {
                // 跳转到图片查看页面
                Intent intent = new Intent(requireContext(), ImageViewActivity.class);
                // 传递图像资源ID
                intent.putExtra(ImageViewActivity.EXTRA_IMAGE_RES_ID, R.drawable.ic_person);
                startActivity(intent);
            } catch (Exception e) {
                LogUtils.e(TAG, "打开头像查看失败：" + e.getMessage(), e);
                Toast.makeText(requireContext(), "打开头像查看失败", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 编辑资料
        binding.cardEditProfile.setOnClickListener(v -> {
            try {
                // 跳转到编辑个人资料页面
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                // 使用startActivityForResult启动编辑页面
                startActivityForResult(intent, com.zjf.fincialsystem.ui.activity.MainActivity.REQUEST_EDIT_PROFILE);
            } catch (Exception e) {
                LogUtils.e(TAG, "跳转到编辑个人资料页面失败：" + e.getMessage(), e);
                Toast.makeText(requireContext(), "跳转失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // 修改密码
        binding.cardChangePassword.setOnClickListener(v -> {
            // 显示修改密码对话框
            showChangePasswordDialog();
        });
        
        // 注销账户
        binding.cardLoginHistory.setOnClickListener(v -> {
            // 显示注销账户确认对话框
            showDeleteAccountDialog();
        });
        
        // 关于
        binding.cardAbout.setOnClickListener(v -> {
            // TODO: 跳转到关于页面
            Toast.makeText(requireContext(), "关于页面待实现", Toast.LENGTH_SHORT).show();
        });
        
        // 退出登录
        binding.btnLogout.setOnClickListener(v -> logout());
    }
    
    /**
     * 加载数据
     */
    public void loadData() {
        try {
            // 显示加载中
            showLoading(true);
            
            // 获取用户ID
            long userId = TokenManager.getInstance().getUserId();
            
            // 如果用户ID为-1，表示未获取到ID，但不一定是未登录状态
            if (userId == -1) {
                LogUtils.e(TAG, "未获取到用户ID，使用默认值");
                userId = 1; // 使用默认ID而不是直接退出登录
                
                // 检查是否真的未登录
                if (!TokenManager.getInstance().isLoggedIn()) {
                    LogUtils.e(TAG, "用户确实未登录");
                    // 显示提示而不是立即退出
                    Toast.makeText(requireContext(), "用户未登录，请重新登录", Toast.LENGTH_SHORT).show();
                    // 延迟执行退出操作，给用户一定反应时间
                    binding.getRoot().postDelayed(this::logout, 1500);
                    return;
                }
            }
            
            // 从服务器获取用户信息（模拟）
            fetchUserData(userId);
            
        } catch (Exception e) {
            LogUtils.e(TAG, "加载数据失败：" + e.getMessage(), e);
            showLoading(false);
        }
    }
    
    /**
     * 获取用户数据（模拟）
     */
    private void fetchUserData(long userId) {
        // 显示加载中
        showLoading(true);
        
        // 从服务器获取用户数据
        userRepository.getUserInfo(userId, new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User user) {
                // 更新UI
                updateUI(user);
                
                // 隐藏加载中
                showLoading(false);
            }
            
            @Override
            public void onError(String errorMsg) {
                LogUtils.e(TAG, "获取用户信息失败：" + errorMsg);
                Toast.makeText(requireContext(), "获取用户信息失败：" + errorMsg, Toast.LENGTH_SHORT).show();
                
                // 使用本地模拟数据作为后备
                User mockUser = createMockUser(userId);
                updateUI(mockUser);
                
                // 隐藏加载中
                showLoading(false);
            }
            
            @Override
            public void isCacheData(boolean isCache) {
                if (isCache) {
                    LogUtils.d(TAG, "显示的是缓存用户数据");
                    // 可以在UI上显示缓存标记
                }
            }
        });
    }
    
    /**
     * 创建模拟用户数据
     */
    private User createMockUser(long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("user_" + userId);
        user.setNickname("测试用户");
        user.setEmail("user" + userId + "@example.com");
        // 个人资料页面显示掩码格式的手机号，但确保格式与完整手机号一致
        user.setPhone("1380013****" + String.format("%04d", userId).substring(2));
        return user;
    }
    
    /**
     * 更新UI
     */
    private void updateUI(User user) {
        if (binding == null) return;
        
        // 设置用户名
        binding.tvUsername.setText(user.getNickname());
        
        // 设置邮箱
        binding.tvEmail.setText(user.getEmail());
        
        // 设置手机号
        binding.tvPhone.setText(user.getPhone());
    }
    
    /**
     * 退出登录
     */
    private void logout() {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                // 执行退出登录操作
                performLogout();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * 执行退出登录操作
     */
    private void performLogout() {
        try {
            // A/新的实现：使用真实的API接口
            // 显示加载中
            showLoading(true);
            
            // 获取用户ID和Token
            long userId = TokenManager.getInstance().getUserId();
            String token = TokenManager.getInstance().getToken();
            
            // 如果用户ID为-1（无效ID），设置为临时ID用于模拟
            if (userId == -1) {
                userId = 1; // 使用默认ID进行模拟
                LogUtils.w(TAG, "未获取到有效的用户ID，使用默认ID: " + userId);
            }
            
            // 调用真实的退出登录API
            userRepository.logout(userId, token, new RepositoryCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    // 在主线程中处理成功响应
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        // 显示成功消息
                        Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show();
                        
                        // 延迟一下，显示退出登录的效果
                        binding.getRoot().postDelayed(ProfileFragment.this::handleLogout, 800);
                    });
                }
                
                @Override
                public void onError(String error) {
                    // 在主线程中处理错误响应
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        LogUtils.e(TAG, "退出登录失败：" + error);
                        showLoading(false);
                        
                        // 即使API调用失败，也应该清除本地状态并退出
                        Toast.makeText(requireContext(), "退出登录：" + error, Toast.LENGTH_SHORT).show();
                        
                        // 延迟一下，显示退出登录的效果
                        binding.getRoot().postDelayed(ProfileFragment.this::handleLogout, 800);
                    });
                }
            });
            
        } catch (Exception e) {
            LogUtils.e(TAG, "退出登录失败：" + e.getMessage(), e);
            showLoading(false);
            Toast.makeText(requireContext(), "退出登录失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // 即使出现异常，也尝试清除本地状态并退出
            userRepository.logout();
            binding.getRoot().postDelayed(this::handleLogout, 800);
        }
    }
    
    /**
     * 处理退出登录后的操作
     */
    private void handleLogout() {
        try {
            if (getActivity() != null) {
                // 跳转到登录页面
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                // 清除堆栈中的其他活动
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "处理退出登录后的操作失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 显示/隐藏加载中
     */
    private void showLoading(boolean show) {
        if (binding != null) {
            binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
    
    /**
     * 显示注销账户确认对话框
     */
    private void showDeleteAccountDialog() {
        // 加载对话框布局
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete_account, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // 设置透明背景以使圆角可见
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.show();
        
        // 设置按钮点击事件
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            // 执行注销账户操作
            performDeleteAccount();
        });
    }
    
    /**
     * 执行注销账户操作
     */
    private void performDeleteAccount() {
        try {
            // 显示加载中
            showLoading(true);
            
            // 获取用户ID
            long userId = TokenManager.getInstance().getUserId();
            
            // 如果用户ID为-1（无效ID），设置为临时ID用于模拟
            if (userId == -1) {
                userId = 1; // 使用默认ID进行模拟
                LogUtils.w(TAG, "未获取到有效的用户ID，使用默认ID: " + userId);
            }
            
            // 调用仓库方法注销账户
            userRepository.deleteAccount(userId, new RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    // 在主线程中更新UI
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        // 显示成功提示
                        Toast.makeText(requireContext(), R.string.account_deleted, Toast.LENGTH_SHORT).show();
                        
                        // 跳转到登录页面
                        handleLogout();
                    });
                }
                
                @Override
                public void onError(String error) {
                    // 在主线程中更新UI
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
            
        } catch (Exception e) {
            LogUtils.e(TAG, "注销账户操作失败：" + e.getMessage(), e);
            showLoading(false);
            Toast.makeText(requireContext(), R.string.account_delete_failed + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示修改密码对话框
     */
    private void showChangePasswordDialog() {
        // 加载对话框布局
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // 设置透明背景以使圆角可见
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.show();
        
        // 获取对话框控件
        TextInputLayout tilOldPassword = dialogView.findViewById(R.id.tilOldPassword);
        TextInputEditText etOldPassword = dialogView.findViewById(R.id.etOldPassword);
        TextInputLayout tilNewPassword = dialogView.findViewById(R.id.tilNewPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextInputLayout tilConfirmNewPassword = dialogView.findViewById(R.id.tilConfirmNewPassword);
        TextInputEditText etConfirmNewPassword = dialogView.findViewById(R.id.etConfirmNewPassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        
        // 取消按钮点击事件
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // 确认按钮点击事件
        btnConfirm.setOnClickListener(v -> {
            // 获取输入的密码
            String oldPassword = etOldPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmNewPassword = etConfirmNewPassword.getText().toString().trim();
            
            // 验证输入
            if (oldPassword.isEmpty()) {
                tilOldPassword.setError(getString(R.string.field_required));
                return;
            } else {
                tilOldPassword.setError(null);
            }
            
            if (newPassword.isEmpty()) {
                tilNewPassword.setError(getString(R.string.field_required));
                return;
            } else {
                tilNewPassword.setError(null);
            }
            
            if (confirmNewPassword.isEmpty()) {
                tilConfirmNewPassword.setError(getString(R.string.field_required));
                return;
            } else {
                tilConfirmNewPassword.setError(null);
            }
            
            // 检查两次密码是否一致
            if (!newPassword.equals(confirmNewPassword)) {
                tilConfirmNewPassword.setError(getString(R.string.password_not_match));
                return;
            } else {
                tilConfirmNewPassword.setError(null);
            }
            
            // 检查新密码是否符合强度要求
            if (!SecurityUtils.isPasswordStrong(newPassword)) {
                tilNewPassword.setError(getString(R.string.password_too_simple));
                return;
            } else {
                tilNewPassword.setError(null);
            }
            
            // 显示加载中
            dialog.dismiss();
            showLoading(true);
            
            // 获取用户ID
            long userId = TokenManager.getInstance().getUserId();
            if (userId == -1) {
                userId = 1; // 使用默认ID进行模拟
                LogUtils.w(TAG, "未获取到有效的用户ID，使用默认ID: " + userId);
            }
            
            // 调用修改密码的接口
            userRepository.changePassword(userId, oldPassword, newPassword, new RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(requireContext(), R.string.password_changed, Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onError(String error) {
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.d(TAG, "ProfileFragment重新可见，刷新用户数据");
        // 如果界面可见，重新加载数据以确保显示最新的用户信息
        if (isAdded() && !isDetached()) {
            loadData();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtils.d(TAG, "收到活动结果 - requestCode: " + requestCode + ", resultCode: " + resultCode);
        
        if (requestCode == com.zjf.fincialsystem.ui.activity.MainActivity.REQUEST_EDIT_PROFILE && 
            resultCode == requireActivity().RESULT_OK) {
            LogUtils.d(TAG, "编辑个人资料成功，刷新数据");
            loadData();
        }
    }
} 