package com.zjf.fincialsystem.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityEditProfileBinding;
import com.zjf.fincialsystem.model.User;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.repository.UserRepository;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.StatusBarUtils;
import com.zjf.fincialsystem.utils.TokenManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 编辑个人资料页面
 */
public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private ActivityEditProfileBinding binding;
    private UserRepository userRepository;
    private User currentUser;
    private Uri currentPhotoUri; // 当前拍照的图片URI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置沉浸式状态栏
        StatusBarUtils.setTransparentStatusBar(this);

        // 初始化工具栏
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // 初始化仓库
        userRepository = new UserRepository(this);

        // 设置点击事件
        setupClickListeners();

        // 加载用户数据
        loadUserData();
    }

    /**
     * 设置点击事件
     */
    private void setupClickListeners() {
        // 头像点击事件
        View.OnClickListener avatarClickListener = v -> showImageSelectionDialog();
        binding.ivAvatar.setOnClickListener(avatarClickListener);
        binding.ivEditAvatar.setOnClickListener(avatarClickListener);

        // 保存按钮点击事件
        binding.btnSave.setOnClickListener(v -> saveUserProfile());
    }

    /**
     * 显示选择图片来源的对话框
     */
    private void showImageSelectionDialog() {
        // 加载对话框布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_image_source, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // 设置透明背景以使圆角可见
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.show();
        
        // 设置按钮点击事件
        LinearLayout cameraLayout = dialogView.findViewById(R.id.cameraLayout);
        LinearLayout galleryLayout = dialogView.findViewById(R.id.galleryLayout);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        // 相机按钮
        cameraLayout.setOnClickListener(v -> {
            dialog.dismiss();
            checkCameraPermissionAndDispatch();
        });
        
        // 相册按钮
        galleryLayout.setOnClickListener(v -> {
            dialog.dismiss();
            dispatchPickImageIntent();
        });
        
        // 取消按钮
        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    /**
     * 检查相机权限并启动相机
     */
    private void checkCameraPermissionAndDispatch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限
            LogUtils.d(TAG, "请求相机权限");
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, 
                    REQUEST_CAMERA_PERMISSION);
        } else {
            // 已有权限，启动相机
            dispatchTakePictureIntent();
        }
    }

    /**
     * 权限请求结果回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，启动相机
                LogUtils.d(TAG, "相机权限已授予");
                dispatchTakePictureIntent();
            } else {
                // 权限被拒绝
                LogUtils.e(TAG, "相机权限被拒绝");
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 启动相机拍照
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 先检查是否有相机应用
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                // 创建保存图片的文件
                File photoFile = createImageFile();
                if (photoFile != null) {
                    currentPhotoUri = FileProvider.getUriForFile(this,
                            "com.zjf.fincialsystem.fileprovider",
                            photoFile);
                    LogUtils.d(TAG, "创建文件URI成功: " + currentPhotoUri);
                    
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                    
                    // 授予URI的写入权限
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    
                    try {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    } catch (SecurityException e) {
                        LogUtils.e(TAG, "启动相机时安全异常: " + e.getMessage(), e);
                        Toast.makeText(this, "无法启动相机: 权限被拒绝", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        LogUtils.e(TAG, "启动相机时出现异常: " + e.getMessage(), e);
                        Toast.makeText(this, "无法启动相机", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (IOException ex) {
                LogUtils.e(TAG, "无法创建图片文件: " + ex.getMessage(), ex);
                Toast.makeText(this, "无法创建图片文件", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                LogUtils.e(TAG, "准备拍照时出现异常: " + e.getMessage(), e);
                Toast.makeText(this, "拍照准备失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            LogUtils.e(TAG, "未找到相机应用");
            Toast.makeText(this, "未找到相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 创建保存图片的文件
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* 前缀 */
                ".jpg",   /* 后缀 */
                storageDir      /* 目录 */
        );
    }

    /**
     * 启动图片选择器
     */
    private void dispatchPickImageIntent() {
        LogUtils.d(TAG, "启动图片选择器");
        try {
            // 主选择器
            Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            
            // 检查是否有应用可以处理这个Intent
            if (pickPhotoIntent.resolveActivity(getPackageManager()) != null) {
                LogUtils.d(TAG, "使用ACTION_PICK启动图片选择器");
                startActivityForResult(pickPhotoIntent, REQUEST_PICK_IMAGE);
            } else {
                LogUtils.w(TAG, "没有应用可以处理ACTION_PICK，尝试使用备用方法");
                
                // 备用选择器1
                Intent backupIntent = new Intent(Intent.ACTION_GET_CONTENT);
                backupIntent.setType("image/*");
                
                if (backupIntent.resolveActivity(getPackageManager()) != null) {
                    LogUtils.d(TAG, "使用ACTION_GET_CONTENT启动图片选择器");
                    startActivityForResult(backupIntent, REQUEST_PICK_IMAGE);
                } else {
                    // 备用选择器2
                    Intent openDocumentIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    openDocumentIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    openDocumentIntent.setType("image/*");
                    
                    if (openDocumentIntent.resolveActivity(getPackageManager()) != null) {
                        LogUtils.d(TAG, "使用ACTION_OPEN_DOCUMENT启动图片选择器");
                        startActivityForResult(openDocumentIntent, REQUEST_PICK_IMAGE);
                    } else {
                        LogUtils.e(TAG, "设备不支持图片选择功能");
                        Toast.makeText(this, "设备不支持图片选择功能", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "打开图片选择器失败：" + e.getMessage(), e);
            Toast.makeText(this, "无法打开图片选择器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtils.d(TAG, "onActivityResult - requestCode:" + requestCode + ", resultCode:" + resultCode);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // 相机返回
                LogUtils.d(TAG, "相机返回: currentPhotoUri = " + (currentPhotoUri != null ? currentPhotoUri.toString() : "null"));
                if (currentPhotoUri != null) {
                    updateAvatarPreview(currentPhotoUri);
                } else {
                    LogUtils.e(TAG, "相机返回的URI为空");
                    Toast.makeText(this, "无法加载拍摄的图片", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                // 相册返回
                try {
                    Uri selectedImageUri = data.getData();
                    LogUtils.d(TAG, "相册返回: selectedImageUri = " + (selectedImageUri != null ? selectedImageUri.toString() : "null"));
                    
                    if (selectedImageUri != null) {
                        // 确保URI有读取权限
                        try {
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(selectedImageUri, takeFlags);
                        } catch (SecurityException e) {
                            LogUtils.w(TAG, "无法获取持久化URI权限: " + e.getMessage());
                            // 继续处理，因为权限可能只是一次性的
                        } catch (Exception e) {
                            LogUtils.w(TAG, "获取URI权限时出现异常: " + e.getMessage());
                            // 继续处理
                        }
                        
                        updateAvatarPreview(selectedImageUri);
                    } else {
                        LogUtils.e(TAG, "相册返回的URI为空");
                        Toast.makeText(this, "无法获取选择的图片", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    LogUtils.e(TAG, "处理相册返回结果异常: " + e.getMessage(), e);
                    Toast.makeText(this, "处理图片出错", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            LogUtils.d(TAG, "用户取消了操作");
        } else {
            LogUtils.e(TAG, "未知结果代码: " + resultCode);
            Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 更新头像预览
     */
    private void updateAvatarPreview(Uri imageUri) {
        // 添加日志输出
        LogUtils.d(TAG, "更新头像预览，图片URI：" + imageUri.toString());
        
        try {
            // 清除ImageView的背景和内边距，它们可能影响图片显示
            binding.ivAvatar.setBackground(null);
            binding.ivAvatar.setPadding(0, 0, 0, 0);
            
            // 创建一个自定义视图目标，用于直接处理Drawable
            Target<Drawable> customTarget = new Target<Drawable>() {
                @Override
                public void onLoadStarted(@Nullable Drawable placeholder) {
                    LogUtils.d(TAG, "Glide开始加载图片");
                    binding.ivAvatar.setImageDrawable(placeholder);
                }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    LogUtils.e(TAG, "Glide加载图片失败");
                    if (errorDrawable != null) {
                        binding.ivAvatar.setImageDrawable(errorDrawable);
                    }
                }

                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    LogUtils.d(TAG, "Glide加载图片成功，资源宽度：" + resource.getIntrinsicWidth() + "，高度：" + resource.getIntrinsicHeight());
                    
                    // 直接使用Drawable
                    binding.ivAvatar.setImageDrawable(resource);
                    
                    // 显示成功消息
                    Toast.makeText(EditProfileActivity.this, R.string.avatar_updated, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    LogUtils.d(TAG, "Glide清除加载");
                    binding.ivAvatar.setImageDrawable(placeholder);
                }

                @Override
                public void getSize(@NonNull SizeReadyCallback cb) {
                    cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
                }

                @Override
                public void removeCallback(@NonNull SizeReadyCallback cb) {
                    // 不需要实现
                }

                @Override
                public void setRequest(@Nullable Request request) {
                    // 不需要实现
                }

                @Nullable
                @Override
                public Request getRequest() {
                    return null;
                }

                @Override
                public void onStart() {
                    // 不需要实现
                }

                @Override
                public void onStop() {
                    // 不需要实现
                }

                @Override
                public void onDestroy() {
                    // 不需要实现
                }
            };

            // 尝试使用不同的设置加载图片
            Glide.with(this)
                    .load(imageUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // 不使用缓存
                    .skipMemoryCache(true) // 跳过内存缓存
                    .placeholder(R.drawable.ic_person) // 添加默认占位图
                    .error(R.drawable.ic_error) // 添加错误占位图
                    .circleCrop() // 圆形裁剪
                    .into(customTarget);
            
        } catch (Exception e) {
            LogUtils.e(TAG, "更新头像预览失败: " + e.getMessage(), e);
            Toast.makeText(this, "头像更新失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 加载用户数据
     */
    private void loadUserData() {
        showLoading(true);

        // 获取用户ID
        final long userId = TokenManager.getInstance().getUserId() > 0 ? 
                TokenManager.getInstance().getUserId() : 1; // 使用默认ID
        
        if (TokenManager.getInstance().getUserId() <= 0) {
            LogUtils.w(TAG, "未获取到有效的用户ID，使用默认ID: " + userId);
        }

        // 从服务器获取用户数据
        userRepository.getUserInfo(userId, new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    currentUser = user;
                    fillUserData(user);
                    showLoading(false);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    LogUtils.e(TAG, "获取用户信息失败：" + error);
                    Toast.makeText(EditProfileActivity.this, "获取用户信息失败：" + error, Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    
                    // 如果获取失败，使用本地模拟数据作为后备
                    currentUser = createMockUser(userId);
                    fillUserData(currentUser);
                });
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
        // 编辑页面应该显示完整的11位手机号码
        user.setPhone("1380013" + String.format("%04d", userId));
        // 设置社交账号信息
        user.setWechat("wxid_" + userId);
        user.setQq("10" + (1000000 + userId));
        return user;
    }

    /**
     * 填充用户数据到表单
     */
    private void fillUserData(User user) {
        binding.etNickname.setText(user.getNickname());
        binding.etEmail.setText(user.getEmail());
        binding.etPhone.setText(user.getPhone());
        
        // 设置社交账号
        binding.etWechat.setText(user.getWechat());
        binding.etQq.setText(user.getQq());
    }

    /**
     * 保存用户资料
     */
    private void saveUserProfile() {
        if (currentUser == null) {
            Toast.makeText(this, "用户数据不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取编辑后的数据
        String nickname = binding.etNickname.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();

        // 基本验证
        if (TextUtils.isEmpty(nickname)) {
            Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新用户对象
        currentUser.setNickname(nickname);
        currentUser.setEmail(email);
        currentUser.setPhone(phone);

        showLoading(true);

        // 调用API保存用户信息
        userRepository.updateUserProfile(currentUser, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, "保存失败：" + error, Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
            }

            @Override
            public void isCacheData(boolean isCache) {
                // 保存操作不需要处理缓存状态
            }
        });
    }

    /**
     * 显示/隐藏加载中
     */
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.scrollView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 