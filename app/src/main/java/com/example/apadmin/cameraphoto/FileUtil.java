package com.example.apadmin.cameraphoto;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhongjihao100@163.com on 19-11-19.
 */
public class FileUtil {
    private final static String TAG = "FileUtil";

    //用来存储设备信息和异常信息
    private static Map<String, String> infos = new HashMap<>();

    //用于格式化日期,作为日志文件名的一部分
    private static DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    /**
     * 判断SD卡是否被挂载
     * @param sdcardPath
     * @return
     */
    public static boolean isMount(String sdcardPath) {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState(new File(sdcardPath)));
    }

    public static String getMp4FileName(long timeMillis){
        return String.format("%1$tY-%1$tm-%1$td_%1$tH_%1$tM_%1$tS_%1$tL.mp4", timeMillis);
    }

    public static int copyAssetToFileDir(AssetManager assetManager, final String assetName, final String targetFilePath) {
        File targetFile = new File(targetFilePath);
        byte buf[] = new byte[1024];
        int cnt;

        InputStream src = null;
        FileOutputStream dst = null;
        if (!targetFile.exists()) {
            try {
                targetFile.createNewFile();
                dst = new FileOutputStream(targetFile);
                src = assetManager.open(assetName);

                while ((cnt = src.read(buf)) > 0) {
                    dst.write(buf, 0, cnt);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "copyAssetToFileDir exception " + e.toString());
            } finally {
                try {
                    if (src != null) {
                        src.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if(dst != null){
                        dst.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return 0;
    }

    /**
     * 收集设备参数信息
     * @param ctx
     */
    public static void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return	返回文件名称,便于将文件传送到服务器
     */
    public static String saveCrashInfo2File(Throwable ex) {

        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".log";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String path = Environment.getExternalStorageDirectory()+"/crash/";
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }
}
