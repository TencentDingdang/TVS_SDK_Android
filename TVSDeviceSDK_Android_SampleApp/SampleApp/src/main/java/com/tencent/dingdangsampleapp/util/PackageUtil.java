package com.tencent.dingdangsampleapp.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

public class PackageUtil {

    /**
     * 获取应用的versionCode
     *
     * @param applicationContext
     * @param pkgName
     * @return
     */
    public static String getVersionCode(Context applicationContext, String pkgName) {
        String appVersionCode = "";
        try {
            PackageInfo packageInfo = applicationContext.getPackageManager().getPackageInfo(pkgName, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                appVersionCode = String.valueOf(packageInfo.getLongVersionCode());
            } else {
                appVersionCode = String.valueOf(packageInfo.versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appVersionCode;
    }

    /**
     * 获取应用的versionName
     *
     * @param applicationContext
     * @param pkgName
     * @return
     */
    public static String getVersionName(Context applicationContext, String pkgName) {
        String appVersionName = "";
        try {
            PackageInfo packageInfo = applicationContext.getPackageManager().getPackageInfo(pkgName, 0);
            appVersionName = String.valueOf(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appVersionName;
    }
}
