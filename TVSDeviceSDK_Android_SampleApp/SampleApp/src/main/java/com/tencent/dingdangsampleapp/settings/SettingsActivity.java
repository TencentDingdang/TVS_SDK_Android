package com.tencent.dingdangsampleapp.settings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.Toast;

import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.tvsinterface.IRequestCallback;
import com.tencent.ai.tvs.tvsinterface.ResultCode;
import com.tencent.dingdangsampleapp.R;
import com.tencent.dingdangsampleapp.util.AppSharedPreference;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String TAG = "SettingsActivity";

    PreferenceManager mPreferenceManager;
    ListPreference mEnvListPreference;
    SwitchPreference mSanboxSwitchPreference;
    SwitchPreference mDownChannelSwitchPreference;
    SwitchPreference mSuspendSwitchPreference;
    SwitchPreference mASROnlyPreference;
    Preference mVersionPreference;
//    SwitchPreference mWakeupSwitchPreference;
//    SwitchPreference mAutoTestference;
    Preference mTestLogReportPreference;
    SwitchPreference mStatPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        mPreferenceManager = getPreferenceManager();
        mVersionPreference = mPreferenceManager.findPreference("version_num");
        mEnvListPreference = (ListPreference) mPreferenceManager.findPreference("env_switch");
        mSanboxSwitchPreference = (SwitchPreference) mPreferenceManager.findPreference("sanbox");
        mDownChannelSwitchPreference = (SwitchPreference) mPreferenceManager.findPreference("down_channel");
        mSuspendSwitchPreference = (SwitchPreference) mPreferenceManager.findPreference("suspend");
        mASROnlyPreference = (SwitchPreference) mPreferenceManager.findPreference("asr_only");
//        mWakeupSwitchPreference = (SwitchPreference) mPreferenceManager.findPreference("wakeup");
//        mAutoTestference = (SwitchPreference) mPreferenceManager.findPreference("auto_test");
        mTestLogReportPreference = mPreferenceManager.findPreference("log_report_test");
        mTestLogReportPreference.setOnPreferenceClickListener(this);
        mStatPreference = (SwitchPreference) mPreferenceManager.findPreference("stat_report");
        mPreferenceManager.findPreference("clear_auth").setOnPreferenceClickListener(this);

        setDefaultValue();

        mEnvListPreference.setOnPreferenceChangeListener(this);
        mSanboxSwitchPreference.setOnPreferenceChangeListener(this);
        mDownChannelSwitchPreference.setOnPreferenceChangeListener(this);
        mSuspendSwitchPreference.setOnPreferenceChangeListener(this);
        mASROnlyPreference.setOnPreferenceChangeListener(this);
