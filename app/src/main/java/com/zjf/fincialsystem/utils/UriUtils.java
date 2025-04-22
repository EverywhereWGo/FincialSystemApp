package com.zjf.fincialsystem.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * URI工具类
 * 用于处理文件URI和路径转换
 */
public class UriUtils {
    private static final String TAG = "UriUtils";
    
    /**
     * 从URI获取文件路径
     * 处理多种可能的URI格式
     */
    public static String getPath(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        
        LogUtils.d(TAG, "获取URI路径: " + uri.toString());
        
        // 检查是否是"content://"开头的URI
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // 检查是否是"file://"开头的URI
        else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return uri.getPath();
        }
        
        return null;
    }
    
    /**
     * 获取数据列的值
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = MediaStore.Images.Media.DATA;
        final String[] projection = {column};
        
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "获取数据列失败: " + uri, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        // 如果无法获取路径，尝试从URI获取文件名并创建临时文件
        try {
            String fileName = getFileNameFromUri(context, uri);
            if (!TextUtils.isEmpty(fileName)) {
                File tempFile = File.createTempFile("temp_", fileName.substring(fileName.lastIndexOf(".")), context.getCacheDir());
                
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.close();
                    inputStream.close();
                    
                    LogUtils.d(TAG, "已创建临时文件: " + tempFile.getAbsolutePath());
                    return tempFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "创建临时文件失败: " + uri, e);
        }
        
        return null;
    }
    
    /**
     * 从URI获取文件名
     */
    public static String getFileNameFromUri(Context context, Uri uri) {
        String result = null;
        
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex >= 0) {
                        result = cursor.getString(columnIndex);
                    }
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "获取文件名失败", e);
            }
        }
        
        if (result == null) {
            // 如果从cursor获取失败，尝试从URI路径获取
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        
        return result;
    }
} 