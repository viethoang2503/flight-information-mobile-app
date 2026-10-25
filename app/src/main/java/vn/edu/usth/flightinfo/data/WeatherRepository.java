package vn.edu.usth.flightinfo.data;

import android.content.Context;

import vn.edu.usth.flightinfo.data.local.Prefs;
import vn.edu.usth.flightinfo.data.mock.MockDataSource;
import vn.edu.usth.flightinfo.data.remote.ApiClient;
import vn.edu.usth.flightinfo.data.remote.MetarService;
import vn.edu.usth.flightinfo.model.Result;
import vn.edu.usth.flightinfo.model.Weather;

/** Airport weather (METAR) with mock fallback. */
public class WeatherRepository {

    private static WeatherRepository instance;

    private final MetarService metar = new MetarService();
    private final MockDataSource mock;
    private final Prefs prefs;

    private WeatherRepository(Context context) {
        mock = MockDataSource.get(context);
        prefs = new Prefs(context);
    }

    public static synchronized WeatherRepository get(Context context) {
        if (instance == null) {
            instance = new WeatherRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void getMetar(String icao, Object tag, Callback<Result<Weather>> callback) {
        FallbackLoader.load(prefs.getDataMode(),
                cb -> metar.getMetar(icao, tag, cb),
                cb -> mock.getMetar(icao, cb),
                callback);
    }

    public void cancel(Object tag) {
        ApiClient.get().cancel(tag);
    }
}
