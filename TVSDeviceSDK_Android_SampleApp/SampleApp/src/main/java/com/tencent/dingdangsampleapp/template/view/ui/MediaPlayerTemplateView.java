package com.tencent.dingdangsampleapp.template.view.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tencent.ai.tvs.tvsinterface.IMediaCtrlListener;
import com.tencent.dingdangsampleapp.R;
import com.tencent.dingdangsampleapp.mediamanager.MediaPlayManager;
import com.tencent.dingdangsampleapp.template.data.AudioTmplData;
import com.tencent.dingdangsampleapp.template.data.MediaData;
import com.tencent.dingdangsampleapp.template.presenter.TemplatePresenter;
import com.tencent.dingdangsampleapp.util.TimeUtils;
import com.tencent.dingdangsampleapp.view.MediaListPopWin;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 媒体播放模板
 */
public class MediaPlayerTemplateView extends AbstractTemplate implements View.OnClickListener, Handler.Callback,
        MediaPlayManager.IMediaStatusListener, IMediaCtrlListener {

    protected final String TAG = "MediaPlayerTemplateView";
    private View mMusicInfoView;
    private MediaListPopWin mListPopWin;
    /**
     * 封面View
     */
    private RelativeLayout mIconFrameLayout;
    /**
     * 歌曲名
     */
    private TextView mSongView;
    /**
     * 专辑名
     */
    private TextView mAlbumView;
    /**
     * 明星
     */
    private TextView mSingerView;
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
    public ImageView mRepeatView;
    public ImageView mFavoriteView;
    public ImageView mMediaList;

    public MediaData mMediaData;
    private Handler mUIHandler;
    private Handler mWorkHandler;
    private HandlerThread mThreadHandler;
    private Context mContext;
    private RelativeLayout mBgLayout = null;
//    public ImageView mRefrainDot;

    private final int MSG_KEY_LOAD_MEDIA = 1;
    private final int MSG_KEY_UPDATE_MEDIA = 2;
    private final int MSG_KEY_UPDATE_BG = 9;
    private View bg = null;
//    private MusicAdapter mAdapter = null;
    private LinkedBlockingQueue<String> mMediaIdQueue = null;
    //在播放界面连续切换歌曲的情况，最多缓存多少首歌曲的图片的数量
    public MediaPlayerTemplateView(Context context) {
        this(context, null);
    }

    public MediaPlayerTemplateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        inflate(context, R.layout.template_media_player, this);
        mUIHandler = new Handler(Looper.getMainLooper(), this);
        mThreadHandler = new HandlerThread("music_handler_thread");
        mThreadHandler.start();
        mWorkHandler = new Handler(mThreadHandler.getLooper(), null);
        mMediaIdQueue = new LinkedBlockingQueue<String>();

        initViews();
    }

    public MediaPlayerTemplateView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    @Override
    public void clearData() {

    }

    @Override
    protected void initViews() {
        super.initViews();
        Log.i(TAG, "initViews");

        mBgLayout = (RelativeLayout) findViewById(R.id.media_bg_ly);
        mPastTimeView = (TextView) findViewById(R.id.media_past_time);
        mTotalTimeView = (TextView) findViewById(R.id.media_total_time);

        mPastTimeView.setIncludeFontPadding(false);
        mTotalTimeView.setIncludeFontPadding(false);
        mSeekBarView = (SeekBar) findViewById(R.id.media_seekbar);

        mPreView = (ImageView) findViewById(R.id.media_prev);
        mPreView.setOnClickListener(this);
        mPlayView = (ImageView) findViewById(R.id.media_play);
        mPlayView.setOnClickListener(this);
        mNextView = (ImageView) findViewById(R.id.media_next);
        mNextView.setOnClickListener(this);

        mRepeatView = (ImageView) findViewById(R.id.media_repeat);
        mRepeatView.setOnClickListener(this);
        mFavoriteView = (ImageView) findViewById(R.id.media_favorite);
        mFavoriteView.setOnClickListener(this);

        mMediaList = (ImageView) findViewById(R.id.media_list);
        mMediaList.setOnClickListener(this);
        mSeekBarView.setOnSeekBarChangeListener(mBarListener);
        mSeekBarView.setMax(100);

        mMusicInfoView = findViewById(R.id.music_info)/*inflate(mContext, R.layout.layout_music_info, null)*/;
        mIconFrameLayout = (RelativeLayout) mMusicInfoView.findViewById(R.id.cover_framelayout);
        mAlbumView = (TextView) mMusicInfoView.findViewById(R.id.media_album);
        mSongView = (TextView) mMusicInfoView.findViewById(R.id.media_song);
        mSingerView = (TextView) mMusicInfoView.findViewById(R.id.media_singer);

        setHeadBarTransparent();//head设置透明
        hideFootView();//隐藏foot
    }

    private void setViewVisiable(View inView, int nVisible) {
        if (null != inView) {
            inView.setVisibility(nVisible);
        }
    }

    public boolean isTemplateMode() {
        boolean isMode = false;
        if (null != mMediaData) {
            isMode = true;
        }
        return isMode;
    }

    @Override
    public void fillData() {
        Log.i(TAG, "fillData mData :"+mData+" mData class :"+mData.getClass());

        if (mData != null) {
            AudioTmplData audioTmplData = (AudioTmplData)mData;
            mMediaData = (MediaData) audioTmplData.getCurMediaData();
            Log.i(TAG, "fillData mMediaData :"+mMediaData);
            handleMediaStatusChange(mMediaData, MediaPlayManager.MediaState.INIT);
        } else {
            Log.e(TAG, "mData inValid :"+mData);
        }
        MediaPlayManager.getInstance().addMediaStatusListener(this);
    }

    public void initMediaInfo() {

        Log.i(TAG, "initMediaInfo mPerson :"+mMediaData.mPerson
                + " mMediaName = " + mMediaData.mMediaName
                + " mAlbumUrl = " + mMediaData.mAlbumUrl);
        mSingerView.setText(mMediaData.mPerson);
        mSongView.setText(mMediaData.mMediaName);
        mAlbumView.setText(mMediaData.mAlbumUrl);
    }

    @Override
    public void onCollectShowChanged(boolean collect, String id) {
        Log.i(TAG, "Override collect :"+collect+" mediaId :"+ id + " mMediaData = " + mMediaData);
        if (mMediaData != null && id != null && id.equals(mMediaData.sMediaId)) {
            if (collect) {
                mFavoriteView.setImageDrawable(getResources().getDrawable(R.drawable.media_favorited));
            } else {
                mFavoriteView.setImageDrawable(getResources().getDrawable(R.drawable.media_unfavorite));
            }
        }
    }

    @Override
    public void onPlayModeChanged(String mode) {
        Log.i(TAG, "onPlayModeChanged  :"+mode);

        MediaPlayManager.PlayMode playMode;
        if ("change_playmode_shuffle".equals(mode)) {
            playMode = MediaPlayManager.PlayMode.RANDOM;
        } else if ("change_playmode_single_cycle".equals(mode)) {
            playMode = MediaPlayManager.PlayMode.SINGLE_CYCLE;
        } else if ("change_playmode_sequential".equals(mode)) {
            playMode = MediaPlayManager.PlayMode.ORDER;
        } else {
            playMode = MediaPlayManager.PlayMode.CYCLE_ALL;
        }
         updatePatternView(playMode);
    }

    @Override
    public void download(String url) {

    }


    @Override
    public void onClick(View v) {
        if (mMediaData == null) {
            Log.d(TAG, "onClick() media is null");
            return;
        }
        switch (v.getId()) {
            case R.id.media_prev:
                Log.d(TAG, "onClick() handlePlayPrev");
                MediaPlayManager.getInstance().handlePlayPrev(null);
                break;
            case R.id.media_play:
                Log.d(TAG, "onClick() handlePlay isPlaying = "+MediaPlayManager.getInstance().isPlaying()
                        + " mMediaData.sMediaId = " + mMediaData.sMediaId);
                if (MediaPlayManager.getInstance().isPlaying()) {
                    mPlayView.setImageDrawable(getResources().getDrawable(R.drawable.media_pause));
                    MediaPlayManager.getInstance().handlePause(mMediaData.sMediaId);
                } else {
                    mPlayView.setImageDrawable(getResources().getDrawable(R.drawable.media_play));
                    MediaPlayManager.getInstance().handlePlay(mMediaData.sMediaId);
                }
                break;
            case R.id.media_next:
                Log.d(TAG, "onClick() handlePlayNext");
                MediaPlayManager.getInstance().handlePlayNext(null);
                break;
            case R.id.media_repeat:
                try {
                    MediaPlayManager.PlayMode mode = MediaPlayManager.getInstance().getPlayMode();
                    Log.d(TAG, "onClick() handlePlayMode"
                    +" mode = "+mode);
                    if (null == mode) {
                        mode = MediaPlayManager.PlayMode.CYCLE_ALL;
                    }
                    MediaPlayManager.PlayMode newMode = getNewPlayModeAfterClick(mode);
                    Log.d(TAG, "onClick() handlePlayMode newMode = "+newMode);
                    updatePatternView(newMode);
                    if (isTemplateMode()) {
                        MediaPlayManager.getInstance().switchPlayMode(newMode, mMediaData);
                    }
                    Log.d(TAG, "onClick() handlePlayMode newMode = "+newMode
                            +" mode = "+mode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.media_list:
                showPlayList();
                break;
            case R.id.media_favorite:
                doFavoriteClicked();
                break;

            default:
                break;
        }
    }


    private void updatePatternView(@Nullable MediaPlayManager.PlayMode mode) {
        try {
            Log.d(TAG, "updatePatternView mode = "+mode);
            MediaPlayManager.PlayMode playMode = mode;
            if (mMediaData != null) {
                if (playMode == null) {
                    playMode = MediaPlayManager.getInstance().getPlayMode();
                }

                if (playMode == null) {
                    playMode = MediaPlayManager.PlayMode.CYCLE_ALL;
                }
                Log.d(TAG, "updatePatternView playMode = "+playMode);

                switch (playMode) {
                    case RANDOM:
                        mRepeatView.setImageDrawable(getResources().getDrawable(R.drawable.mod_random));
                        break;
                    case SINGLE_CYCLE:
                        mRepeatView.setImageDrawable(getResources().getDrawable(R.drawable.mod_single));
                        break;
                    case CYCLE_ALL:
                        mRepeatView.setImageDrawable(getResources().getDrawable(R.drawable.mod_cycle));
                        break;
                    case ORDER:
                        mRepeatView.setImageDrawable(getResources().getDrawable(R.drawable.mod_order));
                        break;
                    default:
                        break;
                }
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "handleMessage()...msg.what:" + msg.what + " mMediaData = "+mMediaData);
        switch (msg.what) {
            case MSG_KEY_LOAD_MEDIA:
                if (mMediaData == null) {
                    break;
                }
                Log.d(TAG, "handleMessage()...load media:" + mMediaData);

                if (null != mSeekBarView) {
                    mSeekBarView.setProgress(0);
                }
                if (mMediaData.iTryBegin != 0 && mMediaData.iTryEnd != 0) {
                    MediaPlayManager.getInstance().handleSeekTo(mMediaData.iTryBegin);
                }
                mTitleView.setText(mMediaData.mMediaName);

                stopAutoUpdateSingerBg();
                initCoverAndBackground();
                initMediaInfo();
                Log.i(TAG, "changeMusicViewMode ifFavroited = " + mMediaData.ifFavroited);


                //收藏的功能入口
                if (TextUtils.equals(mMediaData.isCollect, "true") || TextUtils.equals(mMediaData.isCollect, "false")) {
                    setViewVisiable(mFavoriteView, View.VISIBLE);
                    if (TextUtils.equals(mMediaData.isCollect, "true")) { //状态为收藏
                        mFavoriteView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.media_favorited));
                    } else if (TextUtils.equals(mMediaData.isCollect, "false")) {//状态为非收藏
                        mFavoriteView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.media_unfavorite));
                    }
                } else {
                    setViewVisiable(mFavoriteView, View.GONE);
                }
                // TODO: 2020-03-17  收藏和没收藏都要去拉取最新的状态，服务已经支持收藏取消xxx歌曲，切换到这首歌曲时状态可能变化了
                break;
            case MSG_KEY_UPDATE_BG:
                if (mMediaData == null) {
                    break;
                }
                Log.d(TAG, "MSG_KEY_UPDATE_BG sAlbumPic=" + mMediaData.sAlbumPic);

                if (mMediaData.mBgUrl != null) {
                    Log.d(TAG, "MSG_KEY_UPDATE_BG mBgUrl = " + mMediaData.mBgUrl);
                    mPresenter.loadImage(mMediaData.mBgUrl, null, new TemplatePresenter.Callback() {
                        @Override
                        public void onLoadImageEnd(ImageView imageView, Bitmap bitmap) {
                            Log.d(TAG, "onLoadImageEnd bitmap=" + bitmap);
                            mBgLayout.setBackground(new BitmapDrawable(bitmap));
                        }
                    });
                }
                break;
            case MSG_KEY_UPDATE_MEDIA:
                if (msg.obj != null) {
                    refreshDuration((int) msg.obj);
                }
                break;
            default:
                break;
        }
        return false;
    }

    public void refreshDuration(int nPercent) {
        if (mMediaData == null) {
            return;
        }
        long totalDuration = MediaPlayManager.getInstance().getTotalDuration() / 1000;
        long pastDuration = MediaPlayManager.getInstance().getPlayDuration() / 1000;
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

    private void handleMediaStatusChange(MediaData mediaData, MediaPlayManager.MediaState state) {
        Log.i(TAG, "handleMediaStatusChange mMediaData = "+ mediaData + " state = "+mediaData);

        if (mMediaData == null) {
            Log.i(TAG, "handleMediaStatusChange mMediaData == null");
            mMediaData = mediaData;
            mUIHandler.removeMessages(MSG_KEY_LOAD_MEDIA);
            mUIHandler.sendEmptyMessage(MSG_KEY_LOAD_MEDIA);
        } else {
            Log.i(TAG, "handleMediaStatusChange mMediaData != null");
//            changeMusicViewMode();
            if (state ==  MediaPlayManager.MediaState.INIT) {
                //用id判断是否重复加载，用歌名是否为空判断数据是否加载
                if (null != mMediaData ) {
                    //上个状态的操作初始化
                    mUIHandler.removeCallbacksAndMessages(null);
                    mWorkHandler.removeCallbacksAndMessages(null);
                    mMediaIdQueue.offer(mediaData.sMediaId);

                    mMediaData = mediaData;

                    mUIHandler.removeMessages(MSG_KEY_LOAD_MEDIA);
                    mUIHandler.sendEmptyMessage(MSG_KEY_LOAD_MEDIA);
                }
            } else {
                mUIHandler.removeMessages(MSG_KEY_UPDATE_MEDIA);
                mUIHandler.sendEmptyMessage(MSG_KEY_UPDATE_MEDIA);
            }
        }
    }
    private SeekBar.OnSeekBarChangeListener mBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) {
            //Log.d(TAG, "onProgressChanged:" + seekBar.getProgress()+" progress = "+progress);
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    refreshDuration(seekBar.getProgress());
                }
            });
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "onProgressChanged:" + seekBar.getProgress());
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d(TAG, "onStopTrackingTouch:");
            try {
                if (seekBar != null) {
                    float scale = 1f * seekBar.getProgress() / seekBar.getMax();
                    int pos = Math.round(MediaPlayManager.getInstance().getTotalDuration() * scale);

                    Log.d(TAG, "onStopTrackingTouch getProgress:" + seekBar.getProgress()+" scale = "+scale
                            +" pos = "+pos);
                    mSeekBarView.setProgress(seekBar.getProgress());
                    MediaPlayManager.getInstance().handleSeekTo(pos);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i(TAG, "onDetachedFromWindow is in");
        MediaPlayManager.getInstance().removeMediaStatusListener(this);
        if (getBackView().getVisibility() == View.GONE || getBackView().getAlpha() == 0.0f) {
            getBackView().setVisibility(VISIBLE);
            getBackView().setAlpha(1.0f);
        }

        if(null != mMediaIdQueue){
            mMediaIdQueue.clear();
        }
        closePlayList();
    }

    private void closePlayList() {
        if (mListPopWin != null && mListPopWin.isShowing()) {
            mListPopWin.dismiss();
            mListPopWin = null;
        }
    }

    private void showPlayList() {
        Log.d(TAG, "showPlayList");
        if (mListPopWin == null) {
            mListPopWin = new MediaListPopWin(getContext());
        }
        mListPopWin.refreshListView();
        if (null != mMediaList) {
            mMediaList.post(new Runnable() {
                @Override
                public void run() {
                    mListPopWin.showAtLocation(mMediaList, Gravity.BOTTOM, 0, 0);
                }
            });
        }
    }


    private void doFavoriteClicked() {
        if (mMediaData == null || MediaPlayManager.getInstance() ==  null) {
            Log.e(TAG, "doFavoriteClicked error");
            return;
        }
        if (mMediaData.ifFavroited == 1) {
            mFavoriteView.setImageDrawable(getResources().getDrawable(R.drawable.media_unfavorite));
            MediaPlayManager.getInstance().handleFavorite(false, mMediaData.sSkillInfo, mMediaData.sMediaId);
            mMediaData.ifFavroited = 0;
        } else {
            mFavoriteView.setImageDrawable(getResources().getDrawable(R.drawable.media_favorited));
            mMediaData.ifFavroited = 1;
            MediaPlayManager.getInstance().handleFavorite(true, mMediaData.sSkillInfo, mMediaData.sMediaId);
        }
        Log.d(TAG, "doFavoriteClicked ifFavroited:" + mMediaData.ifFavroited);
    }

    private MediaPlayManager.PlayMode getNewPlayModeAfterClick(MediaPlayManager.PlayMode inCurMode) {
        MediaPlayManager.PlayMode newMode = null;
        switch (inCurMode) {
            case RANDOM:
                newMode = MediaPlayManager.PlayMode.CYCLE_ALL;
                break;
            case SINGLE_CYCLE:
                newMode = MediaPlayManager.PlayMode.RANDOM;
                break;
            case CYCLE_ALL:
                newMode = MediaPlayManager.PlayMode.ORDER;
                break;
            case ORDER:
                newMode = MediaPlayManager.PlayMode.SINGLE_CYCLE;
                break;
            default:
                break;
        }
        return newMode;
    }

    @Override
    public void onMediaStatusChange(MediaData mediaData, MediaPlayManager.MediaState state) {
        Log.i(TAG, "onMediaStatusChange state ：" + state+" mediaData = "+mediaData);
        if (null == mediaData) {
            return;
        }
        if (mMediaData == null) {
            mMediaData = mediaData;
            Log.i(TAG, "handleMediaStatusChange mMediaData == null");
            mUIHandler.removeMessages(MSG_KEY_LOAD_MEDIA);
            mUIHandler.sendEmptyMessage(MSG_KEY_LOAD_MEDIA);
        } else {
            if (state == MediaPlayManager.MediaState.INIT) {
                //用id判断是否重复加载，用歌名是否为空判断数据是否加载
                if (null != mMediaData) {
                    //上个状态的操作初始化
                    mUIHandler.removeCallbacksAndMessages(null);
                    mWorkHandler.removeCallbacksAndMessages(null);
                    mMediaIdQueue.offer(mediaData.sMediaId);

                    mMediaData = mediaData;
                    stopAutoUpdateSingerBg();

                    mUIHandler.removeMessages(MSG_KEY_LOAD_MEDIA);
                    mUIHandler.sendEmptyMessage(MSG_KEY_LOAD_MEDIA);
                }
            } else {
                //如果播放列表正在显示，不进行模式切换
                if (null != mListPopWin && mListPopWin.isShowing()) {
                    return;
                }
                if (state == MediaPlayManager.MediaState.PAUSE || state == MediaPlayManager.MediaState.STOP) {
                    stopAutoUpdateSingerBg();
                } else if (state == MediaPlayManager.MediaState.PLAY) {
                    startAutoUpdateSingerBg();
                }
//                initCurShowPage(true);
                mUIHandler.removeMessages(MSG_KEY_UPDATE_MEDIA);
                mUIHandler.sendEmptyMessage(MSG_KEY_UPDATE_MEDIA);
            }
            updatePatternView(MediaPlayManager.getInstance().getPlayMode());
        }
    }

    @Override
    public void onUpdateProgress(int percent) {
        refreshDuration(percent);
    }

    @Override
    public void onMediaDisAlive() {
        Log.i(TAG, "onMediaDisAlive");
        if (mPlayView != null) {
            mPlayView.setImageDrawable(getResources().getDrawable(R.drawable.media_pause));
        }
    }

    @Override
    public void onRefreshSongList() {

    }

    private static final int UPDATE_BG_DURATION = 3 * 1000;

    private void startAutoUpdateSingerBg() {
        if (null != mUIHandler && MediaPlayManager.getInstance().isPlaying()) {
            mUIHandler.removeMessages(MSG_KEY_UPDATE_BG);
            mUIHandler.sendEmptyMessageDelayed(MSG_KEY_UPDATE_BG, UPDATE_BG_DURATION);
        }
    }

    private void stopAutoUpdateSingerBg() {
        if (null != mUIHandler) {
            mUIHandler.removeMessages(MSG_KEY_UPDATE_BG);
        }
    }
    
    private void initCoverAndBackground() {
        Log.i(TAG, "loadImage sAlbumPic() mMediaData.sAlbumPic = " + mMediaData.sAlbumPic);
        if (!TextUtils.isEmpty(mMediaData.sAlbumPic)) {
            //初始化专辑
            if (mIconFrameLayout != null) {
                mPresenter.loadImage(mMediaData.sAlbumPic, null, new TemplatePresenter.Callback() {
                    @Override
                    public void onLoadImageEnd(ImageView imageView, Bitmap bitmap) {
                        Log.i(TAG, "loadImage sAlbumPic() imageView = " + imageView + " bitmap: " + bitmap);
                        mIconFrameLayout.setBackground(new BitmapDrawable(bitmap));
                    }
                });
            }
        }
    }

}
