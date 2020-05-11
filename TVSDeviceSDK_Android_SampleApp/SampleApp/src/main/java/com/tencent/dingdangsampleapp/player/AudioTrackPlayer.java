package com.tencent.dingdangsampleapp.player;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.IMediaPlayer;
import com.tencent.ai.tvs.tvsinterface.IMediaPlayerListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * TTS播放器-播放pcm数据
 *
 * TTS播放器需要申请audio focus，SDK内部会申请并在丢失焦点的时候调用stop
 */
public class AudioTrackPlayer implements IMediaPlayer, Handler.Callback {

    private static final String TAG = "AudioTrackPlayer";

    private Context mContext;
    private AudioTrack mAudioTrack;
    private int mMiniBufferSize;
    private int mCurrentSampleSize;
    private int mCurrentChannelConfig;

    // 播放器状态
    private int mCurState = StateConstants.STATE_IDLE;
    // 播放器回调列表
    private List<IMediaPlayerListener> mMediaPlayerListeners;
    // 媒体ID
    private String mMediaId;
    // 媒体流（PCM）
    private InputStream inputStream;

    // 播放线程
    private AudioPlayerWorkThread mAudioPlayerWorkThread;

    private Handler handler = new Handler(Looper.getMainLooper(), this);
    // 播放结束的通知
    private final int MSG_KEY_NOTIFY_FINISH = 1;

    public AudioTrackPlayer(Context applicationContext) {

        mContext = applicationContext;
        // 播放器回调列表
        mMediaPlayerListeners = Collections.synchronizedList(new LinkedList<>());
    }

    private final class AudioPlayerWorkThread extends Thread {

        private String mediaId;
        private InputStream inputStream;
        private volatile boolean isStop;

        public AudioPlayerWorkThread(String mediaId, InputStream inputStream) {
            Log.d(TAG, "AudioPlayerWorkThread id : " + mediaId + ", " + inputStream + ":" + this);
            this.mediaId = mediaId;
            this.inputStream = inputStream;
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        }

        public void closeStream() {
            Log.d(TAG, "closeStream : " + this);
            isStop = true;
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.interrupt();
        }

        @Override
        public void run() {
            super.run();
            if (mAudioTrack != null) {
                mAudioTrack.play();
            }
            try {
                byte[] inputBytes = new byte[(int)(mMiniBufferSize*2)];
                while (!isStop && inputStream.read(inputBytes) > -1) {
                    mAudioTrack.write(inputBytes, 0, inputBytes.length);
                }
            } catch (Exception e) {
                Log.e(TAG, this + " AudioPlayerWorkThread run Exception.", e);
            }

            handler.removeMessages(MSG_KEY_NOTIFY_FINISH);
            Message message = handler.obtainMessage(MSG_KEY_NOTIFY_FINISH);
            message.obj = this.mediaId;
            handler.sendMessage(message);
        }
    }

