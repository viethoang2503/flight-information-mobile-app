package vn.edu.usth.flightinfo.model;

/** Data plus the source it was loaded from. */
public class Result<T> {

    private final T data;
    private final DataSource source;

    public Result(T data, DataSource source) {
        this.data = data;
        this.source = source;
    }

    public T getData() {
        return data;
    }

    public DataSource getSource() {
        return source;
    }

    public boolean isMock() {
        return source == DataSource.MOCK;
    }
}
