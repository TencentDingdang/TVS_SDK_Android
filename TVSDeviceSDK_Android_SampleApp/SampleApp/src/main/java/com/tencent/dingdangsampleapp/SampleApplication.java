package com.tencent.dingdangsampleapp;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.env.EnvManager;
import com.tencent.ai.tvs.offlinewebtemplate.IOfflineWebUniAccessCallback;
import com.tencent.ai.tvs.offlinewebtemplate.OfflineWebCallback;
import com.tencent.ai.tvs.offlinewebtemplate.OfflineWebManager;
import com.tencent.ai.tvs.tvsinterface.IRequestCallback;
import com.tencent.dingdangsampleapp.activity.CommonTemplateActivity;


public class SampleApplication extends Application {
    private static final String TAG = "SampleApplication";

    private static SampleApplication sApplicationContext = null;


    public static SampleApplication getInstance() {
        return sApplicationContext;
    }


    @Override
    public void onCreate() {
        Log.i(TAG, "SampleApplication onCreate");
        super.onCreate();
        sApplicationContext = this;
        // 初始化环境管理器
        EnvManager.getInstance().init(this);

        String sPath = "/sdcard/nativeweb";
        OfflineWebManager.getInstance().init(this, sPath, mWebCallback);
    }


    private OfflineWebCallback mWebCallback = new OfflineWebCallback() {

        @Override
        public void initStatusCallback(boolean isSuccess) {
            Log.i(TAG, "OfflineWebManager initStatusCallback isSuccess=" + isSuccess);
        }

        @Override
        public void onOpenMediaUi() {
            Log.i(TAG, "OfflineWebManager onOpenMediaUi");
            Intent intent = new Intent(SampleApplication.getInstance(), CommonTemplateActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        @Override
        public void uniAccess(String domain, String intent, String jsonBlobInfo, String tag, IOfflineWebUniAccessCallback callback) {

        }

        @Override
        public void requestTTS(String text) {
            TVSApi.getInstance().getDialogManager().requestTTS(text);
        }

        @Override
        public void reportAction(String domain, String type, String param) {
            Log.i(TAG, "OfflineWebManager reportAction domain=" + domain + " type=" + type + " param=" + param);
            TVSApi.getInstance().sendActionTriggeredEvent(domain, type, param, new IRequestCallback() {
                @Override
                public void onRequestResult(int errorCode, String errorMsg) {
                    Log.i(TAG, "OfflineWebManager onRequestResult errorCode=" + errorCode + " errorMsg=" + errorMsg);

                }
            });
        }

        @Override
        public void stopSpeech(String dialogRequestId) {
            TVSApi.getInstance().getDialogManager().stopSpeech(dialogRequestId);
        }

        @Override
        public void startRecognize() {
            TVSApi.getInstance().getDialogManager().startRecognize();
        }

        @Override
        public void stopRecognize() {
            TVSApi.getInstance().getDialogManager().stopRecognize();
        }
    };

}
