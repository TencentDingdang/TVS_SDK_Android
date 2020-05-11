
package com.tencent.dingdangsampleapp.devicectrl;
import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.IRemoteServiceCallback;

/**
 * 当语音助手与第三方App通过aidl连接时的通信回调
 */
public class RemoteServiceCallbackImpl implements IRemoteServiceCallback {

    private static final String TAG = "RemoteServiceImpl";

    @Override
    public String[] getTvAgentAuth() {
        String[] auth = new String[2];
        // 请替换为腾讯视频分配的appId
        auth[0] = "appId";
        // 请替换为腾讯视频分配的secretKey
        auth[1] = "secretKey";

        // 返回腾讯视频分配的auth信息，用于aidl校验，只有腾讯视频需要
        return auth;
    }

    @Override
    public void onConnectedAppChanged(String appPkg) {
        Log.i(TAG, "onConnectedAppChanged : " + appPkg);

        // 当连接到SDK远程服务的App变化的通知
    }

    @Override
    public String onRemoteCall(String callingAppPkg, String method, String jsonData) {
        Log.i(TAG, "onRemoteCall : " + callingAppPkg + ", " + method + ", " + jsonData);

        // 第三方App向语音助手发送的调用

        // 如接入层不处理这次call，请返回null，SDK再判断是否处理；
        // 如接入层需要处理这次call，如该call需要返回值，则返回对应的字符串；如不需要，则返回任意字符串
        return null;
    }
}
