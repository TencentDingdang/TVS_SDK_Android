package com.tencent.dingdangsampleapp.template.view.ui;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import com.tencent.dingdangsampleapp.R;

import android.util.Log;
import android.widget.TextView;

import com.tencent.dingdangsampleapp.template.view.viewinterface.TextTemplate;

public class TextTemplateView extends AbstractTemplate implements TextTemplate {

    protected final String TAG = this.getClass().getSimpleName();

    //views
    private TextView mContentView;

    public TextTemplateView(Context context) {
        this(context, null);
    }

    public TextTemplateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.template_text, this);
        initViews();
    }

    public TextTemplateView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }


    @Override
    protected void initViews() {
        super.initViews();
        mContentView = findViewById(R.id.text_card_content);
    }


    private void showTitle(@Nullable String title) {
        Log.i(TAG,"title = " + title);
        mTitleView.setText(title);
    }


    private void showContent(@Nullable String content) {
        mContentView.setText(content);
    }

    @Override
    public void fillData() {
        setDuration(5000);
        showContent(mData.mContent);
    }

    @Override
    public void clearData() {

    }

    @Override
    public void showImmersed(boolean show) {
        if (show) {
            hideFootView();
            hideHead();
        } else {
            showHead();
            showFootView();
        }
    }

    @Override
    public void startScroll() {

    }

    @Override
    public void stopScroll() {

    }


}
