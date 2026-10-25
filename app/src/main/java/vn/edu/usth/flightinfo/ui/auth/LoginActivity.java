package vn.edu.usth.flightinfo.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import vn.edu.usth.flightinfo.R;

/** Email/password login, reset by email, or full offline access as a guest. */
public class LoginActivity extends AuthActivity {
    @Override
    protected int layoutResource() {
        return R.layout.activity_login;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViewById(R.id.auth_submit).setOnClickListener(v -> signIn());
        findViewById(R.id.auth_reset).setOnClickListener(v -> {
            if (validateEmail()) {
                model.submit(auth, AuthViewModel.Action.RESET_PASSWORD);
            }
        });
        findViewById(R.id.auth_register).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.auth_guest).setOnClickListener(v -> {
            auth.continueAsGuest();
            AuthNavigation.openMain(this);
        });
        onKeyboardDone(R.id.auth_password, this::signIn);
        observeForm();
    }

    private void signIn() {
        if (validateEmail() && validatePassword()) {
            model.submit(auth, AuthViewModel.Action.SIGN_IN);
        }
    }
}
