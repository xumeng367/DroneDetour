package com.dronesky.dronedetour.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 共用工具类
 *
 * @Author: Lyq
 * @CreateDate: 2020/1/17 11:19
 */
public class CommonUtil {
    private static Gson gson = null;

    public static Gson getGson() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }

    /**
     * map to jsonString
     */
    public static String map2str(Map<String, Object> map) {
        try {
            JSONObject mapJsonObj = new JSONObject(map);
            return mapJsonObj.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * object转map
     */
    public static Map<String, Object> object2Map(Object obj) throws IllegalAccessException {
        if (null == obj) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        Class<?> clazz = obj.getClass();
        System.out.println(clazz);
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Object value = field.get(obj);
            if (value == null) {
                value = "";
            }
            map.put(fieldName, value);
        }
        return map;
    }

    /**
     * url编码
     */
    public static String urlEncode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }

    /**
     * 解密数据
     *
     * @param str string
     * @return string
     */
    public static String decryptStr(String str) {
        return TextUtils.isEmpty(str) ? null : new String(Base64.decode(str.getBytes(), Base64.DEFAULT));
    }


    /**
     * 加密数据
     *
     * @param str string
     * @return string
     */
    public static String encryptStr(String str) {
        return TextUtils.isEmpty(str) ? null : Base64.encodeToString(str.getBytes(), Base64.DEFAULT);
    }

    /**
     * 把json转换成一个类的实例
     *
     * @param c       class
     * @param jsonStr json
     * @param <T>     class
     * @return entity
     */
    public static <T> T fromJsonStr(Class<T> c, String jsonStr) {
        if (jsonStr == null) {
            return null;
        }
        T t = null;
        try {
            t = getGson().fromJson(jsonStr, c);
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return t;
    }

    /**
     * 把Object转换成json
     *
     * @param object object
     * @return String
     */
    public static String toJson(Object object) {
        return getGson().toJson(object);
    }

    /**
     * json转list
     */
    public static <T> List<T> jsonToList(String json, Class<T> cls) {
        List<T> list = new ArrayList<>();
        if (TextUtils.isEmpty(json) || !json.startsWith("[")) {
            return list;
        }
        JsonParser.parseString(json)
                .getAsJsonArray()
                .forEach(jsonElement -> list.add(CommonUtil.getGson().fromJson(jsonElement, cls)));
        return list;
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 打开另一个app
     */
    public static int openOtherApp(Context context, String packname, PackageManager packageManager) {
        try {
            Intent intent = packageManager.getLaunchIntentForPackage(packname);
            if (null == intent) {
                return 1;
            }
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
        // 打开第三方后
//        exitAndKillMyself(context);
        return 0;
    }

    /**
     * APP重新打开
     */
    public static void appReRunning(Context context, String packageName) {
        ActivityManager mActivityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> listOfProcesses = mActivityManager
                .getRunningAppProcesses();
        boolean isRun = false;
        for (ActivityManager.RunningAppProcessInfo process : listOfProcesses) {
            if (process.processName.contains(packageName)) {
                isRun = true;
                break;
            }
        }
        if (!isRun) {
            // 拉活
            CommonUtil.openOtherApp(context.getApplicationContext(), packageName, context.getPackageManager());
        }
    }
}