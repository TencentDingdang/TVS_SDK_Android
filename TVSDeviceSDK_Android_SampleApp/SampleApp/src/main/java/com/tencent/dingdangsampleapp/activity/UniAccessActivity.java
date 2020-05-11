package com.tencent.dingdangsampleapp.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.tvsinterface.CustomEntity;
import com.tencent.ai.tvs.tvsinterface.IUpdateEntitiesCallback;
import com.tencent.dingdangsampleapp.R;

import java.util.ArrayList;
import java.util.List;

public class UniAccessActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "UniAccessActivity";
    private TextView mInfoTv;

    private View mOriginalNameView;
    private EditText mProjectIdET;
    private EditText mTypeET;
    private EditText mNameET;
    private EditText mOriginalNameET;
    private Button mCommitBtn;
    private Button mAddBtn;
    private Button mCancelBtn;
    RadioGroup mRadioGroup;
    List<CustomEntity> mEntities;
    int mCurOperation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_entities_layout);
        mRadioGroup = (RadioGroup)findViewById(R.id.radio_operate);
        mOriginalNameView = findViewById(R.id.original_name_view);
        mAddBtn = (Button) findViewById(R.id.add_btn);
        mCommitBtn = (Button) findViewById(R.id.commit_btn);
        mCancelBtn = (Button) findViewById(R.id.cancel_btn);
        mAddBtn.setOnClickListener(this);
        mCommitBtn.setOnClickListener(this);
        mCancelBtn.setOnClickListener(this);

        mProjectIdET = (EditText)findViewById(R.id.projectid_et);
        mTypeET = (EditText)findViewById(R.id.type_et);
        mNameET = (EditText)findViewById(R.id.name_et);
        mOriginalNameET = (EditText)findViewById(R.id.original_name_et);

        mInfoTv = (TextView) findViewById(R.id.entities_info_tv);

        mCurOperation = CustomEntity.ENTITY_OPERATION_UPDATE;
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                Log.d(TAG, "onCheckedChanged checkedId : " + checkedId );
                if(checkedId == R.id.radio_add) {
                    mCurOperation = CustomEntity.ENTITY_OPERATION_ADD;
                    mOriginalNameView.setVisibility(View.GONE);
                }else if(checkedId == R.id.radio_delete){
                    mCurOperation = CustomEntity.ENTITY_OPERATION_DELETE;
                    mOriginalNameView.setVisibility(View.GONE);
                }else if(checkedId == R.id.radio_update){
                    mCurOperation = CustomEntity.ENTITY_OPERATION_UPDATE;
                    mOriginalNameView.setVisibility(View.VISIBLE);
                }else if(checkedId == R.id.radio_cover){
                    mCurOperation = CustomEntity.ENTITY_OPERATION_ALL;
                    mOriginalNameView.setVisibility(View.GONE);
                }
                resetInput();
            }
        });
        mEntities = new ArrayList<>();
    }


    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick："+view.getId());
        TVSApi.getInstance().getDialogManager();
        switch (view.getId()) {
            case R.id.add_btn:
                addOneEntities();
                break;
            case R.id.commit_btn:
                doOperateEntities();
                break;
            case R.id.cancel_btn:
                resetInput();
                break;
            default:
                break;
        }
    }
    private void resetInput () {
        Log.d(TAG, "resetInput");
        if (mEntities != null) {
            mEntities.clear();
        }
        mInfoTv.setText("");
        mNameET.setText("");
        mOriginalNameET.setText("");
        mTypeET.setText("");
        mProjectIdET.setText("");
    }

    //更新实体库并接收回调
    private void doOperateEntities() {
        Log.d(TAG, "doOperateEntities mCurAuthType : " + mCurOperation);
        if (mEntities ==  null || mEntities.size() == 0) {
            Toast.makeText(this, "没有实体内容", Toast.LENGTH_SHORT).show();
            return;
        }
        TVSApi.getInstance().updateEntities(mCurOperation, mEntities, new IUpdateEntitiesCallback() {
            @Override
            public void onUpdateEntitiesResult(int resultCode, String errorMsg) {
                Log.d(TAG, "onUpdateEntitiesResult resultCode : " + resultCode + ", errorMsg = "+errorMsg);
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        resetInput();

                        String opertationStr = null;
                        switch (mCurOperation) {
                            case CustomEntity.ENTITY_OPERATION_ADD:
                                opertationStr = "添加";
                                break;
                            case CustomEntity.ENTITY_OPERATION_DELETE:
                                opertationStr = "删除";
                                break;
                            case CustomEntity.ENTITY_OPERATION_UPDATE:
                                opertationStr = "更新";
                                break;
                            case CustomEntity.ENTITY_OPERATION_ALL:
                                opertationStr = "覆盖";
                                break;
                        }
                        String result = resultCode == 0 ? "成功" : "失败";
                        mInfoTv.setText("更新实体结束，操作："+ opertationStr + ", 结果: "
                                + result  + ", Msg = "+errorMsg);
                    }
                });
            }
        });
    }

//    添加一个实体到mEntities
    private void addOneEntities() {
        if (TextUtils.isEmpty(mProjectIdET.getText())
                || TextUtils.isEmpty(mTypeET.getText())
                || TextUtils.isEmpty(mNameET.getText())) {
            Toast.makeText(this, "projectId、type 和 name 都不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mCurOperation == CustomEntity.ENTITY_OPERATION_UPDATE && TextUtils.isEmpty(mOriginalNameET.getText())) {
            Toast.makeText(this, "更新操作 原name 不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mEntities == null) {
            mEntities = new ArrayList<>();
        }
        String projectId = mProjectIdET.getText().toString();
        String type = mTypeET.getText().toString();
        String name = mNameET.getText().toString();
        String originalName = mOriginalNameET.getText().toString();
        CustomEntity customEntity = new CustomEntity(projectId, type, name, originalName);
        mEntities.add(customEntity);
        mInfoTv.setText("");
        StringBuilder sb = new StringBuilder();
        sb.append("实体个数："+mEntities.size());
        for (int i = 0; i<mEntities.size(); i++) {
            CustomEntity entity = mEntities.get(i);
            sb.append("\n projectId: "+entity.getProjectId() + ", type: "+entity.getType()
                    + ", name: "+entity.getName() + ", originName: "+entity.getOriginalName());
         }
        mInfoTv.setText(sb.toString());
        mNameET.setText("");
        mOriginalNameET.setText("");
        mTypeET.setText("");
        mProjectIdET.setText("");
    }

}
