package vn.edu.usth.flightinfo.data;

import android.content.Context;

import java.util.List;

import vn.edu.usth.flightinfo.data.local.Prefs;
import vn.edu.usth.flightinfo.data.mock.MockDataSource;
import vn.edu.usth.flightinfo.data.remote.ApiClient;
import vn.edu.usth.flightinfo.data.remote.OpenSkyService;
import vn.edu.usth.flightinfo.model.AircraftState;
import vn.edu.usth.flightinfo.model.Result;

/** Live aircraft positions over Vietnam (OpenSky) with mock fallback. */
public class MapRepository {

    private static MapRepository instance;

    private final OpenSkyService openSky = new OpenSkyService();
    private final MockDataSource mock;
    private final Prefs prefs;

    private MapRepository(Context context) {
        mock = MockDataSource.get(context);
        prefs = new Prefs(context);
    }

    public static synchronized MapRepository get(Context context) {
        if (instance == null) {
            instance = new MapRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void getAircraft(Object tag, Callback<Result<List<AircraftState>>> callback) {
        FallbackLoader.load(prefs.getDataMode(),
                cb -> openSky.getStates(tag, cb),
                mock::getAircraft,
                callback);
    }

    public void cancel(Object tag) {
        ApiClient.get().cancel(tag);
    }
}
