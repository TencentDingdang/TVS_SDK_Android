package com.tencent.dingdangsampleapp.template;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.tencent.ai.tvs.capability.userinterface.data.UIDataMessageBody;
import com.tencent.dingdangsampleapp.SampleApplication;
import com.tencent.dingdangsampleapp.activity.OfflineWebActivity;
import com.tencent.dingdangsampleapp.template.data.BaseAIData;
import com.tencent.dingdangsampleapp.tskuidata.BaseInfo;
import com.tencent.dingdangsampleapp.tskuidata.ControlInfo;
import com.tencent.dingdangsampleapp.tskuidata.ExtraInfo;
import com.tencent.dingdangsampleapp.tskuidata.GlobalInfo;
import com.tencent.dingdangsampleapp.tskuidata.TemplateInfo;
import com.tencent.dingdangsampleapp.tskuidata.TskData;
import com.tencent.dingdangsampleapp.tskuidata.listitem.Audio;
import com.tencent.dingdangsampleapp.tskuidata.listitem.Image;
import com.tencent.dingdangsampleapp.tskuidata.listitem.Items;
import com.tencent.dingdangsampleapp.tskuidata.listitem.ListItems;
import com.tencent.dingdangsampleapp.tskuidata.listitem.Video;
import com.tencent.dingdangsampleapp.tskuidata.listitem.data.ExpandAbility;
import com.tencent.dingdangsampleapp.tskuidata.listitem.data.Metadata;
import com.tencent.dingdangsampleapp.tskuidata.listitem.data.Sources;
import com.tencent.dingdangsampleapp.tskuidata.listitem.data.Stream;
import com.tencent.ai.tvs.offlinewebtemplate.OfflineWebManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * 解析UI数据管理类
 */
public class UIDataManager {

    private static final String TAG = "UIDataManager";
    //是否使用离线WebSDK UI模板，默认使用
    private static boolean useWebSDKTemplate = true;

    public static void handleUiData(String dialogRequestId, UIDataMessageBody uiData, boolean isShow, IUIDataCallback callback) {
        new Handler(Looper.getMainLooper()).post(() -> doHandleUIData(
                uiData.jsonUI.data, "", isShow, dialogRequestId, callback));
    }

