package com.tencent.dingdangsampleapp.devicectrl;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.ControlApplicationInfo;
import com.tencent.ai.tvs.tvsinterface.IDeviceAbility;
import com.tencent.dingdangsampleapp.SampleApplication;
import com.tencent.dingdangsampleapp.util.PackageUtil;

import java.util.ArrayList;
import java.util.List;

public class DeviceAbilityImpl implements IDeviceAbility {

    private static final String TAG = "DeviceAbilityImpl";

    private Context mContext;

    private List<ControlApplicationInfo> mControlAppPkgList;

    public DeviceAbilityImpl(Context context) {
        mContext = context.getApplicationContext();
        // 要被控制的APP包名列表
        mControlAppPkgList = new ArrayList<>();
        // 腾讯视频
        String qqlivePkgName = "com.tencent.qqlivepad";
        mControlAppPkgList.add(new ControlApplicationInfo(qqlivePkgName, PackageUtil.getVersionCode(mContext, qqlivePkgName)));
    }

    @Override
    public void changeMicrophone(boolean open, boolean isVoice) {

    }

    @Override
    public boolean isMicrophoneOpen() {
        return false;
    }

    @Override
    public void changeWiFi(boolean open) {

    }

    @Override
    public long getBrightness() {
        return 0;
    }

    @Override
    public void setBrightness(long brightness) {

    }

    @Override
    public long getPower() {
        return 0;
    }

    @Override
    public void showRebootDialog(String dialogRequestId) {

    }

    @Override
    public void confirmReboot() {

    }

    @Override
    public void cancelReboot() {

    }

    @Override
    public void showShutdownDialog(String dialogRequestId) {

    }

    @Override
    public void confirmShutdown() {

    }

    @Override
    public void cancelShutdown() {

    }

    @Override
    public void closeMonitor() {

    }

    @Override
    public boolean isMonitorOpen() {
        return false;
    }

    @Override
    public void changeCamera(boolean open) {

    }

    @Override
    public boolean isCameraOpen() {
        return false;
    }

    @Override
    public void back() {

    }

    @Override
    public void exit() {

    }

    @Override
    public void backToHome() {

    }

    @Override
    public void openPage(String dialogRequestId, String pageInfo) {

    }

    @Override
    public void openThis() {

    }

    @Override
    public boolean enterSleepMode() {
        return false;
    }

    @Override
    public boolean exitSleepMode() {
        return false;
    }

    @Override
    public boolean isInSleepMode() {
        return false;
    }

    @Override
    public boolean changeBluetooth(boolean open) {
        return false;
    }

    @Override
    public boolean isBluetoothOpen() {
        return false;
    }

    @Override
    public void enterStandBy() {

    }

    @Override
    public void SetFixedTime(String s, String s1, long l) {

    }

    @Override
    public void ClearFixedTime() {

    }

    @Override
    public List<ControlApplicationInfo> getControlAppInfos() {
        // 获取要控制的第三方App包名列表，当云端的技能需要区分下发数据的时候才需要上报
        return mControlAppPkgList;
    }

    @Override
    public String getActiveMediaAppPkg() {
        // 获取当前活跃的媒体类App的包名，用于区分暂停、继续等播放控制
        ActiveMediaAppDemo activeMediaAppDemo = ActiveMediaAppDemo.getInstance();
        if (null != activeMediaAppDemo) {
            return activeMediaAppDemo.getTopPackageName();
        }

        return "";
    }

    @Override
    public void openUri(String uri) {
        // 打开某一个网页
    }

    @Override
    public void openIntent(String openType, Intent intent) {
        Log.i(TAG, "openIntent : " + openType + ", " + intent);

        // 打开Activity
        if ("AndroidActivity".equals(openType)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            SampleApplication.getInstance().startActivity(intent);
            // 发送广播
        } else if ("AndroidBroadcast".equals(openType)) {
            SampleApplication.getInstance().sendBroadcast(intent);
        }
    }
}
