package com.tencent.dingdangsampleapp.tskuidata;

import com.tencent.dingdangsampleapp.tskuidata.listitem.Audio;
import com.tencent.dingdangsampleapp.tskuidata.listitem.Image;

public class GlobalInfo {
    public Image backgroundImage; // 用于存放背景图信息
    public Audio backgroundAudio; // 用于存放背景音信息
    public String seeMore;  // 查看更多
    public String selfData;  // 可缺省，技能私有字段，模版实现不依赖该字段信息
    public String listUpdateType;//用于指示当前下发的列表数据与终端已缓存列表数据之间的关系 COVER 覆盖 PRE_APPEND 前置追加 POST_APPEND 后置追加
    public String playMode; // 可缺省，默认值"LIST"，用于标识播放模式
}