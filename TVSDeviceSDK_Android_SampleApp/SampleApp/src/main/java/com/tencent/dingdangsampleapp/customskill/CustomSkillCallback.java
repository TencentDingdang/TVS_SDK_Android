package com.tencent.dingdangsampleapp.customskill;

import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.CustomSkillData;
import com.tencent.ai.tvs.tvsinterface.CustomSkillNLPInfo;
import com.tencent.ai.tvs.tvsinterface.ICustomSkillCallback;

public class CustomSkillCallback implements ICustomSkillCallback {

    private static final String TAG = "CustomSkillCallback";

    @Override
    public void handleCustomSkill(String dialogRequestId, CustomSkillNLPInfo nlpInfo, CustomSkillData data, String tag) {
        // TODO: 2019/6/1 处理自定义技能
        Log.i(TAG, "handleCustomSkill dialogRequestId: " + dialogRequestId + " tag = " + tag);
        if (nlpInfo != null) {
            Log.i(TAG, "handleCustomSkill domain = " + nlpInfo.getDomain()
                    + " intent = " + nlpInfo.getIntent()
                    + " semantic = " + nlpInfo.getSemantic());
        }
        //自建技能数据，服务中定义的"feedbackAttributes"字段，如果没有部署服务，此data为空
        if (data != null) {
            Log.i(TAG, "handleCustomSkill data = "+data.getControlData());
        }
    }
}
