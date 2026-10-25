package vn.edu.usth.flightinfo.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Worker threads + a main-thread Handler (the Thread/Handler model from the "Background Tasks" lecture,
 * used instead of the deprecated AsyncTask). Disk work runs on {@link #io}, results go back via {@link #main}.
 */
public final class AppExecutors {

    private static AppExecutors instance;

    private final ExecutorService io = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static synchronized AppExecutors get() {
        if (instance == null) {
            instance = new AppExecutors();
        }
        return instance;
    }

    public void io(Runnable task) {
        io.execute(task);
    }

    public void main(Runnable task) {
        mainHandler.post(task);
    }
}
