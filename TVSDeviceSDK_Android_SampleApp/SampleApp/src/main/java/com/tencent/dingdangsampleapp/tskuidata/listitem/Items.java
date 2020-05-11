package com.tencent.dingdangsampleapp.tskuidata.listitem;

public class Items {
    public String title;    // 标题，默认呈现和播报
    public String subTitle; // 副标题，默认呈现和播报
    public String textContent;// 内容，默认呈现和播报
    public String htmlView; // 落地页/跳转链接
    public String mediaId;  // 内容ID，该条资源的唯一ID，上报使用
    public Image image;     // 非背景图片资源
    public Image backgroundImage;    // 背景图片资源
    public Audio audio; // 音频资源
    public Video video;  // 视频资源
    public String selfData; // Object, 技能私有字段，模版实现不依赖该字段信息
}
