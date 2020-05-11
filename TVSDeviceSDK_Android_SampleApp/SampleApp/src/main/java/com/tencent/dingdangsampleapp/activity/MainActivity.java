package com.tencent.dingdangsampleapp.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.ai.tvs.api.DialogManager;
import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.capability.userinterface.data.ASRTextMessageBody;
import com.tencent.ai.tvs.capability.userinterface.data.UIDataMessageBody;
import com.tencent.ai.tvs.gateway.data.GatewayRespHeader;
import com.tencent.ai.tvs.tvsinterface.AuthResultCode;
import com.tencent.ai.tvs.tvsinterface.IAuthInfoListener;
import com.tencent.ai.tvs.tvsinterface.IContactUploadCallback;
import com.tencent.ai.tvs.tvsinterface.ResultCode;
import com.tencent.dingdangsampleapp.R;
import com.tencent.dingdangsampleapp.TVSSDKProxy;
import com.tencent.dingdangsampleapp.settings.SettingsActivity;
import com.tencent.dingdangsampleapp.settings.SettingsManager;
import com.tencent.dingdangsampleapp.template.UIDataManager;
import com.tencent.dingdangsampleapp.template.data.BaseAIData;
import com.tencent.dingdangsampleapp.util.CommonUtils;
import com.tencent.dingdangsampleapp.view.MediaPlayerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener, TVSSDKProxy.TVSSDKProxyListener {

    private static final String TAG = "MainActivity";

    /** 启动时申请权限 */
    private static final int PERMISSIONS_REQUEST_PERMISSIONS = 1;

    //APP_KEY，ACCESS_TOKEN, 填入开放平台申请应用的key和secret
    public final String APP_KEY = "key_test";
    public final String ACCESS_TOKEN = "secret_test";
    // DSN，设置唯一标识，这里需要换成真实的
    public final String DSN = "test_dsn";

    // 模型路径，写死
    private String mModelPath = "/sdcard/tencent/aifile";

    private TextView mLogTv;
    private Handler mHandler;

    private MediaPlayerView mMediaPlayerView;

    private View mTextRecognizeView;
    private EditText mTextRecognizeET;
    private Button mText2VoiceBtn;
    private Button mText2SemanticBtn;

    /**
     * 进行授权的按钮
     */
    private Button mGoAuthBtn;
    /**
     * 清除授权的按钮
     */
    private Button mClearAuthBtn;
    /**
     * 重试授权的按钮
     */
    private Button mRetryAuthBtn;

    TVSSDKProxy mTvsProxy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 检查权限
        // 目前SDK依赖录音和写SD卡权限，如获取不到,SDK无法正常使用
        boolean allPermissionsGranted = checkPermissions();
        Log.i(TAG, "onCreate allPermissionsGranted : " + allPermissionsGranted);

        setContentView(R.layout.activity_main);

        // 录音按钮
        findViewById(R.id.speech_recognize_btn).setOnClickListener(this);
        // 运行信息输出
        mLogTv = (TextView)findViewById(R.id.log_tv);
        // 播放器页面
        mMediaPlayerView = (MediaPlayerView)findViewById(R.id.media_player);

        mTextRecognizeView = findViewById(R.id.text_recognize_view);
        mTextRecognizeET = (EditText) findViewById(R.id.text_2_semantic_edit);
        mText2VoiceBtn = (Button) findViewById(R.id.text_2_voice_btn);
        mText2SemanticBtn = (Button) findViewById(R.id.text_2_semantic_btn);
        mText2VoiceBtn.setOnClickListener(this);
        mText2SemanticBtn.setOnClickListener(this);

        mGoAuthBtn = findViewById(R.id.go_auth_btn);
        mGoAuthBtn.setOnClickListener(this);
        mClearAuthBtn = findViewById(R.id.clear_auth_btn);
        mClearAuthBtn.setOnClickListener(this);
        mRetryAuthBtn = findViewById(R.id.retry_auth_btn);
        mRetryAuthBtn.setOnClickListener(this);


        mHandler = new Handler(Looper.getMainLooper());

        // 如果所有权限均有，可以初始化SDK
        if (allPermissionsGranted) {
            initSDK();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();

        if (mMediaPlayerView != null) {
            mMediaPlayerView.stopMedia();
        }
        if (mTvsProxy != null) {
            mTvsProxy.release();
        }
    }
    private void initSDK() {

        if (mTvsProxy == null) {
            mTvsProxy = new TVSSDKProxy(getApplicationContext(), APP_KEY, ACCESS_TOKEN, DSN, mAuthInfoListener);
        }
        mTvsProxy.setTVSSDKProxyListener(this);
        SettingsManager.getInstance().initEnvValue();
        //初始化SDK
        int initSDKResult = mTvsProxy.initSDK();
        if (initSDKResult == ResultCode.RESULT_OK) {
            //初始化DialogManager
            int ret = initDialogManager();
            Log.d(TAG, "initSDK initDialogManager ret ：" + ret);
        } else if (initSDKResult == ResultCode.RESULT_SDK_INIT_PARAM_EMPTY) {
            Log.e(TAG, "initSDK error ：必填参数未填");
        } else if (initSDKResult == ResultCode.RESULT_SDK_INIT_ALREADY) {
            Log.e(TAG, "initSDK error ：重复初始化");
        }
        SettingsManager.getInstance().initSettingsValue();
        Log.d(TAG, "initSDK end Env is ：" + TVSApi.getInstance().getEnv()
                + " sandbox is: " + TVSApi.getInstance().getSandBoxEnable());
    }
    /**
     * 授权结果通知
     */
    IAuthInfoListener mAuthInfoListener = new IAuthInfoListener() {
        @Override
        public void onMissingClientId(boolean isInit) {
            Log.i(TAG, "onMissingClientId isInit : " + isInit);
            DialogManager dialogManager = TVSApi.getInstance().getDialogManager();
            if (null != dialogManager) {
                dialogManager.disableVoiceCapture();
            }
            printLog("未授权状态，请执行授权流程", true);

            // 本地无授权信息，此时需要执行授权操作
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // 跳转授权的按钮可用
                    mGoAuthBtn.setVisibility(View.VISIBLE);
                    // 清除授权的按钮隐藏
                    mClearAuthBtn.setVisibility(View.GONE);
                    // 重试授权的按钮隐藏
                    mRetryAuthBtn.setVisibility(View.GONE);
                }
            });
        }

        @Override
        public void onInitTokenSucceed(String grantType) {
            Log.i(TAG, "onInitTokenSucceed ：" + grantType);
            DialogManager dialogManager = TVSApi.getInstance().getDialogManager();
            // 设备授权成功，启动收音！
            if (null != dialogManager) {
                dialogManager.enableVoiceCapture();
            }

            // 云端授权成功，授权信息可以正常使用
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // 如果有授权页，则关闭
                    finishActivity(10086);
                    // 跳转授权的按钮隐藏
                    mGoAuthBtn.setVisibility(View.GONE);
                    // 清除授权的按钮可用
                    mClearAuthBtn.setVisibility(View.VISIBLE);
                    // 重试授权的按钮隐藏
                    mRetryAuthBtn.setVisibility(View.GONE);
                }
            });
            printLog("授权成功, authorization : " + TVSApi.getInstance().getAuthManager().getAuthorization()
                    +"\n当前环境：" + TVSApi.getInstance().getEnv()
                    + ", 是否开启沙箱：" + TVSApi.getInstance().getSandBoxEnable(), true);

        }

        @Override
        public void onInitTokenFailed(String grantType, int errorCode, String errorMsg, GatewayRespHeader gatewayRespHeader) {
            Log.e(TAG, "onInitTokenFailed ：" + grantType + ", errorCode : " + errorMsg + ", errorMsg : " + errorMsg);
            // 云端授权失败，此时SDK内部已经存储了clientId等授权信息，可以等有网的时候重试

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // 如果有授权页，则关闭
                    finishActivity(10086);
                    // 跳转授权的按钮隐藏，原因为SDK已经存储了clientId（访客SDK内部生成）
                    // 如需重新走授权流程，可以先清除授权
                    mGoAuthBtn.setVisibility(View.GONE);
                    // 清除授权的按钮可用
                    mClearAuthBtn.setVisibility(View.VISIBLE);
                    // 重试授权的按钮隐藏
                    mRetryAuthBtn.setVisibility(View.VISIBLE);
                }
            });

            String message = "错误码：" + errorCode + "，错误信息：" + errorMsg;
            if (grantType == IAuthInfoListener.GRANT_TYPE_AUTHORIZATION) {
                message += "\n\n云端授权失败：";
            } else {
                message += "\n\n云端刷票失败（票据有可能会失效）";
            }

            if (errorCode == AuthResultCode.RESULT_SEND_REQUEST_FAILED) {
                message += "\n发起请求失败，请在网络良好时重试授权，\n也可以退出授权重走授权流程";
            } else {
                message += "\n服务端返回错误，请查看错误信息进行下一步处理，\n如需要重新走授权流程可先退出授权，\n如要重试请点击重试授权";
            }

            printLog(message, true);
        }
    };


    private int initDialogManager() {
        // 初始化语音识别
        int ret = -999;
        Log.d(TAG, "initSDK isAutoTestMode");
        if (mTvsProxy != null) {
            // 初始化语音识别模块，ret为0表示初始化成功，非0待细化（一般为so、模型加载失败）
            ret = mTvsProxy.initDialogManager(mModelPath, true);
        }
        return ret;
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick："+view.getId());
        switch (view.getId()) {
            case R.id.speech_recognize_btn:// 手动启动识别
                startRecognize();
                break;
            case R.id.text_2_voice_btn: // 启动文字转语音

                if (mTvsProxy != null) {
                    String text = mTextRecognizeET.getText().toString();
                    if (!TextUtils.isEmpty(text)) {
                        printLog("启动文字转语音："+text);
                        mTvsProxy.requestTTS(text);
                        CommonUtils.hideKeyBoard(this, this.mTextRecognizeView);
                    }
                }
                break;
            case R.id.text_2_semantic_btn: // 启动文字转语义
                if (mTvsProxy != null) {
                    String text = mTextRecognizeET.getText().toString();
                    if (!TextUtils.isEmpty(text)) {
                        printLog("启动文字转语义："+text);
                        mTvsProxy.startTextRecognize(text);
                        CommonUtils.hideKeyBoard(this, this.mTextRecognizeView);
                    }
                }
                break;
            case R.id.go_auth_btn:
                Intent intent = new Intent(MainActivity.this, AuthActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityIfNeeded(intent, 10086);
                break;
            case R.id.clear_auth_btn:
                boolean cleared = TVSApi.getInstance().getAuthManager().clear(this);
                if (cleared) {
                    printLog("账号已清除");
                } else {
                    printLog("没有账号需要清除");
                }
                break;
            case R.id.retry_auth_btn:
                boolean reqExecuted = TVSApi.getInstance().getAuthManager().reqAuthIfNeeded();
                Log.i(TAG, "retry_btn clicked reqExecuted ："+reqExecuted);
                break;
            default:
                break;
        }
    }

    private void startRecognize() {
        printLog("启动语音识别..", true);
        if (mTvsProxy != null) {
            //启动识别
            mTvsProxy.startRecognize();
        }
    }

    @Override
    public void handleTvsUiData(String dialogRequestId, UIDataMessageBody uiData, String tag) {

        // 真正的UI数据
        Log.i(TAG, "handleTvsUiData data = " + uiData);
        UIDataManager.handleUiData(dialogRequestId, uiData,true, new UIDataManager.IUIDataCallback() {
            @Override
            public void onUIDataResult(BaseAIData uiData) {
                Log.i(TAG, "onUIDataResult() uiData = [" + uiData + "]");
                if (uiData != null) {
                    MainActivity.this.startActivity(new Intent(MainActivity.this,
                            CommonTemplateActivity.class));
                }
            }
        });
    }

    @Override
    public void onGetASRText(String dialogRequestId, String asrText, boolean isFinal, String status, ASRTextMessageBody.UserInfo userInfo, List<ASRTextMessageBody.AsrClassifierInfo> asrClassifierInfos) {

        Log.i(TAG, "onGetASRText : " + dialogRequestId + ", asrText : " + asrText
                + ", isFinal : " + isFinal + ", status : " + status);

        printLog("识别文字结果：" + asrText + (isFinal ? ", 识别完毕" : ""));
    }

    /**
     * 启动的时候检查录音、写SD卡权限，如果没有就退出
     */
    private boolean checkPermissions() {
        boolean allPermissionsGranted = true;

        List<String> permissionsToRequest = new ArrayList<String>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            allPermissionsGranted = false;
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            allPermissionsGranted = false;
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            allPermissionsGranted = false;
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]),
                    PERMISSIONS_REQUEST_PERMISSIONS);
        }

        return allPermissionsGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        boolean allPermissionsGranted = true;
        if (requestCode == PERMISSIONS_REQUEST_PERMISSIONS) {

            for (int i = 0; i < permissions.length; i++) {
                Log.i(TAG, "onRequestPermissionsResult : " + permissions[i] + " - " + grantResults[i]);
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                }
            }
        }
        Log.i(TAG, "onRequestPermissionsResult allPermissionsGranted : " + allPermissionsGranted);

        if (!allPermissionsGranted) {
            Toast.makeText(MainActivity.this, "some permissions denied, exit", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            initSDK();

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void printLog(final String log) {
        printLog(log, false);
    }

    /**
     * 界面打印
     *
     * @param log
     */
    public void printLog(final String log, final boolean reset) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mLogTv.setText(reset ? log : mLogTv.getText() + "\n" + log);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean checked = item.isChecked();

        // 自动结束录音模式
        if (id == R.id.setting) {
            // 手动结束录音模式
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (id == R.id.text_2_voice) {
            // 文字转语音
            mTextRecognizeView.setVisibility(View.VISIBLE);
            mText2VoiceBtn.setVisibility(View.VISIBLE);
            mText2SemanticBtn.setVisibility(View.GONE);
        } else if (id == R.id.text_2_semantic) {
            // 文字转语义
            mTextRecognizeView.setVisibility(View.VISIBLE);
            mText2VoiceBtn.setVisibility(View.GONE);
            mText2SemanticBtn.setVisibility(View.VISIBLE);
        }else if (id == R.id.speech_recognize) {
            // 默认，语音识别
            if (mTextRecognizeView.getVisibility() == View.VISIBLE) {
                mTextRecognizeView.setVisibility(View.GONE);
            }
        } else if (id == R.id.upload_contacts) {
            // 上传联系人
            List<String> contacts = new ArrayList<>();
            contacts.add("艾米达镁铝");
            contacts.add("荪验资");
            contacts.add("无艳组");
            TVSApi.getInstance().uploadContactList(contacts, mContactUploadCallback);
        } else if (id == R.id.updata_entities) {
            // 上传实体库
            startActivity(new Intent(MainActivity.this, UniAccessActivity.class));
        } else if (id == R.id.updata_state) {
            // 自定义数据通路
            startActivity(new Intent(MainActivity.this, CustomDataActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    IContactUploadCallback mContactUploadCallback = new IContactUploadCallback() {
        @Override
        public void onUploadContactResult(int resultCode, String errorMsg) {
            Log.i(TAG, "onUploadContactResult resultCode : " + resultCode + ", errorMsg = "+errorMsg);
            String msg;
            if (resultCode == ResultCode.RESULT_OK) {
                msg = "上传联系人成功";
            } else {
                msg = "上传联系人失败，resultCode : " + resultCode + ", errorMsg = "+errorMsg;
            }
            printLog(msg);

        }
    };

}
