package vn.edu.usth.flightinfo.data.auth;

import vn.edu.usth.flightinfo.data.ApiException;

/** Auth-specific failures fit the app's existing Callback contract. */
public final class AuthException extends ApiException {
    public enum Reason { NOT_CONFIGURED, BUSY, PROFILE_UPDATE }

    private final Reason reason;

    public AuthException(Reason reason) {
        super(Type.SERVER, reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
