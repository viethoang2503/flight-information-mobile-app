package vn.edu.usth.flightinfo.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;
import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.auth.AuthManager;
import vn.edu.usth.flightinfo.ui.auth.AuthNavigation;
import vn.edu.usth.flightinfo.ui.airports.AirportsFragment;
import vn.edu.usth.flightinfo.ui.map.LiveMapFragment;
import vn.edu.usth.flightinfo.ui.myflights.MyFlightsFragment;
import vn.edu.usth.flightinfo.ui.search.SearchFragment;
import vn.edu.usth.flightinfo.ui.settings.SettingsActivity;

/**
 * Main screen: Toolbar + one fragment per bottom-navigation tab. Tabs are created lazily and then
 * shown/hidden (not replaced), so each keeps its scroll position and loaded data.
 */
public class MainActivity extends AppCompatActivity implements TabFragment.Host {

    private static final String STATE_SELECTED_TAB = "selected_tab";
    /** Blur radius of the glass tab bar, applied to the downscaled snapshot of the content. */
    private static final float NAV_BAR_BLUR_RADIUS = 20f;

    private MaterialToolbar toolbar;
    private TextView mockBanner;
    @IdRes
    private int selectedTabId = R.id.nav_search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthManager.get(this).hasSession()) {
            AuthNavigation.openLogin(this);
            return;
        }
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mockBanner = findViewById(R.id.mock_banner);
        setUpGlassNavBar();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        // BottomNavigationView always wraps its background in a MaterialShapeDrawable, which is
        // drawn even when transparent. The glass bar behind it is enough: no extra layer.
        bottomNav.setBackground(null);
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

    /**
     * The floating tab bar is real frosted glass on Android 12+ (RenderEffect blur of the content
     * scrolling under it). Older versions get a near-opaque fill instead of a costly software blur.
     */
    private void setUpGlassNavBar() {
        BlurView navBar = findViewById(R.id.nav_bar);
        navBar.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        navBar.setClipToOutline(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BlurTarget target = findViewById(R.id.blur_target);
            // No frame-clear drawable: the window gradient already shows through the transparent
            // parts of the blurred snapshot, so repainting it would only add a layer of overdraw.
            navBar.setupWith(target).setBlurRadius(NAV_BAR_BLUR_RADIUS);
        } else {
            navBar.setBackgroundResource(R.drawable.bg_blur_glass_fallback);
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
            startActivity(SettingsActivity.newIntent(this));
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
        // Set on the toolbar itself: it has a title from XML, so Activity.setTitle() would be ignored.
        toolbar.setTitle(tab.getTitleRes());
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
            return new LiveMapFragment();
        }
        if (tabId == R.id.nav_my_flights) {
            return new MyFlightsFragment();
        }
        return new SearchFragment();
    }
}
