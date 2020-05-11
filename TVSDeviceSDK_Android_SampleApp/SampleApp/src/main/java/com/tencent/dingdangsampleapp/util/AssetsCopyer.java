package com.tencent.dingdangsampleapp.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetsCopyer {
    private static final String TAG = "AssetsCopyer";

    public static boolean releaseAssetsMD5(Context context, String assetsDir,
                                        String releaseDir) {
        boolean ret = true;

        Log.i(TAG, "releaseAssetsMD5 is in");
        if (TextUtils.isEmpty(releaseDir)) {
            return false;
        } else if (releaseDir.endsWith("/")) {
            releaseDir = releaseDir.substring(0, releaseDir.length() - 1);
        }

        if (TextUtils.isEmpty(assetsDir) || assetsDir.equals("/")) {
            assetsDir = "";
        } else if (assetsDir.endsWith("/")) {
            assetsDir = assetsDir.substring(0, assetsDir.length() - 1);
        }

        AssetManager assets = context.getAssets();
        try {
            String[] fileNames = assets.list(assetsDir);//只能获取到文件(夹)名,所以还得判断是文件夹还是文件
            if (fileNames.length > 0) {// is dir
                for (String name : fileNames) {
                    if (!TextUtils.isEmpty(assetsDir)) {
                        name = assetsDir + File.separator + name;//补全assets资源路径
                    }
                    Log.i(TAG, "releaseAssetsMD5 name ="+name);
                    String[] childNames = assets.list(name);//判断是文件还是文件夹
                    if (!TextUtils.isEmpty(name) && childNames.length > 0) {
                        checkFolderExists(releaseDir + File.separator + name);
                        releaseAssetsMD5(context, name, releaseDir);//递归, 因为资源都是带着全路径,
                        //所以不需要在递归是设置目标文件夹的路径
                    } else if (!writeFile(releaseDir + File.separator + name, assets.open(name))) {
                        ret = false;
                    }
                }
            } else if (!writeFile(releaseDir + File.separator + assetsDir, assets.open(assetsDir))) {
                    ret = false;
            }
        } catch (Exception e) {
            Log.i(TAG, "releaseAssets e ="+e.getMessage());
            ret = false;
        }
        return ret;
    }

    private static boolean writeFile(String fileName, InputStream in) throws IOException {
        boolean bRet = true;
        OutputStream os = null;
        try {
//            Log.i(TAG, "writeFile fileName="+fileName);
            os = new FileOutputStream(fileName);
            byte[] buffer = new byte[4112];
            int read;
            while((read = in.read(buffer)) != -1)
            {
                os.write(buffer, 0, read);
            }
            in.close();
            in = null;
            os.flush();
            os.close();
            os = null;
//			Log.v(TAG, "copyed file: " + fileName);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.i(TAG, "writeFile Exception="+e.getMessage());
            bRet = false;
        } finally {
            if( null != os ){
                os.close();
            }

            if( null != in ){
                in.close();
            }
        }
        Log.i(TAG, "writeFile fileName="+fileName+", ret="+bRet);
        return bRet;
    }

    private static void checkFolderExists(String path) {
        File file = new File(path);
        if((file.exists() && !file.isDirectory()) || !file.exists()) {
            file.mkdirs();
        }
    }

}
