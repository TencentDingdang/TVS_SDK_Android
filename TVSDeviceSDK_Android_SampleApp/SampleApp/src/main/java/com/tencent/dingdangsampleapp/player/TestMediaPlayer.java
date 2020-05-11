package com.tencent.dingdangsampleapp.player;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.IMediaPlayerListener;
import com.tencent.dingdangsampleapp.devicectrl.ActiveMediaAppDemo;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 媒体播放器的demo，使用Android的MediaPlayer
 * 注：仅用于体验，未做很多异常处理
 *
 * 媒体播放器需要自行处理audio focus，并决定与SDK内的录音等抢占的交互逻辑
 */
public class TestMediaPlayer {

    private static final String TAG = "TestMediaPlayer";

    private int mState = StateConstants.STATE_IDLE;

    // MediaPlayer实例
    private MediaPlayer mMediaPlayer;

    // 当前的媒体id（可能是叮当发起的，也可能是QQ音乐拉取的）
    private String mCurrentMediaId;

    // 是否加载完成，用于判断是否可以开始播放
    private boolean mIsPrepared = false;
    // 云端有可能直接指定seek的位置，要在prepare结束后seekTo
    private int mPendingSeekMs;

    // 播放器回调
    private List<IMediaPlayerListener> mListeners;

    // audio focus相关
    private AudioManager mAudioManager;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.i(TAG, "onAudioFocusChange focusChange: " + focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN: {
                    TestMediaPlayer.this.resume(mCurrentMediaId);
                    break;
                }
                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:{
                    TestMediaPlayer.this.pause(mCurrentMediaId);
                    break;
                }
                default: {
                    break;
                }
            }
        }
    };

    public TestMediaPlayer(Context context) {
        mListeners = new CopyOnWriteArrayList<>();

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
        mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
        mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
        mMediaPlayer.setOnErrorListener(mOnErrorListener);
    }

    /**
     * 检查mediaId与播放器内存储的mediaId是否相同
     *
     * @param mediaId
     * @return
     */
    private boolean isMediaIdValid(String mediaId) {
        return TextUtils.equals(mCurrentMediaId, mediaId);
    }

    /**
     * 当前是否是准备播放，或者播放状态
     *
     * @return
     */
    private boolean isNowPlaying() {
        return mState == StateConstants.STATE_INIT || mState == StateConstants.STATE_PREPARE_PLAYING
                || mState == StateConstants.STATE_PLAYING || mState == StateConstants.STATE_PAUSED;
    }

    /**
     * 清理局部变量
     */
    private void clear() {
        mIsPrepared = false;
        mPendingSeekMs = 0;
        mCurrentMediaId = null;
        mMediaPlayer.setOnBufferingUpdateListener(null);
    }

    /**
     * 如果条件满足，启动播放
     * <p>
     * 由于prepare完成 和 真正调用播放无法确定时序，所以只能当两个状态都满足才能播放，有以下场景触发：
     * 1、prepare完成，调用play
     * 2、prepare完成，PAUSED 状态下调用resume
     * 3、prepare完成，当前状态是 PREPARE_PLAYING（也就是调用过play或者resume）
     */
    private void startPlayMediaIfPrepared() {
        Log.i(TAG, "startPlayMediaIfPrepared : " + mState);

        if (mIsPrepared && mState == StateConstants.STATE_PREPARE_PLAYING) {
            // 云端媒体开始播放的时候，关闭第三方媒体类App（可选）
            ActiveMediaAppDemo activeMediaAppDemo = ActiveMediaAppDemo.getInstance();
            if (null != activeMediaAppDemo) {
                activeMediaAppDemo.removeTopPackageName();
            }

            // TODO 不判断是否获取成功了
            mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            mMediaPlayer.start();
            mState = StateConstants.STATE_PLAYING;

            for (IMediaPlayerListener listener : mListeners) {
                listener.onPlaying(mCurrentMediaId);
                // TODO
                listener.onDuration(mCurrentMediaId, mMediaPlayer.getDuration());
            }
        }
    }

    private void doOnStop() {
        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
    }

    public void setSource(String mediaId, String url) {
        Log.i(TAG, "setSource url = "+url);

        clear();

        mCurrentMediaId = mediaId;
        mState = StateConstants.STATE_INIT;
        for (IMediaPlayerListener listener : mListeners) {
            listener.onInit(mediaId);
        }

        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(url);
                mMediaPlayer.prepareAsync();
            }
        } catch (IOException e) {
            e.printStackTrace();

            handleError(mediaId, "IOException play url :" + url, IMediaPlayerListener.ErrorType.MEDIA_ERROR_INTERNAL_DEVICE_ERROR);
        }
    }

    public void play(String mediaId) {
        Log.i(TAG, "play : " + mediaId + ", state : " + mState);

        if (!isMediaIdValid(mediaId)) {
            return;
        }

        if (mState == StateConstants.STATE_PREPARE_PLAYING || mState == StateConstants.STATE_PLAYING) {
            return;
        }

        try {
            // mediaPlayer调用stop后再次start，需要先prepare
            if (mState == StateConstants.STATE_STOPPED) {
                mMediaPlayer.prepareAsync();
            }
        } catch (Exception e) {
            Log.e(TAG, "play prepareAsync exception : " + e.getMessage());
        }


        // 设置STATE_PREPARE_PLAYING
        mState = StateConstants.STATE_PREPARE_PLAYING;
        startPlayMediaIfPrepared();
    }

    public void pause(String mediaId) {
        Log.i(TAG, "pause : " + mediaId + ", state : " + mState);

        if (!isMediaIdValid(mediaId)) {
            return;
        }

        if (isNowPlaying()) {
            // 未缓冲完调用会有-38错误
            if (mIsPrepared) {
                mMediaPlayer.pause();
            }
            mState = StateConstants.STATE_PAUSED;

            for (IMediaPlayerListener listener : mListeners) {
                listener.onPaused(mCurrentMediaId);
            }
        }
    }

    public void stop(String mediaId) {
        Log.i(TAG, "stop : " + mediaId + ", state : " + mState);

        if (!isMediaIdValid(mediaId)) {
            return;
        }

        if (isNowPlaying()) {
            doOnStop();

            // 未缓冲完调用会有-38错误
            if (mIsPrepared) {
                mMediaPlayer.stop();
            }
            mPendingSeekMs = (int) getCurrentPosition();
            mIsPrepared = false;
            mState = StateConstants.STATE_STOPPED;

            for (IMediaPlayerListener listener : mListeners) {
                listener.onStopped(mCurrentMediaId);
            }
        }
    }

    public void resume(String mediaId) {
        Log.i(TAG, "resume : " + mediaId + ", state : " + mState);

        if (!isMediaIdValid(mediaId)) {
            return;
        }

        if (mState == StateConstants.STATE_PAUSED) {
            // 设置STATE_PREPARE_PLAYING
            mState = StateConstants.STATE_PREPARE_PLAYING;
            startPlayMediaIfPrepared();
        }
    }

    public void seekTo(String mediaId, int milliseconds) {
        Log.i(TAG, "seekTo : " + mediaId + ", milliseconds : " + milliseconds + ", state : " + mState);

        if (!isMediaIdValid(mediaId)) {
            return;
        }

        mPendingSeekMs = milliseconds;

        // 未缓冲完调用会有-38错误
        if (mIsPrepared) {
            mMediaPlayer.seekTo(milliseconds);
        }
    }

    public long getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public long getDuration() {
        // 未缓冲完调用会有-38错误，先返回0l，使用的地方需要判断合法性
        if (!mIsPrepared) {
            return 0l;
        }

        return mMediaPlayer.getDuration();
    }

    protected void handleError(String mediaId, String error, IMediaPlayerListener.ErrorType errorType) {
        Log.i(TAG, "handleError : " + mediaId + ", state : " + mState + ", error : " + error);

        mState = StateConstants.STATE_IDLE;
        doOnStop();
        for (IMediaPlayerListener listener : mListeners) {
            listener.onError(mediaId, error, errorType);
        }
        mMediaPlayer.reset();
    }

    private MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.i(TAG, "onPrepared : state : " + mState);

            mIsPrepared = true;

            if (mPendingSeekMs > 0) {
                mMediaPlayer.seekTo(mPendingSeekMs);
            } else {
                startPlayMediaIfPrepared();
            }
        }
    };

    private MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            // TODO
            if (what == -38) {
                return false;
            }
            // TODO, 略去错误类型的细分
            handleError(mCurrentMediaId, "test error", IMediaPlayerListener.ErrorType.MEDIA_ERROR_INTERNAL_DEVICE_ERROR);
            return false;
        }
    };

    private MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(MediaPlayer mp) {
            startPlayMediaIfPrepared();
        }
    };

    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mState = StateConstants.STATE_COMPLETED;
            doOnStop();
            for (IMediaPlayerListener listener : mListeners) {
                listener.onCompletion(mCurrentMediaId);
            }
        }
    };

    public void addMediaPlayerListener(IMediaPlayerListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeMediaPlayerListener(IMediaPlayerListener listener) {
        mListeners.remove(listener);
    }
}