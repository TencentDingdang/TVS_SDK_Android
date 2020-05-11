package com.tencent.dingdangsampleapp.util;

import android.content.Context;
import android.util.Log;

import com.tencent.dingdangsampleapp.BuildConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class FileUtils {

    private static final String TAG = "FileUtils";
    public static final String APPKEY = "appkey";
    public static final String APPSECRET = "appsecret";
    public static final String CUSTOMDSN = "customdsn";

    public static String copyModelFiles(Context inContext) {
        final String sPath = "aifile";
        //拷贝assert目录下面的keywords_model目录到sdcard根目录
        final String sdcardFolder  = "/sdcard";
        final String sKeywordsPath = sdcardFolder + "/aifile";
        //先清除原来有的目录
        File mFile = new File(sKeywordsPath);
        delete(mFile);
        //创建目录
        if (!mFile.exists()) {
            mFile.mkdirs();
        }

        Log.i(TAG, "copyModelFiles is step 2");

        File mFileCopy1 = new File(sKeywordsPath + "/keywords_model");
        if (!mFileCopy1.exists()) {
            mFileCopy1.mkdirs();
        }

        Log.i(TAG, "copyModelFiles is step 3");
        File mFileCopy2 = new File(sKeywordsPath + "/oneshot");
        if (!mFileCopy2.exists()) {
            mFileCopy2.mkdirs();
        }
        Log.i(TAG, "copyModelFiles is step 4");
        File mFileCopy3 = new File(sKeywordsPath + "/vad");
        if (!mFileCopy3.exists()) {
            mFileCopy3.mkdirs();
        }

        //进行文件拷贝的操作
        boolean result = AssetsCopyer.releaseAssetsMD5(inContext, sPath, sdcardFolder);
        Log.i(TAG, "releaseAssetsMD5 iresult = "+result);

        return sKeywordsPath;
    }

    public static boolean isFileExist(String sFilePath) {
        boolean isExist = false;
        File mFile = new File(sFilePath);
        if (mFile.exists()) {
            isExist = true;
        }

        return isExist;
    }

    /**
     * 递归删除文件及文件夹
     *
     * @param file
     */
    public static void delete(File file) {
        if (file.isFile()) {
            file.delete();
            return;
        }

        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete();
                return;
            }

            for (int i = 0; i < childFiles.length; i++) {
                delete(childFiles[i]);
            }
            file.delete();
        }
    }

    public static Map<String, String> loadDeviceInfo(String path){
        Log.i(TAG, "loadConfig path = "+path);
        FileInputStream in;
        BufferedReader reader = null;
        Map<String, String> botMap = new HashMap<>();
        try{
            in = new FileInputStream(path);
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null){
                if (line.startsWith(APPKEY)) {
                    String [] key = line.split(APPKEY+"-");
                    botMap.put(APPKEY, key[1]);
                }
                if (line.startsWith(APPSECRET)) {
                    String [] secret = line.split(APPSECRET+"-");
                    botMap.put(APPSECRET, secret[1]);
                }
                if (line.startsWith(CUSTOMDSN)) {
                    String [] dsn= line.split(CUSTOMDSN+"-");
                    botMap.put(CUSTOMDSN, dsn[1]);
                }
            }
            Log.i(TAG, "loadConfig botMap : " + botMap);
        }
        catch (IOException e){
            e.printStackTrace();
            Log.e(TAG, "loadDeviceInfo e =  "+e);
        }
        finally {
            if (reader !=null){
                try{
                    reader.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return botMap;
    }

    @SuppressWarnings("rawtypes")
    public static Object invokeMethod(Object target, String className, String methodName, Class[] paramTypes,
                                      Object[] paramValues) {

        try {
            Class clazz = Class.forName(className);
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(target, paramValues);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }
}
