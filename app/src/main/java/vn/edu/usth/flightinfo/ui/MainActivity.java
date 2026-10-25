package vn.edu.usth.flightinfo.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.ui.airports.AirportsFragment;
import vn.edu.usth.flightinfo.ui.search.SearchFragment;

/**
 * Main screen: Toolbar + one fragment per bottom-navigation tab. Tabs are created lazily and then
 * shown/hidden (not replaced), so each keeps its scroll position and loaded data.
 */
public class MainActivity extends AppCompatActivity implements TabFragment.Host {

    private static final String STATE_SELECTED_TAB = "selected_tab";

    private TextView mockBanner;
    @IdRes
    private int selectedTabId = R.id.nav_search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mockBanner = findViewById(R.id.mock_banner);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        // Tapping the current tab again acts like Refresh.
        bottomNav.setOnItemReselectedListener(item -> {
            TabFragment tab = currentTab();
            if (tab != null) {
                tab.onRefreshRequested();
            }
        });

        if (savedInstanceState == null) {
            showTab(R.id.nav_search);
        } else {
            // FragmentManager restores the fragments and their hidden state by itself.
            selectedTabId = savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.nav_search);
            updateChrome();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, selectedTabId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            TabFragment tab = currentTab();
            if (tab != null) {
                tab.onRefreshRequested();
            }
            return true;
        }
        if (id == R.id.action_settings) {
            // SettingsActivity arrives in Phase 5.
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabDataSourceChanged(TabFragment tab) {
        if (tab == currentTab()) {
            updateChrome();
        }
    }

    private void showTab(@IdRes int tabId) {
        selectedTabId = tabId;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        for (Fragment fragment : fm.getFragments()) {
            if (fragment instanceof TabFragment) {
                tx.hide(fragment);
            }
        }
        String tag = tagFor(tabId);
        Fragment target = fm.findFragmentByTag(tag);
        if (target == null) {
            tx.add(R.id.fragment_container, createTab(tabId), tag);
        } else {
            tx.show(target);
        }
        tx.commitNow();
        updateChrome();
    }

    /** Title and mock banner follow the visible tab. */
    private void updateChrome() {
        TabFragment tab = currentTab();
        if (tab == null) {
            return;
        }
        setTitle(tab.getTitleRes());
        mockBanner.setVisibility(tab.isShowingMockData() ? View.VISIBLE : View.GONE);
    }

    private TabFragment currentTab() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tagFor(selectedTabId));
        return fragment instanceof TabFragment ? (TabFragment) fragment : null;
    }

    private static String tagFor(@IdRes int tabId) {
        return "tab_" + tabId;
    }

    private static TabFragment createTab(@IdRes int tabId) {
        if (tabId == R.id.nav_airports) {
            return new AirportsFragment();
        }
        if (tabId == R.id.nav_map) {
            return ComingSoonFragment.newInstance(R.string.tab_map);
        }
        if (tabId == R.id.nav_my_flights) {
            return ComingSoonFragment.newInstance(R.string.tab_my_flights);
        }
        return new SearchFragment();
    }
}
