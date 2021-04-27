package com.tencent.dingdangsampleapp.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.ai.tvs.api.AuthManager;
import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.tvsinterface.ResultCode;
import com.tencent.dingdangsampleapp.R;
import com.tencent.dingdangsampleapp.util.CommonUtils;

public class AuthActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "AuthActivity";

    private View mAccountView;
    private View mGuestView;
    private EditText mAuthRespET;
    private EditText mClientIDET;
    private Button mAuthReqBtn;
    private Button mVisitorBtn;
    private Button mReqAuthBtn;
    private RadioGroup mRadioGroup;
    int mCurAuthType;
    private Handler mHandler;
    private TextView mAuthLogTv;
    private ProgressBar mprogressBar;
    public static final String AUTH_LOG_MSG = "auth_log_msg";
    public String mLogMsg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_layout);
        mRadioGroup = (RadioGroup)findViewById(R.id.auth_radio_operate);
        mGuestView = findViewById(R.id.guest_view);
        mAccountView = findViewById(R.id.account_view);
        mReqAuthBtn = (Button) findViewById(R.id.clientId_auth_btn);
        mAuthReqBtn = (Button) findViewById(R.id.get_authreqinfo_btn);
        mVisitorBtn = (Button) findViewById(R.id.visitor_btn);
        mReqAuthBtn.setOnClickListener(this);
        mAuthReqBtn.setOnClickListener(this);
        mVisitorBtn.setOnClickListener(this);
        //日志
        mAuthLogTv = (TextView)findViewById(R.id.log_tv);

        mAuthRespET = (EditText)findViewById(R.id.authRespInfo_et);
        mClientIDET = (EditText)findViewById(R.id.client_id_et);
        mprogressBar = (ProgressBar)findViewById(R.id.init_progressBar);
        showAuthProgress(false);

        mCurAuthType = 0;
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                Log.d(TAG, "onCheckedChanged checkedId : " + checkedId );
                if(checkedId == R.id.radio_guest) {
                    mCurAuthType = 0;
                    mGuestView.setVisibility(View.VISIBLE);
                    mAccountView.setVisibility(View.GONE);
                    printLog("访客模式下，SDK内部生成访客账号，与设备DSN一一对应", true);
                }else if(checkedId == R.id.radio_account){
                    mCurAuthType = 1;
                    mAccountView.setVisibility(View.VISIBLE);
                    mGuestView.setVisibility(View.GONE);
                    printLog("按上述1-4步骤实现账号授权，需要与手机DMSDK配合，详细流程请参考开放平台账号文档", true);
                }
            }
        });
        mHandler = new Handler(Looper.getMainLooper());
        printLog("选择账号或访客为设备授权。", true);
    }


    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick："+view.getId());
        AuthManager authManager = TVSApi.getInstance().getAuthManager();
        switch (view.getId()) {
            case R.id.get_authreqinfo_btn:
                String authReqInfo = authManager.generateAuthReqInfo();
                String result = TextUtils.isEmpty(authReqInfo) ? "获取设备授权信息为空"
                        : "将如下设备授权信息与云小微账号文档中要求的其他字段一起传给手机端的DMSDK，" +
                        "以生成ClientID，并将ClientID填入Step3，用于账号授权。\n\n" + authReqInfo;
                printLog(result, true);
                break;
            case R.id.clientId_auth_btn:
                String clientID = mClientIDET.getText().toString();
                String authCodeInfo = mAuthRespET.getText().toString();
                Log.i(TAG, "clientId_auth_btn clicked clientID ："+clientID);
                if (!TextUtils.isEmpty(clientID)) {
                    int ret = TVSApi.getInstance().getAuthManager().setClientId(clientID, authCodeInfo);
                    // 0代表参数合法，可以启动授权
                    if (ret == ResultCode.RESULT_OK) {
                        showAuthProgress(true);
                    } else {
                        handleSetClientIdRet(ret);
                    }
                }
                break;
            case R.id.visitor_btn:
                Log.i(TAG, "visitor_btn clicked");
                int ret = TVSApi.getInstance().getAuthManager().setGuestClientId();
                // 0代表参数合法，可以启动授权
                if (ret == ResultCode.RESULT_OK) {
                    showAuthProgress(true);
                } else {
                    handleSetClientIdRet(ret);
                }
                break;
            default:
                break;
        }
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
                mAuthLogTv.setText(reset ? log : mAuthLogTv.getText() + "\n" + log);
            }
        });
    }  /**
     * 处理setClientId返回码
     *
     * @param ret
     */
    private void handleSetClientIdRet(int ret) {
        Log.i(TAG, "handleSetClientIdRet : " + ret);
        switch (ret) {
            case ResultCode.RESULT_AUTH_NOT_INITED:
                printLog("错误：AuthManager还未初始化");
                break;
            case ResultCode.RESULT_AUTH_HAD_CLIENTID_ALREADY:
                printLog("错误：已存在授权信息，请先调用clear后，再次授权");
                break;
            case ResultCode.RESULT_AUTH_SET_CLIENTID_EMPTY:
                printLog("错误：传入的clientId为空");
                break;
            case ResultCode.RESULT_AUTH_PARSE_AUTHCODE_FAILED:
                printLog("错误：AuthRespInfo的json解析失败");
                break;
            case ResultCode.RESULT_AUTH_SESSIONID_INVALID:
                printLog("错误：AuthRespInfo中的sessionId已失效，请重新生成");
                break;
            default:
                printLog("授权错误码未知：" + ret);
                break;
        }
    }

    public void showAuthProgress(boolean show) {
        if (show) {
            CommonUtils.hideKeyBoard(this, mAccountView);
            mprogressBar.setVisibility(View.VISIBLE);
        } else {
            mprogressBar.setVisibility(View.GONE);
        }
    }

}