    private static void openNativeWebActivity() {
        Intent intent = new Intent(SampleApplication.getInstance(), OfflineWebActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SampleApplication.getInstance().startActivity(intent);
    }


    /**
     * 解析通用字段部分的方法
     *  @param jsonData
     */
    public static void doHandleUIData(String jsonData, String query, boolean isShow,
                                      String dialogRequestId, IUIDataCallback callback) {
        Log.i(TAG, "doHandleUIData UIDataManager#doHandleUIData() userWebSDKTemplate = " + useWebSDKTemplate);

        JSONObject mJson = null;
        BaseAIData mAiData = null;

        Log.i(TAG, "doHandleUIData jsonData : " + jsonData
                + "\n query : " + query);

        try {
            /**
             * 如果下发的模版是支持web模版方式展示的，就不需要继续走native层的模版解析和展示逻辑了
             */
            if (useWebSDKTemplate && OfflineWebManager.getInstance().isCanOpenByWebTemplate(jsonData, query,
                    false, dialogRequestId)) {
                Log.i(TAG, "doHandleUIData openWebTemplateUI is success dialogRequestId = "+dialogRequestId);
                //需要打开一个装载webview的activity
                openNativeWebActivity();

                String sType = new JSONObject(jsonData).optJSONObject("controlInfo").optString(
                        "type");

                return;
            }

            mJson = new JSONObject(jsonData);
            if (null != mJson) {
                TskData mTskData = new TskData();

                //controlInfo解析
                JSONObject mControlInfo = mJson.optJSONObject("controlInfo");
                if (null != mControlInfo) {
                    ControlInfo mControlObj = new ControlInfo();
                    mControlObj.version = mControlInfo.optString("version");
                    mControlObj.type = mControlInfo.optString("type");
                    mControlObj.textSpeak = mControlInfo.optString("textSpeak");
                    mControlObj.audioConsole = mControlInfo.optString("audioConsole");
                    mControlObj.orientation = mControlInfo.optString("orientation");
                    mTskData.mControlInfo = mControlObj;
                }

                //baseInfo解析
                JSONObject mBaseInfo = mJson.optJSONObject("baseInfo");
                if (null != mBaseInfo) {
                    BaseInfo mBaseObj = new BaseInfo();
                    mBaseObj.skillIcon = mBaseInfo.optString("skillIcon");
                    mBaseObj.skillName = mBaseInfo.optString("skillName");

                    mTskData.mBaseInfo = mBaseObj;
                }

                //globalInfo解析
                JSONObject mGlobalInfo = mJson.optJSONObject("globalInfo");
                if (null != mGlobalInfo) {
                    GlobalInfo mGlobalObj = new GlobalInfo();

                    //解析image
                    JSONObject imageJson = mGlobalInfo.optJSONObject("backgroundImage");
                    if (null != imageJson) {
                        Image image = new Image();
                        image.contentDescription = imageJson.optString("contentDescription");
                        JSONArray imageList = imageJson.optJSONArray("sources");
                        List<Sources> imageSourceList = parseSourceList(imageList);
                        image.sourcesList = imageSourceList;
                        mGlobalObj.backgroundImage = image;
                    }


                    //解析audio
                    JSONObject audioJson = mGlobalInfo.optJSONObject("backgroundAudio");
                    if (null != audioJson) {
                        Audio audio = new Audio();
                        JSONObject mStreamObj = audioJson.optJSONObject("stream");
                        if (null != mStreamObj) {
                            Stream stream = new Stream();
                            String sUrl = mStreamObj.optString("url");
                            stream.url = sUrl;
                            audio.stream = stream;
                        }
                        audio.metadata = parseMetadata(audioJson);
                        mGlobalObj.backgroundAudio = audio;
                    }


                    mGlobalObj.seeMore = mGlobalInfo.optString("seeMore");
                    mGlobalObj.selfData = mGlobalInfo.optString("selfData");
                    //用于指示当前下发的列表数据与终端已缓存列表数据之间的关系 COVER 覆盖 PRE_APPEND 前置追加 POST_APPEND 后置追加
                    mGlobalObj.listUpdateType = mGlobalInfo.optString("listUpdateType");
                    mGlobalObj.playMode = mGlobalInfo.optString("playMode");
                    mTskData.mGlobalInfo = mGlobalObj;

                    Log.d(TAG, "selfData:" + mTskData.mGlobalInfo.selfData);
                }

                //解析templateInfo
                JSONObject templateJson = mJson.optJSONObject("templateInfo");
                Log.d(TAG, "templateJson: " + templateJson);
                if (null != templateJson) {
                    TemplateInfo mTemplateInfo = new TemplateInfo();
                    mTemplateInfo.mID = templateJson.optString("t_id");
                    mTemplateInfo.mSkillInfo = templateJson.optString("skill_info");
                    mTskData.mTmplInfo = mTemplateInfo;
                    Log.d(TAG, "t_id: " + mTemplateInfo.mID+" skill_info: " + mTemplateInfo.mSkillInfo);
                }

                JSONObject mExtraObj = mJson.optJSONObject("extraInfo");
                if (null != mExtraObj) {
                    ExtraInfo mExtra = new ExtraInfo();
                    mExtra.contentCategory = mExtraObj.optString("contentCategory");
                    mTskData.mExtraInfo = mExtra;
                }

                //解析item数据
                parseListItemsData(mTskData, mJson);

                if (mTskData.mTmplInfo != null && TemplateHelper.getInstance().isValidTemplateInfo(mTskData.mTmplInfo)) {
                    Log.d(TAG, "---通用模板---");
                    mAiData = TemplateHelper.getInstance().parseTemplateData(mTskData);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "doHandleUIData mAiData = "+mAiData + " isShow = " + isShow);
        if (isShow) {
            Log.d(TAG, "onUIDataResult mAiData = "+mAiData);
            callback.onUIDataResult(mAiData);
        }
    }

    private static void parseListItemsData(TskData inTskData, JSONObject inJason) {
        try {
            if (null != inJason) {
                JSONArray listArray = null;
                listArray = inJason.optJSONArray("listItems");

                if (null != listArray) {
                    List<Items> mTempList = new ArrayList<Items>();
                    ListItems mItemList = null;
                    mItemList = new ListItems();
                    mItemList.mItemsList = mTempList;

                    for (int i = 0; i < listArray.length(); i++) {
                        JSONObject itemObj = (JSONObject) listArray.get(i);
                        if (null != itemObj) {
                            Items mItem = new Items();
                            mItem.title = itemObj.optString("title");
                            mItem.subTitle = itemObj.optString("subTitle");
                            mItem.textContent = itemObj.optString("textContent");
                            mItem.htmlView = itemObj.optString("htmlView");
                            mItem.mediaId = itemObj.optString("mediaId");
                            mItem.selfData = itemObj.optString("selfData");

                            JSONObject mImageObj = itemObj.optJSONObject("image");
                            if (null != mImageObj) {
                                Image mImage = new Image();
                                mImage.contentDescription = mImageObj.optString(
                                        "contentDescription");

                                JSONArray imageList = mImageObj.optJSONArray("sources");
                                List<Sources> imageSourceList = parseSourceList(imageList);
                                mImage.sourcesList = imageSourceList;
                                mItem.image = mImage;
                            }

                            JSONObject mbackImageObj = itemObj.optJSONObject("backgroundImage");
                            if (null != mbackImageObj) {
                                Image mImage = new Image();
                                mImage.contentDescription = mbackImageObj.optString(
                                        "contentDescription");

                                JSONArray imageList = mbackImageObj.optJSONArray("sources");
                                List<Sources> imageSourceList = parseSourceList(imageList);
                                mImage.sourcesList = imageSourceList;
                                mItem.backgroundImage = mImage;
                            }

                            JSONObject mAudioObj = itemObj.optJSONObject("audio");
                            if (null != mAudioObj) {
                                Audio mAudio = new Audio();
                                JSONObject mStreamObj = mAudioObj.optJSONObject("stream");
                                if (null != mStreamObj) {
                                    Stream stream = new Stream();
                                    String sUrl = mStreamObj.optString("url");
                                    stream.url = sUrl;
                                    mAudio.stream = stream;
                                }

                                mAudio.metadata = parseMetadata(mAudioObj);

                                mItem.audio = mAudio;
                            }

                            JSONObject mVideoObj = itemObj.optJSONObject("video");
                            if (null != mVideoObj) {
                                Video mVideo = new Video();

                                JSONArray videoList = mVideoObj.optJSONArray("sources");
                                List<Sources> videoSourceList = parseSourceList(videoList);
                                mVideo.sourcesList = videoSourceList;
                                mVideo.metadata = parseMetadata(mVideoObj);

                                mItem.video = mVideo;
                            }

                            mTempList.add(mItem);
                        }
                    }
                    inTskData.mListItems = mItemList;

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 
     * 解析资源
     */
    private static List<Sources> parseSourceList(JSONArray inSourceArray) {
        List<Sources> mList = null;

        try {
            mList = new ArrayList<Sources>();
            for (int i = 0; i < inSourceArray.length(); i++) {
                JSONObject mSourceObj = (JSONObject) inSourceArray.get(i);
                if (null != mSourceObj) {
                    Sources mSource = new Sources();
                    mSource.size = mSourceObj.optString("size");
                    mSource.url = mSourceObj.optString("url");
                    mSource.widthPixels = mSourceObj.optInt("widthPixels");
                    mSource.heightPixels = mSourceObj.optInt("heightPixels");

                    mList.add(mSource);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mList;
    }

    private static Metadata parseMetadata(JSONObject inJsonObj) {
        Metadata mData = null;
        if (null != inJsonObj) {
            JSONObject metaObj = inJsonObj.optJSONObject("metadata");
            if (null != metaObj) {
                mData = new Metadata();
                mData.offsetInMilliseconds = metaObj.optInt("offsetInMilliseconds");
                mData.totalMilliseconds = metaObj.optInt("totalMilliseconds");
                mData.groupId = metaObj.optString("groupId");
                JSONObject expandAblityObj = metaObj.optJSONObject("expandAbility");
                ExpandAbility mExpandAbility = null;
                if (null != expandAblityObj) {
                    mExpandAbility = new ExpandAbility();
                    mExpandAbility.isCollect = expandAblityObj.optString("isCollect");
                    mExpandAbility.hasMV = expandAblityObj.optString("hasMV");
                    mExpandAbility.hasLyrics = expandAblityObj.optString("hasLyrics");
                    mExpandAbility.isSupportPlayMode = expandAblityObj.optString(
                            "isSupportPlayMode");
                }
                mData.expandAbility = mExpandAbility;
            }
        }

        return mData;
    }


    public interface IUIDataCallback {

        /**
         * 获得UI数据回调
         *
         * @param uiData UI数据
         */
        void onUIDataResult(BaseAIData uiData);
    }

}
