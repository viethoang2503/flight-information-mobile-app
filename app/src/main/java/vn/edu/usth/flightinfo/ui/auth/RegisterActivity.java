package vn.edu.usth.flightinfo.ui.auth;

import android.os.Bundle;

import com.google.android.material.textfield.TextInputLayout;

import vn.edu.usth.flightinfo.R;

/** Creates an account and updates its display name before opening MainActivity. */
public class RegisterActivity extends AuthActivity {
    private TextInputLayout nameLayout;
    private TextInputLayout confirmationLayout;

    @Override
    protected int layoutResource() {
        return R.layout.activity_register;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nameLayout = findViewById(R.id.auth_name_layout);
        confirmationLayout = findViewById(R.id.auth_confirmation_layout);
        bind(R.id.auth_name, model.name, value -> model.name = value, nameLayout);
        bind(R.id.auth_confirmation, model.confirmation,
                value -> model.confirmation = value, confirmationLayout);
        findViewById(R.id.auth_submit).setOnClickListener(v -> register());
        findViewById(R.id.auth_back_to_login).setOnClickListener(v -> finish());
        onKeyboardDone(R.id.auth_confirmation, this::register);
        observeForm();
    }

    private void register() {
        nameLayout.setError(null);
        confirmationLayout.setError(null);
        if (model.name.trim().isEmpty()) {
            nameLayout.setError(getString(R.string.auth_validation_name));
            nameLayout.getEditText().requestFocus();
            return;
        }
        if (!validateEmail() || !validatePassword()) {
            return;
        }
        if (!model.password.equals(model.confirmation)) {
            confirmationLayout.setError(getString(R.string.auth_validation_confirmation));
            confirmationLayout.getEditText().requestFocus();
            return;
        }
        model.submit(auth, AuthViewModel.Action.REGISTER);
    }
}
