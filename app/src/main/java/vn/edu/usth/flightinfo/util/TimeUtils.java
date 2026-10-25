package vn.edu.usth.flightinfo.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Helpers for AirLabs "yyyy-MM-dd HH:mm" strings and UNIX-second timestamps. */
public final class TimeUtils {

    public static final String API_PATTERN = "yyyy-MM-dd HH:mm";

    private TimeUtils() {
    }

    /** "2025-01-01 08:05" -> "08:05"; null if the input is missing or malformed. */
    public static String hhmm(String dateTime) {
        if (dateTime == null || dateTime.length() < 16) {
            return null;
        }
        return dateTime.substring(11, 16);
    }

    /**
     * How far along a flight is, 0..100, based on departure/arrival timestamps.
     * Returns -1 when either timestamp is unknown.
     */
    public static int progressPercent(long depTs, long arrTs, long nowTs) {
        if (depTs <= 0 || arrTs <= depTs) {
            return -1;
        }
        if (nowTs <= depTs) {
            return 0;
        }
        if (nowTs >= arrTs) {
            return 100;
        }
        return (int) ((nowTs - depTs) * 100 / (arrTs - depTs));
    }

    /**
     * Moves a "yyyy-MM-dd HH:mm" string by the given number of seconds. The string is treated as a
     * plain wall-clock value (no time zone), so local and UTC strings shift identically.
     */
    public static String shift(String dateTime, long offsetSeconds) {
        if (dateTime == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(API_PATTERN, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = format.parse(dateTime);
            return format.format(new Date(date.getTime() + offsetSeconds * 1000L));
        } catch (ParseException e) {
            return dateTime;
        }
    }

    public static long nowSeconds() {
        return System.currentTimeMillis() / 1000L;
    }
}
