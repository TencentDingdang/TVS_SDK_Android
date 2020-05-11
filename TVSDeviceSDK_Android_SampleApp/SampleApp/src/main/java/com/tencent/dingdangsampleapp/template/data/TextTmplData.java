package com.tencent.dingdangsampleapp.template.data;

import android.os.Parcel;

public class TextTmplData extends BaseTemplateData{

    public TextTmplData(){}

    public TextTmplData(Parcel in) {
        super(in);
    }

    public static final Creator<TextTmplData> CREATOR = new Creator<TextTmplData>() {
        @Override
        public TextTmplData createFromParcel(Parcel source) {
            return new TextTmplData(source);
        }

        @Override
        public TextTmplData[] newArray(int size) {
            return new TextTmplData[size];
        }
    };
}
