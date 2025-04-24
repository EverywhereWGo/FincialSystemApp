package com.zjf.fincialsystem.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.zjf.fincialsystem.R;

/**
 * 对话框工具类
 * 提供各种对话框的显示和隐藏功能
 */
public class DialogUtils {

    private static Dialog progressDialog;

    /**
     * 显示加载对话框
     *
     * @param context 上下文
     * @param message 显示的消息，如果为null则显示默认消息
     */
    public static void showProgressDialog(Context context, String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            hideProgressDialog();
        }

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);
        if (message != null) {
            TextView tvMessage = view.findViewById(R.id.tv_message);
            if (tvMessage != null) {
                tvMessage.setText(message);
            }
        }

        progressDialog = new Dialog(context, R.style.ProgressDialogStyle);
        progressDialog.setContentView(view);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    /**
     * 隐藏加载对话框
     */
    public static void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    /**
     * 显示确认对话框
     *
     * @param context        上下文
     * @param title          标题
     * @param message        消息内容
     * @param positiveText   确认按钮文本
     * @param negativeText   取消按钮文本
     * @param positiveListener 确认按钮点击监听
     * @param negativeListener 取消按钮点击监听
     */
    public static void showConfirmDialog(Context context, String title, String message,
                                         String positiveText, String negativeText,
                                         DialogInterface.OnClickListener positiveListener,
                                         DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, positiveListener)
                .setNegativeButton(negativeText, negativeListener);
        builder.create().show();
    }

    /**
     * 显示简单确认对话框（确认和取消按钮）
     *
     * @param context        上下文
     * @param title          标题
     * @param message        消息内容
     * @param positiveListener 确认按钮点击监听
     */
    public static void showSimpleConfirmDialog(Context context, String title, String message,
                                             DialogInterface.OnClickListener positiveListener) {
        showConfirmDialog(context, title, message, "确认", "取消", positiveListener, null);
    }

    /**
     * 显示提示对话框（只有确认按钮）
     *
     * @param context       上下文
     * @param title         标题
     * @param message       消息内容
     * @param positiveText  确认按钮文本
     * @param listener      确认按钮点击监听
     */
    public static void showAlertDialog(Context context, String title, String message,
                                      String positiveText, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, listener);
        builder.create().show();
    }
}