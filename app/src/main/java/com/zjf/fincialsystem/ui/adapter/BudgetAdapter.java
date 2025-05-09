package com.zjf.fincialsystem.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.model.Budget;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.repository.CategoryRepository;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.utils.IconUtil;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NumberUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 预算列表适配器
 */
public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private static final String TAG = "BudgetAdapter";
    private final List<Budget> budgetList;
    private OnBudgetClickListener listener;
    private Context context;
    private CategoryRepository categoryRepository;

    public BudgetAdapter(Context context) {
        this.context = context;
        categoryRepository = new CategoryRepository(context);
        this.budgetList = new ArrayList<>();
    }

    public void setData(List<Budget> budgetList) {
        this.budgetList.clear();
        if (budgetList != null) {
            this.budgetList.addAll(budgetList);
        }
        notifyDataSetChanged();
    }

    /**
     * 平滑更新数据，避免闪烁
     *
     * @param newBudgetList 新的预算列表
     */
    public void updateDataSmoothly(List<Budget> newBudgetList) {
        if (newBudgetList == null) {
            return;
        }

        // 如果当前列表为空，直接设置数据
        if (budgetList.isEmpty()) {
            setData(newBudgetList);
            return;
        }

        // 保留原始大小供后续比较
        int originalSize = budgetList.size();

        // 清空原始数据但不通知UI更新
        budgetList.clear();
        budgetList.addAll(newBudgetList);

        // 如果数据条数相同，使用notifyItemRangeChanged避免全局刷新
        if (originalSize == newBudgetList.size()) {
            notifyItemRangeChanged(0, newBudgetList.size());
        } else {
            // 数据条数不同，使用notifyDataSetChanged
            notifyDataSetChanged();
        }
    }

    public void addBudget(Budget budget) {
        this.budgetList.add(budget);
        notifyItemInserted(budgetList.size() - 1);
    }

    public void updateBudget(Budget budget) {
        for (int i = 0; i < budgetList.size(); i++) {
            if (budgetList.get(i).getId() == budget.getId()) {
                budgetList.set(i, budget);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeBudget(long budgetId) {
        for (int i = 0; i < budgetList.size(); i++) {
            if (budgetList.get(i).getId() == budgetId) {
                budgetList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void setOnBudgetClickListener(OnBudgetClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgetList.get(position);
        holder.bind(budget);
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    public class BudgetViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgCategory;
        private final TextView tvCategoryName;
        private final TextView tvUsedAmount;
        private final TextView tvBudgetAmount;
        private final ProgressBar progressBar;
        private final TextView tvStatus;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCategory = itemView.findViewById(R.id.img_category);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvUsedAmount = itemView.findViewById(R.id.tv_used_amount);
            tvBudgetAmount = itemView.findViewById(R.id.tv_budget_amount);
            progressBar = itemView.findViewById(R.id.progress_bar);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);

            // 设置点击事件
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBudgetClick(budgetList.get(position));
                }
            });

            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEditClick(budgetList.get(position));
                }
            });

            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteClick(budgetList.get(position));
                }
            });
        }

        public void bind(Budget budget) {
            Context context = itemView.getContext();

            // 设置分类图标
            categoryRepository.getCategoryById(budget.getCategoryId(), new RepositoryCallback<Category>() {
                @Override
                public void onSuccess(Category category) {
                    if (category != null) {
                        // 加载分类图标
                        loadCategoryIcon(category, imgCategory);

                        // 设置图标背景颜色
                        if (category.getColor() != null) {
                            try {
                                int color = android.graphics.Color.parseColor(category.getColor());
                                imgCategory.getBackground().setTint(color);
                            } catch (Exception e) {
                                // 颜色解析错误，使用默认颜色
                                imgCategory.getBackground().setTint(ContextCompat.getColor(context, R.color.colorPrimaryLight));
                            }
                        }
                        tvCategoryName.setText(category.getName());
                    } else {
                        imgCategory.setImageResource(R.drawable.ic_category);
                        tvCategoryName.setText(R.string.unknown);
                    }
                }

                @Override
                public void onError(String error) {
                    imgCategory.setImageResource(R.drawable.ic_category);
                    tvCategoryName.setText(R.string.unknown);
                }
            });

            // 设置金额
            tvUsedAmount.setText(NumberUtils.formatAmountWithCurrency(budget.getUsedAmount()));
            tvBudgetAmount.setText(NumberUtils.formatAmountWithCurrency(budget.getAmount()));

            // 设置进度条
            int progress = (int) budget.getUsedPercentage();
            progressBar.setProgress(progress);

            // 根据进度设置不同样式
            if (progress >= 100) {
                progressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.progress_danger));
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.expense));
                tvStatus.setText(context.getString(R.string.budget_status_danger, progress - 100));
            } else if (progress >= 80) {
                progressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.progress_warning));
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warning));
                tvStatus.setText(context.getString(R.string.budget_status_warning, progress));
            } else {
                progressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.progress_success));
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success));
                tvStatus.setText(R.string.budget_status_safe);
            }
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
                        imageView.setImageResource(R.drawable.ic_category);
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
                imageView.setImageResource(R.drawable.ic_category);
                
                final String finalIconUrl = iconUrl;
                // 使用Glide加载图片
                Glide.with(context)
                        .load(finalIconUrl)
                        .placeholder(R.drawable.ic_category)
                        .error(R.drawable.ic_category)
                        .centerCrop()
                        .circleCrop()
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
                imageView.setImageResource(R.drawable.ic_category);
            }
        }
    }

    public interface OnBudgetClickListener {
        void onBudgetClick(Budget budget);

        void onEditClick(Budget budget);

        void onDeleteClick(Budget budget);
    }
} 