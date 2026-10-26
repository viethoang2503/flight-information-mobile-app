package vn.edu.usth.flightinfo.ui.auth;

import androidx.annotation.StringRes;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.auth.AuthException;

/** Resource IDs keep Firebase errors localized and never expose raw server messages. */
public final class AuthErrorMessages {
    private AuthErrorMessages() { }

    @StringRes
    public static int of(ApiException error) {
        if (error instanceof AuthException) {
            switch (((AuthException) error).getReason()) {
                case NOT_CONFIGURED: return R.string.auth_error_not_configured;
                case BUSY: return R.string.auth_error_busy;
                case PROFILE_UPDATE: return R.string.auth_error_profile;
            }
        }
        Throwable cause = error.getCause();
        // WeakPassword is a subclass of InvalidCredentials: test the specific type first.
        if (cause instanceof FirebaseAuthWeakPasswordException) {
            return R.string.auth_error_weak_password;
        }
        if (cause instanceof FirebaseAuthUserCollisionException) {
            return R.string.auth_error_email_in_use;
        }
        if (cause instanceof FirebaseAuthInvalidCredentialsException
                || cause instanceof FirebaseAuthInvalidUserException) {
            return R.string.auth_error_credentials;
        }
        if (cause instanceof FirebaseNetworkException) {
            return R.string.auth_error_network;
        }
        if (cause instanceof FirebaseTooManyRequestsException) {
            return R.string.auth_error_too_many_requests;
        }
        return R.string.error_generic;
    }
}
