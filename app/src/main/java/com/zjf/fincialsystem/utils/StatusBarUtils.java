package com.zjf.fincialsystem.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * 状态栏工具类
 * 提供状态栏相关的工具方法
 */
public class StatusBarUtils {

    /**
     * 获取状态栏高度
     *
     * @param context 上下文
     * @return 状态栏高度（像素）
     */
    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    /**
     * 设置沉浸式状态栏
     *
     * @param activity 活动
     * @param darkMode 是否为深色模式（状态栏文字为深色）
     */
    public static void setImmersiveStatusBar(Activity activity, boolean darkMode) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            
            int systemUiVisibility = window.getDecorView().getSystemUiVisibility() |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            
            if (darkMode) {
                systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                systemUiVisibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            
            window.getDecorView().setSystemUiVisibility(systemUiVisibility);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    /**
     * 为View添加状态栏高度的顶部内边距
     *
     * @param view    需要添加内边距的视图
     * @param context 上下文
     */
    public static void addStatusBarTopPadding(View view, Context context) {
        if (view != null) {
            int statusBarHeight = getStatusBarHeight(context);
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop() + statusBarHeight,
                    view.getPaddingRight(),
                    view.getPaddingBottom()
            );
        }
    }
    
    /**
     * 为AppBar/Toolbar设置正确的内边距和高度
     * 
     * @param toolbar Toolbar或其父容器
     * @param context 上下文
     */
    public static void adjustToolbarForStatusBar(View toolbar, Context context) {
        if (toolbar == null || context == null) return;
        
        int statusBarHeight = getStatusBarHeight(context);
        
        // 设置顶部内边距
        toolbar.setPadding(
                toolbar.getPaddingLeft(),
                statusBarHeight, // 添加状态栏高度的顶部内边距
                toolbar.getPaddingRight(),
                toolbar.getPaddingBottom()
        );
        
        // 调整高度
        ViewGroup.LayoutParams layoutParams = toolbar.getLayoutParams();
        if (layoutParams != null && layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            try {
                // 获取toolbar_height资源ID
                int toolbarHeightResId = context.getResources().getIdentifier("toolbar_height", "dimen", context.getPackageName());
                if (toolbarHeightResId > 0) {
                    // 如果存在toolbar_height资源，使用它
                    int toolbarHeight = context.getResources().getDimensionPixelSize(toolbarHeightResId);
                    layoutParams.height = toolbarHeight + statusBarHeight;
                } else {
                    // 否则增加默认的56dp
                    int defaultToolbarHeight = (int) (56 * context.getResources().getDisplayMetrics().density);
                    layoutParams.height = defaultToolbarHeight + statusBarHeight;
                }
                toolbar.setLayoutParams(layoutParams);
            } catch (Exception e) {
                LogUtils.e("StatusBarUtils", "调整工具栏高度失败: " + e.getMessage());
            }
        }
        
        LogUtils.d("StatusBarUtils", "已调整工具栏内边距，状态栏高度: " + statusBarHeight + "px");
    }
    
    /**
     * 为滚动视图中的内容布局设置正确的顶部内边距
     * 
     * @param scrollView 滚动视图
     * @param context 上下文
     */
    public static void adjustScrollViewContentPadding(ViewGroup scrollView, Context context) {
        if (scrollView == null || context == null || scrollView.getChildCount() == 0) return;
        
        View contentView = scrollView.getChildAt(0);
        int statusBarHeight = getStatusBarHeight(context);
        
        // 为内容视图添加顶部内边距
        contentView.setPadding(
                contentView.getPaddingLeft(),
                statusBarHeight + contentView.getPaddingTop(),
                contentView.getPaddingRight(),
                contentView.getPaddingBottom()
        );
        
        LogUtils.d("StatusBarUtils", "已调整滚动视图内容内边距，状态栏高度: " + statusBarHeight + "px");
    }
    
    /**
     * 为视图设置足够的顶部margin，避免被状态栏遮挡
     * 
     * @param view 需要设置margin的视图
     * @param context 上下文
     */
    public static void addStatusBarTopMargin(View view, Context context) {
        if (view == null || context == null) return;
        
        ViewGroup.MarginLayoutParams params = null;
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        }
        
        if (params != null) {
            int statusBarHeight = getStatusBarHeight(context);
            params.topMargin = params.topMargin + statusBarHeight;
            view.setLayoutParams(params);
            LogUtils.d("StatusBarUtils", "已设置视图顶部Margin，状态栏高度: " + statusBarHeight + "px");
        }
    }

    /**
     * 设置透明状态栏
     *
     * @param activity 活动
     */
    public static void setTransparentStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }
} 