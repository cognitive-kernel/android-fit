package com.cognitivekernel.androidfit;

import android.app.Application;

public final class FitYarApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        AppErrorLog.initialize(this);
    }
}