//        mWakeupSwitchPreference.setOnPreferenceChangeListener(this);
//        mAutoTestference.setOnPreferenceChangeListener(this);
        mStatPreference.setOnPreferenceChangeListener(this);
    }


    private void setDefaultValue() {
        if (mVersionPreference != null) {
            mVersionPreference.setTitle("Version："+ TVSApi.getInstance().getSDKVersion());
        }

        if (mEnvListPreference != null) {
            int value = AppSharedPreference.getInt(this, SettingsManager.KEY_ENV, 0);
            Log.d(TAG, "setDefaultValue value = " + value);
            String summary = "";
            switch (value) {
                case 0:
                    summary = getResources().getString(R.string.env_formal);
                    break;
                case 1:
                    summary = getResources().getString(R.string.env_test);
                    break;
                case 2:
                    summary = getResources().getString(R.string.env_exp);
                    break;
                case 3:
                    summary = getResources().getString(R.string.env_dev);
                    break;
            }
            SettingsManager.getInstance().setEnv(value);
            mEnvListPreference.setValueIndex(value);
            mEnvListPreference.setSummary(summary);
        } if (mSanboxSwitchPreference != null) {
            Boolean isSandboxOpen = AppSharedPreference.getBoolean(this, SettingsManager.KEY_IS_SANDBOX_OPEN, false);
            mSanboxSwitchPreference.setChecked(isSandboxOpen);
            SettingsManager.getInstance().setSandboxEnable(isSandboxOpen);
            Log.d(TAG, "setDefaultValue isSandboxOpen = " + isSandboxOpen);
        }
        if (mDownChannelSwitchPreference != null) {
            Boolean isDownChannelOpen = AppSharedPreference.getBoolean(this, SettingsManager.KEY_IS_DOWNCHANNEL_OPEN, true);
            mDownChannelSwitchPreference.setChecked(isDownChannelOpen);
            SettingsManager.getInstance().setDownChannelEnable(isDownChannelOpen);
            Log.d(TAG, "setDefaultValue isDownChannelOpen = " + isDownChannelOpen);
        }
        if (mSuspendSwitchPreference != null) {
            Boolean isSuspend = AppSharedPreference.getBoolean(this, SettingsManager.KEY_IS_SUSPEND_OPEN, false);
            mSuspendSwitchPreference.setChecked(isSuspend);
            SettingsManager.getInstance().setSuspendOpen(isSuspend);
            Log.d(TAG, "setDefaultValue isSuspend = " + isSuspend);
        }
        if (mASROnlyPreference != null) {
            Boolean isASROnly = AppSharedPreference.getBoolean(this, SettingsManager.KEY_IS_ASR_ONLY, false);
            mASROnlyPreference.setChecked(isASROnly);
            SettingsManager.getInstance().setASROnly(isASROnly);
            Log.d(TAG, "setDefaultValue isASROnly = " + isASROnly);
        }
        /*if (mWakeupSwitchPreference != null) {
            mWakeupSwitchPreference.setChecked(true);
        }*/
        /*if (mAutoTestference != null) {
            mAutoTestference.setChecked(false);
        }*/
        if (mStatPreference != null) {
            Boolean isStatOpen = AppSharedPreference.getBoolean(this, SettingsManager.KEY_IS_STAT_OPEN, true);
            mStatPreference.setChecked(isStatOpen);
            SettingsManager.getInstance().setStatOpen(isStatOpen);
            Log.d(TAG, "setDefaultValue isStatOpen = " + isStatOpen);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange preference = " + preference + " newValue = "+newValue);
        if (preference == mEnvListPreference) {
            int value = Integer.parseInt(newValue.toString());
            String summary = "";
            switch (value) {
                case 0:
                    summary = getResources().getString(R.string.env_formal);
                    break;
                case 1:
                    summary = getResources().getString(R.string.env_test);
                    break;
                case 2:
                    summary = getResources().getString(R.string.env_exp);
                    break;
                case 3:
                    summary = getResources().getString(R.string.env_dev);
                    break;
            }

            SettingsManager.getInstance().setEnv(value);
            mEnvListPreference.setSummary(summary);
            AppSharedPreference.setInt(this, SettingsManager.KEY_ENV, value);
            Toast.makeText(SettingsActivity.this, "环境已设置为：" + summary, Toast.LENGTH_SHORT).show();
        } else if (preference == mSanboxSwitchPreference) {
            SettingsManager.getInstance().setSandboxEnable((Boolean) newValue);
            AppSharedPreference.setBoolean(this, SettingsManager.KEY_IS_SANDBOX_OPEN, (Boolean)newValue);
        } else if (preference == mDownChannelSwitchPreference) {
            SettingsManager.getInstance().setDownChannelEnable((Boolean) newValue);
            AppSharedPreference.setBoolean(this, SettingsManager.KEY_IS_DOWNCHANNEL_OPEN, (Boolean)newValue);
        } else if (preference == mSuspendSwitchPreference) {
            SettingsManager.getInstance().setSuspendOpen((Boolean) newValue);
            AppSharedPreference.setBoolean(this, SettingsManager.KEY_IS_SUSPEND_OPEN, (Boolean)newValue);
        } else if (preference == mASROnlyPreference) {
            SettingsManager.getInstance().setASROnly((Boolean) newValue);
            AppSharedPreference.setBoolean(this, SettingsManager.KEY_IS_ASR_ONLY, (Boolean)newValue);
        } else if (preference == mStatPreference) {
            SettingsManager.getInstance().setStatOpen((Boolean) newValue);
            AppSharedPreference.setBoolean(this, SettingsManager.KEY_IS_STAT_OPEN, (Boolean)newValue);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if ("log_report_test".equals(key)) {
            TVSApi.getInstance().reportLog(getApplicationContext(), new IRequestCallback() {
                @Override
                public void onRequestResult(int resultCode, String message) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (resultCode == ResultCode.RESULT_OK) {
                                Toast.makeText(getApplicationContext(), "日志上报成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "日志上报失败：" + message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        } else if ("clear_auth".equals(key)) {
            TVSApi.getInstance().getAuthManager().clear(getApplicationContext());
            // 退回到授权界面
            finish();
        }
        return false;
    }
}
