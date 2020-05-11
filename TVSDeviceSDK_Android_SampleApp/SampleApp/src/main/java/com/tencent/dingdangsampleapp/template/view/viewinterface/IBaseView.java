package com.tencent.dingdangsampleapp.template.view.viewinterface;

import com.tencent.dingdangsampleapp.template.data.BaseTemplateData;

public interface IBaseView{

    void setTemplateData(BaseTemplateData data);

    void fillData();

    void clearData();

    BaseTemplateData getTemplateDataBoundInView();

}
