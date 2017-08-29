package com.xin.android.facerecdemo.util;

import com.xin.android.facerecdemo.bean.FaceInfo;
import com.xin.android.facerecdemo.bean.ImageData;

import java.util.ArrayList;

/**
 * Project ${PROJECT}
 * Created by danny on 2017/7/27.
 */

public class JniLoader {
    private static JniLoader instance = null;

    // Used to load the 'native-lib' library on application startup.
    /*static {
        System.loadLibrary("face_rec");
        OpenCVLoader.initDebug();
//        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }*/

    private JniLoader() {

    }

    public static JniLoader getInstance() {
        if (instance == null) {
            instance = new JniLoader();
        }
        return instance;
    }

    public void loadLibrary() {
        System.loadLibrary("abc");

        System.loadLibrary("native-lib");

        System.loadLibrary("face_rec");
//        OpenCVLoader.initDebug();
        System.loadLibrary("opencv_java3");

        setLogEnable(false);
    }

    public int callInitFaceRec(int channels, String path) {

        return initFaceRecWithPath(channels, path);
    }

    public float[] callFaceRecExtract(ImageData imgColor, ImageData imgGray) {
        return faceRecExtract(0, imgColor, imgGray);
    }

    public ArrayList<FaceInfo> callFaceRecDetect(int channelID, ImageData imgColor, ImageData imgGray) {
        return faceRecDetect(channelID, imgColor, imgGray);
    }

    public float callFaceCampare(float[] src, float[] dst) {
        return faceRecCompare(src, dst);
    }

    public float callFaceRecDeinit() {
        return faceRecDeinit();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public static native int[] getGrayImage(int[] pixels, int w, int h);

    public native int initFaceRec(int channelNum);
    public native int initFaceRecWithPath(int channelNum, String path);

    public native float[] faceRecExtract(int channelID, ImageData imgColor, ImageData imgGray);

    public native ArrayList<FaceInfo> faceRecDetect(int channelID, ImageData imgColor, ImageData
            imgGray);

    public native float faceRecCompare(float[] imgFea1, float[] imgFea2);

    public native int faceRecDeinit();

/*    public native boolean hasProductKey();

    public native String getDeviceId();

    public native boolean enterProductKey(String key);*/

    public native boolean checkDeviceState(String path);
    public native String getDeviceUuid();
    public native boolean registerDeviceKey(String path, String key);

    public native void setLogEnable(boolean enable);

}
