package com.tencent.dingdangsampleapp.template.data;

import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;


import com.tencent.dingdangsampleapp.mediamanager.MediaPlayManager;
import com.tencent.dingdangsampleapp.tskuidata.TskData;
import com.tencent.dingdangsampleapp.tskuidata.listitem.Image;
import com.tencent.dingdangsampleapp.tskuidata.listitem.Items;
import com.tencent.dingdangsampleapp.tskuidata.listitem.data.ExpandAbility;
import com.tencent.dingdangsampleapp.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class AudioTmplData extends BaseTemplateData {
    protected final String TAG = "AudioTmplData";
    public List<Items> mListData;
    private MediaData mPlayData;
    private String sTid;
    private String mlistUpdateType;
    private String mSkillInfo;

    private String mContentType;
    private String mSkillIcon;

    private String mBackgroundImageUrl; // 用于存放背景图信息

    public AudioTmplData(List<Items> data, String tid, TskData taskData) {
        mListData = data;
        sTid = tid;
        if (null != taskData && null != taskData.mGlobalInfo) {
            mlistUpdateType = taskData.mGlobalInfo.listUpdateType;
            Image bgImage = taskData.mGlobalInfo.backgroundImage;
            if (null != bgImage && null != bgImage.sourcesList
                    &&  0 < bgImage.sourcesList.size()) {
                mBackgroundImageUrl = bgImage.sourcesList.get(0).url;
                Log.d(TAG, "backgroundImage url = " + mBackgroundImageUrl);
            }
            Log.d(TAG, "backgroundImage"+taskData.mGlobalInfo.backgroundImage+" mlistUpdateType = "+mlistUpdateType);
        }
        if (null != taskData.mTmplInfo) {
            mSkillInfo = taskData.mTmplInfo.mSkillInfo;
        }
        if (null != taskData && null != taskData.mBaseInfo) {
            mSkillIcon = taskData.mBaseInfo.skillIcon;
        }
        if (null != taskData && null != taskData.mExtraInfo) {
            mContentType = taskData.mExtraInfo.contentCategory;
        }
        convertToMediaData();
        //根据服务下发的播放模式来初始化音频模版的播放模式
        initMediaPlayMode(taskData);
    }

    public AudioTmplData(Parcel in) {
        super(in);
    }

    public static final Creator<AudioTmplData> CREATOR = new Creator<AudioTmplData>() {
        @Override
        public AudioTmplData createFromParcel(Parcel source) {
            return new AudioTmplData(source);
        }

        @Override
        public AudioTmplData[] newArray(int size) {
            return new AudioTmplData[size];
        }
    };

    /**
     * 从模板数据中提取每一个音乐媒体数据
     */
    private void convertToMediaData() {
        Log.i(TAG, "convertToMediaData");
        try {
            if (null != mListData && mListData.size() > 0) {
                ArrayList<MediaData> mAudioList = new ArrayList<MediaData>();
                for (int i = 0; i < mListData.size(); i++) {
                    Items itemobj = mListData.get(i);
                    if (null != itemobj) {
                        MediaData mediaData = getMediaDataByItems(itemobj);
                        mediaData.sCurTaskType = mContentType;
                        mediaData.sSkillInfo = mSkillInfo;
                        mediaData.sSkillIcon = mSkillIcon;
                        mediaData.mBgUrl = mBackgroundImageUrl;
                        mAudioList.add(mediaData);
                    }
                }
                if (null != mAudioList && mAudioList.size() > 0) {
                    mPlayData = mAudioList.get(0); //默认第一项是播放项
                    handleMedialistUpdate(mAudioList);
                }
            } else {
                handleMedialistUpdate(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "convertToMediaData Exception:"+e);
        }
    }

    public MediaData getCurMediaData() {
        return mPlayData;
    }

    /**
     * 更新媒体播放列表
     */
    private void handleMedialistUpdate(ArrayList<MediaData> mAudioList) {
        Log.d(TAG, "handleMedialistUpdate mAudioList:"+mAudioList);
        if (!TextUtils.isEmpty(mlistUpdateType)) {
            processMedialistUpdate(mAudioList, mlistUpdateType);
        } else {
            MediaPlayManager.getInstance().setIfCanLoadMore(false);
            MediaPlayManager.getInstance().setMediaList(mAudioList);
        }
    }


    /**
     * 处理播放列表更新
     */
    public static void processMedialistUpdate(ArrayList<MediaData> mAudioList,
                                              String slistUpdateType) {
        MediaPlayManager.getInstance().setIfCanLoadMore(true);
        //特殊处理，验证喜马拉雅技能，默认打开前部拉取
        MediaPlayManager.getInstance().setIfCanPullMore(true);
        Log.d("AudioTmplData", "processMedialistUpdate mlistUpdateType = "+slistUpdateType+" mAudioList:"+mAudioList);

        if (slistUpdateType.equals("COVER")) { //覆盖列表数据，显示加载更多
            MediaPlayManager.getInstance().setMediaList(mAudioList);
        } else if (slistUpdateType.equals("PRE_APPEND")) {//新下发的数据 追加到 终端已缓存列表的起始位置，显示加载更多
            MediaPlayManager.getInstance().setAppendAtHead(true);
            MediaPlayManager.getInstance().appendMediaList(mAudioList);
        } else if (slistUpdateType.equals("POST_APPEND")) {//新下发的数据 追加到 终端已缓存列表的末尾位置，显示加载更多
            MediaPlayManager.getInstance().setAppendAtHead(false);
            MediaPlayManager.getInstance().appendMediaList(mAudioList);
        } else if (slistUpdateType.equals("PRE_APPEND_END")) {//新下发的数据 追加到
            // 终端已缓存列表的起始位置，显示没有更多数据了
            MediaPlayManager.getInstance().setAppendAtHead(true);
            MediaPlayManager.getInstance().setIfCanPullMore(false);
            MediaPlayManager.getInstance().appendMediaList(mAudioList);
        } else if (slistUpdateType.equals("POST_APPEND_END")) {//新下发的数据 追加到
            // 终端已缓存列表的末尾位置，显示没有更多数据了
            MediaPlayManager.getInstance().setAppendAtHead(false);
            MediaPlayManager.getInstance().setIfCanLoadMore(false);
            MediaPlayManager.getInstance().appendMediaList(mAudioList);
        } else if (slistUpdateType.equals("ALL")) {//覆盖列表数据，不显示加载更多
            MediaPlayManager.getInstance().setIfCanLoadMore(false);
            MediaPlayManager.getInstance().setMediaList(mAudioList);
        }
        MediaPlayManager.getInstance().setIfCanLoadMore(false);// TODO: 2020/4/17 暂时禁止上拉加载更多
        if (null == mAudioList || (null != mAudioList && mAudioList.size() <= 0)) {
            MediaPlayManager.getInstance().notifyRefreshSongList();
        }
    }


    /**
     * 初始化播放模式
     */
    private void initMediaPlayMode(TskData inData) {
        if (null != inData && null != inData.mGlobalInfo) {
            String sPlayMode = inData.mGlobalInfo.playMode;
            Log.d(TAG, "initMediaPlayMode sPlayMode:"+sPlayMode);
            MediaPlayManager.PlayMode mode = null;
            if (TextUtils.equals(sPlayMode, "LIST")) {
                mode = MediaPlayManager.PlayMode.ORDER;
            } else if (TextUtils.equals(sPlayMode, "LIST_CYCLE")) {
                mode = MediaPlayManager.PlayMode.CYCLE_ALL;
            } else if (TextUtils.equals(sPlayMode, "RANDOM")) {
                mode = MediaPlayManager.PlayMode.RANDOM;
            } else if (TextUtils.equals(sPlayMode, "SINGLE_CYCLE")) {
                mode = MediaPlayManager.PlayMode.SINGLE_CYCLE;
            }
            MediaPlayManager.getInstance().setPlayMode(mode);
        }
    }

    public static MediaData getMediaDataByItems(Items itemobj) {
        MediaData mediaData = new MediaData();
        mediaData.sMediaId = itemobj.mediaId;
        mediaData.mMediaName = itemobj.title;
        mediaData.mPerson = itemobj.subTitle;
        mediaData.mAlbumUrl = itemobj.textContent;
        if (null != itemobj.audio && null != itemobj.audio.stream) {
            mediaData.sUrl = itemobj.audio.stream.url;
        }

        //添加专辑图片信息（前景图）
        if (null != itemobj.image && null != itemobj.image.sourcesList &&
                0 < itemobj.image.sourcesList.size()) {
            mediaData.sAlbumPic = itemobj.image.sourcesList.get(0).url;
        }

        if (null != itemobj.audio && null != itemobj.audio.metadata) {
            mediaData.sAlbumId = itemobj.audio.metadata.groupId;
            mediaData.iSongPlayTime = itemobj.audio.metadata.totalMilliseconds;
            mediaData.sSongPlayTime = TimeUtils.secToTimeString(mediaData.iSongPlayTime / 1000);
        }
        //添加歌手图片信息
        /*mediaData.vSingerPic = new ArrayList<String>();
        if (null != itemobj.backgroundImage) {
            List<Sources> mSouceList = itemobj.backgroundImage.sourcesList;
            Log.i("AudioTmplData", "itemobj mSouceList:"+mSouceList);
            if (null != mSouceList && mSouceList.size() > 0) {
                for (int j = 0; j < mSouceList.size(); j++) {
                    Sources mSouce = mSouceList.get(j);
                    if (null != mSouce && !TextUtils.isEmpty(mSouce.url)) {
                        Log.i("AudioTmplData", "getMediaDataByItems url:"+mSouce.url);
                        mediaData.vSingerPic.add(mSouce.url);
                    }
                }
            }
        }*/
        //添加音频模版详情页面的，扩展功能区的控制字段
        if (null != itemobj.audio && null != itemobj.audio.metadata) {
            ExpandAbility expandAbility = itemobj.audio.metadata.expandAbility;
            if (null != expandAbility) {
                mediaData.isCollect = expandAbility.isCollect;
                mediaData.hasMV = expandAbility.hasMV;
                mediaData.hasLyrics = expandAbility.hasLyrics;
                mediaData.isSupportPlayMode = expandAbility.isSupportPlayMode;
            }
        }
        return mediaData;
    }
}
