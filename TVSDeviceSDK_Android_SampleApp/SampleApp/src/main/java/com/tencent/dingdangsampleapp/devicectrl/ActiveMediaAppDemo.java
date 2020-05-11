package com.tencent.dingdangsampleapp.devicectrl;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.tvsinterface.IDeviceAbility;
import com.tencent.dingdangsampleapp.SampleApplication;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 获取当前活跃的媒体类App的demo
 *
 * 用途：多播放场景（云端播放音乐、腾讯视频App等），如需进行播放控制，如暂停、继续、下一个等，
 * 需要上报当前正在活跃的媒体类App的包名给后台，后台根据包名区分下发的指令
 * @see IDeviceAbility#getActiveMediaAppPkg()
 *
 * <p/>
 * demo采用的为【App互斥】的方式
 * 1. 确定一个包名列表，需要进行语音控制的媒体类App
 * 2. 缓存一个局部变量包名，代表当前活跃的媒体类App
 * 3. 通过辅助模式，监听App的启动（需要系统默认给辅助模式授权）
 * 4. 当有新的媒体类App启动，或语音助手有云端媒体（比如音乐，广播）播放，则kill当前缓存的App（依赖系统权限），并记录新的App包名
 *
 * <p/>
 * 接入方可自己实现获取当前活跃的媒体类App的方案
 * PS：Android默认的MediaSession是最优方案，但是由于不是每个App都正确实现了MediaSession，所以demo未采用
 */
public class ActiveMediaAppDemo extends AccessibilityService {

    private static final String TAG = "ActiveMediaAppDemo";

    private static ActiveMediaAppDemo instance;

    public static ActiveMediaAppDemo getInstance(){
        return instance;
    }

    // 当前活跃的媒体类App包名
    private String mTopPackageName;

    // 定义的媒体类App的列表（用于互斥逻辑）
    private List<String> mMediaAppPkgList;

    @Override
    public void onCreate() {
        super.onCreate();

        // 目前仅控制腾讯视频，与语音助手的音乐/广播等云端媒体互斥
        mMediaAppPkgList = new ArrayList<>();
        mMediaAppPkgList.add("com.tencent.qqlivepad");

        instance = this;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        Log.i(TAG, "onServiceConnected");

        // 设置辅助模式的服务，用于检测Activity窗口变化，进一步获取某个App被启动了
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        if (Build.VERSION.SDK_INT >= 16)
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        setServiceInfo(config);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (null == event || null == event.getPackageName()) {
            Log.e(TAG, "onAccessibilityEvent event is null");
            return;
        }

        String pkgName = event.getPackageName().toString();
        Log.i(TAG, "onAccessibilityEvent, eventType : " + event.getEventType() + ", pkgName : " + pkgName);

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            if(pkgName == null || event.getClassName() == null){
                return;
            }

            // 当mMediaAppPkgList内的另一个App启动，则kill当前的App
            if (!TextUtils.equals(pkgName, mTopPackageName)
                    && mMediaAppPkgList.contains(pkgName)) {
                removeTopPackageName();
                mTopPackageName = pkgName;
            }
        }
    }

    @Override
    public void onInterrupt() {

    }

    /**
     * Kill当前的媒体类App
     */
    public void removeTopPackageName() {
        Log.i(TAG, "removeTopPackageName : " + mTopPackageName);

        if (!TextUtils.isEmpty(mTopPackageName)) {
            // TODO kill应用需要系统权限
            killProcess(SampleApplication.getInstance(), mTopPackageName);

            mTopPackageName = null;
        }
    }

    /**
     * 获取当前的媒体类App
     *
     * @return
     */
    public String getTopPackageName() {
        Log.i(TAG, "getTopPackageName : " + mTopPackageName);
        // 如果当前mTopPackageName已经不再运行了，则置空
        if (!isAppRunning(mTopPackageName)) {
            mTopPackageName = null;
        }

        return mTopPackageName;
    }

    /**
     * 某个App是否在运行中
     * 需要系统签名，以及android.permission.REAL_GET_TASKS
     */
    private boolean isAppRunning(String pkgName) {
        if (TextUtils.isEmpty(mTopPackageName)) {
            return false;
        }

        ActivityManager am = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
        Log.i(TAG, "isAppRunning : " + processInfos.size());

        for (ActivityManager.RunningAppProcessInfo info : processInfos) {
            if (TextUtils.equals(pkgName, info.processName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * kill某一个应用
     * 需要系统签名，以及android.permission.FORCE_STOP_PACKAGES
     *
     * @param context
     * @param packageName
     * @return
     */
    private static boolean killProcess(Context context, String packageName){
        Log.d(TAG, "kill process:"+packageName);

        // 先看SDK是否可以让App安全退出
        boolean killedBySDK = TVSApi.getInstance().exitAppIfNeeded(packageName);
        if (killedBySDK) {
            return true;
        }

        // 再用系统强杀
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        try {
            Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            method.invoke(am, packageName);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 默认开启辅助模式服务
     * 需要系统签名，以及android.permission.FORCE_STOP_PACKAGES
     *
     */
    public static void enableAccessibility(){
        try {
            Application application = SampleApplication.getInstance();
            boolean ret = Settings.Secure.putString(application.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    application.getPackageName() + "/com.tencent.dingdangsampleapp.devicectrl.ActiveMediaAppDemo");
            Log.d(TAG, "enableAccessibility, ret : " + ret);
        } catch (Exception e) {
            Log.d(TAG, "enableAccessibility, Exception : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
