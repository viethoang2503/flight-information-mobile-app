package vn.edu.usth.flightinfo.ui.auth;

import static org.junit.Assert.assertEquals;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.auth.AuthException;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class AuthErrorMessagesTest {
    @Test public void specificWeakPasswordErrorIsNotHiddenBySuperclass() {
        assertEquals(R.string.auth_error_weak_password, message(
                new FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "weak", "too short")));
    }

    @Test public void knownFirebaseErrorsHaveLocalizedResources() {
        assertEquals(R.string.auth_error_credentials, message(
                new FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "invalid")));
        assertEquals(R.string.auth_error_credentials, message(
                new FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "missing")));
        assertEquals(R.string.auth_error_email_in_use, message(
                new FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "used")));
        assertEquals(R.string.auth_error_network, message(new FirebaseNetworkException("offline")));
        assertEquals(R.string.auth_error_too_many_requests, message(
                new FirebaseTooManyRequestsException("slow down")));
        assertEquals(R.string.error_generic, message(new IllegalStateException("unexpected")));
    }

    @Test public void missingConfigAndPartialRegistrationAreActionable() {
        assertEquals(R.string.auth_error_not_configured, AuthErrorMessages.of(
                new AuthException(AuthException.Reason.NOT_CONFIGURED)));
        assertEquals(R.string.auth_error_profile, AuthErrorMessages.of(
                new AuthException(AuthException.Reason.PROFILE_UPDATE)));
    }

    private static int message(Exception cause) {
        return AuthErrorMessages.of(new ApiException(ApiException.Type.SERVER, "auth", cause));
    }
}
