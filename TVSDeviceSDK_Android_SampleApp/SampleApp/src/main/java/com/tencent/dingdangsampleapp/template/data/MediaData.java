package com.tencent.dingdangsampleapp.template.data;

import android.os.Parcel;
import android.text.TextUtils;

import java.util.ArrayList;


/**
 * 媒体播放的实体类
 */
public class MediaData extends BaseAIData {

    private static final String TAG = "MediaData";

    public String sMediaId = "";
    //name
    public String mMediaName = "";

    public String mPerson = "";

    public String sUrl = "";

    public String mBgUrl = "";

    public String mAlbumUrl = "";

    public String mTitle = "";

    public int iSongPlayTime = 0;

    // 直接格式化显示时间，避免在展示时过渡计算，优化性能
    public String sSongPlayTime = "";

    public int iTryBegin = 0;
    public int iTryEnd = 0;

    public String sAlbumPic = "";

    public ArrayList<String> vSingerPic;

    //歌词
    public String sLyrics;

    public String sAlbumId = "";
    public String sTextContent = "";
    public String sArea = "";


    public int eShowType = 0;
    private int currStatus = 0;

    //-1为初始状态，1为收藏状态，0为非收藏状态
    public int ifFavroited = -1;
    //音频模版详情界面的扩展功能控制
    public String isCollect = ""; //是否显示收藏，收藏"true",没有收藏 "false"
    public String hasMV = "";
    public String hasLyrics = "";
    public String isSupportPlayMode = "";

    public String sSkillIcon = null; //技能图标的链接
    public String sSkillName = null; //技能的标题名称
    public String sSkillInfo = null;//模版技能执行动作时需要透传给服务的字段

    public int nShowBgIndex = 0; //音乐扩展的当前显示的哪张歌手背景的标记

    public boolean isMediaInited = false;
    public int mPlayPercent = 0;

    public String sCurTaskType = null;


    public MediaData() {

    }

    protected MediaData(Parcel in) {
        super(in);
        sMediaId = in.readString();
        mMediaName = in.readString();
        mPerson = in.readString();
        sUrl = in.readString();
        mBgUrl = in.readString();
        mTitle = in.readString();
        sAlbumPic = in.readString();
        sAlbumId = in.readString();
        sArea = in.readString();
        eShowType = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(sMediaId);
        dest.writeString(mMediaName);
        dest.writeString(mPerson);
        dest.writeString(sUrl);
        dest.writeString(mBgUrl);
        dest.writeString(mTitle);
        dest.writeString(sAlbumPic);
        dest.writeString(sAlbumId);
        dest.writeString(sArea);
        dest.writeInt(eShowType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MediaData> CREATOR = new Creator<MediaData>() {
        @Override
        public MediaData createFromParcel(Parcel in) {
            return new MediaData(in);
        }

        @Override
        public MediaData[] newArray(int size) {
            return new MediaData[size];
        }
    };

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ").append(sMediaId);
        sb.append(", ").append(mMediaName);
        sb.append(", ").append(mPerson);
        sb.append(", status = ").append(currStatus);
        sb.append(", vSingerPic = ").append(vSingerPic);
        sb.append(" ]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (null == o || !(o instanceof MediaData)) {
            return false;
        }

        return TextUtils.equals(sMediaId, ((MediaData) o).sMediaId);
    }

}
