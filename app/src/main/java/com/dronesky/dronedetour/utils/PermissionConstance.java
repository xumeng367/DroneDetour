package com.dronesky.dronedetour.utils;

import android.Manifest;

/**
 * @Author: Lyq
 * @CreateDate: 2019/10/22 15:02
 */
public class PermissionConstance {

    public static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,

    };
    public static final int REQUEST_PERMISSION_CODE = 12345;

    public static final String USB_PERMISSION = "com.android.example.USB_PERMISSION";
}
