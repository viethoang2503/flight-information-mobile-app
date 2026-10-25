package vn.edu.usth.flightinfo.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import vn.edu.usth.flightinfo.R;

/** Hosts SettingsFragment (PreferenceFragmentCompat) under the glass toolbar. */
public class SettingsActivity extends AppCompatActivity {

    public static Intent newIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setTitle(R.string.settings_title);

        // After a rotation or a language change the FragmentManager restores the fragment itself.
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    /** Back to the existing MainActivity (keeps its tabs) instead of re-creating the parent. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
