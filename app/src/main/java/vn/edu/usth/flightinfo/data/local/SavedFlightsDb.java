package vn.edu.usth.flightinfo.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.data.parser.AirLabsParser;
import vn.edu.usth.flightinfo.model.Flight;

/**
 * SQLite storage for "My Flights". Each row keeps the flight's original JSON so it can be
 * shown offline and refreshed later. All methods do disk I/O: call them from a worker thread.
 */
public class SavedFlightsDb extends SQLiteOpenHelper {

    private static final String DB_NAME = "flight_info.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE = "saved_flights";
    private static final String COL_IATA = "flight_iata";
    private static final String COL_JSON = "json";
    private static final String COL_SAVED_AT = "saved_at";

    private static SavedFlightsDb instance;

    private SavedFlightsDb(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    public static synchronized SavedFlightsDb get(Context context) {
        if (instance == null) {
            instance = new SavedFlightsDb(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_IATA + " TEXT PRIMARY KEY, "
                + COL_JSON + " TEXT NOT NULL, "
                + COL_SAVED_AT + " INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    /** Saves (or re-saves) a flight; it moves to the top of the list. */
    public void save(Flight flight) {
        ContentValues values = new ContentValues();
        values.put(COL_IATA, flight.getFlightIata());
        values.put(COL_JSON, flight.getRawJson());
        values.put(COL_SAVED_AT, System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Stores fresher data for an already saved flight without changing its position. */
    public void update(Flight flight) {
        ContentValues values = new ContentValues();
        values.put(COL_JSON, flight.getRawJson());
        getWritableDatabase().update(TABLE, values, COL_IATA + " = ?", new String[]{flight.getFlightIata()});
    }

    public void delete(String flightIata) {
        getWritableDatabase().delete(TABLE, COL_IATA + " = ?", new String[]{flightIata});
    }

    public boolean isSaved(String flightIata) {
        try (Cursor c = getReadableDatabase().query(TABLE, new String[]{COL_IATA},
                COL_IATA + " = ?", new String[]{flightIata}, null, null, null)) {
            return c.moveToFirst();
        }
    }

    /** All saved flights, most recently saved first. */
    public List<Flight> getAll() {
        List<Flight> flights = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE, new String[]{COL_JSON},
                null, null, null, null, COL_SAVED_AT + " DESC")) {
            while (c.moveToNext()) {
                try {
                    flights.add(AirLabsParser.parseFlight(new JSONObject(c.getString(0))));
                } catch (JSONException ignored) {
                    // Skip a corrupted row rather than failing the whole list.
                }
            }
        }
        return flights;
    }
}
