package com.tencent.dingdangsampleapp.template.data;

import android.os.Parcel;


//继承BaseAIData
public class BaseTemplateData extends BaseAIData {

    public String mTemplateID;//通用模板id

    public String mTitle;

    public String mContent;

    public String mLogoUrl;

    public BaseTemplateData(){

    }

    public BaseTemplateData(Parcel in){
        super(in);
    }

    public static final Creator<BaseTemplateData> CREATOR = new Creator<BaseTemplateData>() {
        @Override
        public BaseTemplateData createFromParcel(Parcel source) {
            return new BaseTemplateData(source);
        }

        @Override
        public BaseTemplateData[] newArray(int size) {
            return new BaseTemplateData[size];
        }
    };
}
