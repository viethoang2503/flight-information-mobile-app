package vn.edu.usth.flightinfo.data.remote;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.ParseError;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;

/**
 * The app's single Volley RequestQueue ("one per app") plus an ImageLoader with an in-memory
 * bitmap cache for airline logos. Initialized once from {@code FlightInfoApp.onCreate()}.
 */
public final class ApiClient {

    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_RETRIES = 1;

    private static ApiClient instance;

    private final RequestQueue requestQueue;
    private final ImageLoader imageLoader;

    private ApiClient(Context context) {
        requestQueue = Volley.newRequestQueue(context);
        imageLoader = new ImageLoader(requestQueue, new BitmapLruCache());
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
    }

    public static ApiClient get() {
        if (instance == null) {
            throw new IllegalStateException("ApiClient.init() was not called");
        }
        return instance;
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    /** Asynchronous GET; the callback runs on the main thread. */
    public void getString(String url, Object tag, Callback<String> callback) {
        Utf8StringRequest request = new Utf8StringRequest(url,
                callback::onSuccess,
                error -> callback.onError(toApiException(error)));
        request.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS, MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        if (tag != null) {
            request.setTag(tag);
        }
        requestQueue.add(request);
    }

    /** Cancels pending requests of a screen, e.g. from onStop(). */
    public void cancel(Object tag) {
        if (tag != null) {
            requestQueue.cancelAll(tag);
        }
    }

    static ApiException toApiException(VolleyError error) {
        if (error instanceof TimeoutError || error instanceof NetworkError) {
            return new ApiException(ApiException.Type.NETWORK, "Network error", error);
        }
        if (error instanceof AuthFailureError) {
            return new ApiException(ApiException.Type.NO_KEY, "Authentication failed", error);
        }
        if (error instanceof ParseError) {
            return new ApiException(ApiException.Type.PARSE, "Parse error", error);
        }
        int status = error.networkResponse != null ? error.networkResponse.statusCode : -1;
        switch (status) {
            case 401:
            case 403:
                return new ApiException(ApiException.Type.NO_KEY, "HTTP " + status, error);
            case 404:
                return new ApiException(ApiException.Type.NOT_FOUND, "HTTP 404", error);
            case 429:
                return new ApiException(ApiException.Type.QUOTA, "HTTP 429", error);
            default:
                return new ApiException(ApiException.Type.SERVER, "HTTP " + status, error);
        }
    }

    /** Memory cache for decoded logos, sized to 1/8 of the app's heap. */
    private static class BitmapLruCache extends LruCache<String, Bitmap> implements ImageLoader.ImageCache {

        BitmapLruCache() {
            super((int) (Runtime.getRuntime().maxMemory() / 1024 / 8));
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }

        @Override
        public Bitmap getBitmap(String url) {
            return get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            put(url, bitmap);
        }
    }
}
