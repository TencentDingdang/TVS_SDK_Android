package com.tencent.dingdangsampleapp.template.view.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.dingdangsampleapp.R;
import com.tencent.dingdangsampleapp.template.data.ImageTmplData;
import com.tencent.dingdangsampleapp.template.view.viewinterface.ImageTemplate;

/**
 * 简约图文模板
 */
public class BriefImageTemplateView extends AbstractTemplate implements ImageTemplate {

    protected final String TAG = this.getClass().getSimpleName();

    protected /*CacheableImageView*/ ImageView mImageView;
    protected TextView mImageTitleView;
    protected TextView mImageContentView;

    @Override
    public void clearData() {

    }

    public BriefImageTemplateView(Context context) {
        this(context, null);
    }

    public BriefImageTemplateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.template_image_brief, this);
        initViews();
    }

    public BriefImageTemplateView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    @Override
    protected void initViews() {
        super.initViews();
        mImageView = findViewById(R.id.image_main);
        mImageTitleView = findViewById(R.id.image_title);
        mImageContentView = findViewById(R.id.image_text_contdent);
        mBackView.setImageResource(R.drawable.template_head_back);
        setHeadBarTransparent();
        setFootBarTransparent();
//        hideFootView();
    }

    @Override
    public void fillData() {
        if (mData != null) {
            //下载背景图
            if(((ImageTmplData)mData).mBgUrl != null ){
                mPresenter.loadImage(((ImageTmplData)mData).mBgUrl,mImageView, null);
            }
            Log.i(TAG,"fill data mTitle = " + mData.mTitle+" mContent = "+mData.mContent);
            mTitleView.setText(mData.mTitle);
            mTitleView.setTextColor(Color.WHITE);
            mImageTitleView.setText(mData.mTitle);
            mImageContentView.setText(mData.mContent);
        }
    }
}
