package com.zjf.fincialsystem.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.ActivityImageViewBinding;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.TokenManager;

/**
 * 图片查看活动
 * 支持查看本地资源ID或网络URL图片
 */
public class ImageViewActivity extends AppCompatActivity {
    
    private static final String TAG = "ImageViewActivity";
    
    // 图片来源类型
    public static final String EXTRA_IMAGE_RES_ID = "image_res_id"; // 本地资源ID
    public static final String EXTRA_IMAGE_URL = "image_url"; // 网络URL
    public static final String EXTRA_IMAGE_URI = "image_uri"; // 本地URI
    
    private ActivityImageViewBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 设置返回按钮
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        
        // 加载图片
        loadImage();
    }
    
    /**
     * 加载图片
     */
    private void loadImage() {
        // 显示加载中
        binding.progressBar.setVisibility(View.VISIBLE);
        
        try {
            // 获取传递的参数
            Intent intent = getIntent();
            
            // 优先尝试加载网络URL
            if (intent.hasExtra(EXTRA_IMAGE_URL)) {
                String imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL);
                LogUtils.d(TAG, "加载网络图片: " + imageUrl);
                
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // 如果是服务器URL，需要添加BaseUrl
                    if (imageUrl.startsWith("/") && !imageUrl.startsWith("//")) {
                        // 获取基础URL
                        String baseUrl = com.zjf.fincialsystem.network.NetworkManager.getInstance().getBaseUrl();
                        imageUrl = baseUrl + imageUrl;
                        LogUtils.d(TAG, "完整头像URL: " + imageUrl);
                    }
                    
                    // 使用Glide加载网络图片
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(binding.photoView);
                } else {
                    // 加载默认图片
                    binding.photoView.setImageResource(R.drawable.ic_person);
                    Toast.makeText(this, "无效的图片URL", Toast.LENGTH_SHORT).show();
                }
            }
            // 尝试加载本地URI
            else if (intent.hasExtra(EXTRA_IMAGE_URI)) {
                String uriString = intent.getStringExtra(EXTRA_IMAGE_URI);
                LogUtils.d(TAG, "加载本地URI图片: " + uriString);
                
                if (uriString != null && !uriString.isEmpty()) {
                    Uri uri = Uri.parse(uriString);
                    
                    // 使用Glide加载本地URI
                    Glide.with(this)
                            .load(uri)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(binding.photoView);
                } else {
                    // 加载默认图片
                    binding.photoView.setImageResource(R.drawable.ic_person);
                    Toast.makeText(this, "无效的图片URI", Toast.LENGTH_SHORT).show();
                }
            }
            // 加载本地资源ID图片
            else if (intent.hasExtra(EXTRA_IMAGE_RES_ID)) {
                int resId = intent.getIntExtra(EXTRA_IMAGE_RES_ID, R.drawable.ic_person);
                LogUtils.d(TAG, "加载本地资源图片: " + resId);
                
                // 使用Glide加载本地资源
                Glide.with(this)
                        .load(resId)
                        .into(binding.photoView);
            }
            // 尝试从TokenManager获取头像URL
            else {
                // 从TokenManager获取缓存的头像URL
                String cachedAvatarUrl = TokenManager.getInstance().getUserAvatar();
                LogUtils.d(TAG, "尝试从TokenManager获取头像: " + cachedAvatarUrl);
                
                if (cachedAvatarUrl != null && !cachedAvatarUrl.isEmpty()) {
                    // 如果是服务器URL，需要添加BaseUrl
                    if (cachedAvatarUrl.startsWith("/") && !cachedAvatarUrl.startsWith("//")) {
                        // 获取基础URL
                        String baseUrl = com.zjf.fincialsystem.network.NetworkManager.getInstance().getBaseUrl();
                        cachedAvatarUrl = baseUrl + cachedAvatarUrl;
                        LogUtils.d(TAG, "完整头像URL: " + cachedAvatarUrl);
                    }
                    
                    // 使用Glide加载网络图片
                    Glide.with(this)
                            .load(cachedAvatarUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(binding.photoView);
                } else {
                    // 没有找到任何图片来源，加载默认图片
                    binding.photoView.setImageResource(R.drawable.ic_person);
                    Toast.makeText(this, "未找到图片来源", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "加载图片失败: " + e.getMessage(), e);
            binding.photoView.setImageResource(R.drawable.ic_person);
            Toast.makeText(this, "加载图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            // 隐藏进度条
            binding.progressBar.setVisibility(View.GONE);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 