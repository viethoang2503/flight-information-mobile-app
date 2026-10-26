package vn.edu.usth.flightinfo.ui.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import vn.edu.usth.flightinfo.ui.MainActivity;

/** A session change must also discard old screens and their account-specific data. */
public final class AuthNavigation {
    private AuthNavigation() { }

    public static Intent loginIntent(Context context) {
        return new Intent(context, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

    public static void openLogin(Activity activity) {
        activity.startActivity(loginIntent(activity));
        activity.finish();
    }

    static void openMain(Activity activity) {
        activity.startActivity(new Intent(activity, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        activity.finish();
    }
}
