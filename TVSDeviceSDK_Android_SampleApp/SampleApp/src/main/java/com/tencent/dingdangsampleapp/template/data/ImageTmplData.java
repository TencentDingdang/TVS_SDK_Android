package com.tencent.dingdangsampleapp.template.data;

import android.os.Parcel;

public class ImageTmplData extends BaseTemplateData {

   public String mSkillName;

    public String[] mImageUrls;//图片地址

    public String mBgUrl;

    public ImageTmplData(){

    }

    public ImageTmplData(Parcel in){
        super(in);
    }

    public static final Creator<ImageTmplData> CREATOR = new Creator<ImageTmplData>() {
        @Override
        public ImageTmplData createFromParcel(Parcel source) {
            return new ImageTmplData(source);
        }

        @Override
        public ImageTmplData[] newArray(int size) {
            return new ImageTmplData[size];
        }
    };
}
