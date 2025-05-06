package com.dronesky.dronedetour;

import android.app.Application;
import android.content.Context;

import com.dronesky.dronedetour.utils.ContextHolder;

public class App extends Application {

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        ContextHolder.injectApplicationContext(this);
    }
}
