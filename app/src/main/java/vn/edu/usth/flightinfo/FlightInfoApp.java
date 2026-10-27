package vn.edu.usth.flightinfo;

import android.app.Application;
import android.os.StrictMode;

import vn.edu.usth.flightinfo.data.auth.AuthManager;
import vn.edu.usth.flightinfo.data.local.Prefs;
import vn.edu.usth.flightinfo.data.local.RecentSearches;
import vn.edu.usth.flightinfo.data.remote.ApiClient;
import vn.edu.usth.flightinfo.util.AppExecutors;

/**
 * Application subclass: the one place for app-wide, early initialization
 * (one Volley RequestQueue per app, StrictMode in debug builds). osmdroid is configured later,
 * off the main thread, the first time the map is opened (LiveMapFragment).
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

        // Opening a SharedPreferences file reads the disk once per process. Do it here, off the
        // main thread, for the files the first screens need: the session check in MainActivity,
        // then the settings and recent searches in SearchFragment. If the UI gets there first, it
        // only waits on AuthManager.get()'s lock or for the already started load to finish.
        AppExecutors.get().io(() -> {
            AuthManager.get(this);
            new Prefs(this).getDataMode();
            new RecentSearches(this).getAll();
        });
    }
}
