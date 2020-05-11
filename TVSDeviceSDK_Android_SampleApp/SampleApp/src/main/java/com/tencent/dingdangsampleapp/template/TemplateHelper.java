package com.tencent.dingdangsampleapp.template;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import android.util.Log;

import com.tencent.dingdangsampleapp.template.data.AudioTmplData;
import com.tencent.dingdangsampleapp.template.data.BaseAIData;
import com.tencent.dingdangsampleapp.template.data.BaseTemplateData;
import com.tencent.dingdangsampleapp.template.data.ListTmplData;
import com.tencent.dingdangsampleapp.template.data.ImageTmplData;
import com.tencent.dingdangsampleapp.template.data.MediaData;
import com.tencent.dingdangsampleapp.template.data.TextTmplData;
import com.tencent.dingdangsampleapp.template.view.ui.AbstractTemplate;
import com.tencent.dingdangsampleapp.template.view.ui.BriefImageTemplateView;
import com.tencent.dingdangsampleapp.template.view.ui.ListTemplateView;
import com.tencent.dingdangsampleapp.template.view.ui.ImageTemplateView;
import com.tencent.dingdangsampleapp.template.view.ui.MediaPlayerTemplateView;
import com.tencent.dingdangsampleapp.template.view.ui.TextTemplateView;
import com.tencent.dingdangsampleapp.template.view.viewinterface.IBaseView;
import com.tencent.dingdangsampleapp.tskuidata.TemplateInfo;
import com.tencent.dingdangsampleapp.tskuidata.TskData;
import com.tencent.dingdangsampleapp.tskuidata.listitem.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateHelper {
    private static final String TAG = "TemplateHelper";

    private static final TemplateHelper ourInstance = new TemplateHelper();

    private static int mMaxPoolSize = 1;

    public static TemplateHelper getInstance() {
        return ourInstance;
    }

    private BaseAIData mData;
    private Object mLock = new Object();
    private BaseAIData mAudioData;

    //通用模板
    protected static Map<String, Pools.SynchronizedPool> mTemplateMapping = new HashMap<>();

    static {
        mTemplateMapping.put("10001", new Pools.SynchronizedPool<TextTemplateView>(mMaxPoolSize));//长文本模版
        mTemplateMapping.put("10002", new Pools.SynchronizedPool<ImageTemplateView>(mMaxPoolSize));//多图模版
        mTemplateMapping.put("20001", new Pools.SynchronizedPool<BriefImageTemplateView>(mMaxPoolSize));//大图模版
        mTemplateMapping.put("20002", new Pools.SynchronizedPool<TextTemplateView>(mMaxPoolSize));//短文本模版
        mTemplateMapping.put("20004", new Pools.SynchronizedPool<ImageTemplateView>(mMaxPoolSize));//图文模板B
        mTemplateMapping.put("30002", new Pools.SynchronizedPool<ListTemplateView>(mMaxPoolSize));//列表模板
        mTemplateMapping.put("50001", new Pools.SynchronizedPool<ListTemplateView>(mMaxPoolSize));//音频模板
        mTemplateMapping.put("50002", new Pools.SynchronizedPool<ListTemplateView>(mMaxPoolSize));//音频模板
        mTemplateMapping.put("80002", new Pools.SynchronizedPool<ListTemplateView>(mMaxPoolSize));//音频模板 test
    }

    /**
     * 判断模板id是否有效
     *
     * @param templateInfo
     * @return
     */
    public boolean isValidTemplateInfo(@Nullable TemplateInfo templateInfo) {
        Log.i(TAG, "isValidTemplateInfo = " + templateInfo.mID);
        return mTemplateMapping.containsKey(templateInfo.mID);
    }

    public BaseAIData getTemplateData() {
        synchronized (mLock) {
            return mData;
        }
    }

    public void setAIData(@Nullable BaseAIData baseAIData) {
        synchronized (mLock) {
            Log.i(TAG, "set new data" + baseAIData);
            mData = baseAIData;
        }
    }

    public BaseAIData getAudioTemplateData() {
        return mAudioData;
    }

    public void setAudioAIData(@Nullable BaseAIData baseAIData) {
        mAudioData = baseAIData;
    }

    /**
     * 解析数据
     *
     * @param data
     * @return
     */
    public BaseAIData parseTemplateData(@Nullable TskData data) {
        Log.i(TAG, "parseTemplateData");
        BaseAIData baseAIData = null;
        if (data.mTmplInfo != null) {
            //解析 items
            String title = "";
            String content = "";
            List<String> imgUrl = new ArrayList<>();
            List<Items> itemsList = data.mListItems.mItemsList;
            if (null != itemsList && itemsList.size() > 0) {
                Log.i(TAG, "items size is " + String.valueOf(itemsList.size()));
                for (int i = 0; i < itemsList.size(); i++) {
                    Items mItem = itemsList.get(i);
                    if (null != mItem) {
                        if (mItem.title != null) {
                            title += mItem.title + "\n";
                        }
                        if (mItem.textContent != null) {
                            content += mItem.textContent + "\n";
                        }
                    }
                    if (null != mItem && mItem.image != null && mItem.image.sourcesList != null && mItem.image.sourcesList.size() > 0) {
                        imgUrl.add(mItem.image.sourcesList.get(0).url);
                        Log.i(TAG, "imgUrl " + i + "|" + mItem.image.sourcesList.get(0).url);
                    }
                    if (null != mItem && mItem.backgroundImage != null && mItem.backgroundImage.sourcesList != null && mItem.backgroundImage.sourcesList.size() > 0) {

                        Log.i(TAG, "backgroundImage " + i + "|" + mItem.backgroundImage.sourcesList.get(0).url);
                    }
                }
            }
            String tid = data.mTmplInfo.mID;
            Log.i(TAG, "tid = " + tid);
            //logo
            String skillName = "";
            String skillIcon = "";
            if (data.mBaseInfo != null) {
                skillIcon = data.mBaseInfo.skillIcon;
                skillName = data.mBaseInfo.skillName;
            }

            if ("10001".equals(tid) || "20002".equals(tid)) {//纯文本

                Log.i(TAG, "纯文本");
                baseAIData = new TextTmplData();
            } else if ("20001".equals(tid) || "20003".equals(tid) || "20004".equals(tid)) {
                Log.i(TAG, "图文");
                baseAIData = new ImageTmplData();
                ((ImageTmplData) baseAIData).mTitle = title;
                ((ImageTmplData) baseAIData).mSkillName = skillName;
                if (data.mGlobalInfo != null && data.mGlobalInfo.backgroundImage != null
                        && data.mGlobalInfo.backgroundImage.sourcesList != null &&
                        data.mGlobalInfo.backgroundImage.sourcesList.size() > 0) {
                    ((ImageTmplData) baseAIData).mBgUrl = data.mGlobalInfo.backgroundImage.sourcesList.get(0).url;
                    Log.i(TAG, "bg url = " + ((ImageTmplData) baseAIData).mBgUrl);
                } else {
                    ((ImageTmplData) baseAIData).mBgUrl = imgUrl.get(0);
                }
            } else if ("30001".equals(tid) || "30002".equals(tid) || "40001".equals(tid)) {//宫格
                Log.i(TAG, "宫格");
                baseAIData = new ListTmplData(data.mListItems.mItemsList);
                if (data.mControlInfo.type.equals("AUDIO")) {
                    AudioTmplData mAudio = new AudioTmplData(data.mListItems.mItemsList, tid, data);
                    MediaData mData = mAudio.getCurMediaData();
                }
                Log.i(TAG, "create grid data");
            } else if ("50001".equals(tid)|| "50002".equals(tid) ) {
                Log.i(TAG, "音频 mGlobalInfo = "+data.mGlobalInfo);
                //音频模版的逻辑复用之前的view,50001复用音乐的view,60001复用新闻的view
                AudioTmplData mAudio = new AudioTmplData(data.mListItems.mItemsList, tid, data);
                MediaData mData = mAudio.getCurMediaData();
                mData.sSkillInfo = data.mTmplInfo.mSkillInfo;
                mData.vSingerPic = new ArrayList<>(imgUrl);
                if (data.mGlobalInfo != null && data.mGlobalInfo.backgroundImage != null
                        && data.mGlobalInfo.backgroundImage.sourcesList != null &&
                        data.mGlobalInfo.backgroundImage.sourcesList.size() > 0) {
                    mData.sAlbumPic = data.mGlobalInfo.backgroundImage.sourcesList.get(0).url;
                    Log.i(TAG, "bg url = " + mData.sAlbumPic);
                }
                mData.sSkillIcon = skillIcon;
                baseAIData = mAudio;
                Log.i(TAG, "title =" + title);
                Log.i(TAG, "content = " + content);
                Log.i(TAG, "sSkillInfo = " + mData.sSkillInfo);
                ((BaseTemplateData) baseAIData).mTemplateID = tid;
                ((BaseTemplateData) baseAIData).mTitle = title;
                ((BaseTemplateData) baseAIData).mContent = content;
//                setAIData(baseAIData);
                setAIData(baseAIData);
                Log.i(TAG, "create audio data =" + mAudio);
                return baseAIData;
            }
            if (baseAIData != null)//公共字段
            {
                Log.i(TAG, "title =" + title);
                Log.i(TAG, "content = " + content);
                ((BaseTemplateData) baseAIData).mTemplateID = tid;
                ((BaseTemplateData) baseAIData).mLogoUrl = skillIcon;
                ((BaseTemplateData) baseAIData).mTitle = title;
                ((BaseTemplateData) baseAIData).mContent = content;
                setAIData(baseAIData);
                Log.i(TAG, "resolve data success!");
            }
        }
        Log.i(TAG, "parseTemplateData baseAIData = "+baseAIData);
        return baseAIData;
    }


    public AbstractTemplate generateTemplate(final Context context) {
        if (mData == null) {
            Log.i(TAG, "no tsk data to show");
            return null;
        }

        //通用模板
        String tID = ((BaseTemplateData) mData).mTemplateID;//
        Log.i(TAG, "new next template view " + tID);
        AbstractTemplate template = null;
        if (mTemplateMapping.containsKey(tID)) {
            Pools.SynchronizedPool pool = mTemplateMapping.get(tID);
            AbstractTemplate instance = (AbstractTemplate) pool.acquire();
            if ("10001".equals(tID) || "20002".equals(tID)) {
                template = instance == null ? new TextTemplateView(context) : instance;
            } else if ("20003".equals(tID) || "20004".equals(tID)) {
                template = instance == null ? new ImageTemplateView(context) : instance;
            } else if ("20001".equals(tID)) {
                template = instance == null ? new BriefImageTemplateView(context) : instance;
            } else if ("40001".equals(tID) || "30001".equals(tID) || "30002".equals(tID)) {
                template = instance == null ? new ListTemplateView(context) : instance;
            } else if ("50001".equals(tID) || "50002".equals(tID)) {
                template = instance == null ? new MediaPlayerTemplateView(context) : instance;
            }
        } else {
            Pools.SynchronizedPool pool = mTemplateMapping.get("10001");
            AbstractTemplate instance = (AbstractTemplate) pool.acquire();
            template = instance == null ? new TextTemplateView(context) : instance;
        }
        return template;
    }

    /**
     * 回收view
     *
     * @param view
     */
    public void recycleView(IBaseView view, @Nullable BaseTemplateData data) {
        if (data != null && view instanceof AbstractTemplate) {
            Pools.SynchronizedPool pool = mTemplateMapping.get(data.mTemplateID);
            if (pool != null) {
                Log.i(TAG, "release common template view");
                pool.release(view);
                return;
            }
        }
    }
}
