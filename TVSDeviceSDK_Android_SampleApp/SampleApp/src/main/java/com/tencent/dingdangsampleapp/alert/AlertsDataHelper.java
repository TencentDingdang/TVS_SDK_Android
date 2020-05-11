package com.tencent.dingdangsampleapp.alert;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by shengyujin on 2017/3/16.
 */
public class AlertsDataHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "alerts.db";

    /**
     * 升级记录:
     * DATABASE_VERSION = 1; 创建数据库
     * DATABASE_VERSION = 2; 修改数据库的表结构
    */
    public static final int DATABASE_VERSION = 2;

    /**
     * 创建alerts表的SQL语句
     */
    public static final String CREATE_TABLE_ALERTS =
            "CREATE TABLE IF NOT EXISTS " + AlertsDataDictionary.TABLE_ALERTS + " ("
                    + AlertsDataDictionary.AlertsColumns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + AlertsDataDictionary.AlertsColumns.TOKEN + " TEXT NOT NULL,"
                    + AlertsDataDictionary.AlertsColumns.TYPE + " TEXT NOT NULL,"
                    + AlertsDataDictionary.AlertsColumns.STATE + " INTEGER NOT NULL DEFAULT 0,"
                    + AlertsDataDictionary.AlertsColumns.SCHEDULED_TIME_ISO8601 + " TEXT NOT NULL,"
                    + AlertsDataDictionary.AlertsColumns.LOOP_COUNT + " INTEGER NOT NULL DEFAULT 0,"
                    + AlertsDataDictionary.AlertsColumns.LOOP_PAUSE_MILLIS + " INTEGER NOT NULL DEFAULT 0,"
                    + AlertsDataDictionary.AlertsColumns.BACKGROUND_ASSET + " TEXT,"
                    + AlertsDataDictionary.AlertsColumns.ASSET_PLAY_ORDER + " TEXT" + ");";

    public static final String DROP_TABLES_ALERTS =
            "DROP TABLE IF EXISTS " + AlertsDataDictionary.TABLE_ALERTS;

    public static final String DEL_TABLES_ALERTS =
            "DELETE FROM " + AlertsDataDictionary.TABLE_ALERTS;

    /**
     * 创建alerts表的SQL语句
     */
    public static final String CREATE_TABLE_ALERT_ASSETS =
            "CREATE TABLE IF NOT EXISTS " + AlertsDataDictionary.TABLE_ALERT_ASSETS + " ("
                    + AlertsDataDictionary.AlertAssetsColumns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + AlertsDataDictionary.AlertAssetsColumns.ALERT_ID + " INTEGER NOT NULL DEFAULT 0,"
                    + AlertsDataDictionary.AlertAssetsColumns.ASSET_ID + " TEXT NOT NULL,"
                    + AlertsDataDictionary.AlertAssetsColumns.ASSET_URL + " TEXT NOT NULL" + ");";

    public static final String DROP_TABLES_ALERT_ASSETS =
            "DROP TABLE IF EXISTS " + AlertsDataDictionary.TABLE_ALERT_ASSETS;

    public static final String DEL_TABLES_ALERT_ASSETS =
            "DELETE FROM " + AlertsDataDictionary.TABLE_ALERT_ASSETS;

    private volatile static AlertsDataHelper sInstance = null;

    public static AlertsDataHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AlertsDataHelper.class) {
                if (sInstance == null) {
                    sInstance = new AlertsDataHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
                }
            }
        }
        return sInstance;
    }

    private AlertsDataHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_ALERTS);
        db.execSQL(CREATE_TABLE_ALERT_ASSETS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(DROP_TABLES_ALERTS);
            db.execSQL(CREATE_TABLE_ALERTS);
            db.execSQL(CREATE_TABLE_ALERT_ASSETS);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLES_ALERTS);
    }
}
