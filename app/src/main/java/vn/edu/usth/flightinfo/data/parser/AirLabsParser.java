package vn.edu.usth.flightinfo.data.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.model.Flight;

import static vn.edu.usth.flightinfo.data.parser.JsonUtils.optInteger;
import static vn.edu.usth.flightinfo.data.parser.JsonUtils.optLong;
import static vn.edu.usth.flightinfo.data.parser.JsonUtils.optString;

/**
 * Parses AirLabs v9 responses. The payload is usually wrapped as {"request":…,"response":…}
 * and errors come as {"error":{"message":…,"code":…}}; a bare array body is accepted too.
 */
public final class AirLabsParser {

    private AirLabsParser() {
    }

    /** /flight endpoint: a single flight. */
    public static Flight parseFlightResponse(String body) throws ApiException {
        Object payload = unwrap(body);
        JSONObject flightJson = null;
        if (payload instanceof JSONObject) {
            flightJson = (JSONObject) payload;
        } else if (payload instanceof JSONArray && ((JSONArray) payload).length() > 0) {
            flightJson = ((JSONArray) payload).optJSONObject(0);
        }
        if (flightJson == null || (optString(flightJson, "flight_iata") == null
                && optString(flightJson, "flight_icao") == null)) {
            throw new ApiException(ApiException.Type.NOT_FOUND, "Flight not found");
        }
        return parseFlight(flightJson);
    }

    /** /schedules endpoint: a list of flights (may be empty). */
    public static List<Flight> parseSchedulesResponse(String body) throws ApiException {
        Object payload = unwrap(body);
        List<Flight> flights = new ArrayList<>();
        if (payload instanceof JSONArray) {
            JSONArray array = (JSONArray) payload;
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    flights.add(parseFlight(item));
                }
            }
        }
        return flights;
    }

    public static Flight parseFlight(JSONObject o) {
        Flight f = new Flight();
        f.setFlightIata(optString(o, "flight_iata"));
        f.setFlightIcao(optString(o, "flight_icao"));
        f.setAirlineIata(optString(o, "airline_iata"));
        f.setAirlineIcao(optString(o, "airline_icao"));
        f.setRawStatus(optString(o, "status"));

        f.setDepIata(optString(o, "dep_iata"));
        f.setDepIcao(optString(o, "dep_icao"));
        f.setDepTerminal(optString(o, "dep_terminal"));
        f.setDepGate(optString(o, "dep_gate"));
        f.setDepTime(optString(o, "dep_time"));
        f.setDepTimeTs(optLong(o, "dep_time_ts"));
        f.setDepEstimated(optString(o, "dep_estimated"));
        f.setDepEstimatedTs(optLong(o, "dep_estimated_ts"));
        f.setDepActual(optString(o, "dep_actual"));
        f.setDepActualTs(optLong(o, "dep_actual_ts"));
        f.setDepDelayed(optInteger(o, "dep_delayed"));

        f.setArrIata(optString(o, "arr_iata"));
        f.setArrIcao(optString(o, "arr_icao"));
        f.setArrTerminal(optString(o, "arr_terminal"));
        f.setArrGate(optString(o, "arr_gate"));
        f.setArrBaggage(optString(o, "arr_baggage"));
        f.setArrTime(optString(o, "arr_time"));
        f.setArrTimeTs(optLong(o, "arr_time_ts"));
        f.setArrEstimated(optString(o, "arr_estimated"));
        f.setArrEstimatedTs(optLong(o, "arr_estimated_ts"));
        f.setArrActual(optString(o, "arr_actual"));
        f.setArrActualTs(optLong(o, "arr_actual_ts"));
        f.setArrDelayed(optInteger(o, "arr_delayed"));

        f.setDuration(optInteger(o, "duration"));
        f.setAircraftIcao(optString(o, "aircraft_icao"));
        f.setRegNumber(optString(o, "reg_number"));

        // Airline code is sometimes missing; derive it from the flight number ("VN215" -> "VN").
        if (f.getAirlineIata() == null && f.getFlightIata() != null && f.getFlightIata().length() > 2) {
            f.setAirlineIata(f.getFlightIata().substring(0, 2));
        }
        f.setRawJson(o.toString());
        return f;
    }

    /** Returns the "response" payload (JSONObject or JSONArray), throwing for AirLabs error bodies. */
    private static Object unwrap(String body) throws ApiException {
        if (body == null || body.trim().isEmpty()) {
            throw new ApiException(ApiException.Type.PARSE, "Empty response");
        }
        try {
            String trimmed = body.trim();
            if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed);
            }
            JSONObject root = new JSONObject(trimmed);
            JSONObject error = root.optJSONObject("error");
            if (error != null) {
                throw toApiException(optString(error, "code"), optString(error, "message"));
            }
            if (!root.has("response")) {
                return root;
            }
            return root.isNull("response") ? null : root.get("response");
        } catch (JSONException e) {
            throw new ApiException(ApiException.Type.PARSE, "Invalid JSON", e);
        }
    }

    static ApiException toApiException(String code, String message) {
        String c = code == null ? "" : code.toLowerCase();
        String text = message != null ? message : code;
        if (c.contains("limit") || c.contains("quota")) {
            return new ApiException(ApiException.Type.QUOTA, text);
        }
        if (c.contains("key") || c.contains("auth") || c.contains("permission")) {
            return new ApiException(ApiException.Type.NO_KEY, text);
        }
        if (c.contains("not_found") || c.contains("no_data")) {
            return new ApiException(ApiException.Type.NOT_FOUND, text);
        }
        return new ApiException(ApiException.Type.SERVER, text);
    }
}
