package com.tencent.dingdangsampleapp.template.view.viewinterface;

    /**
     *    文本模板
     */
public interface TextTemplate extends IBaseView
    {
        /**
         * 是否沉浸式显示
         * @param show
         */
        void showImmersed(boolean show);

        /**
         * 开始滚动
         */
        void startScroll();

        /**
         * 停止滚动
         */
        void stopScroll();

    }
