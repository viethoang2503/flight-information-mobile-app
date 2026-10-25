package vn.edu.usth.flightinfo.data;

/** A failed data request, classified so the repository can decide whether to fall back to mock data. */
public class ApiException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum Type {
        /** No connection or timeout. */
        NETWORK,
        /** Missing or rejected API key. */
        NO_KEY,
        /** Rate limit / monthly quota exhausted. */
        QUOTA,
        /** The request worked but there is no such flight / station. */
        NOT_FOUND,
        /** The response could not be parsed. */
        PARSE,
        /** Any other server-side error. */
        SERVER
    }

    private final Type type;

    public ApiException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public ApiException(Type type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
