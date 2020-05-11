package com.tencent.dingdangsampleapp.mediamanager;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.ai.tvs.api.MediaPlayerManager;
import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.offlinewebtemplate.OfflineWebManager;
import com.tencent.ai.tvs.tvsinterface.IMediaPlayerListener;
import com.tencent.ai.tvs.tvsinterface.IPlaybackListener;
import com.tencent.ai.tvs.tvsinterface.IRequestCallback;
import com.tencent.ai.tvs.tvsinterface.MediaType;
import com.tencent.dingdangsampleapp.player.TestMediaPlayer;
import com.tencent.dingdangsampleapp.template.data.MediaData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 媒体播放的管理类
 * 1.媒体播放控制接口，暂停，继续，下一首等
 * 2.维护媒体播放的回调接口，播放进度
 * 3.维护媒体播放的列表数据，当前播放的数据
 */
public class MediaPlayManager implements Handler.Callback{
    private static final String TAG = "Demo_MediaPlayManager";

    public enum MediaState {
        INIT, PAUSE, PLAY, COMPLETE, STOP, ONERROR
    }

    public enum PlayMode {
        SINGLE_CYCLE/**单曲循环*/
        , CYCLE_ALL/**全部循环*/
        , ORDER/**顺序播放*/
        , RANDOM/**随机播放*/
    }

    protected MediaPlayerManager mMediaPlayerManager;
    private boolean isPlaying = false;  //是否正在播放的标志位
    private boolean isAlive = false;    //媒体是否保活的标志
    private boolean isCompleted;
    private long mTotalDuration = 0;
    private long mPlayDuration = 0;
    // 播放的媒体列表
    protected ArrayList<MediaData> mMediaDatas;
    protected List<IMediaStatusListener> mMediaStatusListenerList;
    public MediaData mMediaData = null;

    protected IMediaPlayerListener baseMediaPlayerListener;

    private final int MSG_MEDIA_DISALIVE = 1;

    protected Handler mUiHandler;
    private MediaState mCurState;
    private boolean isForegroundMedia = true; //是否包含UI展示媒体
    public boolean isClicked = false;
    private String sCurMediaId = null;
    public boolean isStoped = false;
    private PlayMode mPlayMode;
    private boolean mIfCanLoadMore = false;
    private boolean mIfCanPullMore = false;
    private boolean mIsLoadEnd = false;
    private boolean bAppendAtHead = false;

    private volatile static MediaPlayManager mInstance;

    public static MediaPlayManager getInstance() {
        if (mInstance == null) {
            synchronized (MediaPlayManager.class) {
                if (mInstance == null) {
                    mInstance = new MediaPlayManager();
                }
            }
        }
        return mInstance;
    }

    private MediaPlayManager() {
        mMediaDatas = new ArrayList<>();
        mMediaStatusListenerList = new ArrayList<>();
        mUiHandler = new Handler(Looper.getMainLooper(), this);
        init();
    }


    public int getDisAliveTimeWhenComplete() {

        return 1000 * 60 * 5;
    }

    public int getDisAliveTimeWhenStop() {
        return 1000 * 60 * 5;
    }


    public int getMediaType() {
        return MediaType.AUDIO;
    }


