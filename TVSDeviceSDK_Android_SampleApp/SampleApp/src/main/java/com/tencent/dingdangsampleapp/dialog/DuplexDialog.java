package com.tencent.dingdangsampleapp.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.ai.tvs.api.DialogManager;
import com.tencent.ai.tvs.api.TVSApi;
import com.tencent.ai.tvs.tvsinterface.IFullDuplexListener;
import com.tencent.dingdangsampleapp.R;

public class DuplexDialog extends Dialog implements View.OnClickListener {

    private static final String TAG = "DuplexDialog";

    private TextView mStateView;

    public DuplexDialog(@NonNull Context context) {
        super(context, R.style.WeLauncherDialogStyle);
        setCanceledOnTouchOutside(false);
        initView();
    }

    private void initView() {
        setContentView(R.layout.duplex_layout);

        mStateView = (TextView)findViewById(R.id.duplex_state);
        findViewById(R.id.enable_duplex).setOnClickListener(this);
        findViewById(R.id.disable_duplex).setOnClickListener(this);
        findViewById(R.id.start_duplex).setOnClickListener(this);
        findViewById(R.id.stop_duplex).setOnClickListener(this);

        refreshTip();

        DialogManager dialogManager = TVSApi.getInstance().getDialogManager();
        if (null != dialogManager) {
            dialogManager.setDuplexListener(new IFullDuplexListener() {
                @Override
                public void onFullDuplexStart() {
                    Log.i(TAG, "onFullDuplexStart");
                }

                @Override
                public void onFullDuplexStop() {
                    Log.i(TAG, "onFullDuplexStop");
                }
            });
        }
    }

    private void refreshTip() {
        DialogManager dialogManager = TVSApi.getInstance().getDialogManager();
        if (null != dialogManager) {
            mStateView.setText("enable：" + dialogManager.isDuplexEnable() + ", " + ", running : " + dialogManager.isInDuplex());
        } else {
            mStateView.setText("SDK未初始化");
        }
    }

    @Override
    public void onClick(View v) {
        DialogManager dialogManager = TVSApi.getInstance().getDialogManager();
        if (null == dialogManager) {
            return;
        }

        switch (v.getId()) {
            case R.id.enable_duplex:
                dialogManager.enableDuplex();
                break;
            case R.id.disable_duplex:
                dialogManager.stopDuplex(true);
                break;
            case R.id.start_duplex:
                boolean started = dialogManager.startDuplex();
                break;
            case R.id.stop_duplex:
                dialogManager.stopDuplex(false);
                break;
            default:
                break;
        }

        refreshTip();
    }
}
