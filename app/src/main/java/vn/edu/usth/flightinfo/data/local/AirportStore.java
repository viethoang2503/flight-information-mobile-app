package vn.edu.usth.flightinfo.data.local;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.parser.AirportParser;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.util.AppExecutors;

/** Airports bundled in assets/airports.json, loaded once on a worker thread and cached. */
public final class AirportStore {

    private static final String FILE_NAME = "airports.json";

    private static AirportStore instance;

    private final Context appContext;
    private volatile List<Airport> airports;
    private volatile Map<String, Airport> byIata = Collections.emptyMap();

    private AirportStore(Context context) {
        appContext = context.getApplicationContext();
    }

    public static synchronized AirportStore get(Context context) {
        if (instance == null) {
            instance = new AirportStore(context);
        }
        return instance;
    }

    /** Delivers all airports on the main thread (immediately if already loaded). */
    public void load(Callback<List<Airport>> callback) {
        if (airports != null) {
            callback.onSuccess(airports);
            return;
        }
        AppExecutors executors = AppExecutors.get();
        executors.io(() -> {
            try {
                List<Airport> loaded = readAirports();
                Map<String, Airport> index = new HashMap<>();
                for (Airport airport : loaded) {
                    index.put(airport.getIata(), airport);
                }
                byIata = index;
                airports = Collections.unmodifiableList(loaded);
                executors.main(() -> callback.onSuccess(airports));
            } catch (Exception e) {
                ApiException error = new ApiException(ApiException.Type.PARSE, "Cannot read " + FILE_NAME, e);
                executors.main(() -> callback.onError(error));
            }
        });
    }

    /** Airport by IATA code, or null if unknown or not loaded yet. */
    public Airport find(String iata) {
        return iata == null ? null : byIata.get(iata.toUpperCase(Locale.US));
    }

    private List<Airport> readAirports() throws Exception {
        try (InputStream in = appContext.getAssets().open(FILE_NAME);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return AirportParser.parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
        }
    }
}
