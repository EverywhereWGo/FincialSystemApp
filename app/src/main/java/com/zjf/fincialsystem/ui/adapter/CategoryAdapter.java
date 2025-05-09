package com.zjf.fincialsystem.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.utils.IconUtil;
import com.zjf.fincialsystem.utils.LogUtils;

import java.util.List;

/**
 * 分类列表适配器
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private static final String TAG = "CategoryAdapter";
    private List<Category> categories;
    private OnCategoryClickListener listener;
    private Context context;

    public CategoryAdapter(List<Category> categories) {
        this.categories = categories;
    }
    
    public CategoryAdapter(Context context, List<Category> categories) {
        this.context = context;
        this.categories = categories;
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    public void updateData(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout frameIcon;
        private final ImageView ivCategoryIcon;
        private final TextView tvCategoryName;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            frameIcon = itemView.findViewById(R.id.frameIcon);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCategoryClicked(categories.get(position));
                }
            });

            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEditClicked(categories.get(position));
                }
            });

            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteClicked(categories.get(position));
                }
            });
        }

        public void bind(Category category) {
            tvCategoryName.setText(category.getName());
            
            // 加载图标
            loadCategoryIcon(category, ivCategoryIcon);
            
            // 设置背景颜色
            try {
                frameIcon.setBackgroundColor(Color.parseColor(category.getColor()));
            } catch (Exception e) {
                // 如果颜色解析失败，使用默认颜色
                frameIcon.setBackgroundColor(Color.parseColor("#4CAF50"));
            }
            
            // 如果是默认分类，不允许删除
            btnDelete.setEnabled(!category.isDefault());
            btnDelete.setAlpha(category.isDefault() ? 0.5f : 1.0f);
        }
        
        /**
         * 加载分类图标
         */
        private void loadCategoryIcon(Category category, ImageView imageView) {
            try {
                String iconUrl = category.getIcon();
                
                // 如果没有图标URL，使用默认图标
                if (TextUtils.isEmpty(iconUrl)) {
                    int iconResId = IconUtil.getIconResourceId(category.getIcon());
                    if (iconResId != 0) {
                        imageView.setImageResource(iconResId);
                    } else {
                        imageView.setImageResource(R.drawable.ic_description);
                    }
                    return;
                }
                
                // 如果是服务器URL，需要添加BaseUrl
                if (iconUrl.startsWith("/") && !iconUrl.startsWith("//")) {
                    // 获取基础URL
                    String baseUrl = NetworkManager.getInstance().getBaseUrl();
                    iconUrl = baseUrl + iconUrl;
                    LogUtils.d(TAG, "完整分类图标URL: " + iconUrl);
                }
                
                // 先显示一个本地占位图，防止闪烁或空白
                imageView.setImageResource(R.drawable.ic_description);
                
                final String finalIconUrl = iconUrl;
                // 使用Glide加载图片
                Glide.with(context)
                        .load(finalIconUrl)
                        .placeholder(R.drawable.ic_description)
                        .error(R.drawable.ic_description)
                        .centerCrop()
                        .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                LogUtils.e(TAG, "分类图标加载失败: " + finalIconUrl + ", 错误: " + (e != null ? e.getMessage() : "未知错误"));
                                
                                // 加载失败时使用本地图标
                                int iconResId = IconUtil.getIconResourceId(category.getIcon());
                                if (iconResId != 0) {
                                    imageView.setImageResource(iconResId);
                                }
                                return false;
                            }
                            
                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                LogUtils.d(TAG, "分类图标加载成功: " + finalIconUrl);
                                return false;
                            }
                        })
                        .into(imageView);
            } catch (Exception e) {
                LogUtils.e(TAG, "加载分类图标异常: " + e.getMessage(), e);
                // 异常时使用默认图标
                imageView.setImageResource(R.drawable.ic_description);
            }
        }
    }

    public interface OnCategoryClickListener {
        void onCategoryClicked(Category category);
        void onEditClicked(Category category);
        void onDeleteClicked(Category category);
    }
} 