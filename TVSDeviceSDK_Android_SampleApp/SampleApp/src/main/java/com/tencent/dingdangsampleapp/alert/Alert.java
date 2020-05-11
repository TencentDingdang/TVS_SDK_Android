package com.tencent.dingdangsampleapp.alert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.AlertInfo;
import com.tencent.dingdangsampleapp.util.TimeUtils;

import java.text.ParseException;
import java.util.Date;

/**
 * 单个闹钟
 */

public class Alert {
    private static final String TAG = "Alert";
    private final AlertInfo alertInfo;
    private Context mContext;
    private AlarmManager mAlarmManager;
    private PendingIntent mPendingIntent = null;
    private static int mRequestCode = 0;

    public Alert(final AlertInfo alertInfo, Context context) {
        super();
        mContext = context;
        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        this.alertInfo = alertInfo;
    }

    /**
     * 设置一个闹钟
     */
    public boolean set() {
        String scheduledTime = alertInfo.getScheduledTime();
        if (!TextUtils.isEmpty(scheduledTime)) {
            try {
                Date date = TimeUtils.toDate(scheduledTime);
                long timeAtMills = date.getTime();
                if(timeAtMills < System.currentTimeMillis()) return false;
                Log.i(TAG, "Set alert, token: " + alertInfo.getToken() + ", at:" + scheduledTime);

                PendingIntent pendingIntent = createPendingintent(alertInfo.getToken(), timeAtMills);
                if (Build.VERSION.SDK_INT < 19) {
                    mAlarmManager.set(AlarmManager.RTC_WAKEUP, timeAtMills, pendingIntent);
                } else {
                    mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, timeAtMills, pendingIntent);
                }
                mPendingIntent = pendingIntent;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 取消一个闹钟
     */
    public void cancel() {
        Log.i(TAG, "cancel alert token: " + alertInfo.getToken() + ", atTime: " + alertInfo.getScheduledTime());
        if (mPendingIntent != null) {
            mAlarmManager.cancel(mPendingIntent);
        }
    }

    public AlertInfo getAlertInfo() {
        return alertInfo;
    }

    private PendingIntent createPendingintent(String alarmToken, long atTimeMillis) {
        Intent intent = new Intent(AlertReceiver.ACTION_ALARM_RECEIVED);
        intent.setComponent(new ComponentName(mContext, AlertReceiver.class));
        intent.putExtra(AlertReceiver.EXTRA_KEY_ALARM_ID, alarmToken);
        intent.putExtra(AlertReceiver.EXTRA_KEY_AT_TIME, atTimeMillis);

        int requestCode = ++mRequestCode;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.i(TAG, "createPendingintent token: " + alertInfo.getToken() + ", requestCode: " + requestCode);
        return pendingIntent;
    }
}
