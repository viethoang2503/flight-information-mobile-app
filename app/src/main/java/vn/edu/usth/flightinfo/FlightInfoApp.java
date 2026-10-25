package vn.edu.usth.flightinfo;

import android.app.Application;
import android.os.StrictMode;

import vn.edu.usth.flightinfo.data.remote.ApiClient;

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
    }
}
