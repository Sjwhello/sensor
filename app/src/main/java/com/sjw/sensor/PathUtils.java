package com.sjw.sensor;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;

public class PathUtils {
    public static String getSDPath(Context context) {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);// 判断sd卡是否存在
        if (sdCardExist) {
            if (Build.VERSION.SDK_INT>=29){
                //Android10之后
                sdDir = context.getExternalFilesDir(Context.DOWNLOAD_SERVICE);
            }else {
                sdDir = Environment.getExternalStorageDirectory();// 获取SD卡根目录
            }
        } else {
            sdDir = Environment.getRootDirectory();// 获取跟目录
        }
        return sdDir.toString().split("data")[0];
    }
}
