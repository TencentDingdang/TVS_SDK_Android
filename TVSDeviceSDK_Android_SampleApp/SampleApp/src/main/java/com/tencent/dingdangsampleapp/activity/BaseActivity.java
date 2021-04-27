package com.tencent.dingdangsampleapp.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;

import androidx.fragment.app.FragmentActivity;
import com.tencent.dingdangsampleapp.BuildConfig;

public abstract class BaseActivity extends FragmentActivity {

    private static final String TAG = "BaseActivity";

    private static final int ONE_DAY = 24 * 60 * 60 * 1000;

    private CountDownTimer timer;
    private Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放资源
        release();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    public abstract String getTag();

    /**
     * 释放资源对外接口
     */
    public void release() {
        onRelease();
    }

    /**
     * 资源释放内部回调接口，派生类应在此进行资源的清理，同时注意在onResume中判断是否需要重新加载资源
     */
    protected void onRelease() {
        Log.d(TAG, "onRelease: " + this);
    }

     /* 退出，默认finish
     *
     * 每个界面如果有特别的退出逻辑，必须需要继承这个方法，比如音乐播放界面
     */
    public void exit() {
        finish();
    }
}
