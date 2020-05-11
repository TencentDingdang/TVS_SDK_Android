package com.tencent.dingdangsampleapp.customdata;

import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.CustomData;
import com.tencent.ai.tvs.tvsinterface.ICustomDataCallback;

import java.util.List;

public class CustomDataCallback implements ICustomDataCallback {

    private static final String TAG = "CustomDataCallback";


    @Override
    public void handleCustomData(String dialogRequestId, List<CustomData> data, String tag) {
        Log.d(TAG, "handleCustomData dialogRequestId = " + dialogRequestId +", tag = " + tag);
        for (int i = 0; i<data.size(); i++) {
            CustomData customData = data.get(i);
            Log.d(TAG, "handleCustomData customData " + i + " = " + customData.toString());
        }
    }
}
