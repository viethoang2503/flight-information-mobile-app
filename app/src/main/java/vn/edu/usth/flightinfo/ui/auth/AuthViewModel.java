package vn.edu.usth.flightinfo.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.auth.AuthManager;

/** Retains an in-flight Task and transient form values across rotation, without retaining Views.
 * Passwords stay in memory only; they are never saved in a Bundle or preferences.
 */
public final class AuthViewModel extends ViewModel {
    enum Action { SIGN_IN, REGISTER, RESET_PASSWORD }

    static final class State {
        final boolean busy;
        final Action action;
        final ApiException error;

        State(boolean busy, Action action, ApiException error) {
            this.busy = busy;
            this.action = action;
            this.error = error;
        }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>(new State(false, null, null));
    String name = "";
    String email = "";
    String password = "";
    String confirmation = "";

    LiveData<State> state() {
        return state;
    }

    boolean isBusy() {
        State value = state.getValue();
        return value != null && value.busy;
    }

    void submit(AuthManager auth, Action action) {
        if (isBusy()) {
            return;
        }
        state.setValue(new State(true, action, null));
        if (action == Action.RESET_PASSWORD) {
            auth.resetPassword(email.trim(), new Callback<Void>() {
                @Override public void onSuccess(Void result) {
                    state.setValue(new State(false, action, null));
                }
                @Override public void onError(ApiException error) {
                    state.setValue(new State(false, action, error));
                }
            });
        } else {
            Callback<FirebaseUser> callback = new Callback<FirebaseUser>() {
                @Override public void onSuccess(FirebaseUser result) {
                    password = "";
                    confirmation = "";
                    state.setValue(new State(false, action, null));
                }
                @Override public void onError(ApiException error) {
                    state.setValue(new State(false, action, error));
                }
            };
            if (action == Action.REGISTER) {
                auth.register(name.trim(), email.trim(), password, callback);
            } else {
                auth.signIn(email.trim(), password, callback);
            }
        }
    }

    void consumeResult() {
        state.setValue(new State(false, null, null));
    }

    @Override protected void onCleared() {
        password = "";
        confirmation = "";
    }
}
