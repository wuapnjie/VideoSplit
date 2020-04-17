package com.xiaopo.flying.demo.utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static String getFolderName(String name) {
        File mediaStorageDir =
            new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                name);

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return "";
            }
        }

        return mediaStorageDir.getAbsolutePath();
    }

    private static boolean isSDAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static File getNewFile(Context context, String folderName, String fileName) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);

        String timeStamp = simpleDateFormat.format(new Date());

        String path;
        if (isSDAvailable()) {
            path = getFolderName(folderName) + File.separator + fileName;
        } else {
            path = context.getFilesDir().getPath() + File.separator + fileName;
        }

        if (TextUtils.isEmpty(path)) {
            return null;
        }

        return new File(path);
    }

    public static void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();

        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();

            if (outChannel != null)
                outChannel.close();
        }
    }
}