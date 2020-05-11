package com.tencent.dingdangsampleapp.template.view.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tencent.dingdangsampleapp.R;
import com.tencent.dingdangsampleapp.tskuidata.listitem.Items;

import java.util.ArrayList;
import java.util.List;

public class ListTemplateAdapter extends BaseAdapter {
    private static final String TAG = "ListTemplateAdapter";

    private Context mContext;

    private List<Items> mListData = new ArrayList<>();;

    public ListTemplateAdapter(List<Items> listData, Context context) {
        this.mContext = context;
        mListData.addAll(listData);
    }

    @Override
    public int getCount() {
        Log.d(TAG, "getCount mListData = "+mListData);
        if (mListData != null) {
            Log.d(TAG, "getCount size = "+mListData.size());
            return mListData.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        Log.d(TAG, "getItem position = "+position);
        if (mListData != null && position < mListData.size() && position >= 0) {
            return mListData.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        Log.d(TAG, "getItemId position = "+position);
        return position;
    }

    @Override
    public int getViewTypeCount() {
        Log.d(TAG, "getViewTypeCount mListData = "+mListData);
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        Log.d(TAG, "getItemViewType position = "+position);
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        Log.d(TAG, "getView position = "+position + " mListData = "+mListData);
        final ViewHolder viewHolder;
        try {
            Items items = mListData.get(position);
            Log.d(TAG, "getView items = "+items);
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_template, null);

                viewHolder.titleView = (TextView) convertView.findViewById(R.id.list_item_title);
                viewHolder.subTitleView = (TextView) convertView.findViewById(R.id.list_item_subtitle);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.bindData(position, items);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return convertView;
    }

    private static class ViewHolder {
        private TextView titleView;
        private TextView subTitleView;

        private void bindData(int position, Items item) {
            Log.d(TAG, "bindData position = "+position +" item = "+item);
            Log.d(TAG, "bindData position = "+position +" title = "+item.title +" subTitle = "+item.subTitle);
            titleView.setText(item.title);
            subTitleView.setText(item.subTitle);
        }
    }

}
