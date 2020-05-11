package com.tencent.dingdangsampleapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;

import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.offlinewebtemplate.IOfflineWebUniAccessCallback;
import com.tencent.ai.tvs.offlinewebtemplate.OfflineWebCallback;
import com.tencent.ai.tvs.tvsinterface.ITTSListener;
import com.tencent.dingdangsampleapp.R;
import com.tencent.dingdangsampleapp.template.TemplateHelper;
import com.tencent.dingdangsampleapp.template.data.BaseAIData;
import com.tencent.dingdangsampleapp.template.presenter.IBasePresenter;
import com.tencent.dingdangsampleapp.template.view.ui.AbstractTemplate;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

//通用模板承载的Activity
public class CommonTemplateActivity extends BaseActivity implements Handler.Callback{
    private static final String TAG = "CommonTemplateActivity";

    private IBasePresenter mPresenter;
    private ViewGroup mContentView;
    private AbstractTemplate mTemplateView;

    private static final int MSG_CLOSE_UI = 12001;
    private static final int MSG_AUTO_CLOSE_UI = 12002;
    private static final int MSG_CLOSE_UI_FORCE = 12003;
    private Handler mUIHandler;
    private boolean mIsHitBack;
    private boolean mIsTouching;
    private BaseAIData mData;
    private String mCurDialogId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_conversation);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "on create()...");
        mContentView = findViewById(R.id.conversation_container);
        mUIHandler = new Handler(Looper.getMainLooper(), this);
        switchTemplateView();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent()...");
        switchTemplateView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()... ");
//        ViewControlManager.getInstance().setTTsCallback(null);
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "on resume()...");
        if (TVSApi.getInstance() != null && TVSApi.getInstance().getDialogManager() != null) {
            TVSApi.getInstance().getDialogManager().addTTSListener(mTTSListener);
        }
        if (null != mUIHandler) {
            mUIHandler.removeMessages(MSG_CLOSE_UI);
            mUIHandler.removeMessages(MSG_AUTO_CLOSE_UI);
            mUIHandler.removeMessages(MSG_CLOSE_UI_FORCE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "on onDestroy()");
        if (TVSApi.getInstance() != null && TVSApi.getInstance().getDialogManager() != null) {
            TVSApi.getInstance().getDialogManager().removeTTSListener(mTTSListener);

            BaseAIData templateData = TemplateHelper.getInstance().getTemplateData();
            if (templateData != null && !templateData.isSessionComplete()) {
                // 停止识别
                TVSApi.getInstance().getDialogManager().stopRecognize(templateData.mDialogRequestId);
                // 退出多轮
                TVSApi.getInstance().getDialogManager().exitMultiSpeech(templateData.mDialogRequestId);
            }
        }
        mUIHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void finish() {
        super.finish();
        Log.d(TAG, "finish");
    }

    public void switchTemplateView() {
        if (null != mUIHandler) {
            mUIHandler.removeMessages(MSG_CLOSE_UI);
            mUIHandler.removeMessages(MSG_AUTO_CLOSE_UI);
            mUIHandler.removeMessages(MSG_CLOSE_UI_FORCE);
        }
        if (mPresenter != null && mPresenter.isTheSameData())
            return;

        mData = TemplateHelper.getInstance().getTemplateData();
        AbstractTemplate newView = TemplateHelper.getInstance().generateTemplate(this);
        if (newView != null) {

            if (mTemplateView != null) {// remove old
                mContentView.removeView(mTemplateView);
                mPresenter.recycleView();
            }
            //add new
            mPresenter = newView.getPresenter();
            mPresenter.bindData();
            mContentView.addView(newView);
            mTemplateView = newView;
            Log.d(TAG, "add new template view...");
        } else {
            //构造失败直接关闭
            finish();
            Log.d(TAG, "add null template view...");
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_CLOSE_UI:
                stopTTSPlay();
                mPresenter.doBack(this, true);
                break;
            case MSG_AUTO_CLOSE_UI:
                handleDoBack();
                break;
        }
        return false;
    }

    private void handleDoBack() {
        stopTTSPlay();

        long mDuration = mTemplateView.getDuration();
        Log.d(TAG, "handleDoBack mDuration = " + mDuration + " mDuration = " + mDuration);
        if (mDuration > 0) {
            mUIHandler.removeMessages(MSG_CLOSE_UI);
            mUIHandler.sendEmptyMessageDelayed(MSG_CLOSE_UI, mDuration);
        }
    }

    private void stopTTSPlay() {
        Log.d(TAG, "stopTTSPlay mCurDialogId = " + mCurDialogId + " mData.mDialogRequestId = " + mData.mDialogRequestId);
        if (TVSApi.getInstance() != null && TVSApi.getInstance().getDialogManager() != null) {
            String dialogID = mData == null ? null : mData.mDialogRequestId;
            TVSApi.getInstance().getDialogManager().stopSpeech(dialogID);
        }
    }

    // TTS播报的回调
    ITTSListener mTTSListener = new ITTSListener() {
        @Override
        public void onGetTTSText(String dialogRequestId, String text, String tag) {

        }

        @Override
        public void onTTSStarted(String dialogRequestId, String tag) {
            Log.i(TAG, "onTTSStarted : " + dialogRequestId + " tag = " + tag);
            //activity oncreate的时候收不到此回调，处理逻辑的的时候要注意
            mPresenter.OnTTSStart();
            mUIHandler.removeMessages(MSG_CLOSE_UI);
        }

        @Override
        public void onTTSFinished(String dialogRequestId, boolean complete, String tag) {
            Log.i(TAG, "onTTSFinished : " + dialogRequestId + " complete = " + complete
                    + " mCurDialogId = "+ mCurDialogId + " tag = " + tag);
            mCurDialogId = dialogRequestId;
            mPresenter.OnTTSStop();
            if(complete){
                handleDoBack();
            }else{
                mUIHandler.removeCallbacksAndMessages(null);
                mUIHandler.sendEmptyMessageDelayed(MSG_AUTO_CLOSE_UI,10000);
            }
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsTouching = true;
                mIsHitBack = false;
                if (null != mUIHandler) {
                    mUIHandler.removeMessages(MSG_CLOSE_UI);
                }
                if (mTemplateView.getBackView().getVisibility() == View.VISIBLE) { //点中back键， finish该界面
                    if (isHitView(mTemplateView.getBackView(), ev)) {
                        mIsHitBack = true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsTouching = false;
                if (mIsHitBack) {
                    mUIHandler.sendEmptyMessage(MSG_CLOSE_UI);
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    public boolean isHitView(View view, MotionEvent event) {
        boolean isHit = false;
        if (view != null) {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            float rawX = event.getRawX();
            float rawY = event.getRawY();
            if (rawX >= location[0] && rawX <= location[0] + view.getWidth() &&
                    rawY >= location[1] && rawY <= location[1] + view.getHeight()) {
                isHit = true;
            }
        }
        return isHit;
    }

    OfflineWebCallback mWebCallback = new OfflineWebCallback() {
        @Override
        public void initStatusCallback(boolean b) {

        }

        @Override
        public void onOpenMediaUi() {

        }

        @Override
        public void uniAccess(String s, String s1, String s2, String s3, IOfflineWebUniAccessCallback iOfflineWebUniAccessCallback) {

        }

        @Override
        public void requestTTS(String s) {

        }

        @Override
        public void reportAction(String s, String s1, String s2) {

        }

        @Override
        public void stopSpeech(String s) {

        }

        @Override
        public void startRecognize() {

        }

        @Override
        public void stopRecognize() {

        }
    };
}


