package com.zjf.fincialsystem.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.utils.IconUtil;

import java.util.List;

/**
 * 分类列表适配器
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Category> categories;
    private OnCategoryClickListener listener;

    public CategoryAdapter(List<Category> categories) {
        this.categories = categories;
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
            
            // 设置图标
            int iconResId = IconUtil.getIconResourceId(category.getIcon());
            if (iconResId != 0) {
                ivCategoryIcon.setImageResource(iconResId);
            } else {
                // 设置默认图标
                ivCategoryIcon.setImageResource(R.drawable.ic_description);
            }
            
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
    }

    public interface OnCategoryClickListener {
        void onCategoryClicked(Category category);
        void onEditClicked(Category category);
        void onDeleteClicked(Category category);
    }
} 