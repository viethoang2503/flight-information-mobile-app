package vn.edu.usth.flightinfo.data.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;

/** Firebase owns credentials/session persistence; preferences only store guest/profile state.
 * All entry points and Firebase completion callbacks run on the main thread.
 */
public final class AuthManager {
    public static final String GUEST_OWNER = "guest";
    private static final String KEY_GUEST = "guest";
    private static final String PROFILE_PREFIX = "pending_profile_";
    private static AuthManager instance;

    private final SharedPreferences prefs;
    @Nullable private final FirebaseAuth firebase;
    private boolean busy;

    private AuthManager(Context context) {
        prefs = context.getSharedPreferences("flight_auth", Context.MODE_PRIVATE);
        FirebaseApp app = FirebaseApp.initializeApp(context);
        firebase = app == null ? null : FirebaseAuth.getInstance(app);
    }

    public static synchronized AuthManager get(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
        }
        return instance;
    }

    @Nullable
    public FirebaseUser currentUser() {
        return firebase == null ? null : firebase.getCurrentUser();
    }

    public boolean isConfigured() {
        return firebase != null;
    }

    public boolean isGuest() {
        return currentUser() == null && prefs.getBoolean(KEY_GUEST, false);
    }

    public boolean hasSession() {
        FirebaseUser user = currentUser();
        // If creation succeeded but profile update was interrupted, login retries that update.
        return isGuest() || (user != null && !prefs.contains(PROFILE_PREFIX + user.getUid()));
    }

    public String ownerId() {
        FirebaseUser user = currentUser();
        return user == null ? GUEST_OWNER : user.getUid();
    }

    public void signIn(String email, String password, Callback<FirebaseUser> callback) {
        if (!begin(callback)) {
            return;
        }
        firebase.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                fail(callback, task.getException());
                return;
            }
            completeProfile(firebase.getCurrentUser(), callback);
        });
    }

    public void register(String name, String email, String password, Callback<FirebaseUser> callback) {
        if (!begin(callback)) {
            return;
        }
        firebase.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                fail(callback, task.getException());
                return;
            }
            FirebaseUser user = firebase.getCurrentUser();
            if (user == null) {
                fail(callback, null);
                return;
            }
            prefs.edit().putString(PROFILE_PREFIX + user.getUid(), name).apply();
            completeProfile(user, callback);
        });
    }

    private void completeProfile(@Nullable FirebaseUser user, Callback<FirebaseUser> callback) {
        if (user == null) {
            fail(callback, null);
            return;
        }
        String name = prefs.getString(PROFILE_PREFIX + user.getUid(), null);
        if (name == null) {
            signedIn(user, callback);
            return;
        }
        UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                .setDisplayName(name).build();
        user.updateProfile(profile).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                prefs.edit().remove(PROFILE_PREFIX + user.getUid()).apply();
                signedIn(user, callback);
            } else {
                // The account already exists: do not ask the user to create it a second time.
                // Keep the pending name so their next sign-in can finish the profile.
                firebase.signOut();
                busy = false;
                callback.onError(new AuthException(AuthException.Reason.PROFILE_UPDATE));
            }
        });
    }

    private void signedIn(FirebaseUser user, Callback<FirebaseUser> callback) {
        prefs.edit().putBoolean(KEY_GUEST, false).apply();
        busy = false;
        callback.onSuccess(user);
    }

    public void resetPassword(String email, Callback<Void> callback) {
        if (!begin(callback)) {
            return;
        }
        firebase.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                busy = false;
                callback.onSuccess(null);
            } else {
                fail(callback, task.getException());
            }
        });
    }

    public void signOut() {
        if (busy) {
            return;
        }
        if (firebase != null) {
            firebase.signOut();
        }
        prefs.edit().putBoolean(KEY_GUEST, false).apply();
    }

    public void continueAsGuest() {
        if (busy) {
            return;
        }
        if (firebase != null) {
            firebase.signOut();
        }
        prefs.edit().putBoolean(KEY_GUEST, true).apply();
    }

    private boolean begin(Callback<?> callback) {
        if (busy) {
            callback.onError(new AuthException(AuthException.Reason.BUSY));
            return false;
        }
        if (firebase == null) {
            callback.onError(new AuthException(AuthException.Reason.NOT_CONFIGURED));
            return false;
        }
        busy = true;
        return true;
    }

    private void fail(Callback<?> callback, @Nullable Exception cause) {
        busy = false;
        callback.onError(new ApiException(ApiException.Type.SERVER, "Authentication failed", cause));
    }
}
