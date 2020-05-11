package com.tencent.dingdangsampleapp.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tencent.ai.tvs.tvsinterface.IMediaCtrlListener;
import com.tencent.ai.tvs.tvsinterface.IPlaybackListener;
import com.tencent.dingdangsampleapp.R;
import com.tencent.dingdangsampleapp.mediamanager.MediaPlayManager;
import com.tencent.dingdangsampleapp.template.data.MediaData;
import com.tencent.dingdangsampleapp.util.TimeUtils;

public class MediaPlayerView extends RelativeLayout implements View.OnClickListener, MediaPlayManager.IMediaStatusListener {
    private static final String TAG = "MediaPlayerView";
    private Handler mHandler;

    /**
     * 歌曲名
     */
    private TextView mSongView;
    /**
     * 已播放的时间
     */
    private TextView mPastTimeView;
    /**
     * 总共时间
     */
    private TextView mTotalTimeView;
    public SeekBar mSeekBarView;

    /**
     * 上一首
     */
    public ImageView mPreView;
    /**
     * 播放控制
     */
    public ImageView mPlayView;
    /**
     * 下一首
     */
    public ImageView mNextView;
    public ImageView mRefrainDot;
    private Context mContext;

    private String mMediaId;
    private boolean mIsPlaying;
//    TestMediaPlayer mMediaPlayer;
    private MediaPlayManager mMediaPlayManager;

    public MediaPlayerView(Context context) {
        this(context, null);
    }

