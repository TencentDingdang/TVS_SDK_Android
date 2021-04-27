package com.tencent.dingdangsampleapp;

import android.content.Context;
import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.CapabilityOption;
import com.tencent.ai.tvs.tvsinterface.IAlertAbility;
import com.tencent.ai.tvs.tvsinterface.IBluetoothAbility;
import com.tencent.ai.tvs.tvsinterface.IDeviceAbility;
import com.tencent.ai.tvs.tvsinterface.IExternalMediaPlayer;
import com.tencent.ai.tvs.tvsinterface.IMediaPlayer;
import com.tencent.ai.tvs.tvsinterface.IPluginProvider;
import com.tencent.ai.tvs.vdpsvoiceinput.IAudioPreprocessorInterface;
import com.tencent.dingdangsampleapp.alert.AlertAbilityImp;
import com.tencent.dingdangsampleapp.devicectrl.DeviceAbilityImpl;
import com.tencent.dingdangsampleapp.player.AudioTrackPlayer;
import com.tencent.dingdangsampleapp.util.FileUtils;

public class DefaultPluginProvider implements IPluginProvider {

    private Context mContext;
    private IMediaPlayer mMediaPlayer;

    public DefaultPluginProvider(Context applicationContext, IMediaPlayer mediaPlayer) {

        mContext = applicationContext.getApplicationContext();

        mMediaPlayer = mediaPlayer;
    }

    @Override
    public IMediaPlayer getMediaPlayer() {
        // TODO 返回自己的媒体播放器
        return mMediaPlayer;
    }

    @Override
    public IAudioPreprocessorInterface getAudioPreprocessor() {
        boolean hasAudioPreprocessor = false;//这里接入方根据自己实际情况接入，默认为没有
        if (hasAudioPreprocessor) {
            try {
                IAudioPreprocessorInterface impl = (IAudioPreprocessorInterface) FileUtils.invokeMethod(null,
                        "com.tencent.ai.tvs.model.AplPreProcessorImpl", "getInstance",  (Class[])null, (Object[])null);
                return impl;
            } catch (Throwable e) {
                Log.e("DefaultPluginProvider", "errMsg:" + e.getMessage());
            }
        }
        return null;
    }

    @Override
    public IMediaPlayer getAudioTrackPlayer() {
        return new AudioTrackPlayer(mContext);
    }

    @Override
    public IMediaPlayer getShortVideoPlayer() {
        return null;
    }

    @Override
    public IDeviceAbility getDeviceAbility() {
        return new DeviceAbilityImpl(mContext);
    }

    @Override
    public IBluetoothAbility getBluetoothAbility() {
        return null;
    }


    @Override
    public IAlertAbility getAlertAbility() {
        return new AlertAbilityImp(mContext);
    }

    @Override
    public String getUIVersion() {
        return null;
    }

    @Override
    public IExternalMediaPlayer getExternalMediaPlayer() {
        return null;
    }

    @Override
    public CapabilityOption getCapabilityOption() {
        CapabilityOption option = new CapabilityOption();
        option.addOption(CapabilityOption.OPTION_USER_INTERFACE);
        option.addOption(CapabilityOption.OPTION_RECOGNIZE);
        option.addOption(CapabilityOption.OPTION_TTS);
        option.addOption(CapabilityOption.OPTION_AUDIO_PLAYER);
        option.addOption(CapabilityOption.OPTION_ALERT);
        option.addOption(CapabilityOption.OPTION_PLAYBACK_CONTROL);
        option.addOption(CapabilityOption.OPTION_DEVICE_CONTROL);
        option.addOption(CapabilityOption.OPTION_CUSTOM_SKILL);
        option.addOption(CapabilityOption.OPTION_COMMUNICATION);
        return option;
    }

    @Override
    public IMediaPlayer getVideoTTSPlayer() {
        return null;
    }
}