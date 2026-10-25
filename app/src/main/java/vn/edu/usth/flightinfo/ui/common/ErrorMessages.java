package vn.edu.usth.flightinfo.ui.common;

import androidx.annotation.StringRes;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;

/** User-facing message for each kind of data error. */
public final class ErrorMessages {

    private ErrorMessages() {
    }

    @StringRes
    public static int of(ApiException error) {
        switch (error.getType()) {
            case NETWORK: return R.string.error_network;
            case NO_KEY: return R.string.error_no_key;
            case QUOTA: return R.string.error_quota;
            case NOT_FOUND: return R.string.error_not_found;
            default: return R.string.error_generic;
        }
    }
}
