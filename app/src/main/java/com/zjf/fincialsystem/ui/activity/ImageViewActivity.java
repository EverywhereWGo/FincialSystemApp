package com.zjf.fincialsystem.ui.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.StatusBarUtils;

/**
 * 图片查看Activity
 */
public class ImageViewActivity extends AppCompatActivity {

    private static final String TAG = "ImageViewActivity";
    
    // 传递图片资源ID的Key
    public static final String EXTRA_IMAGE_RES_ID = "extra_image_res_id";
    
    private ImageView imageView;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        
        // 设置状态栏
        StatusBarUtils.setImmersiveStatusBar(this, true);
        
        // 初始化工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        
        // 调整工具栏以适应状态栏
        StatusBarUtils.adjustToolbarForStatusBar(toolbar, this);
        
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.view_avatar);
        }
        
        // 获取传递过来的图片资源ID
        int imageResId = getIntent().getIntExtra(EXTRA_IMAGE_RES_ID, R.drawable.ic_person);
        
        try {
            // 显示图片
            imageView = findViewById(R.id.image_view);
            imageView.setImageResource(imageResId);
            
            // 设置缩放检测器
            scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
            
            // 设置触摸事件
            imageView.setOnTouchListener((v, event) -> {
                scaleGestureDetector.onTouchEvent(event);
                return true;
            });
            
        } catch (Exception e) {
            LogUtils.e(TAG, "加载图片失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 缩放手势监听器
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // 获取缩放因子
            scaleFactor *= detector.getScaleFactor();
            
            // 限制缩放范围
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3.0f));
            
            // 应用缩放
            imageView.setScaleX(scaleFactor);
            imageView.setScaleY(scaleFactor);
            return true;
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 