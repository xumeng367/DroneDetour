package com.dronesky.dronedetour.utils;

import android.os.Handler;
import android.os.Looper;

public class MainHandler {
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    public static void post(Runnable runnable, long delayMillis) {
        sMainHandler.postDelayed(runnable, delayMillis);
    }
}
