package com.dronesky.dronedetour.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * @Description: SharePreferences 数据缓存工具类
 * @Author: 辛飞龙
 * @Email: xinfeilong@u-care.net.cn
 * @CreateDate: 2021/8/17 2:21 下午
 */
public class SpUtil {
    private static SharedPreferences sharedPreferences = null;
    private static SharedPreferences.Editor edit = null;
    private static final String SP_NAME = "config";
    /**
     * 获取SharedPreferences实例
     */
    private static SharedPreferences getSharedP() {
        if (sharedPreferences == null) {
            sharedPreferences = ContextHolder.applicationContext()
                    .getSharedPreferences(SP_NAME,Context.MODE_PRIVATE);
        }
        return sharedPreferences;
    }

    /**
     * 缓存字符串
     */
    public static void putString(String key, String value) {
        getEdit().putString(key, value).apply();
    }

    private static SharedPreferences.Editor getEdit() {
        if (edit == null) {
            edit = getSharedP().edit();
        }
        return edit;
    }

    /**
     * 取出字符串
     */
    public static String getString(String key, String defaultStr) {
        return getSharedP().getString(key, defaultStr);
    }

    /**
     * 缓存int值
     */
    public static void putInt(String key, int value) {
        getEdit().putInt(key, value).apply();
    }

    /**
     * 取出int值
     */
    public static int getInt(String key, int defaultInt) {
        return getSharedP().getInt(key, defaultInt);
    }

    /**
     *  缓存float值
     * @param key
     * @param value
     */
    public static void putFloat(String key, float value){
        getEdit().putFloat(key, value).apply();
    }

    /**
     * 取出float值
     */
    public static float getFloat(String key, float defaultFloat) {
        return getSharedP().getFloat(key, defaultFloat);
    }

    /**
     * 缓存boolean值
     */
    public static void putBool(String key, boolean value) {
        getEdit().putBoolean(key, value).apply();
    }

    /**
     * 取出boolean值
     */
    public static boolean getBool(String key, boolean defaultBoolean) {
        return getSharedP().getBoolean(key, defaultBoolean);
    }

    /**
     * 将对象已String形式存储
     */
    public static <T> void putObject(String key, T object) {
        String str = CommonUtil.getGson().toJson(object);
        putString(key, str);
    }

    /**
     * 提取对象的String，转换为T
     */
    public static <T> T getObject(String key, Class<T> c) {
        String str = getString(key, "");
        T t = null;
        if (!TextUtils.isEmpty(str)) {
            t = CommonUtil.getGson().fromJson(str, c);
        }
        return t;
    }

    /**
     * 是否缓存过指定key的值
     */
    public static boolean hasSharedPKey(String key) {
        return getSharedP().contains(key);
    }

    /**
     * 删除某个值
     */
    public static void removeKey(String key) {
        getEdit().remove(key).apply();
    }

    /**
     * 清除SharedPreferences数据
     */
    public static void clearSharedP() {
        getEdit().clear().apply();
    }
     
}