    private void handlePlayFinish(String mediaId) {
        Log.d(TAG, "handlePlayFinish mCurState =" + mCurState +
                ", mediaId : " + mediaId + ", current mediaId : " + mMediaId);
        // 只有播放完成，非打断状态才会回调OnCompletion
//        if (mCurState == StateConstants.STATE_PLAYING) {
            for (IMediaPlayerListener listener : mMediaPlayerListeners) {
                if (listener != null) {
                    listener.onCompletion(mediaId);
                }
            }
//        }

        // 当前在播放的TTS，才会影响状态
        if (TextUtils.equals(mediaId, mMediaId)) {
            setCurrentState(StateConstants.STATE_COMPLETED);
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        Log.d(TAG, "handleMessage message.what=" + message.what);
        if (message.what == MSG_KEY_NOTIFY_FINISH) {
            handlePlayFinish((String)message.obj);
        }
        return false;
    }

    private void setCurrentState(int curState) {
        Log.d(TAG, "setCurrentState curState = "+curState + " mCurState = "+mCurState);
        mCurState = curState;
    }

    @Override
    public void setSource(String mediaId, String url, MediaAttributes mediaAttributes) {
        // TODO
    }

    @Override
    public void setSource(String mediaId, InputStream inputStream, int sampleRateInHz, int channelConfig) {
        Log.d(TAG, "setSource : " + mediaId + ", " + inputStream + ", " + mCurState);
        this.mMediaId = mediaId;
        this.inputStream = inputStream;
        // 根据采样率和声道判断是否要重新创建AudioTrack
        initAudioTrackIfNeeded(sampleRateInHz, channelConfig);
        for (IMediaPlayerListener listener : mMediaPlayerListeners) {
            if (listener != null) {
                listener.onInit(this.mMediaId);
            }
        }
        setCurrentState(StateConstants.STATE_PREPARE_PLAYING);
    }

    private void initAudioTrackIfNeeded(int sampleRateInHz, int channelConfig) {
        Log.i(TAG, "initAudioTrackIfNeeded : current - " + mCurrentSampleSize + ", " + mCurrentChannelConfig +
                ", new : " + sampleRateInHz + ", " + channelConfig);

        if (mCurrentSampleSize != sampleRateInHz
                || mCurrentChannelConfig != channelConfig) {
            mCurrentSampleSize = sampleRateInHz;
            mCurrentChannelConfig = channelConfig;

            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            mMiniBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig,
                    audioFormat, mMiniBufferSize, AudioTrack.MODE_STREAM);
            Log.i(TAG, "initAudioTrack minBufferSize = " + mMiniBufferSize);
        }
    }

    @Override
    public void play(String mediaId) {
        Log.d(TAG, "play : " + mediaId + ", current:" + this.mMediaId + ", " + mCurState);
        if (TextUtils.equals(mediaId, this.mMediaId)) {
            //有新的播放逻辑，移除原来的播放结束通知
            handler.removeMessages(MSG_KEY_NOTIFY_FINISH);

            if (mCurState != StateConstants.STATE_PLAYING) {
                setCurrentState(StateConstants.STATE_PLAYING);
                for (IMediaPlayerListener listener : mMediaPlayerListeners) {
                    if (listener != null) {
                        listener.onPlaying(this.mMediaId);
                    }
                }
            }
            mAudioPlayerWorkThread = new AudioPlayerWorkThread(mediaId, this.inputStream);
            mAudioPlayerWorkThread.start();
        }
    }

    @Override
    public void stop(String mediaId) {
        Log.i(TAG, "stop " + mCurState + ", thread : " + mAudioPlayerWorkThread);

        if (mCurState == StateConstants.STATE_PLAYING || mCurState == StateConstants.STATE_PREPARE_PLAYING
                || mCurState == StateConstants.STATE_PAUSED) {
            setCurrentState(StateConstants.STATE_STOPPED);
            if (mAudioPlayerWorkThread != null) {
                mAudioPlayerWorkThread.closeStream();
            }
            try {
                if (mAudioTrack != null && mAudioTrack.getPlayState() != AudioTrack.STATE_UNINITIALIZED) {
                    Log.d(TAG, "getPlayState:" + mAudioTrack.getPlayState());
                    mAudioTrack.pause();
                    mAudioTrack.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (IMediaPlayerListener listener : mMediaPlayerListeners) {
                if (listener != null) {
                    listener.onStopped(this.mMediaId);
                }
            }
        }
    }

    @Override
    public void seekTo(String mediaId, int milliseconds) {

    }

    @Override
    public long getCurrentPosition() {
        return 0;
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public void setMediaPlayerListener(IMediaPlayerListener listener) {
        if (!mMediaPlayerListeners.contains(listener)) {
            mMediaPlayerListeners.add(listener);
        }
    }

    @Override
    public void removeMediaPlayerListener(IMediaPlayerListener listener) {
        if (mMediaPlayerListeners.contains(listener)) {
            mMediaPlayerListeners.remove(listener);
        }
    }
}