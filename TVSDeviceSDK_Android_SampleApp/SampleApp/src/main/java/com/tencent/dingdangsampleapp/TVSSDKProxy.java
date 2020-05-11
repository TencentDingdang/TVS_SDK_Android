package com.tencent.dingdangsampleapp;

import android.content.Context;
import android.util.Log;

import com.tencent.ai.tvs.api.DialogManager;
import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.capability.userinterface.data.ASRTextMessageBody;
import com.tencent.ai.tvs.capability.userinterface.data.UIDataMessageBody;
import com.tencent.ai.tvs.tvsinterface.DialogOptions;
import com.tencent.ai.tvs.tvsinterface.IAuthInfoListener;
import com.tencent.ai.tvs.tvsinterface.IMediaPlayer;
import com.tencent.ai.tvs.tvsinterface.IRecognizeListener;
import com.tencent.ai.tvs.tvsinterface.ITTSListener;
import com.tencent.ai.tvs.tvsinterface.IUIDataListener;
import com.tencent.ai.tvs.tvsinterface.ResultCode;
import com.tencent.dingdangsampleapp.alert.AlertControlManager;
import com.tencent.dingdangsampleapp.communication.PhoneCallCallback;
import com.tencent.dingdangsampleapp.customdata.CustomDataCallback;
import com.tencent.dingdangsampleapp.customskill.CustomSkillCallback;
import com.tencent.dingdangsampleapp.mediamanager.MediaPlayManager;
import com.tencent.dingdangsampleapp.player.DingdangMediaPlayer;
import com.tencent.dingdangsampleapp.player.TestMediaPlayer;
import com.tencent.dingdangsampleapp.record.VoiceRecord;
import com.tencent.dingdangsampleapp.util.PackageUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TVSSDKProxy {
    private static final String TAG = "TVSSDKProxy";

    private String mAppKey, mAccessToken, mDsn;
    /**
     * 录音实例
     * 注：目前要保证是同一个，否则会出现资源抢占问题
     */
    private VoiceRecord mVoiceRecord;

    private PhoneCallCallback mPhoneCallCallback;
    private CustomSkillCallback mCustomSkillCallback;
    private CustomDataCallback mCustomDataCallback;
    private Context mContext;
    private IAuthInfoListener mAuthInfoListener;
    private TVSSDKProxyListener mListener;

    public TVSSDKProxy(Context context, String appKey, String accessToken, String dsn, IAuthInfoListener authInfoListener) {
        mContext = context;
        mAppKey = appKey;
        mAccessToken = accessToken;
        mDsn = dsn;
        mAuthInfoListener = authInfoListener;
    }

    /**
     * 初始化SDK
     */
    public int initSDK() {

        // 云端注册成功，开始初始化SDK
        TVSApi tvsApi = TVSApi.getInstance();

        // TODO 这里可以替换成自己实现的媒体播放器
        TestMediaPlayer mediaPlayer = new TestMediaPlayer(mContext);

        // 语音发起的媒体的播放器接口实现，需要正确处理接口，并回调播放器状态
        IMediaPlayer dingdangMediaPlayer = new DingdangMediaPlayer(mediaPlayer);
        MediaPlayManager.getInstance().setMediaPlayer(mediaPlayer);

        // 设置设备版本号（VN），必须是 x.x.x.x 格式，每一段必须为数字，且新版本的版本号必须比旧版本大
        // 该参数会与设备开放平台的应用版本进行匹配，用于后台确定下发哪些技能
        // 当需要区分终端版本下发不同技能的时候生效，映射规则为设备开放平台上配置的比VN小的最大的应用版本号
        Map<String, String> quaMap = new HashMap<>();
        quaMap.put(TVSApi.QUA_KEY_VN, "1.0.0.2");
        // 上报应用版本号，辅助查问题
        quaMap.put(TVSApi.QUA_KEY_APPVN, PackageUtil.getVersionName(mContext, mContext.getPackageName()));

        Log.d(TAG, "initSDK mAppKey is ：" + mAppKey + " mAccessToken : " + mAccessToken
                + " mDsn : " + mDsn);
        // 初始化TVSApi
        DefaultPluginProvider defaultPluginProvider = new DefaultPluginProvider(mContext, dingdangMediaPlayer);
        int result = tvsApi.init(defaultPluginProvider, mContext, mAppKey, mAccessToken,
                mDsn, mAuthInfoListener, quaMap);
        return result;
    }

    /**
     * 初始化DialogManager
     */
    public int initDialogManager(String modelPath, boolean enableWakeup) {
        // 初始化语音识别
        DialogManager dialogManager = TVSApi.getInstance().getDialogManager();
        if (dialogManager == null) {
            return -999;
        }
        // 初始化语音识别模块，ret为0表示初始化成功，非0待细化（一般为so、模型加载失败）
        int ret = dialogManager.init(modelPath, enableWakeup);
        // 设置录音的实现
        mVoiceRecord = new VoiceRecord();
        dialogManager.setOuterAudioRecorder(mVoiceRecord);
        if (ret == ResultCode.RESULT_OK) {
            // 先静音，等设备授权成功后再启动收音
            dialogManager.disableVoiceCapture();

            // 设置语音识别状态变化的listener
            dialogManager.addRecognizeListener(mRecognizeListener);
            // 设置语音合成状态变化的listener
            dialogManager.addTTSListener(mTTSListener);
            // 设置UI数据的listener
            dialogManager.addUIDataListener(mUIDataListener);

//            // 注册通用媒体指令的回调（收藏状态变化、播放模式变化、下载）
//            mMediaPlayerView.registerMediaListener();
            // 实现通话接口
            mPhoneCallCallback = new PhoneCallCallback(mContext);
            TVSApi.getInstance().addCommunicationListener(mPhoneCallCallback);
            // 实现自定义技能接口
            mCustomSkillCallback = new CustomSkillCallback();
            TVSApi.getInstance().addCustomSkillHandler(mCustomSkillCallback);
            // 实现自定义数据
            mCustomDataCallback = new CustomDataCallback();
            TVSApi.getInstance().addCustomDataHandler(mCustomDataCallback);

            // TODO 需要启动远程服务，控制第三方App，再开启
             /*如果要控制腾讯视频等媒体类的App，需要上报当前活跃的媒体App包名，用于区分播放控制的响应
             ActiveMediaAppDemo是一个示例，通过媒体类应用互斥的方式，接入方也可以修改成自己的实现*/
//            ActiveMediaAppDemo.enableAccessibility();
            // 如果要控制腾讯视频等第三方App，需要开启SDK的远程服务，不需要可以删除
//            TVSApi.getInstance().openRemoteService(new RemoteServiceCallbackImpl());

        }
        return ret;
    }

    /**
     * 启动语音识别
     *
     * @return dialogRequestId 本次会话的唯一id，后续结果的回调均会携带此id
     */
    public void startRecognize() {
        DialogManager dialogManager = TVSApi.getInstance().getDialogManager();
        if (dialogManager != null) {
            DialogOptions dialogOptions = new DialogOptions();
            dialogOptions.tag = "demo_test_user_tag";
            // 如果要保存音频
            dialogManager.startRecognize(IRecognizeListener.RECO_TYPE_MANUAL, dialogOptions,null);
        }
    }

    /**
     * 文字识别
     *
     * @param text 需要识别的文本
     * @return dialogRequestId 会话id
     */
    public String startTextRecognize (String text) {
        String dialogRequestId = null;
        DialogManager dialogManager = TVSApi.getInstance().getDialogManager();
        if (dialogManager != null) {
            dialogRequestId = dialogManager.startTextRecognize(text);
        }
        return dialogRequestId;
    }

    /**
     * 请求合成一段TTS
     *
     * @param text 要合成的文字
     *
     * @return dialogRequestId 会话的id
     */
    public String requestTTS (String text) {
        String dialogRequestId = null;
        DialogManager dialogManager = TVSApi.getInstance().getDialogManager();
        if (dialogManager != null) {
            dialogRequestId = dialogManager.requestTTS(text);
        }
        return dialogRequestId;
    }

    public void setTVSSDKProxyListener(TVSSDKProxyListener listener) {
        mListener = listener;
    }


    /**
     * 语音识别流程的状态回调
     */
    IRecognizeListener mRecognizeListener = new IRecognizeListener() {

        @Override
        public void onRecognizationStart(int recoType, String dialogRequestId, String tag) {
            Log.i(TAG, "onRecognizationStart : " + dialogRequestId + ", tag : " + tag );
            //demo在启动语音识别的时候，停止正在响铃的闹钟，接入方可根据闹钟UI，自行调用停止闹钟。
            if (AlertControlManager.getInstance() != null
                    && AlertControlManager.getInstance().isAlertPlaying()) {
                Log.i(TAG, "stopPlayingAlert ");
                AlertControlManager.getInstance().stopPlayingAlert();
            }
        }

        @Override
        public void onStartRecord(int recoType, String dialogRequestId, String tag) {
            Log.i(TAG, "onStartRecord : " + dialogRequestId + " tag = " + tag);
            if (mListener != null) {
                mListener.printLog("开始录音", false);
            }

        }

        @Override
        public void onSpeechStart(int recoType, String dialogRequestId, String tag) {
            Log.i(TAG, "onSpeechStart : " + dialogRequestId + " tag = " + tag);
            // 没有本地VAD，这个回调不会触发
            if (mListener != null) {
                mListener.printLog("检测到说话开始", false);
            }
        }

        @Override
        public void onSpeechEnd(int recoType, String dialogRequestId, String tag) {
            Log.i(TAG, "onSpeechEnd : " + dialogRequestId + " tag = " + tag);
            if (mListener != null) {
                mListener.printLog("检测到说话结束", false);
            }

        }

        @Override
        public void onFinishRecord(int recoType, String dialogRequestId, String tag) {
            Log.i(TAG, "onFinishRecord : " + dialogRequestId + " tag = " + tag);

            if (mListener != null) {
                mListener.printLog("结束录音", false);
            }

        }

        @Override
        public void onGetASRText(String dialogRequestId, String asrText, boolean isFinal, String status,
                                 ASRTextMessageBody.UserInfo userInfo, List<ASRTextMessageBody.AsrClassifierInfo> asrClassifierInfos) {
            Log.i(TAG, "onGetASRText : " + dialogRequestId + ", asrText : " + asrText
                    + ", isFinal : " + isFinal + ", status : " + status);
            if (mListener != null) {
                mListener.onGetASRText(dialogRequestId, asrText, isFinal, status, userInfo, asrClassifierInfos);
            }
        }

        @Override
        public void onGetResponse(int recoType, String dialogRequestId, String tag) {
            Log.i(TAG, "onGetResponse : " + dialogRequestId + " tag = " + tag);
            if (mListener != null) {
                mListener.printLog("收到服务器数据", false);
            }
        }

        @Override
        public void onRecognizationFinished(int recoType, String dialogRequestId, String sessionId, String tag) {
            Log.i(TAG, "onRecognizationFinished : " + dialogRequestId + ", sessionId : " + sessionId + " tag = " + tag);
            if (mListener != null) {
                mListener.printLog("会话结束", false);
            }
        }

        @Override
        public void onVolume(int volume) {
            //Log.i(TAG, "onVolume : " + volume);
        }

        @Override
        public void onRecognizeError(int errorCode, String errorMessage, int recoType, String dialogRequestId, String tag) {
            Log.i(TAG, "onRecognizeError : " + dialogRequestId + ", errorCode : " + errorCode + ", msg : " + errorMessage + " tag = " + tag);
            if (mListener != null) {
                mListener.printLog("出现错误，错误码：" + errorCode + ", errorMessage : " + errorMessage, false);
            }
        }

        @Override
        public void onRecognizeCancel(int recoType, String dialogRequestId, String tag) {
            Log.i(TAG, "onRecognizeCancel : " + dialogRequestId + " tag = " + tag);
        }

        @Override
        public void onSaveRecord(String tag, String recordPath) {
            Log.i(TAG, "onSaveRecord : " + tag + ", " + recordPath);
        }
    };

    /**
     * TTS播报的回调
     */
    ITTSListener mTTSListener = new ITTSListener() {
        @Override
        public void onGetTTSText(String dialogRequestId, String text, String tag) {
            Log.i(TAG, "onGetTTSText : " + dialogRequestId + " tag = " + tag + ", text = " + text);
        }

        @Override
        public void onTTSStarted(String dialogRequestId, String tag) {
            Log.i(TAG, "onTTSStarted : " + dialogRequestId + " tag = " + tag);

            if (mListener != null) {
                mListener.printLog("语音播报开始", false);
            }
        }

        @Override
        public void onTTSFinished(String dialogRequestId, boolean complete, String tag) {
            Log.i(TAG, "onTTSFinished : " + dialogRequestId + " tag = " + tag);
            if (mListener != null) {
                mListener.printLog("语音播报结束", false);
            }
        }
    };

    /**
     * UI模版的回调
     */
    IUIDataListener mUIDataListener = new IUIDataListener() {
        @Override
        public void onGetUIData(String tag, String dialogRequestId, UIDataMessageBody uiData, boolean reportEnd) {
            Log.i(TAG, "onGetUIData : " + dialogRequestId + " tag = " + tag + ", UI : " + uiData.jsonUI.data);
            if (mListener != null) {
                mListener.handleTvsUiData(dialogRequestId, uiData, tag);
            }
        }
    };


    public void release() {
        TVSApi tvsApi = TVSApi.getInstance();
        tvsApi.removeCommunicationListener(mPhoneCallCallback);
        tvsApi.removeCustomSkillHandler(mCustomSkillCallback);
        tvsApi.removeCustomDataHandler(mCustomDataCallback);

        DialogManager dialogManager = tvsApi.getDialogManager();
        if (null != dialogManager) {
            dialogManager.setOuterAudioRecorder(null);
            dialogManager.removeRecognizeListener(mRecognizeListener);
            dialogManager.removeTTSListener(mTTSListener);
            dialogManager.removeUIDataListener(mUIDataListener);
        }
        tvsApi.release();
    }


    /**
     * Demo示例，接入方可根据自身需求处理回调
     * */
    public interface TVSSDKProxyListener {

        /**
         * 打印回调信息
         * */
        void printLog(String logText, boolean reset);

        /**
         * 处理UI数据
         *
         * @param dialogRequestId
         * @param uiData
         * @param tag*/
        void handleTvsUiData(String dialogRequestId, UIDataMessageBody uiData, String tag);

        /**
         * 获取到ASR文字
         * */
        void onGetASRText(String dialogRequestId, String asrText, boolean isFinal, String status,
                          ASRTextMessageBody.UserInfo userInfo, List<ASRTextMessageBody.AsrClassifierInfo> asrClassifierInfos);
    }
}
