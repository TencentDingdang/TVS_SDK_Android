package com.tencent.dingdangsampleapp.alert;

import android.content.Context;
import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.AlertBaseInfo;
import com.tencent.ai.tvs.tvsinterface.AlertInfo;
import com.tencent.ai.tvs.tvsinterface.IAlertAbility;

import java.util.List;

/**
 * 闹钟能力的实现
 */
public class AlertAbilityImp implements IAlertAbility {
    private static final String TAG = "AlertAbilityImp";
    private Context mContext;
    private AlertControlManager alertControlManager;

    public AlertAbilityImp(Context context) {
        mContext = context;
        alertControlManager = new AlertControlManager(mContext);

    }

    @Override
    public void setAlert(AlertInfo alertInfo) {
        Log.d(TAG, "setAlert alertInfo = "+ alertInfo);
        if (alertControlManager == null) {
            alertControlManager = AlertControlManager.getInstance();
        }
        alertControlManager.addAlert(alertInfo, false);
    }

    @Override
    public void deleteAlert(String alertToken) {
        Log.d(TAG, "deleteAlert alertToken = "+alertToken);
        if (alertControlManager == null) {
            alertControlManager = AlertControlManager.getInstance();
        }
        alertControlManager.deleteAlert(alertToken);
    }

    @Override
    public void deleteAllAlerts() {
        Log.d(TAG, "deleteAllAlerts");
        if (alertControlManager == null) {
            alertControlManager = AlertControlManager.getInstance();
        }
        alertControlManager.clearLocalAlerts();
    }

    @Override
    public List<AlertBaseInfo> getAllAlerts() {
        Log.d(TAG, "getAllAlerts");
        if (alertControlManager == null) {
            alertControlManager = AlertControlManager.getInstance();
        }
        return alertControlManager.getAllAlertsInfo();
    }

    @Override
    public List<AlertBaseInfo> getActiveAlerts() {
        Log.d(TAG, "getActiveAlerts");
        if (alertControlManager == null) {
            alertControlManager = AlertControlManager.getInstance();
        }
        return alertControlManager.getActiveAlertsInfo();
    }

    @Override
    public void addListener(IHandleAlertListener listener) {
        if (listener != null) {
            Log.d(TAG, "addListener listener = "+listener.getClass());
            if (alertControlManager == null) {
                alertControlManager = AlertControlManager.getInstance();
            }
            alertControlManager.addHandleAlertListener(listener);
        }
    }

    @Override
    public void removeListener(IHandleAlertListener listener) {
        if (listener != null) {
            Log.d(TAG, "removeListener listener = "+listener.getClass());
            if (alertControlManager == null) {
                alertControlManager = AlertControlManager.getInstance();
            }
            alertControlManager.removeHandleAlertListener(listener);
        }
    }

}
