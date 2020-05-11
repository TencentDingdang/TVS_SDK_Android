package com.tencent.dingdangsampleapp.template.data;

import com.tencent.dingdangsampleapp.tskuidata.listitem.Items;

import java.util.List;

public class ListTmplData extends BaseTemplateData {
    public List<Items> mListData;
    public ListTmplData(List<Items> data){
        mListData = data;
    }
}
