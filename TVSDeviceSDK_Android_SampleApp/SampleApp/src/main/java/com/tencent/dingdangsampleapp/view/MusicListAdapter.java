package com.tencent.dingdangsampleapp.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.tencent.dingdangsampleapp.R;

import com.tencent.dingdangsampleapp.mediamanager.MediaPlayManager;
import com.tencent.dingdangsampleapp.template.data.MediaData;

import java.util.ArrayList;
import java.util.List;

public class MusicListAdapter extends BaseAdapter {
    private static final String TAG = "MusicListAdapter";

    private Context mContext;

    private List<MediaData> mMediaList = new ArrayList<MediaData>();
    private MediaData curMedia;
    private final int TYPE_NORMAL = 0, TYPE_TEMPLATE = 1, TYPE_TEMPLATE_NOSUBTITLE = 2;
    private final int TYPE_COUNT = 3;

    public MusicListAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return mMediaList.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < mMediaList.size() && position >= 0) {
            return mMediaList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        // TODO Auto-generated method stub
        return TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        MediaData mediaData = mMediaList.get(position);
        if (null != mediaData) {
            if (!TextUtils.isEmpty(mediaData.mPerson)) {
                return TYPE_TEMPLATE;
            } else {
                return TYPE_TEMPLATE_NOSUBTITLE;
            }
        } else {
            return TYPE_TEMPLATE;
        }
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final ViewHolder viewHolder;
        try {
            MediaData mediaData = mMediaList.get(position);
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.music_list_item_template, null);

                viewHolder.song = (TextView) convertView.findViewById(R.id.music_list_item_song);
                viewHolder.singer = (TextView) convertView.findViewById(R.id.music_list_item_singer);
                viewHolder.index = (TextView) convertView.findViewById(R.id.index);
                viewHolder.time = (TextView) convertView.findViewById(R.id.time);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.bindData(position, mediaData);

            if ((curMedia != null && mediaData != null && !TextUtils.isEmpty(curMedia.sMediaId) &&
                    !TextUtils.isEmpty(mediaData.sMediaId) && TextUtils.equals(mediaData.sMediaId, curMedia.sMediaId))
                    || (curMedia != null && mediaData != null)) {
                convertView.setActivated(true);
                viewHolder.song.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                viewHolder.song.setSelected(true);

            } else {
                convertView.setActivated(false);
                viewHolder.index.setVisibility(View.VISIBLE);
                viewHolder.singer.setVisibility(View.VISIBLE);
                viewHolder.song.setEllipsize(TextUtils.TruncateAt.END);
                viewHolder.song.setSelected(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return convertView;
    }

    public void updateMusicList() {
        mMediaList.clear();
        mMediaList.addAll(MediaPlayManager.getInstance().getMediaList());
        curMedia = MediaPlayManager.getInstance().getCurrentMedia();
        notifyDataSetChanged();
    }

    public void removeHandlerMessage() {
        if (null != myHandler) {
            myHandler.removeCallbacksAndMessages(null);
        }
    }

    Handler myHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.obj == "play") {
                notifyDataSetChanged();
            } else if (msg.obj == "pause") {
                notifyDataSetChanged();
            } else if (msg.obj == "stop") {
                notifyDataSetChanged();
            } else if (msg.obj == "prepared") {
                notifyDataSetChanged();
            }
        }
    };

    private static class ViewHolder {
        private TextView song;
        private TextView singer;
        private TextView time;
        private TextView index;

        private void bindData(int position, MediaData mediaData) {
            if (mediaData == null) {
                song.setText("");
                singer.setText("");
                return;
            }

            if (!TextUtils.isEmpty(mediaData.mMediaName)) {
                song.setText(mediaData.mMediaName);
            } else {
                song.setText(mediaData.sTextContent);
            }
            //如果是音频模版卡片，并且没有音频的时长数据，就不显示
            if (mediaData.iSongPlayTime <= 0) {
                time.setText("");
            } else {
                time.setText(mediaData.sSongPlayTime);
            }

            int indexInt = position + 1;
            index.setText(String.valueOf(indexInt));
            Log.d(TAG, "bindData mPerson"+mediaData.mPerson +" name+"+mediaData.mMediaName);
            if (!TextUtils.isEmpty(mediaData.mPerson)) {
                singer.setText(mediaData.mPerson);
            }
        }
    }

}
