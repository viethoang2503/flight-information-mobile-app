package vn.edu.usth.flightinfo.data.mock;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MockTimeShifterTest {

    @Test
    public void roundsOffsetToFiveMinutes() {
        assertEquals(0, MockTimeShifter.offsetSeconds(1000, 1000));
        assertEquals(300, MockTimeShifter.offsetSeconds(0, 299));
        assertEquals(86_400, MockTimeShifter.offsetSeconds(0, 86_400 + 100));
    }

    @Test
    public void shiftsStringsAndTimestampsTogether() throws Exception {
        JSONObject flight = new JSONObject()
                .put("flight_iata", "VN215")
                .put("dep_time", "2025-01-01 11:00")
                .put("dep_time_utc", "2025-01-01 04:00")
                .put("dep_time_ts", 1735704000L);

        JSONObject shifted = MockTimeShifter.shift(flight, 3600);

        assertEquals("2025-01-01 12:00", shifted.getString("dep_time"));
        assertEquals("2025-01-01 05:00", shifted.getString("dep_time_utc"));
        assertEquals(1735707600L, shifted.getLong("dep_time_ts"));
        assertEquals("VN215", shifted.getString("flight_iata"));
        assertFalse("absent fields stay absent", shifted.has("arr_actual"));
        assertEquals("original is untouched", "2025-01-01 11:00", flight.getString("dep_time"));
    }
}
