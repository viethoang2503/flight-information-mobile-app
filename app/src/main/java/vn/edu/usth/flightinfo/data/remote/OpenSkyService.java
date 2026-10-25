package vn.edu.usth.flightinfo.data.remote;

import java.util.List;
import java.util.Locale;

import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.parser.OpenSkyParser;
import vn.edu.usth.flightinfo.model.AircraftState;

/** OpenSky Network live aircraft positions (anonymous access, rate limited). */
public class OpenSkyService {

    /** Bounding box around Vietnam: lat 8..24, lon 102..110. */
    public static final double MIN_LAT = 8.0;
    public static final double MIN_LON = 102.0;
    public static final double MAX_LAT = 24.0;
    public static final double MAX_LON = 110.0;

    private static final String URL_FORMAT =
            "https://opensky-network.org/api/states/all?lamin=%.1f&lomin=%.1f&lamax=%.1f&lomax=%.1f";

    public void getStates(Object tag, Callback<List<AircraftState>> callback) {
        String url = String.format(Locale.US, URL_FORMAT, MIN_LAT, MIN_LON, MAX_LAT, MAX_LON);
        ApiClient.get().getString(url, tag, new Callback<String>() {
            @Override
            public void onSuccess(String body) {
                List<AircraftState> states;
                try {
                    states = OpenSkyParser.parse(body);
                } catch (ApiException e) {
                    callback.onError(e);
                    return;
                }
                callback.onSuccess(states);
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        });
    }
}
