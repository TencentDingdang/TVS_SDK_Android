package com.tencent.dingdangsampleapp.tskuidata;

public class ControlInfo {
    public String version;
    public String type;  // 模版类型 文本，图文 等
    public String textSpeak; // 基础元组 中数据是否需要播报 默认播报 (false 不播报 true 播报)
    public String titleSpeak; // 基础元组 中 title 是否需要播报 默认播报 （false 不播报 true 播报）
    public String subTitleSpeak; // 基础元组 中 subTitle 是否需要播报 默认播报 （false 不播报 true 播报）
    public String audioConsole; // 基础元组 中音频数据是否显示控制台，默认显示 (false 不显示 true 显示)
    public String orientation; // 列表样式 默认竖向 横向（landscape）/竖向（portrait）
}
