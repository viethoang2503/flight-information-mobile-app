package vn.edu.usth.flightinfo.ui.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.List;

import vn.edu.usth.flightinfo.BuildConfig;
import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.local.AirportStore;
import vn.edu.usth.flightinfo.data.local.Prefs;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.ui.common.LocaleUtils;

/**
 * Preferences from res/xml/preferences.xml, saved by the library in the default
 * SharedPreferences and read back through {@link Prefs}.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String LANGUAGE_SYSTEM = "system";
    private static final String KEY_VERSION = "version";

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        setUpLanguage();
        setUpDefaultAirport();

        Preference version = findPreference(KEY_VERSION);
        if (version != null) {
            version.setSummary(BuildConfig.VERSION_NAME);
        }
    }

    /**
     * The in-app language is owned by AppCompat (it also follows the system's per-app language
     * setting on Android 13+), so the list shows AppCompat's value and writes back to it.
     * Changing it re-creates the open activities in the new language.
     */
    private void setUpLanguage() {
        ListPreference language = findPreference(Prefs.KEY_LANGUAGE);
        if (language == null) {
            return;
        }
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        String value = current.isEmpty() || current.get(0) == null
                ? LANGUAGE_SYSTEM : current.get(0).getLanguage();
        language.setValue(language.findIndexOfValue(value) >= 0 ? value : LANGUAGE_SYSTEM);

        language.setOnPreferenceChangeListener((preference, newValue) -> {
            String tag = (String) newValue;
            AppCompatDelegate.setApplicationLocales(LANGUAGE_SYSTEM.equals(tag)
                    ? LocaleListCompat.getEmptyLocaleList()
                    : LocaleListCompat.forLanguageTags(tag));
            return true;
        });
    }

    /** Fills the airport list from assets/airports.json (read on a worker thread). */
    private void setUpDefaultAirport() {
        ListPreference airport = findPreference(Prefs.KEY_DEFAULT_AIRPORT);
        if (airport == null) {
            return;
        }
        AirportStore.get(requireContext()).load(new Callback<List<Airport>>() {
            @Override
            public void onSuccess(List<Airport> airports) {
                if (!isAdded()) {
                    return;
                }
                boolean vietnamese = LocaleUtils.isVietnamese(requireContext());
                CharSequence[] entries = new CharSequence[airports.size()];
                CharSequence[] values = new CharSequence[airports.size()];
                for (int i = 0; i < airports.size(); i++) {
                    Airport a = airports.get(i);
                    entries[i] = getString(R.string.format_airport_entry, a.getIata(), a.getCity(vietnamese));
                    values[i] = a.getIata();
                }
                airport.setEntries(entries);
                airport.setEntryValues(values);
                // Enabling refreshes the summary, which can now show the airport's name.
                airport.setEnabled(true);
            }

            @Override
            public void onError(ApiException error) {
                // Without the list the preference stays disabled and the default (HAN) is used.
            }
        });
    }
}