    /**
     * playmore的意图，进行追加媒体列表数据的操作
     *
     * @param newDatas
     */
    public void appendMediaList(ArrayList<MediaData> newDatas) {
        try {
            if (null == newDatas || newDatas.size() == 0) {
                Log.i(TAG, "appendMediaList newDatas = null");
                return;
            }

            Log.i(TAG, "appendMediaList : " + newDatas);
            MediaData newMediaObj = newDatas.get(0);
            if (null != mMediaDatas && null != getCurrentMedia()) {
                if (!isContainMediaList(newDatas, mMediaDatas)) {
                    if (bAppendAtHead) {
                        mMediaDatas.addAll(0, newDatas);
                    } else {
                        mMediaDatas.addAll(newDatas);
                    }
                }

                if (null != mMediaStatusListenerList && mMediaStatusListenerList.size() > 0) {
                    for (IMediaStatusListener item : mMediaStatusListenerList) {
                        item.onRefreshSongList();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void notifyRefreshSongList() {
        if (null != mMediaStatusListenerList && mMediaStatusListenerList.size() > 0) {
            for (IMediaStatusListener item : mMediaStatusListenerList) {
                item.onRefreshSongList();
            }
        }
    }

    private boolean isContainMediaList(ArrayList<MediaData> newDatas,
                                       ArrayList<MediaData> oldDatas) {
        boolean isContain = false;
        if (null != newDatas && null != oldDatas) {
            if (oldDatas.containsAll(newDatas)) {
                isContain = true;
            }
        }
        return isContain;
    }
    
    protected void init() {

        mMediaPlayerManager = TVSApi.getInstance().getMediaPlayerManager();
        baseMediaPlayerListener = new IMediaPlayerListener() {
            @Override
            public void onInit(String mediaId) {
                Log.i(TAG, "onInit mediaId=" + mediaId);
                mUiHandler.removeMessages(MSG_MEDIA_DISALIVE);
                isCompleted = false;
                isStoped = false;
                mCurState = MediaState.INIT;
                sCurMediaId = mediaId;
                setAlive(true);
                resetMediaData();
                handleChangeMedia(mediaId);
            }

            @Override
            public void onDuration(String mediaId, long milliseconds) {
                Log.i(TAG, "onDuration milliseconds=" + milliseconds);
                mTotalDuration = milliseconds;
            }

            @Override
            public void onPlaying(String mediaId) {
                Log.d(TAG, "onPlaying()");
                mUiHandler.removeMessages(MSG_MEDIA_DISALIVE);
                isPlaying = true;
                isStoped = false;
                setAlive(true);
                isCompleted = false;
                updateMediaUIStatus(MediaState.PLAY, mediaId);
                mUiHandler.post(mUpdateProgressRunnable);
            }

            @Override
            public void onPaused(String mediaId) {
                Log.d(TAG, "onPaused()");

                isPlaying = false;
                updateMediaUIStatus(MediaState.PAUSE, mediaId);
                mUiHandler.removeCallbacks(mUpdateProgressRunnable);
            }

            @Override
            public void onCompletion(String mediaId) {
                Log.d(TAG, "onCompletion() mediaId = " + mediaId);
                mUiHandler.sendEmptyMessageDelayed(MSG_MEDIA_DISALIVE, getDisAliveTimeWhenComplete());
                isPlaying = false;
                isCompleted = true;
                updateMediaUIStatus(MediaState.COMPLETE, mediaId);
                mUiHandler.removeCallbacks(mUpdateProgressRunnable);
            }

            @Override
            public void onStopped(String mediaId) {
                mUiHandler.sendEmptyMessageDelayed(MSG_MEDIA_DISALIVE, getDisAliveTimeWhenStop());
                isPlaying = false;
                isStoped = true;
                Log.d(TAG, "onStopped()");
                updateMediaUIStatus(MediaState.STOP, mediaId);
                mUiHandler.removeCallbacks(mUpdateProgressRunnable);
            }

            @Override
            public void onError(String mediaId, String error, ErrorType errorType) {
                mUiHandler.sendEmptyMessageDelayed(MSG_MEDIA_DISALIVE, getDisAliveTimeWhenStop());
                Log.d(TAG, "onError error" + error);
                updateMediaUIStatus(MediaState.ONERROR, mediaId);
                mUiHandler.removeCallbacks(mUpdateProgressRunnable);
            }

            @Override
            public void onRelease(String mediaId) {
                mUiHandler.removeCallbacks(mUpdateProgressRunnable);

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
        };
    }

    private void resetMediaData() {
        mTotalDuration = 0;
        mPlayDuration = 0;
    }

    private void handleChangeMedia(String inMediaId) {
        Log.d(TAG, "handleChangeMedia inMediaId = "+inMediaId);
        if (TextUtils.isEmpty(inMediaId)) {
            setCurrentMedia(null);
            return;
        }
        MediaData mPlayMedia = null;
        if (null != mMediaDatas && mMediaDatas.size() > 0) {
            for (int i = 0; i < mMediaDatas.size(); i++) {
                MediaData mData = mMediaDatas.get(i);
                if (null != mData && !TextUtils.isEmpty(mData.sMediaId)) {
                    if ((!TextUtils.isEmpty(mData.sMediaId) && mData.sMediaId.contains(inMediaId))) {
                        Log.d(TAG, "handleChangeMedia inMediaId=" + inMediaId);
                        mPlayMedia = mData;
                        mPlayMedia.nShowBgIndex = i;
                        break;
                    }
                }
            }

            if (null != mPlayMedia) {
                Log.d(TAG, "null != mPlayMedia : "+mPlayMedia);
                setForegroundMedia(true);
                MediaData preData = getCurrentMedia();
                setCurrentMedia(mPlayMedia);

                updateMediaUIStatusInner(MediaState.INIT, mPlayMedia);
            } else {
                setCurrentMedia(null);
            }
        }
    }


    TestMediaPlayer mMediaPlayer;
    public void setMediaPlayer(TestMediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
        if (null != mMediaPlayer) {
            mMediaPlayer.addMediaPlayerListener(baseMediaPlayerListener);
        }
    }


    public boolean handlePlay(String mediaId) {
        Log.d(TAG, "handlePlay..");
        isPlaying = true;
        mMediaPlayer.play(mediaId);
        return true;
    }

    public boolean handlePause(String mediaId) {
        Log.d(TAG, "handlePause..mediaId = "+mediaId);
        isPlaying = false;
        mMediaPlayer.stop(mediaId);
        return true;
    }

    public void handlePlayNext(IPlaybackListener playbackListener) {
        Log.d(TAG, "handlePlayNext..");
        isClicked = true;
        if (mMediaPlayerManager == null) {
            mMediaPlayerManager = TVSApi.getInstance().getMediaPlayerManager();
        }
        mMediaPlayerManager.playNextMedia(playbackListener);
        mUiHandler.removeMessages(MSG_MEDIA_DISALIVE);
    }

    public void handlePlayPrev(IPlaybackListener playbackListener) {
        Log.d(TAG, "handlePlayPrev..");
        isClicked = true;
        if (mMediaPlayerManager == null) {
            mMediaPlayerManager = TVSApi.getInstance().getMediaPlayerManager();
        }
        mMediaPlayerManager.playPrevMedia(playbackListener);

        mUiHandler.removeMessages(MSG_MEDIA_DISALIVE);
    }

    public void handleSeekTo(int nSeek) {
        Log.i(TAG,  "上层 handleSeekTo : " + nSeek);
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(sCurMediaId, nSeek);
        } else {
            Log.e(TAG,  "handleSeekTo mMediaPlayer is null : " + nSeek);
        }
        mUiHandler.removeMessages(MSG_MEDIA_DISALIVE);
    }

    /**
     * 当前是否正在播放
     *
     * @return
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean inPlaying) {
        isPlaying = inPlaying;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public long getTotalDuration() {
        return mTotalDuration;
    }

    public void setTotalDuration(long inTotalDuration) {
        mTotalDuration = inTotalDuration;
    }

    public long getPlayDuration() {
        if (mMediaPlayer == null) {
            return -1;
        }
        mPlayDuration = mMediaPlayer.getCurrentPosition();
        return mPlayDuration;
    }

    public void setPlayDuration(long inPlayDuration) {
        mPlayDuration = inPlayDuration;
    }

    public void setMediaList(ArrayList<MediaData> newDatas) {
        Log.i(TAG, "setMediaList : " + newDatas);
        if (null == newDatas || newDatas.size() == 0) {
            Log.i(TAG, "setMediaList newDatas = null");
            return;
        }
        if (mMediaDatas == null) {
            mMediaDatas = new ArrayList<>();
        } else {
            mMediaDatas.clear();
        }

        if (null == newDatas || newDatas.size() == 0) {
            Log.i(TAG, "setMediaList newDatas = null");
            return;
        }

        mMediaDatas.addAll(newDatas);
        //添加播放列表时，默认第一首为当前媒体
        setCurrentMedia(mMediaDatas.get(0));

        if (null != newDatas && newDatas.size() > 0) {
            setIsLoadEnd(false);
        }

        if (null != mMediaDatas && mMediaDatas.size() > 0) {
            mPlayMode = PlayMode.CYCLE_ALL;
        }
    }

    public void addMediaList(ArrayList<MediaData> newDatas, boolean ifInsertToStart) {
        if (ifInsertToStart) {
            mMediaDatas.addAll(0, newDatas);
        } else {
            mMediaDatas.addAll(newDatas);
        }
    }

    public ArrayList<MediaData> getMediaList() {
        return mMediaDatas;
    }

    public void addMediaStatusListener(IMediaStatusListener listener) {
        if (listener != null && !mMediaStatusListenerList.contains(listener)) {
            mMediaStatusListenerList.add(listener);

            // 注册Listener进来，如果当前已经有媒体在活跃，则马上回调状态
            if (null != mCurState) {
                listener.onMediaStatusChange(getCurrentMedia(), mCurState);
            }
        }
    }

    public void removeMediaStatusListener(IMediaStatusListener listener) {
        if (listener != null && mMediaStatusListenerList.contains(listener)) {
            mMediaStatusListenerList.remove(listener);
        }
    }

    public MediaData getCurrentMedia() {
        if (null != mMediaData) {
            Log.i(TAG, "getCurrentMedia mMediaData="+mMediaData);
        }
        return mMediaData;
    }

    public void setCurrentMedia(MediaData inData) {
        if (null != inData) {
            Log.i(TAG, "setCurrentMedia inData=" + inData + " " + inData.nShowBgIndex);
        }
        mMediaData = inData;
    }

    private void updateProgressUI(int inPercent) {
        try {
            if (null != mMediaStatusListenerList && mMediaStatusListenerList.size() > 0) {
                for (IMediaStatusListener item : mMediaStatusListenerList) {
                    item.onUpdateProgress(inPercent);
                }
            } else {
                Log.i(TAG, "updateProgressUI mMediaStatusListenerList null or size 0");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateMediaUIStatus(MediaState mediaState, String mediaId) {
        MediaData currentMedia = getCurrentMedia();
        if (currentMedia == null) {
            Log.e(TAG, "updateMediaUIStatus currentMedia is null");
            return;
        }
        Log.i(TAG, "updateMediaUIStatus mediaState=" + mediaState + " mediaId = "+mediaId
                + " currentMedia.sMediaId = " + currentMedia.sMediaId+ " sCurMediaId = " + sCurMediaId);
        if (mediaId.equals(currentMedia.sMediaId)) {
            mCurState = mediaState;
            updateMediaUIStatusInner(mediaState, currentMedia);
        }

        if (null != currentMedia) {
            OfflineWebManager.getInstance().notifyMediaIdAndPlayStatus(currentMedia.sMediaId, currentMedia.sAlbumId, mediaState.ordinal() + "");
        } else {
            if (!TextUtils.isEmpty(sCurMediaId)) {
                OfflineWebManager.getInstance().notifyMediaIdAndPlayStatus(sCurMediaId, "", mediaState.ordinal() + "");
            }
        }
    }
    private void updateMediaUIStatusInner(final MediaState mediaState, final MediaData inMediaData) {
        try {

            if (null != mMediaStatusListenerList && mMediaStatusListenerList.size() > 0) {
                for (final IMediaStatusListener item : mMediaStatusListenerList) {
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "updateMediaUIStatusInner mediaState=" + mediaState);
                            item.onMediaStatusChange(inMediaData, mediaState);
                        }
                    });
                }
            } else {
                Log.i(TAG, "updateMediaUIStatus mMediaStatusListenerList null or size 0");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean inAlive) {
        isAlive = inAlive;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_MEDIA_DISALIVE:
                closeAllMedia();
                break;
            default:
                break;
        }
        return false;
    }

    /**
     * 媒体播放状态回调
     */
    public interface IMediaStatusListener {

        void onMediaStatusChange(MediaData mediaData, MediaState state);

        void onUpdateProgress(int percent);

        void onMediaDisAlive();

        void onRefreshSongList();
    }

    public void setForegroundMedia(boolean foregroundMedia) {
        isForegroundMedia = foregroundMedia;
    }

    public MediaState getCurStatus() {
        return mCurState;
    }

    /**
     * 关闭所有媒体ui和停止媒体播放的接口
     * (包括桌面的widget的显示和通知栏媒体的显示都移除)
     * 使用的场景
     * 1、语音 退出
     * 2、被其他媒体，腾讯视频打断，导致通道退出
     * 3、暂停一定时间后自动退出
     */
    public void closeAllMedia() {
        mUiHandler.removeMessages(MSG_MEDIA_DISALIVE);
        //暂停播放
        handlePause(sCurMediaId);
        closeAllMediaUI();
    }

    /**
     * 关闭所有媒体UI
     * (包括桌面的widget的显示和通知栏媒体的显示都移除)
     */
    public void closeAllMediaUI() {
        setAlive(false);
        if (null != mMediaStatusListenerList && mMediaStatusListenerList.size() > 0) {
            for (IMediaStatusListener item : mMediaStatusListenerList) {
                item.onMediaDisAlive();
            }
        }
    }


    Runnable mUpdateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer != null) {
                long duration = getTotalDuration();
                long currentPosition = mMediaPlayer.getCurrentPosition();
//                Log.i(TAG, "refreshDuration duration = " + duration + " currentPosition = " + currentPosition);
                if (duration > 0 && currentPosition >= 0 && duration >= currentPosition) {
                    int nPercent  = (int) (currentPosition * 100 / duration);
//                    Log.i(TAG, "refreshDuration nPercent = " + nPercent);
                    updateProgressUI(nPercent);
                }
            }
            mUiHandler.postDelayed(mUpdateProgressRunnable, 300);
        }
    };

    //收藏
    public void handleFavorite(boolean isFavorite, String skillInfo, String mediaId) {
        if (TVSApi.getInstance() == null) {
            Log.e(TAG, "handleFavorite error" + skillInfo + " mediaId=" + mediaId);
            return;
        }
        String inType;
        if (isFavorite) {
            inType = "favorite";
        } else {
            inType = "cancel_favorite";
        }
        Log.i(TAG, "handleFavorite isFavorite=" + isFavorite + " skillInfo=" + skillInfo
                + " mediaId=" + mediaId);
        TVSApi.getInstance().sendActionTriggeredEvent(skillInfo, inType,
                mediaId, new IRequestCallback() {
                    @Override
                    public void onRequestResult(int errorCode, String errorMsg) {
                        Log.i(TAG, "handleFavorite onRequestResult errorCode=" + errorCode
                                + " errorMsg=" + errorMsg);

                    }
                });
    }

    //点播
    public void handlePlayAnotherAudio(String skillInfo, String mediaId) {
        Log.i(TAG, "handlePlayAnotherAudio skillInfo=" + skillInfo + " mediaId=" + mediaId);
        if (TVSApi.getInstance() == null) {
            Log.e(TAG, "handlePlayAnotherAudio error");
            return;
        }

        TVSApi.getInstance().sendActionTriggeredEvent(skillInfo, "play_audio",
                mediaId, new IRequestCallback() {
                    @Override
                    public void onRequestResult(int errorCode, String errorMsg) {
                        Log.i(TAG, "handlePlayAnotherAudio onRequestResult errorCode=" + errorCode
                                + " errorMsg=" + errorMsg);

                    }
                });
    }


    public void handleLoadMore(String skillInfo, String mediaId) {
        Log.i(TAG, "handleLoadMore skillInfo=" + skillInfo + " mediaId=" + mediaId);
        if (TVSApi.getInstance() == null) {
            Log.e(TAG, "handleLoadMore error" + skillInfo + " mediaId=" + mediaId);
            return;
        }
        TVSApi.getInstance().sendActionTriggeredEvent(skillInfo, "load_more_audio_items",
                mediaId, new IRequestCallback() {
                    @Override
                    public void onRequestResult(int errorCode, String errorMsg) {
                        Log.i(TAG, "handleLoadMore onRequestResult errorCode=" + errorCode
                                + " errorMsg=" + errorMsg);
                    }
                });
    }

    public void handleLoadEarlier(String skillInfo, String mediaId) {
        Log.i(TAG, "handleLoadEarlier skillInfo=" + skillInfo + " mediaId=" + mediaId);
        if (TVSApi.getInstance() == null) {
            Log.e(TAG, "handleLoadEarlier error" + skillInfo + " mediaId=" + mediaId);
            return;
        }
        TVSApi.getInstance().sendActionTriggeredEvent(skillInfo, "load_earlier_items",
                mediaId, new IRequestCallback() {
                    @Override
                    public void onRequestResult(int errorCode, String errorMsg) {
                        Log.i(TAG, "handleLoadEarlier onRequestResult errorCode=" + errorCode
                                + " errorMsg=" + errorMsg);
                    }
                });
    }


    /**
     * 设置播放模式
     *
     * @param playMode
     */
    public void setPlayMode(MediaPlayManager.PlayMode playMode) {
        mPlayMode = playMode;
    }

    public MediaPlayManager.PlayMode getPlayMode() {
        return mPlayMode;
    }

    public void switchPlayMode(MediaPlayManager.PlayMode playMode, MediaData mediaData) {
        if (mediaData != null) {
            mPlayMode = playMode;
            String inType = null;
            if (playMode == MediaPlayManager.PlayMode.CYCLE_ALL) {
                inType = "change_playmode_list_cycle";
            } else if (playMode == MediaPlayManager.PlayMode.RANDOM) {
                inType = "change_playmode_shuffle";
            } else if (playMode == MediaPlayManager.PlayMode.SINGLE_CYCLE) {
                inType = "change_playmode_single_cycle";
            } else if (playMode == MediaPlayManager.PlayMode.ORDER) {
                inType = "change_playmode_sequential";
            }
            TVSApi.getInstance().sendActionTriggeredEvent(mediaData.sSkillInfo, inType,
                    mediaData.sMediaId, new IRequestCallback() {
                        @Override
                        public void onRequestResult(int errorCode, String errorMsg) {
                            Log.i(TAG, "switchPlayMode onRequestResult errorCode=" + errorCode
                                    + " errorMsg=" + errorMsg);

                        }
                    });
        }
    }


    public boolean isLoadEnd() {
        return mIsLoadEnd;
    }

    public void setIsLoadEnd(boolean mIsLoadEnd) {
        this.mIsLoadEnd = mIsLoadEnd;
    }

    public void setIfCanLoadMore(boolean mIfCanLoadMore) {
        this.mIfCanLoadMore = mIfCanLoadMore;
    }

    public void setAppendAtHead(boolean isAppendHead) {
        this.bAppendAtHead = isAppendHead;
    }

    public boolean ifCanLoadMore() {
        return mIfCanLoadMore;
    }

    public boolean ifCanPullMore() {
        return mIfCanPullMore;
    }

    public void setIfCanPullMore(boolean mIfCanPullMore) {
        this.mIfCanPullMore = mIfCanPullMore;
    }

}
