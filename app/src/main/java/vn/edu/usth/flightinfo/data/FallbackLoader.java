package vn.edu.usth.flightinfo.data;

import vn.edu.usth.flightinfo.model.DataSource;
import vn.edu.usth.flightinfo.model.Result;

/**
 * Picks live or mock data according to the {@link DataMode}. In AUTO mode a failed live request
 * (no key, quota, network, server, bad JSON) is retried against mock data; "not found" is a real
 * answer and is passed through as an error.
 */
public final class FallbackLoader {

    /** One way of loading T (a live API call or a mock lookup). */
    public interface Source<T> {
        void load(Callback<T> callback);
    }

    private FallbackLoader() {
    }

    public static <T> void load(DataMode mode, Source<T> live, Source<T> mock, Callback<Result<T>> callback) {
        if (mode == DataMode.MOCK) {
            mock.load(wrap(DataSource.MOCK, callback));
            return;
        }
        live.load(new Callback<T>() {
            @Override
            public void onSuccess(T result) {
                callback.onSuccess(new Result<>(result, DataSource.LIVE));
            }

            @Override
            public void onError(ApiException error) {
                if (mode == DataMode.AUTO && shouldFallBack(error)) {
                    mock.load(wrap(DataSource.MOCK, callback));
                } else {
                    callback.onError(error);
                }
            }
        });
    }

    static boolean shouldFallBack(ApiException error) {
        return error.getType() != ApiException.Type.NOT_FOUND;
    }

    private static <T> Callback<T> wrap(DataSource source, Callback<Result<T>> callback) {
        return new Callback<T>() {
            @Override
            public void onSuccess(T result) {
                callback.onSuccess(new Result<>(result, source));
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        };
    }
}
