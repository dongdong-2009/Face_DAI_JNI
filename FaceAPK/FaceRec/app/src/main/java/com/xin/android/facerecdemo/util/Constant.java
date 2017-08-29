package com.xin.android.facerecdemo.util;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Project ${PROJECT}
 * Created by danny on 2017/8/3.
 */

public class Constant {
    public static final int FIND_ONE_FACE_MESSAGE = 10000;
    public static final int FIND_NULL_FACE_MESSAGE = 10001;
    public static final int FIND_MULT_FACE_MESSAGE = 10002;

    public static final int FIND_ID_CARD_MESSAGE = 10010;
    public static final int LOST_ID_CARD_MESSAGE = 10011;

    public static final int START_FACE_COMPARE_MESSAGE = 10020;
    public static final int COMPARING_MESSAGE = 10021;

    public static final int CLEAR_ID_INFO_MESSAGE = 10030;
    public static final int CLEAR_CAMERA_IMAGE_MESSAGE = 10031;
    public static final int NEAR_TO_DISPLAY_MESSAGE = 10032;

    public static final int SHOW_FACE_COMPARE_RESULT_MESSAGE = 10040;

    public static final String LOCK = "lock";

    public static String getHomePath() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/FaceRec/";
        File file = new File(path);

        if (!file.exists()) {
            file.mkdir();
        }
        return path;
    }

    public static boolean hasServerConfi() {
        String licensePath = getHomePath() + "server.conf";
        File file = new File(licensePath);

        if (!file.exists()) {
            return false;
        }
        return true;
    }

    public static String readServerInfo() {
        String server = "";

        String licensePath = getHomePath() + "server.conf";
        File file = new File(licensePath);

        if (!file.exists()) {
            return server;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString;
            while ((tempString = reader.readLine()) != null) {
                server = server + tempString;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return server;
    }
}
