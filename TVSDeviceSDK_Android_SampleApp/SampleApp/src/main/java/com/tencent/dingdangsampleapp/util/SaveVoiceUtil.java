package com.tencent.dingdangsampleapp.util;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by simonsheng on 2017/3/28.
 */
public class SaveVoiceUtil {

    private static String TAG = "SaveVoiceUtil";

    private static String sRecordFileName = "";
    private static String sRecordFileNameTmp = "";
    private static Map<String,ByteArrayOutputStream> mStreamMap = new HashMap<String,ByteArrayOutputStream>();
    private static final int FLUSH_SIZE = 96000;

    private SaveVoiceUtil() {

    }

    private static boolean checkAndCreateDir(String strFolder) {
        File file = new File(strFolder);
        if (!file.exists()) {
            if (file.mkdirs()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 可以给录音加前缀关键词的接口，例如aec,处理前的音频保存，加前缀original
     * @param inFolder
     * @param sExtra
     */
    public static CustomFileOutputStream initSaveVoiceExtra(String inFolder,String sExtra) {
        CustomFileOutputStream mOutTmp = null;
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + inFolder;
        if(checkAndCreateDir(path)) {
            try {
                Calendar c = Calendar.getInstance();
                int nYear = c.get(Calendar.YEAR);
                int nMonth = c.get(Calendar.MONTH)+1;
                int nDay = c.get(Calendar.DAY_OF_MONTH);
                int nHour = c.get(Calendar.HOUR_OF_DAY);
                int nMinute = c.get(Calendar.MINUTE);
                int nSecond = c.get(Calendar.SECOND);
                sRecordFileName = nYear+""+nMonth+""+nDay+"-"+nHour+"-"+nMinute+"-"+nSecond;
                sRecordFileNameTmp = nYear+""+nMonth+""+nDay+"-"+nHour+"-"+nMinute+"-"+nSecond+"-"+sExtra+ ".pcm";
                mOutTmp = new CustomFileOutputStream(path + "/" + sRecordFileNameTmp, true);
                Log.d(TAG, "initSaveVoiceExtra sKey="+mOutTmp.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return mOutTmp;
    }

    /**
     * 可以给录音加前缀关键词的接口，例如aec,处理前的音频保存，加前缀original
     * @param inFolder
     * @param sExtra
     *
     * added by cassliu，这个方法只会返回目录的路径
     */
    public static String initSaveVoice(String inFolder,String sExtra) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + inFolder;
        if(checkAndCreateDir(path)) {
            Calendar c = Calendar.getInstance();
            int nYear = c.get(Calendar.YEAR);
            int nMonth = c.get(Calendar.MONTH) + 1;
            int nDay = c.get(Calendar.DAY_OF_MONTH);
            int nHour = c.get(Calendar.HOUR_OF_DAY);
            int nMinute = c.get(Calendar.MINUTE);
            int nSecond = c.get(Calendar.SECOND);
            sRecordFileName = nYear + "" + nMonth + "" + nDay + "-" + nHour + "-" + nMinute + "-" + nSecond;
            String filePreffix = !TextUtils.isEmpty(sExtra) ? sExtra + "-" : "";
            sRecordFileNameTmp = filePreffix + nYear + "" + nMonth + "" + nDay + "-" + nHour + "-" + nMinute + "-" + nSecond + ".pcm";
            return path + "/" + sRecordFileNameTmp;
        }

        return null;
    }

    public static void writeRecordDataForce(final FileOutputStream inStream,final byte[] data){
        try{
            inStream.write(data);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void saveRecordData(final FileOutputStream inStream){
        if( null != inStream ){
            try{
                inStream.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void writeRecordDataExtra(final FileOutputStream inStream,final byte[] data,final boolean isEnd) {
        if(null == inStream) {
            return;
        }

        if( null != mStreamMap ){
            ByteArrayOutputStream mStream = (ByteArrayOutputStream)mStreamMap.get(inStream.toString());
            if( null == mStream ){
                mStream = new ByteArrayOutputStream();
                mStreamMap.put(inStream.toString(),mStream);
            }

            try {
                if( null != data && data.length > 0 ){
                    mStream.write(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if( isEnd ){
            writeEnd(inStream);
        }
    }

    private static void writeEnd(final FileOutputStream inStream) {
        new android.os.AsyncTask<String, Void, Void>(){
            @Override
            protected Void doInBackground(String... params) {
                try {
                    if( null != mStreamMap ){
                        String sKey = params[0];
                        Log.d(TAG, "writeRecordDataExtra sKey=" + sKey);
                        ByteArrayOutputStream mStream = (ByteArrayOutputStream)mStreamMap.get(sKey);
                        if( null != mStream ){
                            Log.d(TAG, "writeRecordDataExtra close");
                            inStream.write(mStream.toByteArray());
                            inStream.close();
                            //清空缓存
                            mStream.reset();
                            mStream.close();
                            mStream = null;
                            mStreamMap.remove(inStream.toString());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(inStream.toString());
    }


    public static void writeRecordDataExtra(final CustomFileOutputStream inStream,final byte[] data,final boolean isEnd) {
        if(null == inStream) {
            return;
        }

        int cacheSize = 0;
        if( null != mStreamMap ){
            ByteArrayOutputStream mStream = (ByteArrayOutputStream)mStreamMap.get(inStream.toString());
            if( null == mStream ){
                mStream = new ByteArrayOutputStream();
                mStreamMap.put(inStream.toString(),mStream);
            }

            try {
                if( null != data && data.length > 0 ){
                    mStream.write(data);
                }
                cacheSize = mStream.size();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        handleCheckFlush(inStream, isEnd ? true : cacheSize >= FLUSH_SIZE, isEnd);
    }

    private static void handleCheckFlush(final CustomFileOutputStream inStream, boolean checkFlush, final boolean isEnd) {
        if( checkFlush ){
            new android.os.AsyncTask<String, Void, Void>(){
                @Override
                protected Void doInBackground(String... params) {
                    try {
                        synchronized (mStreamMap) {
                            if( null != mStreamMap ){
                                String sKey = params[0];
                                Log.d(TAG, "writeRecordDataExtra sKey=" + sKey);
                                onWriteStreamMap(inStream, (ByteArrayOutputStream)mStreamMap.get(sKey), isEnd);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute(inStream.toString());
        }
    }

    private static void onWriteStreamMap(final CustomFileOutputStream inStream, ByteArrayOutputStream mStream, boolean isEnd) throws IOException {
        if( null != mStream ){
            Log.d(TAG, "writeRecordDataExtra close");
            byte[] output = mStream.toByteArray();
            inStream.write(output, 0, output.length);
            //清空缓存
            mStream.reset();
            if (isEnd) {
                inStream.close();
                mStream.close();
                mStream = null;
                inStream.setSessionId(-1);
                mStreamMap.remove(inStream.toString());
            }
        }
    }

    /**
     * 清理某个目录下的录音文件
     *
     * @param folderName
     */
    public static void clearRecordFiles(String folderName) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + folderName;
        File folder = new File(path);
        if (folder.isDirectory()) {
            for (File child : folder.listFiles()) {
                child.delete();
            }
        }
        folder.delete();
    }

    public static class CustomFileOutputStream extends FileOutputStream {
        private int mSessionId = -1;
        /**
         * Creates a file output stream to write to the file with the
         * specified name. A new <code>FileDescriptor</code> object is
         * created to represent this file connection.
         * <p>
         * First, if there is a security manager, its <code>checkWrite</code>
         * method is called with <code>name</code> as its argument.
         * <p>
         * If the file exists but is a directory rather than a regular file, does
         * not exist but cannot be created, or cannot be opened for any other
         * reason then a <code>FileNotFoundException</code> is thrown.
         *
         * @param name the system-dependent filename
         * @throws FileNotFoundException if the file exists but is a directory
         *                               rather than a regular file, does not exist but cannot
         *                               be created, or cannot be opened for any other reason
         * @throws SecurityException     if a security manager exists and its
         *                               <code>checkWrite</code> method denies write access
         *                               to the file.
         * @see SecurityManager#checkWrite(String)
         */
        public CustomFileOutputStream(String name) throws FileNotFoundException {
            this(name != null ? new File(name) : null, false);
        }

        /**
         * Creates a file output stream to write to the file with the specified
         * name.  If the second argument is <code>true</code>, then
         * bytes will be written to the end of the file rather than the beginning.
         * A new <code>FileDescriptor</code> object is created to represent this
         * file connection.
         * <p>
         * First, if there is a security manager, its <code>checkWrite</code>
         * method is called with <code>name</code> as its argument.
         * <p>
         * If the file exists but is a directory rather than a regular file, does
         * not exist but cannot be created, or cannot be opened for any other
         * reason then a <code>FileNotFoundException</code> is thrown.
         *
         * @param name   the system-dependent file name
         * @param append if <code>true</code>, then bytes will be written
         *               to the end of the file rather than the beginning
         * @throws FileNotFoundException if the file exists but is a directory
         *                               rather than a regular file, does not exist but cannot
         *                               be created, or cannot be opened for any other reason.
         * @throws SecurityException     if a security manager exists and its
         *                               <code>checkWrite</code> method denies write access
         *                               to the file.
         * @see SecurityManager#checkWrite(String)
         * @since JDK1.1
         */
        public CustomFileOutputStream(String name, boolean append)
                throws FileNotFoundException {
            this(name != null ? new File(name) : null, append);
        }

        /**
         * Creates a file output stream to write to the file represented by
         * the specified <code>File</code> object. A new
         * <code>FileDescriptor</code> object is created to represent this
         * file connection.
         * <p>
         * First, if there is a security manager, its <code>checkWrite</code>
         * method is called with the path represented by the <code>file</code>
         * argument as its argument.
         * <p>
         * If the file exists but is a directory rather than a regular file, does
         * not exist but cannot be created, or cannot be opened for any other
         * reason then a <code>FileNotFoundException</code> is thrown.
         *
         * @param file the file to be opened for writing.
         * @throws FileNotFoundException if the file exists but is a directory
         *                               rather than a regular file, does not exist but cannot
         *                               be created, or cannot be opened for any other reason
         * @throws SecurityException     if a security manager exists and its
         *                               <code>checkWrite</code> method denies write access
         *                               to the file.
         * @see File#getPath()
         * @see SecurityException
         * @see SecurityManager#checkWrite(String)
         */
        public CustomFileOutputStream(File file) throws FileNotFoundException {
            this(file, false);
        }

        /**
         * Creates a file output stream to write to the file represented by
         * the specified <code>File</code> object. If the second argument is
         * <code>true</code>, then bytes will be written to the end of the file
         * rather than the beginning. A new <code>FileDescriptor</code> object is
         * created to represent this file connection.
         * <p>
         * First, if there is a security manager, its <code>checkWrite</code>
         * method is called with the path represented by the <code>file</code>
         * argument as its argument.
         * <p>
         * If the file exists but is a directory rather than a regular file, does
         * not exist but cannot be created, or cannot be opened for any other
         * reason then a <code>FileNotFoundException</code> is thrown.
         *
         * @param file   the file to be opened for writing.
         * @param append if <code>true</code>, then bytes will be written
         *               to the end of the file rather than the beginning
         * @throws FileNotFoundException if the file exists but is a directory
         *                               rather than a regular file, does not exist but cannot
         *                               be created, or cannot be opened for any other reason
         * @throws SecurityException     if a security manager exists and its
         *                               <code>checkWrite</code> method denies write access
         *                               to the file.
         * @see File#getPath()
         * @see SecurityException
         * @see SecurityManager#checkWrite(String)
         * @since 1.4
         */
        public CustomFileOutputStream(File file, boolean append)
                throws FileNotFoundException {
            super(file, append);
        }

        /**
         * Creates a file output stream to write to the specified file
         * descriptor, which represents an existing connection to an actual
         * file in the file system.
         * <p>
         * First, if there is a security manager, its <code>checkWrite</code>
         * method is called with the file descriptor <code>fdObj</code>
         * argument as its argument.
         * <p>
         * If <code>fdObj</code> is null then a <code>NullPointerException</code>
         * is thrown.
         * <p>
         * This constructor does not throw an exception if <code>fdObj</code>
         * is {@link FileDescriptor#valid() invalid}.
         * However, if the methods are invoked on the resulting stream to attempt
         * I/O on the stream, an <code>IOException</code> is thrown.
         *
         * @param fdObj the file descriptor to be opened for writing
         * @throws SecurityException if a security manager exists and its
         *                           <code>checkWrite</code> method denies
         *                           write access to the file descriptor
         * @see SecurityManager#checkWrite(FileDescriptor)
         */
        public CustomFileOutputStream(FileDescriptor fdObj) {
            super(fdObj);
        }

        public int getSessionId() {
            return mSessionId;
        }

        public void setSessionId(int sessionId) {
            this.mSessionId = sessionId;
        }
    }
}
