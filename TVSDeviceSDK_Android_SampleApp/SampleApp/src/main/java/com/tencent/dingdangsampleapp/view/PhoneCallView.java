package com.tencent.dingdangsampleapp.view;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.tvsinterface.CommunicationContactInfo;
import com.tencent.ai.tvs.tvsinterface.ICommunicationCallback;
import com.tencent.dingdangsampleapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 打电话模拟场景
 */
public class PhoneCallView extends Dialog implements View.OnClickListener {

    private static final String TAG = "PhoneCallView";
    private String mType = ICommunicationCallback.CommunicationType.COMMUNICATION_TYPE_PHONE;
    private String mCurToken;
    private List<CommunicationContactInfo> mCurContactList = new ArrayList<>();
    private CommunicationContactInfo mReqContact;
    private EditText mNameET, mNumET;
    private TextView mTextView;
    private Context mContext;
    private View mAddContactView;
    private View mFindContactView;

    public PhoneCallView(Context context) {
        super(context, R.style.WeLauncherDialogStyle);
        setContentView(R.layout.phonecall_dialog_layout);
        mContext = context;
        setCanceledOnTouchOutside(false);

        mAddContactView = findViewById(R.id.add_contact_view);
        mFindContactView = findViewById(R.id.find_contact_view);

        mNameET = (EditText)findViewById(R.id.name_et);
        mNumET = (EditText)findViewById(R.id.num_et);
        mTextView = (TextView)findViewById(R.id.contact_list_tv);
        findViewById(R.id.multi_btn).setOnClickListener(this);
        findViewById(R.id.notfound_btn).setOnClickListener(this);
        findViewById(R.id.add_btn).setOnClickListener(this);
        findViewById(R.id.clear_btn).setOnClickListener(this);
        findViewById(R.id.exit_btn).setOnClickListener(this);
    }

    @Override
    public void show() {
        super.show();
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.gravity=Gravity.CENTER_HORIZONTAL;
        layoutParams.width= WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height= WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().getDecorView().setPadding(0, 0, 0, 0);
        getWindow().setAttributes(layoutParams);

    }

    public void showView() {
        try {
            show();
        } catch (Exception e) {
            Log.e(TAG, "showView Exception ："+e);
        }
    }

    private void dissmissView() {
        try {
            dismiss();
        } catch (Exception e) {
            Log.e(TAG, "dissmissView Exception ："+e);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.multi_btn:
                foundMultiContacts();
                dissmissView();
                break;
            case R.id.notfound_btn:
                cannotFoundContact();
                dissmissView();
                break;
            case R.id.add_btn:
                if (checkInputContactValid()) {
                    addContact();
                }
                break;
            case R.id.clear_btn:
                mTextView.setText(null);
                mNumET.setText("");
                mNameET.setText("");
                if (mCurContactList != null) {
                    mCurContactList.clear();
                }
                break;
            case R.id.exit_btn:
                dissmissView();
                break;
            default:
                break;
        }
    }

    public void setPhoneCallInfo(String type, String token, CommunicationContactInfo contact) {
        mTextView.setText("");
        if (contact ==  null && type == null) {
            //模拟接电话场景
            showCallView(false);
        } else {
            //模拟电话场景
            mType = type;
            mCurToken = token;
            if (mReqContact == null) {
                mReqContact = new CommunicationContactInfo();
            }
            mReqContact.setName(contact.getName());
            mReqContact.setNumber(contact.getNumber());
            showCallView(true);
        }
    }

    /**
     * 找到多个联系人
     */
    public void foundMultiContacts () {
        Log.d(TAG, "foundMultiContacts mCurToken："+mCurToken+" mType = "+mType);
        for (CommunicationContactInfo info : mCurContactList) {
            Log.d(TAG, "姓名："+info.getName()+" 电话："+info.getNumber());
        }
        TVSApi.getInstance().foundMultiContactNumber(mType, mCurToken, mCurContactList);
        mCurContactList.clear();
    }

    /**
     * 未找到联系人
     */
    public void cannotFoundContact () {
        Log.d(TAG, "cannotFoundContact mCurToken："+mCurToken+" mType = "+mType);
        for (CommunicationContactInfo communicationContactInfo : mCurContactList) {
            Log.d(TAG, "cannotFoundContact Name : " + communicationContactInfo.getName()
                    +", num: "+communicationContactInfo.getNumber());
        }
        TVSApi.getInstance().cannotFoundContact(mType, mCurToken, mCurContactList);
        mCurContactList.clear();
    }

    /**
     * 检查输入是否合法
     */
    public boolean checkInputContactValid() {
        if (TextUtils.isEmpty(mNameET.getText())) {
            Toast.makeText(mContext, "请输入name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(mNumET.getText())) {
            Toast.makeText(mContext, "请输入number", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * 添加联系人
     */
    public void addContact() {
        Log.d(TAG, "addContact mCurToken："+mCurToken+" mType = "+mType);
        //输入不匹配
        if (mReqContact != null && !mNameET.getText().toString().equals(mReqContact.getName())) {
            String toastStr = "请输入"+mReqContact.getName()+"的电话号码";
            Toast.makeText(mContext, toastStr, Toast.LENGTH_SHORT).show();
            return;
        }
        String name = mNameET.getText().toString();
        String num = mNumET.getText().toString();
        Log.d(TAG, "addContact name："+name+" num = "+num);
        CommunicationContactInfo communicationContactInfo = new CommunicationContactInfo(name, num);
        if (mCurContactList == null) {
            mCurContactList = new ArrayList<>();
        }
        mCurContactList.add(communicationContactInfo);
        StringBuilder sb = new StringBuilder();
        for (CommunicationContactInfo info : mCurContactList) {
            sb.append("姓名："+info.getName()+" 电话："+info.getNumber()+"\n");
        }
        mTextView.setText(sb.toString());
        mNameET.setText("");
        mNumET.setText("");
    }

    private void showCallView (boolean isCall) {
        Log.d(TAG, "showCallView isCall："+isCall+" mType = "+mType);
        if (isCall) {
            if (mReqContact != null) {
                String info = TextUtils.isEmpty(mReqContact.getName()) ? mReqContact.getNumber() : mReqContact.getName();
                mTextView.setHint("打电话给："+info);
            }
            mAddContactView.setVisibility(View.VISIBLE);
            mFindContactView.setVisibility(View.VISIBLE);
            mAddContactView.setVisibility(View.VISIBLE);
        } else {
            mTextView.setHint("");
            mAddContactView.setVisibility(View.GONE);
            mFindContactView.setVisibility(View.GONE);
            mAddContactView.setVisibility(View.GONE);
        }
    }

}

