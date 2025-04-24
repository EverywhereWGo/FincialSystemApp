package com.zjf.fincialsystem.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.zjf.fincialsystem.app.FinanceApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 文件工具类
 * 用于处理文件操作
 */
public class FileUtils {
    private static final String TAG = "FileUtils";
    
    /**
     * 从输入流创建临时文件
     * 
     * @param inputStream 输入流
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @return 创建的临时文件
     * @throws IOException 如果创建文件失败
     */
    public static File createTempFileFromStream(InputStream inputStream, String prefix, String suffix) throws IOException {
        if (inputStream == null) {
            throw new IOException("输入流为空");
        }
        
        // 获取应用上下文
        Context context = FinanceApplication.getAppContext();
        if (context == null) {
            throw new IOException("无法获取应用上下文");
        }
        
        // 使用应用的缓存目录创建临时文件，而不是外部存储
        File tempFile = File.createTempFile(prefix, suffix, context.getCacheDir());
        tempFile.deleteOnExit();
        
        // 保存输入流到文件
        boolean success = false;
        OutputStream outputStream = null;
        
        try {
            outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            success = true;
            LogUtils.d(TAG, "成功创建临时文件: " + tempFile.getAbsolutePath());
            return tempFile;
        } catch (IOException e) {
            LogUtils.e(TAG, "创建临时文件失败", e);
            throw e;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    LogUtils.e(TAG, "关闭输出流失败", e);
                }
            }
            
            if (!success && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    LogUtils.w(TAG, "无法删除临时文件: " + tempFile.getAbsolutePath());
                }
            }
        }
    }
    
    /**
     * 从位图创建临时文件
     * 
     * @param bitmap 位图
     * @param context 上下文
     * @param quality 图片质量 (0-100)
     * @return 创建的临时文件
     * @throws IOException 如果创建文件失败
     */
    public static File createTempFileFromBitmap(Bitmap bitmap, Context context, int quality) throws IOException {
        if (bitmap == null) {
            throw new IOException("位图为空");
        }
        
        // 创建临时文件
        File tempFile = File.createTempFile("bitmap_", ".jpg", context.getCacheDir());
        tempFile.deleteOnExit();
        
        // 保存位图到文件
        boolean success = false;
        FileOutputStream fos = null;
        
        try {
            fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            success = true;
            LogUtils.d(TAG, "成功从位图创建临时文件: " + tempFile.getAbsolutePath());
            return tempFile;
        } catch (IOException e) {
            LogUtils.e(TAG, "从位图创建临时文件失败", e);
            throw e;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    LogUtils.e(TAG, "关闭文件输出流失败", e);
                }
            }
            
            if (!success && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    LogUtils.w(TAG, "无法删除临时文件: " + tempFile.getAbsolutePath());
                }
            }
        }
    }
    
    /**
     * 将Bitmap保存为文件
     * @param context 上下文
     * @param bitmap 要保存的Bitmap
     * @param filename 文件名
     * @return 保存的文件
     */
    public static File bitmapToFile(Context context, Bitmap bitmap, String filename) {
        File file = new File(context.getCacheDir(), filename);
        try {
            file.createNewFile();
            
            // 压缩图片并保存到文件
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
            
            return file;
        } catch (IOException e) {
            LogUtils.e(TAG, "保存图片到文件失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从Uri获取文件路径
     * @param context 上下文
     * @param uri 文件Uri
     * @return 文件路径
     */
    public static String getPathFromUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        
        // 直接使用Uri路径
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        
        // MediaStore查询
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    return cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "获取文件路径失败: " + e.getMessage(), e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        
        // 无法获取路径，创建临时文件
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            String filename = "temp_image_" + System.currentTimeMillis() + ".jpg";
            File file = bitmapToFile(context, bitmap, filename);
            return file.getAbsolutePath();
        } catch (IOException e) {
            LogUtils.e(TAG, "创建临时文件失败: " + e.getMessage(), e);
            return null;
        }
    }
} 