package com.tencent.dingdangsampleapp.settings;

import android.util.Log;

import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.api.TVSConfig;
import com.tencent.ai.tvs.tvsinterface.Env;
import com.tencent.dingdangsampleapp.SampleApplication;
import com.tencent.dingdangsampleapp.util.AppSharedPreference;

public class SettingsManager {

    private static final String TAG = "SettingsActivity";

    public static final String KEY_ENV = "key_env";
    public static final String KEY_IS_SANDBOX_OPEN = "key_is_sandbox_open";
    public static final String KEY_IS_DOWNCHANNEL_OPEN = "key_is_downchannel_open";
    public static final String KEY_IS_SUSPEND_OPEN = "key_is_suspend_open";
    public static final String KEY_IS_ASR_ONLY = "key_is_asr_only";
    public static final String KEY_RECO_MODEL = "key_reco_model";
    public static final String KEY_IS_STAT_OPEN = "key_is_stat_open";

    private volatile static SettingsManager mInstance = null;

    public static SettingsManager getInstance() {
        if (mInstance == null) {
            synchronized (SettingsManager.class) {
                if (mInstance == null) {
                    mInstance = new SettingsManager();
                }
            }
        }
        return mInstance;
    }

    public void initEnvValue() {

        int value = AppSharedPreference.getInt(SampleApplication.getInstance().getApplicationContext(),
                KEY_ENV, 0);

        TVSApi.getInstance().initEnv(getEnvByValue(value));

        Log.d(TAG, "initEnvValue Env = " + value);
    }


    public void initSettingsValue() {

        Boolean isSandboxOpen = AppSharedPreference.getBoolean(SampleApplication.getInstance().getApplicationContext(),
                KEY_IS_SANDBOX_OPEN, false);
        setSandboxEnable(isSandboxOpen);

        Boolean isDownChannelOpen = AppSharedPreference.getBoolean(SampleApplication.getInstance().getApplicationContext(),
                KEY_IS_DOWNCHANNEL_OPEN, true);
        setDownChannelEnable(isDownChannelOpen);

        Boolean isASROnly = AppSharedPreference.getBoolean(SampleApplication.getInstance().getApplicationContext(),
                KEY_IS_ASR_ONLY, false);
        setASROnly(isASROnly);

        Boolean isStatOpen = AppSharedPreference.getBoolean(SampleApplication.getInstance().getApplicationContext(),
                KEY_IS_STAT_OPEN, true);
        setStatOpen(isStatOpen);

        Log.d(TAG, "initSettingsValue isSandboxOpen = " + isSandboxOpen + " isDownChannelOpen = " + isDownChannelOpen
                + " isASROnly = " + isASROnly + " isStatOpen = " + isStatOpen);
    }

    public Env getEnvByValue(int envValue) {
        Env env = Env.FORMAL;
        switch (envValue) {
            case 0:
                env = Env.FORMAL;
                break;
            case 1:
                env = Env.TEST;
                break;
            case 2:
                env = Env.EXP;
                break;
            case 3:
                env = Env.DEV;
                break;
        }
        return env;
    }

    public void setEnv(int envValue) {
        TVSApi.getInstance().setEnv(getEnvByValue(envValue));
    }

    public void setSandboxEnable(boolean enable) {
        TVSApi.getInstance().setSandBoxEnable(enable);
    }

    public void setDownChannelEnable(boolean enable) {
        TVSApi.getInstance().setDownChannelEnable(enable);
    }

    public void setSuspendOpen(boolean enable) {
        if (enable) {
            TVSApi.getInstance().suspendActivity(0);
        } else {
            TVSApi.getInstance().resumeActivity();
        }
    }


    public void setASROnly(boolean asrOnly) {
        TVSApi.getInstance().getDialogManager().setASROnly(asrOnly);
    }
    public void setStatOpen(boolean open) {
        TVSConfig.setStatOpen(open);
    }
}
