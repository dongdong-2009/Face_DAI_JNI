package com.xin.android.facerecdemo;

import android.app.Application;

/**
 * Project ${PROJECT}
 * Created by danny on 2017/7/27.
 */

public class App extends Application {
    private String TAG = getClass().getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void exitApp() {
        System.exit(0);
    }
}
