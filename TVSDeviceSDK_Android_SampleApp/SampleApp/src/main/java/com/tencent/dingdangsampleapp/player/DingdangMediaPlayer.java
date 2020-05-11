package com.tencent.dingdangsampleapp.player;

import android.text.TextUtils;
import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.IMediaPlayer;
import com.tencent.ai.tvs.tvsinterface.IMediaPlayerListener;

import java.io.InputStream;

/**
 * 云端下发的媒体的播放器响应，以及状态回调
 */
public class DingdangMediaPlayer implements IMediaPlayer {

    private static final String TAG = "DingdangMediaPlayer";

    private TestMediaPlayer mActualMediaPlayer;

    // 当前的媒体信息
    private String mCurrentMediaId;
    private String mUrl;

    // SDK注册的listener
    private IMediaPlayerListener mListener;

    public DingdangMediaPlayer(TestMediaPlayer mediaPlayer) {
        mActualMediaPlayer = mediaPlayer;
    }

    @Override
    public void setSource(String mediaId, String url, MediaAttributes mediaAttributes) {
        Log.i(TAG, "setSource : " + mediaId + ", " + url);

        if (null != mActualMediaPlayer) {
            mCurrentMediaId = mediaId;
            mUrl = url;

            mActualMediaPlayer.setSource(mCurrentMediaId, mUrl);
        }
    }

    @Override
    public void setSource(String mediaId, InputStream inputStream, int sampleRateInHz, int channelConfig) {
        // TODO
    }

    @Override
    public void play(String mediaId) {
        if (!isMediaIdValid(mediaId)) {
            return;
        }

        if (null != mActualMediaPlayer) {
            mActualMediaPlayer.play(mediaId);
        }
    }

    @Override
    public void stop(String mediaId) {
        if (!isMediaIdValid(mediaId)) {
            return;
        }

        if (null != mActualMediaPlayer) {
            mActualMediaPlayer.stop(mediaId);
        }
    }

    @Override
    public void seekTo(String mediaId, int milliseconds) {
        if (!isMediaIdValid(mediaId)) {
            return;
        }

        if (null != mActualMediaPlayer) {
            mActualMediaPlayer.seekTo(mediaId, milliseconds);
        }
    }

    @Override
    public long getCurrentPosition() {
        if (null != mActualMediaPlayer) {
            return mActualMediaPlayer.getCurrentPosition();
        }

        return 0;
    }

    @Override
    public long getDuration() {
        if (null != mActualMediaPlayer) {
            return mActualMediaPlayer.getDuration();
        }

        return 0;
    }

    @Override
    public void setMediaPlayerListener(IMediaPlayerListener listener) {
        mListener = listener;

        if (null != mActualMediaPlayer) {
            mActualMediaPlayer.addMediaPlayerListener(listener);
        }
    }

    @Override
    public void removeMediaPlayerListener(IMediaPlayerListener listener) {
        if (mListener == listener) {
            mListener = null;

            if (null != mActualMediaPlayer) {
                mActualMediaPlayer.removeMediaPlayerListener(listener);
            }
        }
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

}
