package vn.edu.usth.flightinfo.data.remote;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import java.nio.charset.StandardCharsets;

/**
 * StringRequest that always decodes the body as UTF-8. Plain StringRequest falls back to
 * ISO-8859-1 when the server omits the charset, which garbles non-ASCII text.
 */
public class Utf8StringRequest extends StringRequest {

    public Utf8StringRequest(String url, Response.Listener<String> listener,
                             Response.ErrorListener errorListener) {
        super(Method.GET, url, listener, errorListener);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String body = new String(response.data, StandardCharsets.UTF_8);
        return Response.success(body, HttpHeaderParser.parseCacheHeaders(response));
    }
}
