package vn.edu.usth.flightinfo.data.remote;

import android.net.Uri;

import java.util.List;

import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.parser.MetarParser;
import vn.edu.usth.flightinfo.model.Weather;

/** Latest METAR weather report from aviationweather.gov (no key needed). */
public class MetarService {

    private static final String BASE_URL = "https://aviationweather.gov/api/data/metar";

    public void getMetar(String icao, Object tag, Callback<Weather> callback) {
        String url = Uri.parse(BASE_URL).buildUpon()
                .appendQueryParameter("ids", icao)
                .appendQueryParameter("format", "json")
                .build().toString();
        ApiClient.get().getString(url, tag, new Callback<String>() {
            @Override
            public void onSuccess(String body) {
                List<Weather> reports;
                try {
                    reports = MetarParser.parse(body);
                } catch (ApiException e) {
                    callback.onError(e);
                    return;
                }
                if (reports.isEmpty()) {
                    callback.onError(new ApiException(ApiException.Type.NOT_FOUND, "No METAR for " + icao));
                } else {
                    callback.onSuccess(reports.get(0));
                }
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        });
    }
}
