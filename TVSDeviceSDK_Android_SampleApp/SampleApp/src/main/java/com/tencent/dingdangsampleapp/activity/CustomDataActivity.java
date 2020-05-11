package com.tencent.dingdangsampleapp.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.tvsinterface.CustomData;
import com.tencent.dingdangsampleapp.R;

import java.util.ArrayList;
import java.util.List;

public class CustomDataActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "UniAccessActivity";
    private TextView mInfoTv;


    private EditText mTypeET;
    private EditText mStateET;
    private Button mAddBtn;
    private Button mCommitBtn;
    List<CustomData> mCustomDataStateList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_data_layout);
        mAddBtn = (Button) findViewById(R.id.add_btn);
        mAddBtn.setOnClickListener(this);
        mCommitBtn = (Button) findViewById(R.id.commit_btn);
        mCommitBtn.setOnClickListener(this);


        mTypeET = (EditText)findViewById(R.id.type_et);
        mStateET = (EditText)findViewById(R.id.state_et);

        mInfoTv = (TextView) findViewById(R.id.state_info_tv);
        mInfoTv.setText("");

        mCustomDataStateList = new ArrayList<>();
    }


    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick："+view.getId());
        TVSApi.getInstance().getDialogManager();
        switch (view.getId()) {
            case R.id.add_btn:
                addState();
                break;
            case R.id.commit_btn:
                commitState();
                break;
            default:
                break;
        }
    }

    //添加一个State
    private void addState() {
        if (TextUtils.isEmpty(mTypeET.getText())) {
            Toast.makeText(this, "type不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mCustomDataStateList == null) {
            mCustomDataStateList = new ArrayList<>();
        }

        String type = mTypeET.getText().toString();
        String state = mStateET.getText().toString();
        CustomData customData = new CustomData(type, state);
        mCustomDataStateList.add(customData);
        StringBuilder sb = new StringBuilder();
        sb.append("\n type: "+type + ", state: "+state);
        mInfoTv.append(sb.toString());
        mStateET.setText("");
        mTypeET.setText("");
    }

    private void commitState() {
        TVSApi.getInstance().setCustomDataState(mCustomDataStateList);
        mInfoTv.setText("提交完毕");
    }

}
