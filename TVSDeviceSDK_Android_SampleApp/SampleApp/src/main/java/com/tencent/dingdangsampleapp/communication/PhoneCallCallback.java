package com.tencent.dingdangsampleapp.communication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import com.tencent.ai.tvs.tvsinterface.CallingInputInfo;
import com.tencent.ai.tvs.tvsinterface.CommunicationContactInfo;
import com.tencent.ai.tvs.tvsinterface.ICommunicationCallback;
import com.tencent.dingdangsampleapp.view.PhoneCallView;

import java.util.List;

public class PhoneCallCallback implements ICommunicationCallback {

    private static final String TAG = "PhoneCallView-Callback";
    private Context mContext;
    PhoneCallView phoneCallView;

    public PhoneCallCallback(Context context) {
        mContext = context;
        phoneCallView = new PhoneCallView(mContext);
    }

    @Override
    public void handleCall(String type, String token, List<CommunicationContactInfo> contacts) {
        Log.d(TAG, "handleCall type: " + type+", token: "+token+", contacts size: "+contacts.size());
        /**
         *
         * 接入方根据联系人列表contacts匹配本地通讯录
         * 如果找到多个联系人，调用TVSApi.getInstance().foundMultiContactNumber通知后台
         * 后台开启多轮会话，用户根据语音指令选择第几个号码。
         * （注：如果涉及到隐私，需要号码加密，请接入方自行处理，建议保留号码后四位，以便语音播报）
         * 如果没有找到联系人，调用TVSApi.getInstance().cannotFoundContact通知后台
         * 如果有且只有一个联系人，直接执行打电话逻辑
         *
         * */

        CommunicationContactInfo communicationContactInfo = contacts.get(0);
        String name = communicationContactInfo.getName();
        String number = communicationContactInfo.getNumber();
        Log.d(TAG, "handleCall Name : " + name +", num: " + number);
        if (!TextUtils.isEmpty(number)) {
            // TODO: 号码信息完整，接入方实现打电话
        } else {
            // TODO: 接入方根据联系人列表contacts匹配本地通讯录，Demo只做简单的模拟找到多个联系人，和未找到联系人两种情况
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    phoneCallView.setPhoneCallInfo(type, token, communicationContactInfo);
                    phoneCallView.showView();
                }
            });
        }
    }

    @Override
    public void handleAnswer(String token) {
        Log.d(TAG, "handleAnswer token: "+token);
        // TODO: 接入方处理接听指令
    }

    @Override
    public void handleHangUp(String token) {
        Log.d(TAG, "handleHangUp token: "+token);
        // TODO: 接入方处理挂断指令
    }

    @Override
    public void handleCallingInput(CallingInputInfo callInputInfo) {
        Log.d(TAG, "handleCallingInput ");
        // TODO: 通话参数，处理通话过程中的指令，如"人工服务，请按0"
        if (callInputInfo != null) {
            List<CallingInputInfo.NLPParameterInfo> parameterInfoList = callInputInfo.parameters;
            Log.d(TAG, "callInputInfo domain = "+callInputInfo.domain+" intent = "+callInputInfo.intent
                    +" parameterInfoList = "+parameterInfoList.size());
            for (CallingInputInfo.NLPParameterInfo parameterInfo : parameterInfoList) {
                Log.d(TAG, "parameterInfo name = "+parameterInfo.name+" valueList = "+parameterInfo.valueList);
            }
        }
    }
}
