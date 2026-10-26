package vn.edu.usth.flightinfo.data;

import android.content.Context;

import java.util.List;

import vn.edu.usth.flightinfo.BuildConfig;
import vn.edu.usth.flightinfo.data.auth.AuthManager;
import vn.edu.usth.flightinfo.data.local.Prefs;
import vn.edu.usth.flightinfo.data.local.SavedFlightsDb;
import vn.edu.usth.flightinfo.data.mock.MockDataSource;
import vn.edu.usth.flightinfo.data.remote.AirLabsService;
import vn.edu.usth.flightinfo.data.remote.ApiClient;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.model.Result;
import vn.edu.usth.flightinfo.util.AppExecutors;

/**
 * Single entry point for flight data used by the UI: live AirLabs data or mock data according to
 * the Settings, plus the saved "My Flights" list. Callbacks always run on the main thread.
 */
public class FlightRepository {

    private static FlightRepository instance;

    private final AirLabsService airLabs = new AirLabsService(BuildConfig.AIRLABS_API_KEY);
    private final MockDataSource mock;
    private final SavedFlightsDb savedDb;
    private final Prefs prefs;
    private final AuthManager auth;

    private FlightRepository(Context context) {
        mock = MockDataSource.get(context);
        savedDb = SavedFlightsDb.get(context);
        prefs = new Prefs(context);
        auth = AuthManager.get(context);
    }

    public static synchronized FlightRepository get(Context context) {
        if (instance == null) {
            instance = new FlightRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void getFlight(String flightIata, Object tag, Callback<Result<Flight>> callback) {
        FallbackLoader.load(prefs.getDataMode(),
                cb -> airLabs.getFlight(flightIata, tag, cb),
                cb -> mock.getFlight(flightIata, cb),
                callback);
    }

    public void getSchedules(String airportIata, boolean departures, Object tag,
                             Callback<Result<List<Flight>>> callback) {
        FallbackLoader.load(prefs.getDataMode(),
                cb -> airLabs.getSchedules(airportIata, departures, tag, cb),
                cb -> mock.getSchedules(airportIata, departures, cb),
                callback);
    }

    public void cancel(Object tag) {
        ApiClient.get().cancel(tag);
    }

    // --- My Flights (SQLite, on a worker thread) ---

    public void getSavedFlights(Callback<List<Flight>> callback) {
        // Capture before queuing work, so an account change cannot redirect this operation.
        String owner = auth.ownerId();
        AppExecutors executors = AppExecutors.get();
        executors.io(() -> {
            List<Flight> flights = savedDb.getAll(owner);
            executors.main(() -> callback.onSuccess(flights));
        });
    }

    public void isSaved(String flightIata, Callback<Boolean> callback) {
        // Capture before queuing work, so an account change cannot redirect this operation.
        String owner = auth.ownerId();
        AppExecutors executors = AppExecutors.get();
        executors.io(() -> {
            boolean saved = savedDb.isSaved(owner, flightIata);
            executors.main(() -> callback.onSuccess(saved));
        });
    }

    /** Saves or removes a flight, then runs {@code onDone} on the main thread (may be null). */
    public void setSaved(Flight flight, boolean saved, Runnable onDone) {
        // Capture before queuing work, so an account change cannot redirect this operation.
        String owner = auth.ownerId();
        AppExecutors executors = AppExecutors.get();
        executors.io(() -> {
            if (saved) {
                savedDb.save(owner, flight);
            } else {
                savedDb.delete(owner, flight.getFlightIata());
            }
            if (onDone != null) {
                executors.main(onDone);
            }
        });
    }

    /** Stores refreshed data for a saved flight (no-op if it is not saved). */
    public void updateSaved(Flight flight) {
        String owner = auth.ownerId();
        AppExecutors.get().io(() -> savedDb.update(owner, flight));
    }
}
