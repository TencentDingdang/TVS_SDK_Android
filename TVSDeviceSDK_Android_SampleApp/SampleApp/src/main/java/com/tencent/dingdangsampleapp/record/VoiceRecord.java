package com.tencent.dingdangsampleapp.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import com.tencent.ai.tvs.tvsinterface.IAudioRecordListener;
import com.tencent.ai.tvs.tvsinterface.IAudioRecorder;

import java.util.ArrayList;

/**
 * 录音接口的默认实现demo
 */
public class VoiceRecord implements IAudioRecorder {
    private String TAG;
    private AudioRecord mAudioRecord = null;
    private RecordingRunnable mRecordThread;
    private Thread mThread = null;

    public VoiceRecord() {
        TAG = "VoiceRecord" + this.hashCode();
    }

    /**
     * 读取音频数据的同步对象
     */
    protected Object syncObj = new Object();
    private ArrayList<IAudioRecordListener> mListeners = new ArrayList<>();

    /**
     * 开始录音
     */
    public synchronized void startRecord() {
        if(mRecordThread != null && mThread != null){
            Log.d(TAG, "VoiceRecord is Started !");
            return;
        }
        Log.d(TAG, "startRecord()...mAudioRecord = "+mAudioRecord);
        if (mAudioRecord != null) {
            // 这里wait 100ms，等上一次清理完成再开始下一次，防止快速stop -> start可能会出现启动录音失败
            try {
                wait(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            if (mRecordThread == null) {
                mRecordThread = new RecordingRunnable();
            }
            if( mThread == null) {
                mThread = new Thread(mRecordThread);
            }
            mThread.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            for (IAudioRecordListener listener : mListeners) {
                listener.onRecordingFailed();
            }
            mThread = null;
            mRecordThread = null;
            return;
        }
    }

    /**
     * 停止录音
     */
    public synchronized void stopRecord() {

        //临时的修改方案，避免录音的抢占
/*        if( mAudioRecord != null ){
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }*/
        Log.d(TAG, "stopRecord()...");
        if(mRecordThread == null && mThread == null){
            Log.d(TAG, "VoiceRecord is Stoped !");
            return;
        }
        if(mRecordThread != null) {
            // 目前stop只是修改Thread的标志位，并没有真正停止录音
            mRecordThread.stop();

            // 这里wait 50ms，防止快速stop -> start可能会出现启动录音失败
            try {
                wait(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mThread = null;
        mRecordThread = null;
    }

    public void addAudioRecordListener(IAudioRecordListener recordListener) {
        if (!mListeners.contains(recordListener)) mListeners.add(recordListener);
    }

    public void removeAudioRecordListener(IAudioRecordListener recordListener) {
        mListeners.remove(recordListener);
    }

    /**
     * 录音线程
     */
    private class RecordingRunnable implements Runnable {

        /** 是否结束识别 */
        private boolean mIsEnd = false;
        /** 是否退出录音线程 */
        private boolean mIsExit = false;
        /**
         * 录音线程buffer
         */
        int mRecordBufferSize = 0;

        private final int READ_FRAME_SIZE = 2048;

        private boolean init() {
            int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
            int audioEncodingBits = AudioFormat.ENCODING_PCM_16BIT;
            int sampleRate = 16000;
            try {
                mRecordBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfiguration, audioEncodingBits);
                Log.d(TAG, "mRecordBufferSize :" + mRecordBufferSize);
                if (mRecordBufferSize < 0) {
                    for (IAudioRecordListener listener : mListeners) {
                        listener.onRecordCreateError();
                    }
                    return false;
                } else {
                    if(mRecordBufferSize < READ_FRAME_SIZE) {
                        mRecordBufferSize = READ_FRAME_SIZE;
                    }
                    mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                            channelConfiguration, audioEncodingBits, mRecordBufferSize);
                }

                if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    for (IAudioRecordListener listener : mListeners) {
                        listener.onRecordCreateError();
                    }
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                for (IAudioRecordListener listener : mListeners) {
                    listener.onRecordCreateError();
                }
                return false;
            }
            Log.d(TAG, "init Recording");
            return true;
        }

        private boolean startup() {
            Log.d(TAG, "startup");
            synchronized (syncObj){
                mIsEnd = false;
            }

            mIsExit = false;
            long bfs = System.currentTimeMillis();
            if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                try {
                    Log.d(TAG, "start Recording");
                    mAudioRecord.startRecording();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    Log.d(TAG, "start Recording failed");
                    for (IAudioRecordListener listener : mListeners) {
                        listener.onRecordCreateError();
                    }
                    return false;
                }
            } else {
                for (IAudioRecordListener listener : mListeners) {
                    listener.onRecordCreateError();
                }
                return false;
            }
            long afs = System.currentTimeMillis();
            Log.d(TAG, "start recording deltaTime = " + (afs - bfs));
            return true;
        }

        public void stop() {
            synchronized (syncObj) {
                mIsEnd = true;
            }
        }

        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            if (!init()) {
                return;
            }

            try {
                // 每次读取音频数据大小
                byte[] pcmBuffer = new byte[mRecordBufferSize];
                Log.d(TAG, "mRecordBufferSize: " + mRecordBufferSize + ", thread id : " + Thread.currentThread().getId());
                // 实际读取音频数据大小
                int pcmBufferSize;
                if (startup()) {
                    for (IAudioRecordListener listener : mListeners) {
                        listener.onRecordingStart();
                    }
                    while (!mIsExit) {
                        if( null != mAudioRecord ){
                            pcmBufferSize = mAudioRecord.read(pcmBuffer, 0, mRecordBufferSize);
                            if (pcmBufferSize == AudioRecord.ERROR_INVALID_OPERATION) {
                                throw new IllegalStateException(
                                        "read() returned AudioRecord.ERROR_INVALID_OPERATION");
                            } else if (pcmBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                                throw new IllegalStateException(
                                        "read() returned AudioRecord.ERROR_BAD_VALUE");
                            }

                            for (IAudioRecordListener listener : mListeners) {
                                listener.onRecording(pcmBuffer, pcmBufferSize);
                            }

                            boolean isEndFlag = false;
                            synchronized (syncObj){
                                isEndFlag = mIsEnd;
                            }
                            if (isEndFlag) {
                                mIsExit = true;
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "startup failed!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            //临时的修改方案，避免录音的抢占
            if (mAudioRecord != null) {
                Log.d(TAG, "mAudioRecord clear state = "+mAudioRecord.getState());
                if (AudioRecord.STATE_INITIALIZED == mAudioRecord.getState()) {
                    try {
                        mAudioRecord.stop();
                        mAudioRecord.release();
                        mAudioRecord = null;
                        Log.d(TAG, "clear mAudioRecord done");
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        Log.e(TAG, "mAudioRecord Stop error = "+e);
                    }
                }
            }

            for (IAudioRecordListener listener : mListeners) {
                listener.onRecordingEnd();
            }

            Log.d(TAG, "RecordingRuannable is exit");
        }
    }
}
