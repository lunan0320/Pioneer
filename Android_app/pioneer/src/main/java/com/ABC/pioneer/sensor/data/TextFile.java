
package com.ABC.pioneer.sensor.data;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TextFile {
    private final File file;

    public TextFile(final Context context, final String filename) {
        final File folder = new File(getRootFolder(context), "Sensor");
        file = new File(folder, filename);
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /// 获取文件内容
    public synchronized String contentsOf() {
        try {
            final FileInputStream fileInputStream = new FileInputStream(file);
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            final StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            bufferedReader.close();
            fileInputStream.close();
            return stringBuilder.toString();
        } catch (Throwable e) {
            return "";
        }
    }

    /**
     * 获取SD卡或模拟外部存储的根文件夹。
     */
    private static File getRootFolder(final Context context) {
        // 获取SD卡或模拟的外部存储。
        // 在模拟存储后报告SD卡，因此请选择最后一个文件夹
        final File[] externalMediaDirs = context.getExternalMediaDirs();
        if (externalMediaDirs.length > 0) {
            return externalMediaDirs[externalMediaDirs.length - 1];
        } else {
            return Environment.getExternalStorageDirectory();
        }
    }

    public synchronized boolean empty() {
        return !file.exists() || file.length() == 0;
    }

    /// 将行追加到新文件或现有文件
    public synchronized void write(String line) {
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write((line + "\n").getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Throwable e) {
        }
    }

    /// 覆盖文件内容
    public synchronized void overwrite(String content) {
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(content.getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Throwable e) {
        }
    }

    /// CSV输出的报价值（如果需要）。
    public static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("'") || value.contains("’")) {
            return "\"" + value + "\"";
        } else {
            return value;
        }
    }
}
