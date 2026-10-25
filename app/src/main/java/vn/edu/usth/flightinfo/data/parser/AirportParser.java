package vn.edu.usth.flightinfo.data.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.model.Airport;

/** Parses assets/airports.json. */
public final class AirportParser {

    private AirportParser() {
    }

    public static List<Airport> parse(String json) throws JSONException {
        JSONArray array = new JSONArray(json);
        List<Airport> airports = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            airports.add(new Airport(
                    o.getString("iata"),
                    JsonUtils.optString(o, "icao"),
                    o.getString("name"),
                    o.getString("city"),
                    JsonUtils.optString(o, "city_vi"),
                    o.getString("country"),
                    o.getDouble("lat"),
                    o.getDouble("lon")));
        }
        return airports;
    }
}
