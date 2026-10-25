package vn.edu.usth.flightinfo.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.data.parser.AirLabsParser;
import vn.edu.usth.flightinfo.model.Flight;

/** The last {@link #MAX_ITEMS} searched flights, newest first, kept in SharedPreferences. */
public class RecentSearches {

    public static final int MAX_ITEMS = 10;

    private static final String PREFS_NAME = "recent_searches";
    private static final String KEY_ITEMS = "items";

    private final SharedPreferences prefs;

    public RecentSearches(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void add(Flight flight) {
        if (flight.getFlightIata() == null || flight.getRawJson() == null) {
            return;
        }
        JSONArray updated = new JSONArray();
        updated.put(flight.getRawJson());
        for (Flight f : getAll()) {
            if (updated.length() >= MAX_ITEMS) {
                break;
            }
            if (!flight.getFlightIata().equals(f.getFlightIata())) {
                updated.put(f.getRawJson());
            }
        }
        prefs.edit().putString(KEY_ITEMS, updated.toString()).apply();
    }

    public List<Flight> getAll() {
        List<Flight> flights = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_ITEMS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                flights.add(AirLabsParser.parseFlight(new JSONObject(array.getString(i))));
            }
        } catch (JSONException ignored) {
            // Corrupted history: start fresh.
        }
        return flights;
    }

    public void clear() {
        prefs.edit().remove(KEY_ITEMS).apply();
    }
}
