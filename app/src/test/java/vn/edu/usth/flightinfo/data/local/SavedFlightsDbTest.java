package vn.edu.usth.flightinfo.data.local;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import vn.edu.usth.flightinfo.data.parser.AirLabsParser;
import vn.edu.usth.flightinfo.model.Flight;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class SavedFlightsDbTest {
    private Context context;
    private SavedFlightsDb helper;

    @Before public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        // Each Robolectric test owns a different application/database directory.
        java.lang.reflect.Constructor<SavedFlightsDb> constructor =
                SavedFlightsDb.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        helper = constructor.newInstance(context);
        helper.close();
        context.deleteDatabase("flight_info.db");
    }

    @After public void tearDown() {
        helper.close();
        context.deleteDatabase("flight_info.db");
    }

    @Test public void migrationPreservesOldJsonAndSaveTimeForGuestOnly() throws Exception {
        String json = flight("VN215", "11").getRawJson();
        try (SQLiteDatabase old = context.openOrCreateDatabase("flight_info.db", 0, null)) {
            old.execSQL("CREATE TABLE saved_flights (flight_iata TEXT PRIMARY KEY, "
                    + "json TEXT NOT NULL, saved_at INTEGER NOT NULL)");
            old.execSQL("INSERT INTO saved_flights VALUES (?, ?, ?)", new Object[]{"VN215", json, 1234L});
            old.setVersion(1);
        }
        assertEquals(1, helper.getAll("guest").size());
        assertEquals(json, helper.getAll("guest").get(0).getRawJson());
        assertTrue(helper.getAll("user-a").isEmpty());
        assertEquals(2, helper.getReadableDatabase().getVersion());
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT owner, saved_at FROM saved_flights", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("guest", c.getString(0));
            assertEquals(1234L, c.getLong(1));
        }
        helper.close();
        assertTrue(helper.isSaved("guest", "VN215"));
    }

    @Test public void migrationAlsoRetainsLegacyRowsWithoutAnIata() throws Exception {
        // The v1 TEXT PRIMARY KEY allowed NULL, for example an ICAO-only API response.
        try (SQLiteDatabase old = context.openOrCreateDatabase("flight_info.db", 0, null)) {
            old.execSQL("CREATE TABLE saved_flights (flight_iata TEXT PRIMARY KEY, "
                    + "json TEXT NOT NULL, saved_at INTEGER NOT NULL)");
            old.execSQL("INSERT INTO saved_flights VALUES (NULL, ?, ?)",
                    new Object[]{"{\"flight_icao\":\"HVN215\"}", 1234L});
            old.setVersion(1);
        }
        assertEquals(1, helper.getAll("guest").size());
        assertEquals("HVN215", helper.getAll("guest").get(0).getFlightIcao());
        assertTrue(helper.getAll("user-a").isEmpty());
    }

    @Test public void sameFlightIsIndependentForTwoUsersAndGuest() throws Exception {
        helper.save("guest", flight("VN215", "1"));
        helper.save("user-a", flight("VN215", "2"));
        helper.save("user-b", flight("VN215", "3"));
        assertEquals("1", helper.getAll("guest").get(0).getDepGate());
        assertEquals("2", helper.getAll("user-a").get(0).getDepGate());
        assertEquals("3", helper.getAll("user-b").get(0).getDepGate());
        helper.update("user-a", flight("VN215", "20"));
        helper.delete("user-b", "VN215");
        assertEquals("20", helper.getAll("user-a").get(0).getDepGate());
        assertEquals("1", helper.getAll("guest").get(0).getDepGate());
        assertFalse(helper.isSaved("user-b", "VN215"));
        assertTrue(helper.isSaved("user-a", "VN215"));
        helper.close();
        assertEquals("20", helper.getAll("user-a").get(0).getDepGate());
    }

    @Test public void refreshDoesNotCreateAnUnsavedFlightOrChangeItsPosition() throws Exception {
        helper.save("user-a", flight("VN215", "1"));
        helper.getWritableDatabase().execSQL("UPDATE saved_flights SET saved_at = 100");
        helper.save("user-a", flight("VJ123", "2"));
        helper.update("user-a", flight("VN215", "9"));
        helper.update("user-b", flight("VN215", "99"));
        assertEquals("VJ123", helper.getAll("user-a").get(0).getFlightIata());
        assertEquals("9", helper.getAll("user-a").get(1).getDepGate());
        assertTrue(helper.getAll("user-b").isEmpty());
    }

    @Test public void ownerIsBoundAsAValue() throws Exception {
        helper.save("guest", flight("VN215", "1"));
        String owner = "x' OR 1=1 --";
        helper.save(owner, flight("VJ123", "2"));
        helper.delete(owner, "VN215");
        assertTrue(helper.isSaved("guest", "VN215"));
        assertEquals(1, helper.getAll(owner).size());
        assertEquals("VJ123", helper.getAll(owner).get(0).getFlightIata());
    }

    private static Flight flight(String iata, String gate) throws Exception {
        return AirLabsParser.parseFlight(new JSONObject()
                .put("flight_iata", iata).put("dep_iata", "HAN")
                .put("arr_iata", "SGN").put("dep_gate", gate));
    }
}
