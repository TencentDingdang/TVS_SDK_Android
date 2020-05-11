package com.tencent.dingdangsampleapp.template.presenter;

import android.content.Context;
import android.widget.ImageView;

public interface IBasePresenter {

    /**
     * 绑定数据
     */
    void bindData();

    void OnTTSStop();

    void OnTTSStart();

    /**
     * 加载图片
     * @param url
     * @param view
     */
    void loadImage(String url, ImageView view, TemplatePresenter.Callback callback);

    /**
     *
     * @param context
     * @param isForce 是否强制关闭
     */
    void doBack(Context context,boolean isForce);

    /**
     * 回收并缓存view     *
     */
    void recycleView();

    /**
     * 拉起网页
     */
    void startWebActivity(Context context, String url);

    /**
     * 是否是同一份数据
     * @return
     */
    boolean isTheSameData();
}
