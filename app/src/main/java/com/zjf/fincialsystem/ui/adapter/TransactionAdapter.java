package com.zjf.fincialsystem.ui.adapter;

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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.model.Category;
import com.zjf.fincialsystem.model.Transaction;
import com.zjf.fincialsystem.utils.DateUtils;
import com.zjf.fincialsystem.utils.IconUtil;
import com.zjf.fincialsystem.utils.NumberUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易记录适配器
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    
    private List<Transaction> transactions;
    private OnItemClickListener listener;
    
    public TransactionAdapter() {
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
            tvDate.setText(DateUtils.formatDate(transaction.getDate()));
            
            // 设置分类和图标
            Category category = transaction.getCategory();
            if (category != null) {
                tvCategory.setText(category.getName());
                
                int iconResId = IconUtil.getIconResourceId(category.getIcon());
                if (iconResId != 0) {
                    ivIcon.setImageResource(iconResId);
                }
                
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
                tvCategory.setText(R.string.unknown);
                ivIcon.setImageResource(R.drawable.ic_category_default);
                viewIcon.setBackgroundResource(R.drawable.bg_circle_primary);
            }
        }
    }
    
    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }
} 