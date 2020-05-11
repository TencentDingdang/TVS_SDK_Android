package com.tencent.dingdangsampleapp.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.tencent.dingdangsampleapp.mediamanager.MediaPlayManager;
import com.tencent.dingdangsampleapp.template.data.MediaData;
import com.tencent.dingdangsampleapp.view.xlistview.XListView;
import com.tencent.dingdangsampleapp.R;

import java.util.ArrayList;


public class MediaListPopWin extends PopupWindow implements View.OnClickListener,
        Handler.Callback, MediaPlayManager.IMediaStatusListener {

    private static final String TAG = "MusicListPopWin";
    private static final int HANDLER_MSG_MEDIA_STATUS_CHANGE = 1;
    private static final int HANDLER_MSG_AUTO_CLOSE = 2;//按照交互的规则，没有任何操作列表20s消失
    private static final int HANDLER_MSG_AUTO_SCROLL = 3;//按照交互的规则，没有任何操作列表滚动回当前播放歌曲


    private static final int TIME_AUTO_CLOSE = 20 * 1000;//自动关闭时间
    private static final int TIME_AUTO_SCROLL = 5 * 1000;//自动关滚动列表时间

    private XListView listMusicView;
    private MusicListAdapter mMusicAdapter;
    private TextView mTitleView;

    private Handler mUIHandler;
    private RelativeLayout mBgLayout;
    private OnItemClickListener mOnItemClickListener;

    private String sSkillInfoRecord = "";

    public MediaListPopWin(final Context mContext) {
        mUIHandler = new Handler(Looper.getMainLooper(), this);
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_list_pop, null);

        initView(view);
        initAttribute(view);
        initContent(mContext);
    }

    private void initContent(Context mContext) {
        try {
            final MediaData mData = MediaPlayManager.getInstance().getCurrentMedia();

            listMusicView.setOnTouchListener(mToucheListener);
            mMusicAdapter = new MusicListAdapter(mContext);
            listMusicView.setAdapter(mMusicAdapter);
            if (MediaPlayManager.getInstance().ifCanPullMore()) {
                listMusicView.setPullRefreshEnable(true);
            } else {
                listMusicView.setPullRefreshEnable(false);
            }

            if (MediaPlayManager.getInstance().ifCanLoadMore() && null != mData) {
                listMusicView.setPullLoadEnable(true);
            } else {
                listMusicView.setPullLoadEnable(false);
            }

            initLoadMoreStatus();
            listMusicView.setXListViewListener(new XListView.IXListViewListener() {
                @Override
                public void onRefresh() {
                    if (TextUtils.isEmpty(sSkillInfoRecord) && !TextUtils.isEmpty(mData.sSkillInfo)) {
                        sSkillInfoRecord = mData.sSkillInfo;
                    }
                    if (MediaPlayManager.getInstance() != null) {
                        MediaPlayManager.getInstance().handleLoadEarlier(sSkillInfoRecord, mData.sMediaId);
                    }
                }

                @Override
                public void onLoadMore() {
                    MediaData mData = MediaPlayManager.getInstance().getCurrentMedia();
                    if (null != mData ) {
                        if (TextUtils.isEmpty(sSkillInfoRecord) && !TextUtils.isEmpty(mData.sSkillInfo)) {
                            sSkillInfoRecord = mData.sSkillInfo;
                        }
                        if (MediaPlayManager.getInstance() != null) {
                            MediaPlayManager.getInstance().handleLoadMore(sSkillInfoRecord, mData.sMediaId);
                        }
                    }
                }
            });

            listMusicView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    Log.d(TAG, "OnItemClick:" + position);
                    MediaData mediaData = (MediaData) mMusicAdapter.getItem(position - 1);
                    Log.d(TAG, "OnItemClick mediaData:" + mediaData);
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onClick();
                    }

                    if (mediaData != null) {
                        MediaData mPlayMedia = MediaPlayManager.getInstance().getCurrentMedia();
                        Log.d(TAG, "OnItemClick mPlayMedia:" + mPlayMedia);
                        if (!TextUtils.isEmpty(mediaData.sMediaId) && null != mPlayMedia
                                && !TextUtils.isEmpty(mPlayMedia.sMediaId) && mPlayMedia.sMediaId.equals(mediaData.sMediaId)) {
                            if (!MediaPlayManager.getInstance().isPlaying()) {
                                MediaPlayManager.getInstance().handlePlay(mediaData.sMediaId);
                            }
                            MediaListPopWin.this.dismiss();
                            return;
                        }
                        if (TextUtils.isEmpty(sSkillInfoRecord) && !TextUtils.isEmpty(mediaData.sSkillInfo)) {
                            sSkillInfoRecord = mediaData.sSkillInfo;
                        }
                        MediaPlayManager.getInstance().handlePlayAnotherAudio(sSkillInfoRecord, mediaData.sMediaId);
                    }
                    MediaListPopWin.this.dismiss();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initAttribute(View view) {
        // 设置外部可点击
        this.setOutsideTouchable(true);
        /* 设置弹出窗口特征 */
        // 设置视图
        this.setContentView(view);
        // 设置弹出窗体的宽和高
        this.setHeight(LinearLayout.LayoutParams.MATCH_PARENT);
        this.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        // 设置弹出窗体可点击
        this.setFocusable(true);
        // 实例化一个ColorDrawable颜色为透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        // 设置弹出窗体的背景
        this.setBackgroundDrawable(dw);
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        view.setOnTouchListener(mToucheListener);
    }

    private void initView(View view) {

        view.findViewById(R.id.music_list_cancel).setOnClickListener(this);

        listMusicView = view.findViewById(R.id.music_list);

        mBgLayout = view.findViewById(R.id.music_list_bg);

        mTitleView = view.findViewById(R.id.music_list_play);
    }

    private View.OnTouchListener mToucheListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {//手指下来的时候,取消之前操作
                    removeSendMsg();
                    break;
                }
                case MotionEvent.ACTION_UP: {//手指离开屏幕，发送延迟消息
                    startSendMsg();
                    break;
                }
            }
            return false;
        }
    };

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        try {
            MediaPlayManager.getInstance().addMediaStatusListener(this);
            //电视直播的列表标题，调整为“频道列表”
            ArrayList<MediaData> mDataList = MediaPlayManager.getInstance().getMediaList();
            if (null != mDataList && mDataList.size() > 0) {
                MediaData mData = mDataList.get(0);
                mTitleView.setText("播放列表");

                if (TextUtils.isEmpty(sSkillInfoRecord)
                        && null != mData
                        && !TextUtils.isEmpty(mData.sSkillInfo)) {
                    sSkillInfoRecord = mData.sSkillInfo;
                }
            }

            startSendMsg();
            super.showAtLocation(parent, gravity, x, y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void dismiss() {
        super.dismiss();
        removeSendMsg();
        if (null != mMusicAdapter) {
            mMusicAdapter.removeHandlerMessage();
        }

        MediaPlayManager.getInstance().removeMediaStatusListener(this);
    }

    public void stopLoadMore() {
        if (null != listMusicView) {
            listMusicView.stopLoadMore();
        }
    }

    public void stopRefresh() {
        if (null != listMusicView) {
            listMusicView.stopRefresh();
        }
    }

    public void refreshListView() {
        removeSendMsg();
        autoScrollToPlayItem();
        mMusicAdapter.updateMusicList();
        startSendMsg();

        MediaData mCurData = MediaPlayManager.getInstance().getCurrentMedia();
        if (MediaPlayManager.getInstance().ifCanLoadMore() && null != mCurData) {
            listMusicView.setPullLoadEnable(true);
        } else {
            listMusicView.setPullLoadEnable(false);
        }
        mTitleView.setText("播放列表");

        initLoadMoreStatus();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.music_list_cancel:
                dismiss();
            default:
                break;
        }
    }

    @Override
    public void onMediaStatusChange(MediaData mediaData, MediaPlayManager.MediaState state) {
        if (null == mediaData) {
            return;
        }
        mUIHandler.sendEmptyMessage(HANDLER_MSG_MEDIA_STATUS_CHANGE);
    }

    @Override
    public void onUpdateProgress(int percent) {

    }

    @Override
    public void onMediaDisAlive() {

    }

    @Override
    public void onRefreshSongList() {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                listMusicView.stopLoadMore();
            }
        });
        mUIHandler.sendEmptyMessage(HANDLER_MSG_MEDIA_STATUS_CHANGE);
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "handleMessage msg.what=" + msg.what);
        switch (msg.what) {
            case HANDLER_MSG_MEDIA_STATUS_CHANGE:
                refreshListView();
                break;
            case HANDLER_MSG_AUTO_CLOSE:
                dismiss();
                break;
            case HANDLER_MSG_AUTO_SCROLL:
                autoScrollToPlayItem();
                break;
        }
        return false;
    }

    private void autoScrollToPlayItem() {
        //先停止刷新状态
        stopRefresh();
        stopLoadMore();

        int nCurItem = getCurPlayIndex();
        if (-1 != nCurItem) {
            listMusicView.setSelection(nCurItem);
        }
    }

    private void startSendMsg() {
        removeSendMsg();
        if (null != mUIHandler) {
            mUIHandler.sendEmptyMessageDelayed(HANDLER_MSG_AUTO_SCROLL, TIME_AUTO_SCROLL);
            mUIHandler.sendEmptyMessageDelayed(HANDLER_MSG_AUTO_CLOSE, TIME_AUTO_CLOSE);
        }
    }

    private void removeSendMsg() {
        if (null != mUIHandler) {
            mUIHandler.removeMessages(HANDLER_MSG_AUTO_SCROLL);
            mUIHandler.removeMessages(HANDLER_MSG_AUTO_CLOSE);
        }
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public interface OnItemClickListener {
        void onClick();
    }

    private int getCurPlayIndex() {
        int nIndex = -1;
        try {
            ArrayList<MediaData> mList = MediaPlayManager.getInstance().getMediaList();
            MediaData mCurData = MediaPlayManager.getInstance().getCurrentMedia();
            if (null != mList && mList.size() > 0 && null != mCurData) {
                for (int i = 0; i < mList.size(); i++) {
                    MediaData mData = mList.get(i);
                    if (null != mData && !TextUtils.isEmpty(mData.sMediaId) &&
                            !TextUtils.isEmpty(mCurData.sMediaId) && mData.sMediaId.equals(mCurData.sMediaId)) {
                        nIndex = i;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return nIndex;
    }

    private void initLoadMoreStatus() {
        if (MediaPlayManager.getInstance().isLoadEnd()) {
            if (null != listMusicView) {
                listMusicView.loadToEnd();
            }
        }
    }

    public void onResume() {
        autoScrollToPlayItem();
    }
}
