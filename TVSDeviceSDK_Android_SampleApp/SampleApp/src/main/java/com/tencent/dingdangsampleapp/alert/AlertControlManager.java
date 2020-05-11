package com.tencent.dingdangsampleapp.alert;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.AlertBaseInfo;
import com.tencent.ai.tvs.tvsinterface.AlertInfo;
import com.tencent.ai.tvs.tvsinterface.IAlertAbility;
import com.tencent.ai.tvs.tvsinterface.IMediaPlayerListener;
import com.tencent.dingdangsampleapp.SampleApplication;
import com.tencent.dingdangsampleapp.player.TestMediaPlayer;
import com.tencent.dingdangsampleapp.util.TimeUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class AlertControlManager implements Handler.Callback, AlertsDataManager.AlertDataListener {

    private static final String TAG = "AlertControlManager";
    // 默认闹钟音频
    private static final String DEFAULT_ALARM_RING = "asset:///alarm_clock.mp3";
    // 计时器到时
    private static final int MSG_RING_FADE_TIMER_HEARTBEAT = 1;
    // 闹钟过期时间
    private static final int ALERT_EXPIRES_TIME = 30 * 60 * 1000;
    //闹钟响铃周期
    private static final int ALARM_RING_DURATION = 3 * 60 * 1000;
    //提醒响铃周期
    private static final int REMINDER_DURATION = 10 * 1000;

    private volatile static AlertControlManager mInstance;
    private Context mContext;
    // 所有闹钟
    private final Map<String, Alert> alertsMap;
    //处于活跃状态的的闹钟
    private final List<String> activeAlerts;
    private final AlertsDataManager mDataManager;
    // 闹钟播放器
    private TestMediaPlayer mediaPlayer;
    //正在响铃的闹钟
    private AlertInfo mRingingAlertInfo = null;
    //记录当前响铃的次数
    private long currentRingCount = 0;
    //正在响铃闹钟的token
    private String mCurrAlertToken;
    //正在播放的铃声
    private String mCurrRingID;
    //正在删除的闹钟
    Alert mAlertDeleting;
    // 是否正在播放响铃
    private boolean mAlertPlaying;

    private Handler mWorkerHandler;
    private static final Object mListenerLock = new Object();
    List<IAlertAbility.IHandleAlertListener> mHandleListeners;


    public static AlertControlManager getInstance() {
        if (mInstance == null) {
            synchronized (AlertControlManager.class) {
                if( mInstance == null ){
                    mInstance = new AlertControlManager(SampleApplication.getInstance().getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    public AlertControlManager(Context context) {
        mContext = context;

        // 闹铃播放器，需要正确处理接口，并回调播放器状态
        mediaPlayer = new TestMediaPlayer(mContext);
        if (null != mediaPlayer) {
            this.mediaPlayer.addMediaPlayerListener(mediaPlayerListener);
        }

        this.alertsMap = new ConcurrentHashMap<>();
        this.activeAlerts = new ArrayList<>();
        mDataManager = new AlertsDataManager(mContext);
        mDataManager.setAlertDataListener(this);

        //加载DB中alerts
        loadAllAlerts();

        HandlerThread workerThread = new HandlerThread("AlertControlManagerWorker");
        workerThread.start();
        mWorkerHandler = new Handler(workerThread.getLooper(), this);

        synchronized (mListenerLock) {
            this.mHandleListeners = Collections.synchronizedList(new ArrayList<IAlertAbility.IHandleAlertListener>());
        }
        mInstance = this;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_RING_FADE_TIMER_HEARTBEAT) {
            onTime();
        }
        return true;
    }


    /**
     * 开始闹钟响铃
     */
    public void startAlert(String alertToken) {
        Log.i(TAG, "startAlert token: " + alertToken+" mCurrAlertToken = "+mCurrAlertToken);

        if(!TextUtils.isEmpty(mCurrAlertToken) && !mCurrAlertToken.equals(alertToken)) {
            stopAlertNow(mCurrAlertToken);
        }
        currentRingCount = 0;
        mCurrAlertToken = alertToken;

        Alert alert = alertsMap.get(alertToken);
        mRingingAlertInfo = alert.getAlertInfo();
        String assets = mRingingAlertInfo.getBackgroundAsset();
        Log.d(TAG,"startAlert type : " + alert.getAlertInfo().getType()        //类型：闹钟/提醒
                +" LoopCount: "+ alert.getAlertInfo().getLoopCount()                //循环次数
                +" LoopPauseMillis: "+ alert.getAlertInfo().getLoopPauseMillis()    //循环间隔时间
                +" mCurrAlertToken : " + mCurrAlertToken
                +" time: "+ alert.getAlertInfo().getScheduledTimeUnix()             
                +" assets = "+assets);

        Map<String, String> listAssets = mRingingAlertInfo.getAssets();
        mRingingAlertInfo.setRingUrl(listAssets.get(assets));
        mRingingAlertInfo.setRingStartTime(0);
        playAlertRing(mCurrAlertToken);
    }


    /**
     * 停止闹钟响铃
     */
    private void stopAlertNow(String alertToken) {
        Log.i(TAG, "stopAlertNow : " + alertToken);
        if (mRingingAlertInfo != null) {
            if (mRingingAlertInfo.getToken().equals(alertToken)) {
                mRingingAlertInfo = null;
                activeAlerts.remove(alertToken);
                alertsMap.remove(alertToken);
                synchronized (mListenerLock) {
                    for (IAlertAbility.IHandleAlertListener listener : mHandleListeners) {
                        listener.onAlertStopped(alertToken);
                    }
                }
            }
        }
        stopTimer();
        mAlertPlaying = false;
        if (mediaPlayer != null) {
            mediaPlayer.stop(alertToken);
        }
        mCurrAlertToken = null;
        mCurrRingID = null;
    }

    private void startTimer(long duration) {
        Log.i(TAG,"startTimer : " + duration);
        mWorkerHandler.removeMessages(MSG_RING_FADE_TIMER_HEARTBEAT);
        mWorkerHandler.sendEmptyMessageDelayed(MSG_RING_FADE_TIMER_HEARTBEAT, duration);
    }

    private void stopTimer() {
        mWorkerHandler.removeMessages(MSG_RING_FADE_TIMER_HEARTBEAT);
    }

    /**
     * 闹钟计时器到时
     */
    private void onTime() {
        Log.i(TAG,"onTime");
        if(mRingingAlertInfo != null) {
            if(mAlertPlaying) {
                mAlertPlaying = false;
                if (mediaPlayer != null) {
                    // 暂停播放
                    mediaPlayer.pause(mCurrAlertToken);
                }
                // 循环次数+1
                currentRingCount++;
                Log.d(TAG,"curCount : " + currentRingCount+" LoopCount = "+mRingingAlertInfo.getLoopCount());
                if(currentRingCount >= mRingingAlertInfo.getLoopCount()) {
                    // 循环播放结束
                    stopAlertNow(mCurrAlertToken);
                } else {
                    // 重新计时
                    int duration = mRingingAlertInfo.getLoopPauseMillis();
                    Log.d(TAG,"重新开始计时: pauseTime: " + duration);
                    if (duration > 0) {
                        startTimer(mRingingAlertInfo.getLoopPauseMillis());
                    } else {
                        startTimer(REMINDER_DURATION);
                    }
                }
            } else {
                Log.d(TAG,"继续播放,开始计时: getLoopPauseMillis = "+ mRingingAlertInfo.getLoopPauseMillis());
                startTimer(ALARM_RING_DURATION);
                mAlertPlaying = true;
                if (mediaPlayer != null) {
                    String url = getCurrRingUrl();
                    if (url != null) {
                        Log.d(TAG,"getToken: " + mRingingAlertInfo.getToken() + " url = "+ url);
                        mediaPlayer.setSource(mRingingAlertInfo.getToken(), url);
                        mediaPlayer.play(mRingingAlertInfo.getToken());
                    }
                }
            }
        }
    }

    private String getCurrRingUrl() {
        if(mRingingAlertInfo != null) {
            String url = mRingingAlertInfo.getRingUrl();
            if(TextUtils.isEmpty(url)) {
                url = DEFAULT_ALARM_RING;
            }
            return url;
        }
        return null;
    }

    private long getCurrRingStartTime() {
        if(mRingingAlertInfo != null) {
            return mRingingAlertInfo.getRingStartTime();
        }
        return 0;
    }

    /**
     * 播放闹钟铃声
     */
    private void playAlertRing(final String alertToken) {
        Log.d(TAG,"playAlertRing : " + alertToken);
        Alert alert = alertsMap.get(alertToken);
        if (alert != null) {
            mRingingAlertInfo = alert.getAlertInfo();
            activeAlerts.add(alertToken);

            currentRingCount = 0;
            mAlertPlaying = true;
            if (mediaPlayer != null) {
                String url = getCurrRingUrl();
                startTimer(ALARM_RING_DURATION);
                if (url != null) {
                    Log.d(TAG,"当前播放url : " + url+" getCurrRingStartTime = "+getCurrRingStartTime());
                    // 设置铃声
                    mediaPlayer.setSource(alertToken, url);
                    if(getCurrRingStartTime() != 0) {
                        mediaPlayer.seekTo(alertToken, (int)getCurrRingStartTime());
                    }
                    mediaPlayer.play(alertToken);
                }
            }
            synchronized (mListenerLock) {
                for (IAlertAbility.IHandleAlertListener listener : mHandleListeners) {
                    Log.d(TAG,"listener = "+ listener.getClass());
                    listener.onAlertStarted(alertToken);
                }
            }
        }
    }

    /**
     * 从DB读取设置过的闹钟
     */
    private void loadAllAlerts() {
        mDataManager.getAllAlerts();
    }

    /**
     * 是否有闹钟正在播放
     */
    public boolean isAlertPlaying() {
        return mAlertPlaying;
    }

    /**
     * 停止正在播放的闹钟
     */
    public void stopPlayingAlert() {
        Log.d(TAG,"stopPlayingAlert mCurrAlertToken = "+ mCurrAlertToken);
        if (mCurrAlertToken != null) {
            stopAlertNow(mCurrAlertToken);
        }
    }

    /**
     * 获取所有闹钟
     */
    public synchronized List<AlertBaseInfo> getAllAlertsInfo() {
        List<AlertBaseInfo> all = new ArrayList<>(alertsMap.size());
        for (Alert alert : alertsMap.values()) {
            AlertInfo alertInfo = alert.getAlertInfo();
            AlertBaseInfo alertBaseInfo = new AlertBaseInfo(alertInfo.getToken(), alertInfo.getType(), alertInfo.getScheduledTime());
            all.add(alertBaseInfo);
        }
        Log.d(TAG, "getAllAlertsInfo: " + all.size());
        return all;
    }

    /**
     * 获取活跃的闹钟
     */
    public synchronized List<AlertBaseInfo> getActiveAlertsInfo() {
        List<AlertBaseInfo> all = new ArrayList<>(alertsMap.size());
        List<AlertBaseInfo> active = new ArrayList<>(activeAlerts.size());
        for (Alert alert : alertsMap.values()) {
            AlertInfo alertInfo = alert.getAlertInfo();
            AlertBaseInfo alertBaseInfo = new AlertBaseInfo(alertInfo.getToken(), alertInfo.getType(), alertInfo.getScheduledTime());
            all.add(alertBaseInfo);

            if (activeAlerts.contains(alertInfo.getToken())) {
                AlertBaseInfo activeAlertBaseInfo = new AlertBaseInfo(alertInfo.getToken(), alertInfo.getType(), alertInfo.getScheduledTime());
                active.add(activeAlertBaseInfo);
            }
        }
        Log.d(TAG, "getActiveAlertsInfo: " + active.size());
        return active;
    }

    /**
     * 设置一个闹钟
     *
     * @param alertInfo
     * @param suppressEvent 是否需要上报事件
     */
    public synchronized void addAlert(final AlertInfo alertInfo, final boolean suppressEvent) {
        Log.d(TAG, "addAlert alertToken: " + alertInfo.getToken());
        final Alert alert = new Alert(alertInfo, mContext);
        if (alert.set()) {
            alertsMap.put(alertInfo.getToken(), alert);
            // 需要上报事件的说明是新闹钟，也是需要写数据库的
            if (!suppressEvent && mDataManager != null) {
                mDataManager.addAlert(alertInfo);
            }
        } else if (!suppressEvent) {
            synchronized (mListenerLock) {
                for (IAlertAbility.IHandleAlertListener listener : mHandleListeners) {
                    listener.onAlertSet(alertInfo.getToken(), false);
                }
            }
        }
    }

    /**
     * 删除闹钟列表
     */
    private void abandonAlert(List<AlertInfo> deletedAlertInfos) {
        Log.d(TAG, "abandonAlert");
        for(AlertInfo alertInfo : deletedAlertInfos) {
            if (mDataManager != null) {
                mDataManager.deleteAlert(alertInfo);
            }
            synchronized (mListenerLock) {
                for (IAlertAbility.IHandleAlertListener listener : mHandleListeners) {
                    listener.onAlertStopped(alertInfo.getToken());
                }
            }
        }
    }

    /**
     * 删除一个闹钟
     *
     * @param alertToken alertToken
     */
    public synchronized void deleteAlert(final String alertToken) {
        Log.d(TAG, "delete alertToken: " + alertToken);
        mAlertDeleting = alertsMap.remove(alertToken);
        if (mAlertDeleting != null) {
            final AlertInfo alertInfo = mAlertDeleting.getAlertInfo();
            mDataManager.deleteAlert(alertInfo);
        } else {
            //  本地没有查询到就上报删除失败的事件
            Log.d(TAG, "delete alert is  null");
            synchronized (mListenerLock) {
                for (IAlertAbility.IHandleAlertListener listener : mHandleListeners) {
                    listener.onAlertDeleted(alertToken, false);
                }
            }
        }
    }

    public void clearLocalAlerts() {
        Log.i(TAG, "clearLocalAlerts");
        stopActiveAlerts();
        activeAlerts.clear();

        for (String token : alertsMap.keySet()) {
            if (token != null) {
                deleteAlert(token);
            }
        }
        alertsMap.clear();
    }

    public synchronized void stopActiveAlerts() {
        Log.i(TAG, "stopActiveAlerts");
        Set<String> alertsActive = new HashSet<>(activeAlerts);
        for (String token : alertsActive) {
            stopAlertNow(token);
        }
    }

    @Override
    public void onGetAlertsSucceed(List<AlertInfo> alertInfos) {
        Log.d(TAG, "onGetAlertsSucceed alertInfos = "+ alertInfos);
        if (alertInfos != null && alertInfos.size() > 0) {
            List<AlertInfo> deletedAlertInfos = new ArrayList<>();
            for(final AlertInfo alertInfo : alertInfos) {
                String scheduledTimeStr = alertInfo.getScheduledTime();
                if (!TextUtils.isEmpty(scheduledTimeStr)) {
                    try {
                        long time = TimeUtils.toDate(scheduledTimeStr).getTime();
                        //超过闹钟时间半小时则丢弃
                        if (System.currentTimeMillis() > time + ALERT_EXPIRES_TIME) {
                            deletedAlertInfos.add(alertInfo);
                        } else {
                            addAlert(alertInfo, true);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            abandonAlert(deletedAlertInfos);
        }
    }

    @Override
    public void onGetAlertsFailed(String errMsg) {

    }

    @Override
    public void onAddAlertSucceed(AlertInfo alertInfo) {
        Log.d(TAG, "onAddAlertSucceed alertToken = "+ alertInfo.getToken());
        synchronized (mListenerLock) {
            for (IAlertAbility.IHandleAlertListener listener : mHandleListeners) {
                listener.onAlertSet(alertInfo.getToken(), true);
            }
        }
    }

    @Override
    public void onAddAlertFailed(String errMsg, AlertInfo alertInfo) {
        Log.d(TAG, "onAddAlertFailed errMsg : " + errMsg+" alertToken = "+ alertInfo.getToken());
        synchronized (mListenerLock) {
            for (IAlertAbility.IHandleAlertListener listener : mHandleListeners) {
                listener.onAlertSet(alertInfo.getToken(), false);
            }
        }
        Alert alert = alertsMap.remove(alertInfo.getToken());
        alert.cancel();
        if (activeAlerts.contains(alertInfo.getToken())) {
            stopAlertNow(alertInfo.getToken());
        }
    }

    @Override
    public void onDelelteAlertFailed(String errMsg, AlertInfo alertInfo) {
        Log.d(TAG, "onDelelteAlertFailed  errMsg = "+errMsg);
        synchronized (mListenerLock) {
            for (IAlertAbility.IHandleAlertListener listener : mHandleListeners) {
                listener.onAlertDeleted(alertInfo.getToken(), false);
            }
        }
    }

    @Override
    public void onDelelteAlertSucceed(AlertInfo alertInfo) {
        Log.d(TAG, "onDelelteAlertSucceed alertInfo = " + alertInfo.getToken());
        if (mAlertDeleting != null) {
            mAlertDeleting.cancel();
            mAlertDeleting = null;
        }
        if (activeAlerts.contains(alertInfo.getToken())) {
            stopAlertNow(alertInfo.getToken());
        }
        synchronized (mListenerLock) {
            for (IAlertAbility.IHandleAlertListener listener : mHandleListeners) {
                listener.onAlertDeleted(alertInfo.getToken(), true);
            }
        }
    }

    public void removeHandleAlertListener(IAlertAbility.IHandleAlertListener listener) {
        synchronized (mListenerLock) {
            if (mHandleListeners.contains(listener)) {
                mHandleListeners.remove(listener);
            }
        }
    }


    public void addHandleAlertListener(IAlertAbility.IHandleAlertListener listener) {
        synchronized (mListenerLock) {
            if (!mHandleListeners.contains(listener)) {
                this.mHandleListeners.add(listener);
            }
        }
    }

    private IMediaPlayerListener mediaPlayerListener = new IMediaPlayerListener() {
        @Override
        public void onInit(String mediaId) {
            Log.i(TAG,"onInit mediaId = " + mediaId);
        }

        @Override
        public void onPlaying(String mediaId) {
            Log.i(TAG,"onPlaying mediaId = " + mediaId);
        }

        @Override
        public void onPaused(String mediaId) {
            Log.i(TAG,"onPaused mediaId = " + mediaId);
        }

        @Override
        public void onCompletion(String mediaId) {
            if (!mAlertPlaying) {
                return;
            }
            Log.i(TAG,"onCompletion mediaId = " + mediaId);
            if(mRingingAlertInfo != null) {
                String url = mRingingAlertInfo.getRingUrl();
                Log.d(TAG,"响铃完毕 url : " + url);
                if (url != null) {
                    mediaPlayer.setSource(mRingingAlertInfo.getToken(), url);
                    if(getCurrRingStartTime() != 0) {
                        mediaPlayer.seekTo(mRingingAlertInfo.getToken(), (int)getCurrRingStartTime());
                    }
                    mediaPlayer.play(mRingingAlertInfo.getToken());
                } else if(mRingingAlertInfo != null) {
                    // 响铃完毕，停止闹钟
                    mAlertPlaying = false;
                    AlertInfo alertInfoToDelete = mRingingAlertInfo;
                    stopAlertNow(mRingingAlertInfo.getToken());
                    mDataManager.deleteAlert(alertInfoToDelete);
                }
            }
        }

        @Override
        public void onDuration(String mediaId, long milliseconds) {
            Log.d(TAG,"onDuration mediaId = "+mediaId+" milliseconds = "+milliseconds);
            if(mRingingAlertInfo != null) {
                if(getCurrRingStartTime() >= milliseconds) {
                    mRingingAlertInfo.setRingStartTime(0);
                }
            }
        }

        @Override
        public void onError(String mediaId, String error, ErrorType errorType) {
            Log.d(TAG,"onDuration mediaId = "+mediaId+" error = "+error);
            if (mRingingAlertInfo != null) {
                mRingingAlertInfo.setRingUrl(DEFAULT_ALARM_RING);
                mRingingAlertInfo.setRingStartTime(0);

                // 播放铃声
                mediaPlayer.setSource(mRingingAlertInfo.getToken(), mRingingAlertInfo.getRingUrl());
                if(getCurrRingStartTime() != 0) {
                    mediaPlayer.seekTo(mRingingAlertInfo.getToken(), (int)getCurrRingStartTime());
                }
                mediaPlayer.play(mRingingAlertInfo.getToken());
            }
        }

        @Override
        public void onRelease(String mediaId) {
            if (mediaPlayer != null) {
                mediaPlayer.stop(mediaId);
            }
        }

        @Override
        public void onBufferUnderrun(String mediaId) {
        }

        @Override
        public void onBufferRefilled(String mediaId) {
        }

        @Override
        public void onAllMediaPlayCompleted() {
        }

        @Override
        public void onStopped(String mediaId) {
            Log.d(TAG,"onStopped mediaId = " + mediaId);
            stopAlertNow(mCurrAlertToken);
            mAlertPlaying = false;
        }
    };

}
