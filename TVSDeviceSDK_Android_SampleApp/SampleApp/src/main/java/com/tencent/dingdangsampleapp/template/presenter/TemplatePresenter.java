package com.tencent.dingdangsampleapp.template.presenter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.tencent.dingdangsampleapp.template.TemplateHelper;
import com.tencent.dingdangsampleapp.template.data.BaseTemplateData;
import com.tencent.dingdangsampleapp.template.view.ui.TextTemplateView;
import com.tencent.dingdangsampleapp.template.view.viewinterface.IBaseView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 表现层,连接 view 和  data model
 */
public class TemplatePresenter implements IBasePresenter {

    static final String TAG = "TemplatePresenter";

    private IBaseView mView;

    private ImageView mImageView;

    private Callback mCallback;

    public TemplatePresenter(IBaseView view){
        this.mView = view;
    }

    @Override
    public void bindData() {
        Log.i(TAG,"bind data");
        BaseTemplateData data =  (BaseTemplateData) TemplateHelper.getInstance().getTemplateData();
        Log.i(TAG,"" + data);
        mView.setTemplateData(data);
        mView.fillData();
    }

    @Override
    public void recycleView() {
//        mView.clearData();
        TemplateHelper.getInstance().recycleView(mView, mView.getTemplateDataBoundInView());
    }

    @Override
    public void OnTTSStop() {
        if(mView instanceof TextTemplateView){//目前仅有文本模板处理
            ((TextTemplateView)mView).stopScroll();
        }
    }

    @Override
    public void OnTTSStart() {
        if(mView instanceof TextTemplateView){
            ((TextTemplateView)mView).startScroll();
        }
    }

    @Override
    public void doBack(Context context, boolean isForce) {
        //停止tts 播报
        Log.i(TAG,"do back, isForce:" + isForce);

        if(isForce) {
            ((Activity)context).finish();
            Log.i(TAG,"111 ");
            return;
        }
        ViewGroup parent = (ViewGroup) ((View)mView).getParent();
        if(parent != null) {
            int index = parent.indexOfChild((View)mView);
            if(index == 0){
                Log.i(TAG,"activity finish");
                ((Activity)context).finish();
            }else{
                Log.i(TAG,"I will be removed");
                parent.removeView((View)mView);
            }
        }
    }

    @Override
    public void loadImage(String url, ImageView view, Callback callback) {
        Log.i(TAG,"loadImage url = "+url);
        mImageView = view;
        // TODO: 2020-03-13 Demo简易实现，接入方自行实现图片缓存及加载逻辑
        setPicBitmap(url);
        mCallback = callback;
    }

    public void startWebActivity(Context context, String url){

    }

    @Override
    public boolean isTheSameData(){
        if(mView != null){
            BaseTemplateData oldData = mView.getTemplateDataBoundInView();
            BaseTemplateData newData = (BaseTemplateData) TemplateHelper.getInstance().getTemplateData();
            return newData != null && newData.equals(oldData);
        }
        return false;
    }


    private Handler handler = new Handler() {

        public void handleMessage(Message message) {

            Bitmap bitmap = (Bitmap) message.obj;
            if (mImageView != null) {
                mImageView.setImageBitmap(bitmap);
            }
            if (mCallback != null) {
                mCallback.onLoadImageEnd(mImageView, bitmap);
            }

        }
    };


    public interface Callback {
        void onLoadImageEnd(ImageView imageView, Bitmap bitmap);
    }

    public void setPicBitmap(final String pic_url) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(
                            pic_url).openConnection();

// 设置请求方式和超时时间
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(1000 * 10);
                    conn.connect();

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        InputStream is = conn.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(is);

//利用消息的方式把数据传送给handler
                        Message msg = handler.obtainMessage();
                        msg.obj = bitmap;
                        handler.sendMessage(msg);
                    } else {
                        Log.e(TAG,"请求失败 ");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
