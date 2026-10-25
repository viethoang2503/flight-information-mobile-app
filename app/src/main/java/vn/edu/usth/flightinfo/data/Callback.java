package vn.edu.usth.flightinfo.data;

/** Async result callback. Data-layer classes always invoke it on the main thread. */
public interface Callback<T> {

    void onSuccess(T result);

    void onError(ApiException error);
}
