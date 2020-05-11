package com.tencent.dingdangsampleapp.template.view.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.tencent.dingdangsampleapp.R;

import com.tencent.dingdangsampleapp.template.data.BaseTemplateData;
import com.tencent.dingdangsampleapp.template.presenter.IBasePresenter;
import com.tencent.dingdangsampleapp.template.presenter.TemplatePresenter;
import com.tencent.dingdangsampleapp.template.view.viewinterface.IBaseView;

/**
 * 模板基类
 */
public abstract class AbstractTemplate extends RelativeLayout implements IBaseView {

    protected IBasePresenter mPresenter;
    protected BaseTemplateData mData;

    protected View mHeaderView;
    protected View mFootView;
    protected ImageView mBackView;
    protected View mBackAera;

    @Override
    public BaseTemplateData getTemplateDataBoundInView() {
        return mData;
    }

    protected TextView mTitleView;
    protected ImageView mLogoView;

    /**
     * 延迟关闭时间
     */
    protected long mDuration = -1;

    public AbstractTemplate(Context context) {
        this(context, null);
    }

    public AbstractTemplate(final Context context, AttributeSet attrs) {
        super(context, attrs);
        mPresenter = new TemplatePresenter(this);
    }

    public AbstractTemplate(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.i("AbstractTemplate", "onAttachedToWindow");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i("AbstractTemplate", "onDetachedFromWindow");

    }

    public IBasePresenter getPresenter() {
        return mPresenter;
    }

    protected void showHead() {
        mHeaderView.setVisibility(VISIBLE);
    }

    protected void showFootView() {
        mFootView.setVisibility(VISIBLE);
    }

    protected void hideHead() {
        mHeaderView.setVisibility(GONE);
    }

    protected void hideFootView() {
        mFootView.setVisibility(GONE);
    }

    protected void setHeadBarTransparent() {
        mHeaderView.getBackground().setAlpha(0);
    }

    protected void setFootBarTransparent() {
        mFootView.getBackground().setAlpha(0);
    }

    public void setDuration(long duration) {
        if (mData != null && !mData.isSessionComplete()) {
            mDuration = 5000;
        } else {
            mDuration = duration;
        }
    }

    public long getDuration() {
        return mDuration;
    }

    protected void initViews() {
        Log.i("AbstractTemplate", "super init views");
        mHeaderView = findViewById(R.id.head_bar);
        mFootView = findViewById(R.id.foot_bar);
        mBackAera = mHeaderView.findViewById(R.id.backBtn_area);
        mBackView = mHeaderView.findViewById(R.id.backBtn);
        mTitleView = mHeaderView.findViewById(R.id.head_title);
    }

    @Override
    public void setTemplateData(BaseTemplateData data) {
        mData = data;
    }

    public View getBackView(){
        return mBackAera;
    }
}
