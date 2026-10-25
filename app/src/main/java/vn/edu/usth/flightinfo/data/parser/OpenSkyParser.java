package vn.edu.usth.flightinfo.data.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.model.AircraftState;

/**
 * Parses OpenSky /states/all. Each state is a positional array:
 * [0 icao24, 1 callsign, 2 origin_country, 3 time_position, 4 last_contact, 5 longitude,
 *  6 latitude, 7 baro_altitude, 8 on_ground, 9 velocity, 10 true_track, 11 vertical_rate, …]
 */
public final class OpenSkyParser {

    private static final int ICAO24 = 0;
    private static final int CALLSIGN = 1;
    private static final int ORIGIN_COUNTRY = 2;
    private static final int LONGITUDE = 5;
    private static final int LATITUDE = 6;
    private static final int BARO_ALTITUDE = 7;
    private static final int ON_GROUND = 8;
    private static final int VELOCITY = 9;
    private static final int TRUE_TRACK = 10;
    private static final int VERTICAL_RATE = 11;

    private OpenSkyParser() {
    }

    public static List<AircraftState> parse(String body) throws ApiException {
        List<AircraftState> result = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(body);
            JSONArray states = root.optJSONArray("states");
            if (states == null) {
                return result; // "states": null means no aircraft in the box
            }
            for (int i = 0; i < states.length(); i++) {
                JSONArray s = states.optJSONArray(i);
                Double lat = JsonUtils.optDouble(s, LATITUDE);
                Double lon = JsonUtils.optDouble(s, LONGITUDE);
                if (s == null || lat == null || lon == null) {
                    continue; // no position, nothing to draw
                }
                result.add(new AircraftState(
                        JsonUtils.optString(s, ICAO24),
                        JsonUtils.optString(s, CALLSIGN),
                        JsonUtils.optString(s, ORIGIN_COUNTRY),
                        lat,
                        lon,
                        JsonUtils.optDouble(s, BARO_ALTITUDE),
                        s.optBoolean(ON_GROUND, false),
                        JsonUtils.optDouble(s, VELOCITY),
                        JsonUtils.optDouble(s, TRUE_TRACK),
                        JsonUtils.optDouble(s, VERTICAL_RATE)));
            }
            return result;
        } catch (JSONException | NullPointerException e) {
            throw new ApiException(ApiException.Type.PARSE, "Invalid OpenSky response", e);
        }
    }
}
