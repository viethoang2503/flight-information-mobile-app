package vn.edu.usth.flightinfo.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import vn.edu.usth.flightinfo.data.DataMode;

/** Typed access to the values edited in SettingsActivity. Keys must match res/xml/preferences.xml. */
public class Prefs {

    /** Not stored here: the language lives in AppCompatDelegate (see SettingsFragment). */
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_DATA_MODE = "data_mode";
    public static final String KEY_ALTITUDE_UNIT = "altitude_unit";
    public static final String KEY_DEFAULT_AIRPORT = "default_airport";
    public static final String KEY_AUTO_REFRESH = "auto_refresh";

    public static final String DEFAULT_AIRPORT = "HAN";

    private final SharedPreferences prefs;

    public Prefs(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public DataMode getDataMode() {
        return DataMode.fromPreference(prefs.getString(KEY_DATA_MODE, "auto"));
    }

    public boolean useFeet() {
        return "ft".equals(prefs.getString(KEY_ALTITUDE_UNIT, "m"));
    }

    public String getDefaultAirport() {
        return prefs.getString(KEY_DEFAULT_AIRPORT, DEFAULT_AIRPORT);
    }

    public boolean isAutoRefresh() {
        return prefs.getBoolean(KEY_AUTO_REFRESH, true);
    }
}
