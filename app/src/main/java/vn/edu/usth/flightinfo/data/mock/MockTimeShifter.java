package vn.edu.usth.flightinfo.data.mock;

import org.json.JSONException;
import org.json.JSONObject;

import vn.edu.usth.flightinfo.util.TimeUtils;

/**
 * Mock flights are written around a fixed "anchor" moment. Shifting every time by (now - anchor)
 * makes the demo data look current while keeping statuses consistent (an "active" flight is
 * always in the air, a "scheduled" one always ahead).
 */
public final class MockTimeShifter {

    private static final long ROUND_TO_SECONDS = 5 * 60;

    private static final String[] TIME_PREFIXES = {
            "dep_time", "dep_estimated", "dep_actual",
            "arr_time", "arr_estimated", "arr_actual"
    };

    private MockTimeShifter() {
    }

    /** Offset to add to mock times, rounded to 5 minutes so displayed times look tidy. */
    public static long offsetSeconds(long anchorTs, long nowTs) {
        return Math.round((nowTs - anchorTs) / (double) ROUND_TO_SECONDS) * ROUND_TO_SECONDS;
    }

    /** Returns a copy of {@code flight} with all time fields moved by {@code offsetSeconds}. */
    public static JSONObject shift(JSONObject flight, long offsetSeconds) throws JSONException {
        JSONObject copy = new JSONObject(flight.toString());
        for (String prefix : TIME_PREFIXES) {
            if (copy.has(prefix)) {
                copy.put(prefix, TimeUtils.shift(copy.getString(prefix), offsetSeconds));
            }
            if (copy.has(prefix + "_utc")) {
                copy.put(prefix + "_utc", TimeUtils.shift(copy.getString(prefix + "_utc"), offsetSeconds));
            }
            if (copy.has(prefix + "_ts")) {
                copy.put(prefix + "_ts", copy.getLong(prefix + "_ts") + offsetSeconds);
            }
        }
        return copy;
    }
}
