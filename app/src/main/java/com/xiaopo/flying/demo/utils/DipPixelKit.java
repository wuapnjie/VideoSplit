package com.xiaopo.flying.demo.utils;

import android.annotation.SuppressLint;
import android.content.Context;

/**
 * Dip和Pixel之间转化
 */
public final class DipPixelKit {
    private static float sScale = 0;
    private static int sDeviceWidth = 0;
    private static int sDeviceHeight = 0;

    public static int getDeviceWidth(Context context) {
        if (sDeviceWidth == 0) {
            sDeviceWidth = context.getResources().getDisplayMetrics().widthPixels;
        }
        return sDeviceWidth;
    }

    public static int getDeviceHeight(Context context) {
        if (sDeviceHeight == 0) {
            sDeviceHeight = context.getResources().getDisplayMetrics().heightPixels;
        }
        return sDeviceHeight;
    }

    @SuppressLint("DefaultLocale")
    public static int dip2px(Context context, float dpValue) {
        if (sScale == 0) {
            sScale = context.getResources().getDisplayMetrics().density;
        }
        return (int) (dpValue * sScale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        if (sScale == 0) {
            sScale = context.getResources().getDisplayMetrics().density;
        }
        return (int) (pxValue / sScale + 0.5f);
    }

    public static int sp2pix(float spValue, float fontScale) {
        return (int) (spValue * fontScale + 0.5f);
    }

    public static int sp2px(Context context, float spValue) {
        final float scale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * scale);
    }
}
