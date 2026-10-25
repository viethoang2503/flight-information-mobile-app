package vn.edu.usth.flightinfo.ui.auth;

import static org.junit.Assert.*;

import android.content.Intent;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.auth.AuthManager;
import vn.edu.usth.flightinfo.ui.MainActivity;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class AuthScreensTest {
    @Before public void signOut() {
        AuthManager.get(RuntimeEnvironment.getApplication()).signOut();
    }

    @Test public void loginValidatesEmailAndPasswordBeforeNetwork() {
        try (ActivityController<LoginActivity> controller = Robolectric.buildActivity(LoginActivity.class).setup()) {
            LoginActivity activity = controller.get();
            activity.findViewById(R.id.auth_submit).performClick();
            assertError(activity, R.id.auth_email_layout, R.string.auth_validation_email);
            ((EditText) activity.findViewById(R.id.auth_email)).setText("student@example.com");
            ((EditText) activity.findViewById(R.id.auth_password)).setText("123");
            activity.findViewById(R.id.auth_submit).performClick();
            assertError(activity, R.id.auth_password_layout, R.string.auth_validation_password);
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }

    @Test public void resetNeedsOnlyAValidEmail() {
        try (ActivityController<LoginActivity> controller = Robolectric.buildActivity(LoginActivity.class).setup()) {
            LoginActivity activity = controller.get();
            activity.findViewById(R.id.auth_reset).performClick();
            assertError(activity, R.id.auth_email_layout, R.string.auth_validation_email);
            ((EditText) activity.findViewById(R.id.auth_email)).setText("student@example.com");
            activity.findViewById(R.id.auth_reset).performClick();
            assertNull(((TextInputLayout) activity.findViewById(R.id.auth_password_layout)).getError());
        }
    }

    @Test public void registrationValidatesNameAndMatchingPasswords() {
        try (ActivityController<RegisterActivity> controller = Robolectric.buildActivity(RegisterActivity.class).setup()) {
            RegisterActivity activity = controller.get();
            activity.findViewById(R.id.auth_submit).performClick();
            assertError(activity, R.id.auth_name_layout, R.string.auth_validation_name);
            ((EditText) activity.findViewById(R.id.auth_name)).setText("Quoc Anh");
            ((EditText) activity.findViewById(R.id.auth_email)).setText("student@example.com");
            ((EditText) activity.findViewById(R.id.auth_password)).setText("abcdef");
            ((EditText) activity.findViewById(R.id.auth_confirmation)).setText("abcdeg");
            activity.findViewById(R.id.auth_submit).performClick();
            assertError(activity, R.id.auth_confirmation_layout, R.string.auth_validation_confirmation);
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }

    @Test public void guestCanEnterMainAndBackCannotReturnToLogin() {
        try (ActivityController<LoginActivity> controller = Robolectric.buildActivity(LoginActivity.class).setup()) {
            LoginActivity activity = controller.get();
            activity.findViewById(R.id.auth_guest).performClick();
            AuthManager auth = AuthManager.get(activity);
            assertTrue(auth.isGuest());
            assertTrue(auth.hasSession());
            assertEquals("guest", auth.ownerId());
            Intent next = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(MainActivity.class.getName(), next.getComponent().getClassName());
            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK, next.getFlags());
            assertTrue(activity.isFinishing());
            auth.signOut();
            assertFalse(auth.hasSession());
        }
    }

    @Test public void rotationKeepsFormValues() {
        try (ActivityController<RegisterActivity> controller = Robolectric.buildActivity(RegisterActivity.class).setup()) {
            RegisterActivity activity = controller.get();
            ((EditText) activity.findViewById(R.id.auth_name)).setText("Quoc Anh");
            ((EditText) activity.findViewById(R.id.auth_email)).setText("student@example.com");
            ((EditText) activity.findViewById(R.id.auth_password)).setText("abcdef");
            ((EditText) activity.findViewById(R.id.auth_confirmation)).setText("abcdef");
            controller.recreate();
            RegisterActivity recreated = controller.get();
            assertEquals("Quoc Anh", ((EditText) recreated.findViewById(R.id.auth_name)).getText().toString());
            assertEquals("student@example.com", ((EditText) recreated.findViewById(R.id.auth_email)).getText().toString());
            assertEquals("abcdef", ((EditText) recreated.findViewById(R.id.auth_password)).getText().toString());
            assertEquals("abcdef", ((EditText) recreated.findViewById(R.id.auth_confirmation)).getText().toString());
        }
    }

    private static void assertError(android.app.Activity activity, int layout, int message) {
        assertEquals(activity.getString(message),
                ((TextInputLayout) activity.findViewById(layout)).getError().toString());
    }
}
