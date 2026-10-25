package vn.edu.usth.flightinfo.data.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.model.Weather;

import static vn.edu.usth.flightinfo.data.parser.JsonUtils.optDouble;
import static vn.edu.usth.flightinfo.data.parser.JsonUtils.optInteger;
import static vn.edu.usth.flightinfo.data.parser.JsonUtils.optString;

/** Parses aviationweather.gov /api/data/metar?format=json (a JSON array; empty body = no report). */
public final class MetarParser {

    private MetarParser() {
    }

    public static List<Weather> parse(String body) throws ApiException {
        List<Weather> result = new ArrayList<>();
        if (body == null || body.trim().isEmpty()) {
            return result;
        }
        try {
            JSONArray array = new JSONArray(body.trim());
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o != null) {
                    result.add(parseOne(o));
                }
            }
            return result;
        } catch (JSONException e) {
            throw new ApiException(ApiException.Type.PARSE, "Invalid METAR response", e);
        }
    }

    static Weather parseOne(JSONObject o) {
        // "wdir" is a number of degrees, or the text "VRB" for variable wind -> null.
        return new Weather(
                optString(o, "icaoId"),
                optString(o, "name"),
                optDouble(o, "temp"),
                optDouble(o, "dewp"),
                optInteger(o, "wdir"),
                optInteger(o, "wspd"),
                optInteger(o, "wgst"),
                optString(o, "visib"),
                optString(o, "fltCat"),
                optString(o, "rawOb"));
    }
}
