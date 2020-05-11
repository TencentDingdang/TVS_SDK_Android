package com.tencent.dingdangsampleapp.alert;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.tencent.ai.tvs.tvsinterface.AlertBaseInfo;
import com.tencent.ai.tvs.tvsinterface.AlertInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 闹钟数据持久化到database
 * <p>
 */
public class AlertsDataManager {
    private static final String TAG = "AlertsDataManager";
    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private Handler mHandler = new Handler();
    private Context mContext;
    private AlertDataListener mListener;


    /**
     * 闹钟数据库读写回调
     */
    interface AlertDataListener {

        void onGetAlertsSucceed(List<AlertInfo> alertInfos);

        void onGetAlertsFailed(String errMsg);

        void onAddAlertSucceed(AlertInfo alertInfo);

        void onAddAlertFailed(String errMsg, AlertInfo alertInfo);

        void onDelelteAlertSucceed(AlertInfo alertInfo);

        void onDelelteAlertFailed(String errMsg, AlertInfo alertInfo);
    }


    public AlertsDataManager(Context context) {
        mContext = context;
    }

    public void setAlertDataListener(AlertDataListener listener) {
        mListener = listener;
    }

    public void getAllAlerts() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                SQLiteDatabase db = AlertsDataHelper.getInstance(mContext).getWritableDatabase();
                if (db != null) {
                    SparseArray<Map<String, String>> assets = new SparseArray<Map<String, String>>();
                    String assetsOrderBy = AlertsDataDictionary.AlertAssetsColumns.ALERT_ID + " ASC";
                    Cursor assetCursor = null;
                    try {
                        assetCursor = db.query(AlertsDataDictionary.TABLE_ALERT_ASSETS, null, null, null, null, null, assetsOrderBy);
                        if (assetCursor != null && assetCursor.moveToFirst()) {
                            Log.i(TAG, "getAllAlerts match assets size: " + assetCursor.getCount());
                            do {
                                int id = assetCursor.getInt(assetCursor.getColumnIndex(AlertsDataDictionary.AlertAssetsColumns.ID));
                                int alertId = assetCursor.getInt(assetCursor.getColumnIndex(AlertsDataDictionary.AlertAssetsColumns.ALERT_ID));
                                String assetId = assetCursor.getString(assetCursor.getColumnIndex(AlertsDataDictionary.AlertAssetsColumns.ASSET_ID));
                                String assetUrl = assetCursor.getString(assetCursor.getColumnIndex(AlertsDataDictionary.AlertAssetsColumns.ASSET_URL));

                                Map<String, String> assetMap = assets.get(alertId);
                                if (assetMap != null) {
                                    assetMap.put(assetId, assetUrl);
                                } else {
                                    assetMap = new HashMap<String, String>();
                                    assetMap.put(assetId, assetUrl);
                                    assets.put(alertId, assetMap);
                                }
                                Log.i(TAG, "getAllAlerts asset assetId:" + assetId + ", assetUrl: " + assetUrl);
                            } while (assetCursor.moveToNext());
                        } else {
                            Log.d(TAG, "getAllAlerts no match assets found!");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        notifyGetAlertsFailed(ex.getMessage());
                    } finally {
                        if( null != assetCursor ){
                            assetCursor.close();
                        }
                    }

                    List<AlertInfo> alertInfos = new ArrayList<AlertInfo>();
                    String orderBy = AlertsDataDictionary.AlertsColumns.SCHEDULED_TIME_ISO8601 + " ASC";
                    Cursor cursor = null;
                    try {
                        cursor = db.query(AlertsDataDictionary.TABLE_ALERTS, null, null, null, null, null, orderBy);
                        if (cursor != null && cursor.moveToFirst()) {
                            Log.i(TAG, "getAllAlerts match alertInfos size: " + cursor.getCount());
                            do {
                                int id = cursor.getInt(cursor.getColumnIndex(AlertsDataDictionary.AlertsColumns.ID));
                                String token = cursor.getString(cursor.getColumnIndex(AlertsDataDictionary.AlertsColumns.TOKEN));
                                String type = cursor.getString(cursor.getColumnIndex(AlertsDataDictionary.AlertsColumns.TYPE));
                                int state = cursor.getInt(cursor.getColumnIndex(AlertsDataDictionary.AlertsColumns.STATE));
                                String scheduledTime = cursor.getString(cursor.getColumnIndex(AlertsDataDictionary.AlertsColumns.SCHEDULED_TIME_ISO8601));
                                int loopCount = cursor.getInt(cursor.getColumnIndex(AlertsDataDictionary.AlertsColumns.LOOP_COUNT));
                                int loopPauseMillis = cursor.getInt(cursor.getColumnIndex(AlertsDataDictionary.AlertsColumns.LOOP_PAUSE_MILLIS));
                                String backgroundAsset = cursor.getString(cursor.getColumnIndex(AlertsDataDictionary.AlertsColumns.BACKGROUND_ASSET));
                                String assetPlayOrderInString = cursor.getString(cursor.getColumnIndex(AlertsDataDictionary.AlertsColumns.ASSET_PLAY_ORDER));

                                AlertInfo alertInfo = new AlertInfo(token, AlertBaseInfo.AlertType.valueOf(type), scheduledTime, loopCount, loopPauseMillis, backgroundAsset);
                                alertInfo.setId(id);
                                alertInfo.setState(state);
                                alertInfo.setAssetPlayOrderInString(assetPlayOrderInString);

                                if (assets.get(id) != null) {
                                    Map<String, String> assetMap = assets.get(id);
                                    alertInfo.setAssetsInMap(assetMap);
                                }

                                alertInfos.add(alertInfo);
                                Log.i(TAG, "getAllAlerts alertInfo:" + alertInfo.toString());
                            } while (cursor.moveToNext());
                        } else {
                            Log.d(TAG, "getAllAlerts no match alertInfos found!");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        notifyGetAlertsFailed(ex.getMessage());
                    } finally {
                        if( null != cursor ){
                            cursor.close();
                        }
                    }

                    notifyGetAlertsSucceed(alertInfos);
                }
            }
        });
    }

    public void addAlert(final AlertInfo alertInfo) {
        mExecutor.execute(new Runnable() {

            @Override
            public void run() {
                SQLiteDatabase db = AlertsDataHelper.getInstance(mContext).getWritableDatabase();
                if (db != null) {
                    try {
                        db.beginTransaction();
                        ContentValues values = new ContentValues();
                        values.put(AlertsDataDictionary.AlertsColumns.TOKEN, alertInfo.getToken());
                        values.put(AlertsDataDictionary.AlertsColumns.TYPE, alertInfo.getType().name());
                        values.put(AlertsDataDictionary.AlertsColumns.STATE, alertInfo.getState());
                        values.put(AlertsDataDictionary.AlertsColumns.SCHEDULED_TIME_ISO8601, alertInfo.getScheduledTime());
                        values.put(AlertsDataDictionary.AlertsColumns.LOOP_COUNT, alertInfo.getLoopCount());
                        values.put(AlertsDataDictionary.AlertsColumns.LOOP_PAUSE_MILLIS, alertInfo.getLoopPauseMillis());
                        values.put(AlertsDataDictionary.AlertsColumns.BACKGROUND_ASSET, alertInfo.getBackgroundAsset());
                        values.put(AlertsDataDictionary.AlertsColumns.ASSET_PLAY_ORDER, alertInfo.getAssetPlayOrderInString());
                        long rowId = db.insertWithOnConflict(AlertsDataDictionary.TABLE_ALERTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);

                        if (rowId <= 0) {
                            Log.i(TAG, "addAlert add alertInfo failed!");
                            notifyAddAlertFailed("writeToDisk add failed!", alertInfo);
                            return;
                        }
                        Log.i(TAG, "addAlert success, alarmId : " + rowId);
                        alertInfo.setId(rowId);

                        Map<String, String> assets = alertInfo.getAssets();
                        Set<String> assetIds = assets.keySet();
                        for (String assetId : assetIds) {
                            String assetUrl = assets.get(assetId);

                            ContentValues assetValues = new ContentValues();
                            assetValues.put(AlertsDataDictionary.AlertAssetsColumns.ALERT_ID, rowId);
                            assetValues.put(AlertsDataDictionary.AlertAssetsColumns.ASSET_ID, assetId);
                            assetValues.put(AlertsDataDictionary.AlertAssetsColumns.ASSET_URL, assetUrl);
                            long assetRowId = db.insertWithOnConflict(AlertsDataDictionary.TABLE_ALERT_ASSETS, null, assetValues, SQLiteDatabase.CONFLICT_REPLACE);

                            if (assetRowId <= 0) {
                                Log.i(TAG, "addAlert add assets failed!");
                            } else {
                                Log.i(TAG, "addAlert success, asset row id : " + assetRowId);
                            }
                        }
                        db.setTransactionSuccessful();
                        notifyAddAlertSucceed(alertInfo);
                    } catch (Exception ex) {
                        Log.e(TAG, ex.getMessage());
                        notifyAddAlertFailed("addAlert Failed to write to disk,error:" + ex.getMessage(), alertInfo);
                    } finally {
                        db.endTransaction();
                    }
                }
            }
        });
    }

    public void deleteAlert(final AlertInfo alertInfo) {
        mExecutor.execute(new Runnable() {

            @Override
            public void run() {
                SQLiteDatabase db = AlertsDataHelper.getInstance(mContext).getWritableDatabase();
                if (db != null) {
                    try {
                        db.beginTransaction();

                        String assetWhere = AlertsDataDictionary.AlertAssetsColumns.ALERT_ID + "=?";
                        String[] assetWhereArgs = new String[] { String.valueOf(alertInfo.getId()) };
                        int rows = db.delete(AlertsDataDictionary.TABLE_ALERT_ASSETS, assetWhere, assetWhereArgs);
                        Log.i(TAG, "deleteAlert success, delete assets rows : " + rows);

                        String where = AlertsDataDictionary.AlertsColumns.ID + "=?";
                        String[] whereArgs = new String[] { String.valueOf(alertInfo.getId()) };
                        rows = db.delete(AlertsDataDictionary.TABLE_ALERTS, where, whereArgs);
                        Log.i(TAG, "deleteAlert success, delete alerts rows : " + rows);

                        db.setTransactionSuccessful();
                        notifyDeleteAlertSucceed(alertInfo);
                    } catch (Exception ex) {
                        Log.e(TAG, ex.getMessage());
                        notifyDeleteAlertFailed("deleteAlert Failed to write to disk,error:" + ex.getMessage(), alertInfo);
                    } finally {
                        db.endTransaction();
                    }
                }
            }
        });
    }

    private void notifyGetAlertsFailed(final String errorMessage) {
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onGetAlertsFailed(errorMessage);
                }
            });
        }
    }

    private void notifyGetAlertsSucceed(final List<AlertInfo> alertInfos) {
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onGetAlertsSucceed(alertInfos);
                }
            });
        }
    }

    private void notifyAddAlertFailed(final String errorMessage, final AlertInfo alertInfo) {
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onAddAlertFailed(errorMessage, alertInfo);
                }
            });
        }
    }

    private void notifyAddAlertSucceed(final AlertInfo alertInfo) {
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onAddAlertSucceed(alertInfo);
                }
            });
        }
    }


    private void notifyDeleteAlertFailed(final String errorMessage, final AlertInfo alertInfo) {
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onDelelteAlertFailed(errorMessage, alertInfo);
                }
            });
        }
    }

    private void notifyDeleteAlertSucceed(final AlertInfo alertInfo) {
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onDelelteAlertSucceed(alertInfo);
                }
            });
        }
    }
}
