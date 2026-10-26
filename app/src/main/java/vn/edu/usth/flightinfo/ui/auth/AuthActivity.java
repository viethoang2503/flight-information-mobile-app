package vn.edu.usth.flightinfo.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.function.Consumer;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.auth.AuthManager;

/** Shared form feedback; retained state observes only the currently started Activity. */
abstract class AuthActivity extends AppCompatActivity {
    protected AuthManager auth;
    protected AuthViewModel model;
    protected TextInputLayout emailLayout;
    protected TextInputLayout passwordLayout;
    private View progress;
    private View form;
    private OnBackPressedCallback blockBack;

    @LayoutRes
    protected abstract int layoutResource();

    @Override
    protected void onCreate(@Nullable Bundle savedState) {
        super.onCreate(savedState);
        setContentView(layoutResource());
        ViewCompat.setAccessibilityHeading(findViewById(R.id.auth_title), true);
        auth = AuthManager.get(this);
        model = new ViewModelProvider(this).get(AuthViewModel.class);
        emailLayout = findViewById(R.id.auth_email_layout);
        passwordLayout = findViewById(R.id.auth_password_layout);
        progress = findViewById(R.id.auth_progress);
        form = findViewById(R.id.auth_form);
        bind(R.id.auth_email, model.email, value -> model.email = value, emailLayout);
        bind(R.id.auth_password, model.password, value -> model.password = value, passwordLayout);
        TextView configNotice = findViewById(R.id.auth_config_notice);
        configNotice.setVisibility(auth.isConfigured() ? View.GONE : View.VISIBLE);
        blockBack = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() {
                // Finish the current request before navigating away or changing identity.
            }
        };
        getOnBackPressedDispatcher().addCallback(this, blockBack);
    }

    protected void observeForm() {
        model.state().observe(this, state -> {
            setEnabled(form, !state.busy);
            progress.setVisibility(state.busy ? View.VISIBLE : View.INVISIBLE);
            blockBack.setEnabled(state.busy);
            if (state.busy || state.action == null) {
                return;
            }
            model.consumeResult();
            if (state.error != null) {
                showMessage(AuthErrorMessages.of(state.error));
            } else if (state.action == AuthViewModel.Action.RESET_PASSWORD) {
                showMessage(R.string.auth_reset_sent);
            } else {
                AuthNavigation.openMain(this);
            }
        });
    }

    protected void bind(int id, String initial, Consumer<String> update, TextInputLayout layout) {
        EditText input = findViewById(id);
        input.setText(initial);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                update.accept(s.toString());
                layout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    protected boolean validateEmail() {
        emailLayout.setError(null);
        if (!Patterns.EMAIL_ADDRESS.matcher(model.email.trim()).matches()) {
            emailLayout.setError(getString(R.string.auth_validation_email));
            emailLayout.getEditText().requestFocus();
            return false;
        }
        return true;
    }

    protected boolean validatePassword() {
        passwordLayout.setError(null);
        if (model.password.length() < 6) {
            passwordLayout.setError(getString(R.string.auth_validation_password));
            passwordLayout.getEditText().requestFocus();
            return false;
        }
        return true;
    }

    protected void onKeyboardDone(int fieldId, Runnable action) {
        ((EditText) findViewById(fieldId)).setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (!model.isBusy()) {
                    action.run();
                }
                return true;
            }
            return false;
        });
    }

    private void showMessage(int message) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.auth_root), message, Snackbar.LENGTH_LONG);
        // Firebase/configuration errors may need several lines with larger system fonts.
        TextView text = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        text.setMaxLines(6);
        snackbar.show();
    }

    private static void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setEnabled(group.getChildAt(i), enabled);
            }
        }
    }
}
