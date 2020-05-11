package com.tencent.dingdangsampleapp.template.view.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.tencent.dingdangsampleapp.template.data.ListTmplData;
import com.tencent.dingdangsampleapp.R;
import com.tencent.dingdangsampleapp.template.view.adapter.ListTemplateAdapter;
import com.tencent.dingdangsampleapp.tskuidata.listitem.Items;

/**
列表模板
 */
public class ListTemplateView extends AbstractTemplate {
    private static final String TAG = "ListTemplateView";

    protected ListView mListView;
    ListTmplData mListTmplData;

    protected ListTemplateAdapter mAdapter;

    private Context mContext;

    public ListTemplateView(Context context) {
        this(context, null);
    }

    public ListTemplateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.template_list, this);
        mContext = context;
        initViews();
    }

    public ListTemplateView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    @Override
    protected void initViews() {
        super.initViews();
        mListView = findViewById(R.id.template_list);
        mTitleView = findViewById(R.id.list_title);
        setHeadBarTransparent();//head设置透明
        hideFootView();//隐藏foot
    }

    @Override
    public void fillData() {
        if (mData != null) {
            Log.d(TAG, "mData = "+mData);
            mListTmplData = (ListTmplData) mData;
            Log.d(TAG, "listTmplData = " + mListTmplData + " mListTmplData.mListData = " + mListTmplData.mListData);
            initContent(mContext);
        } else {
            Log.e(TAG, "mData is null ");
        }
    }

    @Override
    public void clearData() {

    }

    private void initContent(Context mContext) {
        try {
            mAdapter = new ListTemplateAdapter(mListTmplData.mListData, mContext);
            mListView.setAdapter(mAdapter);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Items item = (Items) mAdapter.getItem(position - 1);
                    Log.d(TAG, "OnItemClick item:" + item + " position = " + position);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
