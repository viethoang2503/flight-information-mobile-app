package vn.edu.usth.flightinfo;

import android.app.Application;
import android.os.StrictMode;

import androidx.preference.PreferenceManager;

import org.osmdroid.config.Configuration;

import vn.edu.usth.flightinfo.data.remote.ApiClient;

/**
 * Application subclass: the one place for app-wide, early initialization
 * (one Volley RequestQueue per app, osmdroid configuration, StrictMode).
 */
public class FlightInfoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            // Flags any disk or network access accidentally done on the main thread.
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
        }

        ApiClient.init(this);

        // osmdroid needs a user agent to be allowed to download OpenStreetMap tiles.
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
    }
}
