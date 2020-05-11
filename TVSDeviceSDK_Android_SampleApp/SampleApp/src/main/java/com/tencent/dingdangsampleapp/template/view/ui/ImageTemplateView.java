package com.tencent.dingdangsampleapp.template.view.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import com.tencent.dingdangsampleapp.R;

import com.tencent.dingdangsampleapp.template.data.ImageTmplData;

/**
 * 图文模板
 */
public class ImageTemplateView extends AbstractTemplate {

    protected final String TAG = "ImageTemplateView";

    protected /*CacheableImageView*/ ImageView mImageView;
    protected TextView mImageTitleView;
    protected TextView mImageContentView;
    public ImageTemplateView(Context context) {
        this(context, null);
    }

    public ImageTemplateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.template_image, this);
        initViews();
    }

    public ImageTemplateView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    @Override
    public void clearData() {

    }

    @Override
    protected void initViews() {
        super.initViews();
        Log.i(TAG, "init");
        mImageView = findViewById(R.id.image_main);
        mImageTitleView = findViewById(R.id.image_title);
        mImageContentView = findViewById(R.id.image_text_content);
        mBackView.setImageResource(R.drawable.template_head_back);
        mTitleView.setTextColor(Color.WHITE);
        setHeadBarTransparent();//head设置透明
        hideFootView();//隐藏foot
    }

    @Override
    public void fillData() {
        if (mData != null) {
            //下载背景图
            if(((ImageTmplData)mData).mBgUrl != null ){
                Log.i(TAG, "fillData mBgUrl = "+((ImageTmplData)mData).mBgUrl);
                mPresenter.loadImage(((ImageTmplData)mData).mBgUrl,mImageView, null);
            }
            mImageTitleView.setText(mData.mTitle);
            mImageContentView.setText(mData.mContent);
        }
    }
}
