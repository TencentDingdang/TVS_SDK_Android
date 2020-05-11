package com.tencent.dingdangsampleapp.alert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Date;

public class AlertReceiver extends BroadcastReceiver {

    private static final String TAG = "AlertReceiver";
    public static final String ACTION_ALARM_RECEIVED = "com.tencent.dingdangsampleapp.alert.ALARM_RECEIVED";
    public static final String EXTRA_KEY_ALARM_ID = "EXTRA_KEY_ALARM_ID";
    public static final String EXTRA_KEY_AT_TIME = "EXTRA_KEY_AT_TIME";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == ACTION_ALARM_RECEIVED) {
            try {
                String action = intent.getAction();
                String token = intent.getStringExtra(AlertReceiver.EXTRA_KEY_ALARM_ID);
                long atTimeMillis = intent.getLongExtra(AlertReceiver.EXTRA_KEY_AT_TIME, 0);
                Date date = new Date(atTimeMillis);

                Log.i(TAG, "onReceive action: " + action + ", alarmToken: " + token
                        + ", atTimeMillis:" + atTimeMillis);
                if (AlertControlManager.getInstance() != null) {
                    AlertControlManager.getInstance().startAlert(token);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
