package vn.edu.usth.flightinfo.data.parser;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Null-safe readers for JSONObject / JSONArray.
 * (org.json's optString() returns the text "null" for JSON null, which we never want.)
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    public static String optString(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) {
            return null;
        }
        String value = o.optString(key, "").trim();
        return value.isEmpty() ? null : value;
    }

    public static long optLong(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) {
            return 0;
        }
        return o.optLong(key, 0);
    }

    public static Integer optInteger(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) {
            return null;
        }
        Object value = o.opt(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double optDouble(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) {
            return null;
        }
        Object value = o.opt(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double optDouble(JSONArray a, int index) {
        if (a == null || index >= a.length() || a.isNull(index)) {
            return null;
        }
        Object value = a.opt(index);
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    public static String optString(JSONArray a, int index) {
        if (a == null || index >= a.length() || a.isNull(index)) {
            return null;
        }
        String value = a.optString(index, "").trim();
        return value.isEmpty() ? null : value;
    }
}
