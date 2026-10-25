package vn.edu.usth.flightinfo.data.remote;

import android.net.Uri;

import java.util.List;

import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.parser.AirLabsParser;
import vn.edu.usth.flightinfo.model.Flight;

/** AirLabs v9 REST API (JSON over HTTPS): flight by number and airport schedules. */
public class AirLabsService {

    private static final String BASE_URL = "https://airlabs.co/api/v9/";

    private final String apiKey;

    public AirLabsService(String apiKey) {
        this.apiKey = apiKey;
    }

    public void getFlight(String flightIata, Object tag, Callback<Flight> callback) {
        if (!hasKey(callback)) {
            return;
        }
        String url = Uri.parse(BASE_URL + "flight").buildUpon()
                .appendQueryParameter("flight_iata", flightIata)
                .appendQueryParameter("api_key", apiKey)
                .build().toString();
        ApiClient.get().getString(url, tag, new Callback<String>() {
            @Override
            public void onSuccess(String body) {
                Flight flight;
                try {
                    flight = AirLabsParser.parseFlightResponse(body);
                } catch (ApiException e) {
                    callback.onError(e);
                    return;
                }
                callback.onSuccess(flight);
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        });
    }

    /** Departures ({@code departures = true}) or arrivals of an airport. */
    public void getSchedules(String airportIata, boolean departures, Object tag, Callback<List<Flight>> callback) {
        if (!hasKey(callback)) {
            return;
        }
        String url = Uri.parse(BASE_URL + "schedules").buildUpon()
                .appendQueryParameter(departures ? "dep_iata" : "arr_iata", airportIata)
                .appendQueryParameter("api_key", apiKey)
                .build().toString();
        ApiClient.get().getString(url, tag, new Callback<String>() {
            @Override
            public void onSuccess(String body) {
                List<Flight> flights;
                try {
                    flights = AirLabsParser.parseSchedulesResponse(body);
                } catch (ApiException e) {
                    callback.onError(e);
                    return;
                }
                callback.onSuccess(flights);
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        });
    }

    private boolean hasKey(Callback<?> callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError(new ApiException(ApiException.Type.NO_KEY, "AIRLABS_API_KEY is empty"));
            return false;
        }
        return true;
    }
}
