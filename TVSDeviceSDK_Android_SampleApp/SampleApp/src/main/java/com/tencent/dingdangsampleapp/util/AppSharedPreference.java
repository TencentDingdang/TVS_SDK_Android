package com.tencent.dingdangsampleapp.util;

import android.content.Context;
import android.content.SharedPreferences;


public class AppSharedPreference {

    public static final String SHARE_PREFERENCES_NAME = "demo_pref";


    public static void setString(Context context, String key, String value) {
        SharedPreferences sp = context.getSharedPreferences(
                SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(key, value).apply();
    }

    public static String getString(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(
                SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return sp.getString(key, "");
    }

    public static String getString(Context context, String key, String defaultValue) {
        SharedPreferences sp = context.getSharedPreferences(
                SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return sp.getString(key, defaultValue);
    }

    public static void setBoolean(Context context, String key,boolean value) {
        SharedPreferences sp = context.getSharedPreferences(
                SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        SharedPreferences sp = context.getSharedPreferences(
                SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(key, defaultValue);
    }

    public static void setInt(Context context, String key, int value) {
        SharedPreferences sp = context.getSharedPreferences(
                SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(key, value).apply();
    }

    public static int getInt(Context context, String key, int defaultValue) {
        SharedPreferences sp = context.getSharedPreferences(
                SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return sp.getInt(key, defaultValue);
    }

    public static void setLong(Context context, String key, long value) {
        SharedPreferences sp = context.getSharedPreferences(
                SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sp.edit().putLong(key, value).apply();
    }

    public static long getLong(Context context, String key, long defaultValue) {
        SharedPreferences sp = context.getSharedPreferences(
                SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return sp.getLong(key, defaultValue);
    }

    public static void setFloat(Context context, String key, float value) {
        SharedPreferences sp = context.getSharedPreferences(SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sp.edit().putFloat(key, value).apply();
    }

    public static float getFloat(Context context, String key, float defaultValue) {
        SharedPreferences sp = context.getSharedPreferences(SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return sp.getFloat(key, defaultValue);
    }
}
