package com.dronesky.dronedetour.utils;

import android.widget.Toast;

/**
 * @Description: 提供弹出Toast两种方式：1.TextView更新content；2.直接Toast。
 * @Author: 辛飞龙
 * @Email: xinfeilong@u-care.net.cn
 * @CreateDate: 2021/7/30 2:39 下午
 */
@SuppressWarnings("unchecked")
public final class ToastUtils {

    /**
     * 单纯吐司
     **/
    public static void showToast(final String str) {
      Toast.makeText(ContextHolder.applicationContext(), str, Toast.LENGTH_LONG).show();
    }

}