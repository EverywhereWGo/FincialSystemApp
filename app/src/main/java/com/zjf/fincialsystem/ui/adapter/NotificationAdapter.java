package com.zjf.fincialsystem.ui.adapter;

import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zjf.fincialsystem.R;
import com.zjf.fincialsystem.model.Notification;
import com.zjf.fincialsystem.utils.LogUtils;

import java.util.List;

/**
 * 通知列表适配器
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private static final String TAG = "NotificationAdapter";
    private final List<Notification> notifications;
    private OnNotificationClickListener listener;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public List<Notification> getDataList() {
        return notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        try {
            // 设置未读标记
            holder.viewUnread.setVisibility(notification.isRead() == 1 ? View.GONE : View.VISIBLE);

            // 设置标题和内容文字样式
            holder.tvTitle.setTypeface(notification.isRead() == 1 ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);
            holder.tvTitle.setText(notification.getTitle());
            holder.tvContent.setText(notification.getContent());

            // 设置时间
            holder.tvTime.setText(getRelativeTimeSpanString(notification.getCreateTime().getTime()));

            // 设置图标
            setNotificationIcon(holder.ivIcon, notification.getType());

            // 设置点击事件
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClicked(notification);
                }
            });

        } catch (Exception e) {
            LogUtils.e(TAG, "绑定通知视图错误: " + e.getMessage(), e);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * 设置通知图标
     */
    private void setNotificationIcon(ImageView imageView, String type) {
        if (type == null) {
            imageView.setImageResource(R.drawable.ic_notification);
            return;
        }

        int iconRes;
        switch (type) {
            case Notification.TYPE_BUDGET_WARNING:
            case Notification.TYPE_BUDGET_EXCEED:
                iconRes = R.drawable.ic_budget;
                break;
            case Notification.TYPE_BILL_REMINDER:
                iconRes = R.drawable.ic_calendar;
                break;
            case Notification.TYPE_LARGE_EXPENSE:
                iconRes = R.drawable.ic_expense;
                break;
            case Notification.TYPE_INCOME_RECEIVED:
                iconRes = R.drawable.ic_income;
                break;
            default:
                iconRes = R.drawable.ic_notification;
                break;
        }

        imageView.setImageResource(iconRes);
    }

    /**
     * 获取相对时间字符串
     */
    private String getRelativeTimeSpanString(long timeMs) {
        long now = System.currentTimeMillis();

        // 使用系统的相对时间格式化
        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                timeMs, now, DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE);

        return relativeTime.toString();
    }

    /**
     * 设置通知点击监听器
     */
    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    /**
     * 通知点击监听器接口
     */
    public interface OnNotificationClickListener {
        void onNotificationClicked(Notification notification);
    }

    /**
     * 通知视图持有者
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        View viewUnread;
        TextView tvTitle;
        TextView tvTime;
        TextView tvContent;

        ViewHolder(View itemView) {
            super(itemView);

            ivIcon = itemView.findViewById(R.id.iv_icon);
            viewUnread = itemView.findViewById(R.id.view_unread);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvContent = itemView.findViewById(R.id.tv_content);
        }
    }
} 