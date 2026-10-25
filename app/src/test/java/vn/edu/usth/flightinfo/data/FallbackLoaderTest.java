package vn.edu.usth.flightinfo.data;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import vn.edu.usth.flightinfo.model.DataSource;
import vn.edu.usth.flightinfo.model.Result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FallbackLoaderTest {

    private final AtomicInteger liveCalls = new AtomicInteger();
    private final AtomicInteger mockCalls = new AtomicInteger();
    private final AtomicReference<Result<String>> result = new AtomicReference<>();
    private final AtomicReference<ApiException> error = new AtomicReference<>();

    private final Callback<Result<String>> callback = new Callback<Result<String>>() {
        @Override
        public void onSuccess(Result<String> r) {
            result.set(r);
        }

        @Override
        public void onError(ApiException e) {
            error.set(e);
        }
    };

    private FallbackLoader.Source<String> liveOk() {
        return cb -> {
            liveCalls.incrementAndGet();
            cb.onSuccess("live");
        };
    }

    private FallbackLoader.Source<String> liveFails(ApiException.Type type) {
        return cb -> {
            liveCalls.incrementAndGet();
            cb.onError(new ApiException(type, "boom"));
        };
    }

    private FallbackLoader.Source<String> mock() {
        return cb -> {
            mockCalls.incrementAndGet();
            cb.onSuccess("mock");
        };
    }

    @Test
    public void mockModeNeverCallsLive() {
        FallbackLoader.load(DataMode.MOCK, liveOk(), mock(), callback);
        assertEquals(0, liveCalls.get());
        assertEquals(DataSource.MOCK, result.get().getSource());
    }

    @Test
    public void autoModeUsesLiveWhenItWorks() {
        FallbackLoader.load(DataMode.AUTO, liveOk(), mock(), callback);
        assertEquals("live", result.get().getData());
        assertEquals(DataSource.LIVE, result.get().getSource());
        assertEquals(0, mockCalls.get());
    }

    @Test
    public void autoModeFallsBackOnFailures() {
        for (ApiException.Type type : new ApiException.Type[]{ApiException.Type.NETWORK,
                ApiException.Type.NO_KEY, ApiException.Type.QUOTA, ApiException.Type.SERVER,
                ApiException.Type.PARSE}) {
            result.set(null);
            FallbackLoader.load(DataMode.AUTO, liveFails(type), mock(), callback);
            assertEquals(type.name(), DataSource.MOCK, result.get().getSource());
        }
        assertNull(error.get());
    }

    @Test
    public void autoModeDoesNotHideNotFound() {
        FallbackLoader.load(DataMode.AUTO, liveFails(ApiException.Type.NOT_FOUND), mock(), callback);
        assertEquals(ApiException.Type.NOT_FOUND, error.get().getType());
        assertEquals(0, mockCalls.get());
    }

    @Test
    public void liveModeReportsErrors() {
        FallbackLoader.load(DataMode.LIVE, liveFails(ApiException.Type.QUOTA), mock(), callback);
        assertEquals(ApiException.Type.QUOTA, error.get().getType());
        assertEquals(0, mockCalls.get());
    }

    @Test
    public void dataModeFromPreference() {
        assertEquals(DataMode.LIVE, DataMode.fromPreference("live"));
        assertEquals(DataMode.MOCK, DataMode.fromPreference("mock"));
        assertEquals(DataMode.AUTO, DataMode.fromPreference(null));
    }
}
