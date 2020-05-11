package com.tencent.dingdangsampleapp.util;

import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {


    private static final String TAG = "ISO8601DateFormatter";
    private static final DateFormat DATE_FORMAT_1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.CHINESE);
    private static final DateFormat DATE_FORMAT_2 = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssZ", Locale.CHINESE);
    private static final String UTC_PLUS = "+";
    private static final String UTC_MINUS = "-";

    /**
     * 将iso8601string格式字符串时间转为Date
     *
     * @param iso8601string iso8601格式的时间字符串
     * @return Date Date类型表示的结果
     * @throws ParseException ParseException
     */
    public static Date toDate(String iso8601string) throws ParseException {
        iso8601string = iso8601string.trim();
        if (iso8601string.toUpperCase().indexOf("Z") > 0) {
            iso8601string = iso8601string.toUpperCase().replace("Z", "+0000");
        } else if (((iso8601string.indexOf(UTC_PLUS)) > 0)) {
            iso8601string = replaceColon(iso8601string, iso8601string.indexOf(UTC_PLUS));
            iso8601string = appendZeros(iso8601string, iso8601string.indexOf(UTC_PLUS), UTC_PLUS);
        } else if (((iso8601string.indexOf(UTC_MINUS)) > 0)) {
            iso8601string = replaceColon(iso8601string, iso8601string.indexOf(UTC_MINUS));
            iso8601string = appendZeros(iso8601string, iso8601string.indexOf(UTC_MINUS), UTC_MINUS);
        }
        Log.d(TAG, "iso8601string:" + iso8601string);
        Date date;
        if (iso8601string.contains(":")) {
            date = DATE_FORMAT_1.parse(iso8601string);
        } else {
            date = DATE_FORMAT_2.parse(iso8601string);
        }
        return date;
    }

    public static String secToTimeString(long time) {
        String timeStr = null;
        long hour = 0;
        long minute = 0;
        long second = 0;
        if (time <= 0) {
            return "00:00";
        } else {
            minute = time / 60;
            if (minute < 60) {
                second = time % 60;
                timeStr = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = time - hour * 3600 - minute * 60;
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    public static String unitFormat(long i) {
        String retStr;
        if (i >= 0 && i < 10)
            retStr = "0" + Long.toString(i);
        else
            retStr = "" + i;
        return retStr;
    }
    public static String formatDate(Date date, String format, Locale locale) {
        return new SimpleDateFormat(format, locale).format(date);
    }

    private static String replaceColon(String sourceStr, int offsetIndex) {
        if (sourceStr.substring(offsetIndex).contains(":")) {
            return sourceStr.substring(0, offsetIndex) + sourceStr.substring(offsetIndex).replace(":", "");
        }
        return sourceStr;
    }

    private static String appendZeros(String sourceStr, int offsetIndex, String offsetChar) {
        if ((sourceStr.length() - 1) - sourceStr.indexOf(offsetChar, offsetIndex) <= 2) {
            return sourceStr + "00";
        }
        return sourceStr;
    }

}
