package com.zjf.fincialsystem.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.databinding.FragmentProfileBinding;
import com.zjf.fincialsystem.utils.LogUtils;

/**
 * 个人资料Fragment
 */
public class ProfileFragment extends Fragment {
    
    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
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
            
            // TODO: 初始化其他视图
            
        } catch (Exception e) {
            LogUtils.e(TAG, "初始化视图失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 加载数据
     */
    private void loadData() {
        try {
            // TODO: 加载用户资料数据
            
        } catch (Exception e) {
            LogUtils.e(TAG, "加载数据失败：" + e.getMessage(), e);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 