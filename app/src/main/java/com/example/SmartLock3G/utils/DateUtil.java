package com.example.SmartLock3G.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Administrator on 2017/11/28.
 */

public class DateUtil {
    public static String formatDate(long timeMillions) {
        Date date = new Date(timeMillions);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE);
        return sdf.format(date);
    }

    public static String formatTime(long timeMillions) {
        Date date = new Date(timeMillions);
        SimpleDateFormat sdf = new SimpleDateFormat("hh-mm-ss", Locale.CHINESE);
        return sdf.format(date);
    }

    public static String formatTime() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.CHINESE);
        return sdf.format(date);
    }
}