    public MediaPlayerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        inflate(context, R.layout.media_player_view, this);
        initViews();
    }

    public MediaPlayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    public void initViews() {
        Log.i(TAG, "initViews ");

        //已播放的时长
        mPastTimeView = (TextView) findViewById(R.id.media_past_time);
        //总的时长
        mTotalTimeView = (TextView) findViewById(R.id.media_total_time);
        //上一首
        mPreView = (ImageView) findViewById(R.id.media_prev);
        mPreView.setOnClickListener(this);
        //播放/暂停
        mPlayView = (ImageView) findViewById(R.id.media_play);
        mPlayView.setOnClickListener(this);
        //下一首
        mNextView = (ImageView) findViewById(R.id.media_next);
        mNextView.setOnClickListener(this);
        //进度条
        mSeekBarView = (SeekBar) findViewById(R.id.media_seekbar);
        mSeekBarView.setOnSeekBarChangeListener(mBarListener);
        mSeekBarView.setMax(100);
        mRefrainDot = (ImageView) findViewById(R.id.refrain_dot);

        mHandler = new Handler(Looper.getMainLooper());
        mMediaPlayManager = MediaPlayManager.getInstance();
        mMediaPlayManager.addMediaStatusListener(this);

    }


    @Override
    public void onClick(View view) {
        MediaPlayManager mediaPlayManager = MediaPlayManager.getInstance();
//        MediaPlayerManager mediaPlayerManager;
        switch (view.getId()) {
            case R.id.media_prev:
                Log.i(TAG, "onClick playPrevMedia ");
                if (null != mediaPlayManager) {
                    mMediaPlayManager.handlePlayPrev(new IPlaybackListener() {
                        @Override
                        public void onSucceed() {
                            Log.i(TAG, "playPrevMedia onSucceed");
                        }

                        @Override
                        public void onFailed(int errorCode, String errorMessage) {
                            Log.e(TAG, "playPrevMedia errorCode = "+errorCode+" errorMessage = "+errorMessage);
                        }
                    });
                }
                break;
            case R.id.media_next:
                Log.i(TAG, "onClick playNextMedia ");
                mMediaPlayManager.handlePlayNext(new IPlaybackListener() {
                    @Override
                    public void onSucceed() {
                        Log.i(TAG, "playNextMedia onSucceed");
                    }

                    @Override
                    public void onFailed(int errorCode, String errorMessage) {
                        Log.e(TAG, "playNextMedia errorCode = "+errorCode+" errorMessage = "+errorMessage);
                    }
                });
                break;
            case R.id.media_play:
                boolean isPlaying = mediaPlayManager.isPlaying();
                Log.i(TAG, "onClick playNextMedia isPlaying = "+isPlaying);
                if (isPlaying) {
                    mediaPlayManager.handlePause(mMediaId);
                    mPlayView.setImageDrawable(getResources().getDrawable(R.drawable.media_pause));
                } else {
                    mediaPlayManager.handlePlay(mMediaId);
                    mPlayView.setImageDrawable(getResources().getDrawable(R.drawable.media_play));
                }
                break;
            default:
                break;
        }
    }

    private SeekBar.OnSeekBarChangeListener mBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) {
            //Log.d(TAG, "onProgressChanged:" + seekBar.getProgress()+" progress = "+progress);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    refreshDuration(seekBar.getProgress());
                }
            });
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            try {
                if (seekBar != null) {
                    float scale = 1f * seekBar.getProgress() / seekBar.getMax();
                    int pos = Math.round(MediaPlayManager.getInstance().getTotalDuration() * scale);

                    Log.d(TAG, "onStopTrackingTouch getProgress:" + seekBar.getProgress()+" scale = "+scale
                            +" pos = "+pos);
                    MediaPlayManager.getInstance().handleSeekTo(pos);
                    mSeekBarView.setProgress(seekBar.getProgress());
//                    mMediaPlayer.seekTo(mMediaId, pos);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void refreshDuration(int nPercent) {
        if (mMediaData == null) {
            return;
        }
        long totalDuration = mMediaPlayManager.getTotalDuration() / 1000;
        long pastDuration = mMediaPlayManager.getPlayDuration() / 1000;
//        Log.i(TAG, "totalDuration = " + totalDuration + " pastDuration = " + pastDuration
//                + " nPercent = " + nPercent);
        mTotalTimeView.setText(" / " + TimeUtils.secToTimeString(totalDuration));
        if (mPastTimeView.getVisibility() == View.VISIBLE) {
            if (nPercent == 100) {
                mPastTimeView.setText(TimeUtils.secToTimeString(totalDuration));
            } else {
                mPastTimeView.setText(TimeUtils.secToTimeString(pastDuration));
            }
        }
        if (totalDuration <= 0) {
            mSeekBarView.setProgress(0);
        } else {
            mSeekBarView.setProgress(nPercent);
        }

    }


    private void updatePlayBtn() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (MediaPlayManager.getInstance().isPlaying()) {
                    mPlayView.setImageDrawable(getResources().getDrawable(R.drawable.media_play));
                } else {
                    mPlayView.setImageDrawable(getResources().getDrawable(R.drawable.media_pause));
                }
            }
        });
    }

    private void updateProgress(int percent) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                refreshDuration(percent);
            }
        });
    }

    // 通用媒体控制的回调
    IMediaCtrlListener mMediaCtrlListener = new IMediaCtrlListener() {
        @Override
        public void onCollectShowChanged(boolean collect, String id) {
            Log.i(TAG, "收藏状态变化 collect : " + collect + ", id : " + id);
        }

        @Override
        public void onPlayModeChanged(String mode) {
            Log.i(TAG, "播放模式变化 mode : " + mode);
        }

        @Override
        public void download(String url) {
            if (TextUtils.isEmpty(url)) {
                Log.i(TAG, "下载当前媒体");
            } else {
                Log.i(TAG, "下载指定URL：" + url);
            }
        }
    };

    public void stopMedia() {
        if (mIsPlaying) {
            Log.i(TAG, "stopMedia");
            if (mPlayView != null) {
                mPlayView.setImageDrawable(getResources().getDrawable(R.drawable.media_pause));
            }
            MediaPlayManager.getInstance().handlePause(mMediaId);
        }
    }

    MediaData mMediaData;
    @Override
    public void onMediaStatusChange(MediaData mediaData, MediaPlayManager.MediaState state) {
        Log.i(TAG, "onMediaStatusChange state ：" + state+" mediaData = "+mediaData);
        mMediaData = mediaData;
        switch (state){
            case PLAY:
                mMediaId = mediaData.sMediaId;
                mIsPlaying = true;
                break;
            case PAUSE:
            case INIT:
                mIsPlaying = false;
                break;
            case COMPLETE:
                mIsPlaying = false;
                mMediaId = null;
                updateProgress(100);
                break;
            case ONERROR:
                mIsPlaying = false;
                mMediaId = null;
                updateProgress(0);
                break;
        }
        updatePlayBtn();
    }

    @Override
    public void onUpdateProgress(int percent) {
        refreshDuration(percent);
    }

    @Override
    public void onMediaDisAlive() {
        stopMedia();
    }

    @Override
    public void onRefreshSongList() {

    }

}
