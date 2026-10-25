package vn.edu.usth.flightinfo.data.mock;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.parser.AirLabsParser;
import vn.edu.usth.flightinfo.data.parser.MetarParser;
import vn.edu.usth.flightinfo.data.parser.OpenSkyParser;
import vn.edu.usth.flightinfo.model.AircraftState;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.model.Weather;
import vn.edu.usth.flightinfo.util.AppExecutors;
import vn.edu.usth.flightinfo.util.TimeUtils;

/**
 * Demo data bundled in res/raw, in the same JSON format as the real APIs so the same parsers are used.
 * Files are read on a worker thread and results are posted back to the main thread.
 */
public class MockDataSource {

    /** Mock aircraft fly for this long, then jump back to their start position. */
    private static final long AIRCRAFT_LOOP_SECONDS = 30 * 60;

    private static MockDataSource instance;

    private final Context appContext;
    private final long createdAt = TimeUtils.nowSeconds();

    // Raw file contents, read once and kept in memory (the files are small).
    private String flightsJson;
    private String openSkyJson;
    private String metarJson;

    private MockDataSource(Context context) {
        appContext = context.getApplicationContext();
    }

    public static synchronized MockDataSource get(Context context) {
        if (instance == null) {
            instance = new MockDataSource(context);
        }
        return instance;
    }

    public void getFlight(String flightIata, Callback<Flight> callback) {
        runAsync(() -> {
            Flight flight = MockQueries.findFlight(loadFlights(), flightIata, TimeUtils.nowSeconds());
            if (flight == null) {
                throw new ApiException(ApiException.Type.NOT_FOUND, "No mock flight " + flightIata);
            }
            return flight;
        }, callback);
    }

    public void getSchedules(String airportIata, boolean departures, Callback<List<Flight>> callback) {
        runAsync(() -> MockQueries.filterSchedules(loadFlights(), airportIata, departures), callback);
    }

    public void getAircraft(Callback<List<AircraftState>> callback) {
        runAsync(() -> {
            long elapsed = (TimeUtils.nowSeconds() - createdAt) % AIRCRAFT_LOOP_SECONDS;
            return MockQueries.moveAircraft(OpenSkyParser.parse(readOpenSky()), elapsed);
        }, callback);
    }

    public void getMetar(String icao, Callback<Weather> callback) {
        runAsync(() -> {
            for (Weather weather : MetarParser.parse(readMetar())) {
                if (icao != null && icao.equalsIgnoreCase(weather.getIcao())) {
                    return weather;
                }
            }
            throw new ApiException(ApiException.Type.NOT_FOUND, "No mock METAR for " + icao);
        }, callback);
    }

    // --- File loading (worker thread only) ---

    private List<Flight> loadFlights() throws Exception {
        if (flightsJson == null) {
            flightsJson = readRaw(R.raw.mock_flights);
        }
        JSONObject root = new JSONObject(flightsJson);
        long offset = MockTimeShifter.offsetSeconds(root.getLong("anchor_ts"), TimeUtils.nowSeconds());
        JSONArray array = root.getJSONArray("flights");
        List<Flight> flights = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            flights.add(AirLabsParser.parseFlight(MockTimeShifter.shift(array.getJSONObject(i), offset)));
        }
        return flights;
    }

    private String readOpenSky() throws IOException {
        if (openSkyJson == null) {
            openSkyJson = readRaw(R.raw.mock_opensky);
        }
        return openSkyJson;
    }

    private String readMetar() throws IOException {
        if (metarJson == null) {
            metarJson = readRaw(R.raw.mock_metar);
        }
        return metarJson;
    }

    private String readRaw(int resId) throws IOException {
        try (InputStream in = appContext.getResources().openRawResource(resId);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /** Runs {@code task} on a worker thread and delivers its result on the main thread. */
    private <T> void runAsync(Callable<T> task, Callback<T> callback) {
        AppExecutors executors = AppExecutors.get();
        executors.io(() -> {
            try {
                T result = task.call();
                executors.main(() -> callback.onSuccess(result));
            } catch (ApiException e) {
                executors.main(() -> callback.onError(e));
            } catch (Exception e) {
                ApiException error = new ApiException(ApiException.Type.PARSE, "Mock data error", e);
                executors.main(() -> callback.onError(error));
            }
        });
    }
}
