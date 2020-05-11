package com.tencent.dingdangsampleapp.template.data;

import android.os.Parcel;
import android.os.Parcelable;

public abstract  class BaseAIData implements Parcelable{

    //DialogRequestId
   public String mDialogRequestId;
    public int mDataType;
    // 文案回复语
    public String mDisplayReplyText;

    //会话是否完成
    protected boolean mIsSessionComplete = true;

    protected boolean mIsEmpty = true;

    //背景图的链接
    public String sBackground;

    public BaseAIData() {
    }

    protected BaseAIData(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.mIsEmpty ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mIsSessionComplete ? (byte) 1 : (byte) 0);
        dest.writeString(mDisplayReplyText);
        dest.writeString(mDialogRequestId);
    }

    protected void readFromParcel(Parcel in){
        this.mIsEmpty = in.readByte() != 0;
        this.mIsSessionComplete = in.readByte() != 0;
        this.mDialogRequestId = in.readString();
    }

    public void setIsSessionComplete(boolean isSessionComplete) {
        this.mIsSessionComplete = isSessionComplete;
    }

    public boolean isSessionComplete() {
        return mIsSessionComplete;
    }

    public void setIsEmpty(boolean isEmpty) {
        this.mIsEmpty = isEmpty;
    }

}
