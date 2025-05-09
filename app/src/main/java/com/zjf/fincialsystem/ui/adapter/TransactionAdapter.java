package com.zjf.fincialsystem.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.network.NetworkManager;
import com.zjf.fincialsystem.repository.CategoryRepository;
import com.zjf.fincialsystem.repository.RepositoryCallback;
import com.zjf.fincialsystem.utils.IconUtil;
import com.zjf.fincialsystem.utils.LogUtils;
import com.zjf.fincialsystem.utils.NumberUtils;

import net.lucode.hackware.magicindicator.buildins.UIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易记录适配器
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private static final String TAG = "TransactionAdapter";
    private List<Transaction> transactions;
    private OnItemClickListener listener;
    private Context context;
    private CategoryRepository categoryRepository;

    public TransactionAdapter(Context context) {
        this.context = context;
        categoryRepository = new CategoryRepository(context);
        this.transactions = new ArrayList<>();
    }

    public void setData(List<Transaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(transactions.get(position));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAmount;
        private final TextView tvCategory;
        private final TextView tvDate;
        private final ImageView ivIcon;
        private final View viewIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvDate = itemView.findViewById(R.id.tv_date);
            ivIcon = itemView.findViewById(R.id.iv_category);
            viewIcon = ivIcon;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(transactions.get(position));
                }
            });
        }

        public void bind(Transaction transaction) {
            Context context = itemView.getContext();

            // 设置金额
            String amountText;
            int amountColor;
            if (transaction.getType() == Transaction.TYPE_EXPENSE) {
                amountText = "-" + NumberUtils.formatAmount(transaction.getAmount());
                amountColor = ContextCompat.getColor(context, R.color.expense);
            } else {
                amountText = "+" + NumberUtils.formatAmount(transaction.getAmount());
                amountColor = ContextCompat.getColor(context, R.color.income);
            }
            tvAmount.setText(amountText);
            tvAmount.setTextColor(amountColor);

            // 设置日期
            tvDate.setText(transaction.getCreateTime());

            // 设置分类和图标
            categoryRepository.getCategoryById(transaction.getCategoryId(), new RepositoryCallback<Category>() {
                @Override
                public void onSuccess(Category category) {
                    if (category != null) {
                        tvCategory.setText(category.getName());

                        // 优先加载网络图片
                        loadCategoryIcon(category, ivIcon);

                        // 设置图标背景颜色
                        if (!TextUtils.isEmpty(category.getColor())) {
                            try {
                                int color = Color.parseColor(category.getColor());
                                GradientDrawable background = new GradientDrawable();
                                background.setShape(GradientDrawable.OVAL);
                                background.setColor(color);
                                viewIcon.setBackground(background);
                            } catch (Exception e) {
                                // 颜色解析错误，使用默认颜色
                                viewIcon.setBackgroundResource(R.drawable.bg_circle_primary);
                            }
                        } else {
                            viewIcon.setBackgroundResource(R.drawable.bg_circle_primary);
                        }
                    } else {
                        tvCategory.setText(transaction.getCategoryName());
                        ivIcon.setImageResource(R.drawable.ic_category_default);
                        viewIcon.setBackgroundResource(R.drawable.bg_circle_primary);
                    }
                }

                @Override
                public void onError(String error) {
                    tvCategory.setText(transaction.getCategoryName());
                    ivIcon.setImageResource(R.drawable.ic_category_default);
                    viewIcon.setBackgroundResource(R.drawable.bg_circle_primary);
                }
            });
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
                        imageView.setImageResource(R.drawable.ic_category_default);
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
                imageView.setImageResource(R.drawable.ic_category_default);
                
                final String finalIconUrl = iconUrl;
                // 使用Glide加载图片
                Glide.with(context)
                        .load(finalIconUrl)
                        .placeholder(R.drawable.ic_category_default)
                        .error(R.drawable.ic_category_default)
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
                imageView.setImageResource(R.drawable.ic_category_default);
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }
} 