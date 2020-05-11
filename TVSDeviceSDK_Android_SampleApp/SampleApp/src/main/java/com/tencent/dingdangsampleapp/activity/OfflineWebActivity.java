package com.tencent.dingdangsampleapp.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.tencent.ai.tvs.offlinewebtemplate.OfflineWebConstants;
import com.tencent.dingdangsampleapp.R;
import android.widget.RelativeLayout;

import com.tencent.ai.tvs.offlinewebtemplate.OfflineWebManager;
import com.tencent.ai.tvs.web.util.AndroidBug5497Workaround;
import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.web.tms.ITMSWebViewJsListener;
import com.tencent.ai.tvs.web.tms.TMSWebView;
import com.tencent.dingdangsampleapp.mediamanager.MediaPlayManager;

import org.json.JSONArray;
import org.json.JSONObject;

public class OfflineWebActivity extends Activity {
    private String TAG = "DMSDK_OfflineWebActivity";

    private TMSWebView mWebView;
    private RelativeLayout mNativeWebViewLayout;
    private ImageView mBackbtn;
    private String sCurJason;
    private String sCurTid;
    private String sDialogRequestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mWebView = OfflineWebManager.getInstance().getLoadWebView();
        if (null != mWebView) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            mWebView.setLayoutParams(params);
            mWebView.registerService("DDApi", mJSListener);
        }

        setContentView(R.layout.nativeweb_layout);
        mNativeWebViewLayout = (RelativeLayout) findViewById(R.id.native_webview_layout);
        mBackbtn = (ImageView) findViewById(R.id.native_webview_backbtn);
        mBackbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                removeWebviewParent();
            }
        });

        if (null != mNativeWebViewLayout && null != mWebView) {
            removeWebviewParent();
            mNativeWebViewLayout.addView(mWebView, 0);
        }

        //当前加载的ui jason 数据
        sCurJason = OfflineWebManager.getInstance().getLoadJasonData();
        //当前加载的模版tid 信息
        sCurTid = OfflineWebManager.getInstance().getCurTid();
        sDialogRequestId = OfflineWebManager.getInstance().mDialogRequestId;
        Log.d(TAG, "onCreate sCurTid = "+sCurTid+" sCurJason = "+sCurJason+" sDialogRequestId = "+sDialogRequestId);
        AndroidBug5497Workaround.assistActivity(this);
        OfflineWebManager.getInstance().setWebActivity(sDialogRequestId,this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在onResume 必须注册当前的Activity对象。
        OfflineWebManager.getInstance().setWebActivity(sDialogRequestId,this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.v(TAG, "OfflineWebActivity-----onNewIntent");
        //前加载的ui jason 数据
        sCurJason = OfflineWebManager.getInstance().getLoadJasonData();
        //当前加载的模版id tid 信息
        sCurTid = OfflineWebManager.getInstance().getCurTid();
        sDialogRequestId = OfflineWebManager.getInstance().mDialogRequestId;
    }
	
    private void removeWebviewParent() {
        if (null != mWebView && null != (ViewGroup) mWebView.getParent()) {
            ((ViewGroup) mWebView.getParent()).removeView(mWebView);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "OfflineWebActivity-----onDestroy");
        if (mWebView != null) {
            removeWebviewParent();
            OfflineWebManager.getInstance().destroyWebView(mWebView);
            mWebView = null;
        }
        //在onDestroy 中，必须反注册当前的Activity 对象。
        OfflineWebManager.getInstance().setWebActivity(sDialogRequestId,null);
        if (isNeedCloseAudio()) {
            Log.d(TAG, "OfflineWebActivity-----onDestroy isNeedCloseAudio");
            MediaPlayManager.getInstance().handlePause(null);
        }
        Log.d(TAG, "OfflineWebActivity-----onDestroy sDialogRequestId=" + sDialogRequestId);
        if (!TextUtils.isEmpty(sDialogRequestId)) {
            TVSApi.getInstance().getDialogManager().stopSpeech(sDialogRequestId);
            TVSApi.getInstance().getDialogManager().exitMultiSpeech(sDialogRequestId);
        }
        super.onDestroy();
    }

    private boolean isNeedCloseAudio() {
        boolean isNeed = false;
        if (!TextUtils.isEmpty(sCurJason)) {
            try {
                JSONObject mObj = new JSONObject(sCurJason);
                if (null != mObj) {
                    JSONArray mArray = mObj.optJSONArray("listItems");
                    String sUrl = null;
                    String sBgAudioUrl = null;
                    if (null != mArray) {
                        JSONObject mItemObj = (JSONObject) mArray.get(0);
                        if (null != mItemObj) {
                            sUrl = mItemObj.optJSONObject("audio").optJSONObject("stream").optString("url");
                        }
                    }

                    JSONObject mGlobalInfo = mObj.optJSONObject("globalInfo");
                    if (null != mGlobalInfo) {
                        JSONObject bgAudio = mGlobalInfo.optJSONObject("backgroundAudio");
                        if (null != bgAudio) {
                            JSONObject mStream = bgAudio.optJSONObject("stream");
                            if (null != mStream) {
                                sBgAudioUrl = mStream.optString("url");
                            }
                        }
                    }

                    if (!TextUtils.equals(sCurTid, "50002") &&
                            !TextUtils.equals(sCurTid, "50001") &&
                            (!TextUtils.isEmpty(sUrl) || !TextUtils.isEmpty(sBgAudioUrl))
                    ) {
                        isNeed = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return isNeed;
    }

    //详细的示例代码参考WebView SDK demo中openNativeWebActivity方法和OfflineWebActivity类
    ITMSWebViewJsListener mJSListener = new ITMSWebViewJsListener() {

        @Override
        public boolean onJsCallNativeFunc(String funcName, JSONObject funcParam) {
            Log.d(TAG, "onJsCallNativeFunc funcName = " + funcName + ", funcParam = " + funcParam);
            if (OfflineWebConstants.JS_CMD_FINISHACT.equals(funcName)) {
                finish();
                removeWebviewParent();
                return true;
            }else if (OfflineWebConstants.JS_CMD_PROXYDATA.equals(funcName)) {
                // TODO: 2020-03-13 接入方自行处理
                return true;
            } else if (OfflineWebConstants.JS_CMD_SETTINGS.equals(funcName)) {
                // TODO: 2020-03-13 接入方自行处理
                return true;
            }
            //web化模版需要处理的一些webview的回调
            if (OfflineWebManager.getInstance().handleJsCallNative(mWebView, funcName, funcParam)) {
                return true;
            }

            return false;
        }

    };

}