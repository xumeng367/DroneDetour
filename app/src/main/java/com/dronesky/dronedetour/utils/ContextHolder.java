package com.dronesky.dronedetour.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/**
 * 持有Application
 *
 * @author xumeng
 */
public class ContextHolder {

    private static Application sApplicationContext = null;
    private static Activity sCurrentActivity = null;

    /**
     * 在Application::onCreate()时注入context
     *
     * @param application Application context
     */
    public static void injectApplicationContext(@NonNull Application application) {
        sApplicationContext = application;
    }

    public static Context applicationContext() {
        return sApplicationContext;
    }

    public static Application application() {
        return sApplicationContext;
    }

    public static Activity getCurrentActivity() {
        return sCurrentActivity;
    }

    public static void setCurrentActivity(Activity activity) {
        sCurrentActivity = activity;
    }

    public static String getString(@StringRes int resId) {
        return applicationContext().getString(resId);
    }

}
