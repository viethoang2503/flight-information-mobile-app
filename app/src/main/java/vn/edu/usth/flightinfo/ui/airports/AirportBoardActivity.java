package vn.edu.usth.flightinfo.ui.airports;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.local.AirportStore;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.ui.common.LocaleUtils;

/**
 * Departure/arrival board of one airport: TabLayout + ViewPager2 with one BoardFragment per tab
 * (swipe horizontally between them).
 */
public class AirportBoardActivity extends AppCompatActivity implements BoardFragment.Host {

    public static final String EXTRA_AIRPORT_IATA = "airport_iata";

    private ViewPager2 pager;
    private TextView mockBanner;
    /** Whether each page currently shows mock data. */
    private final boolean[] pageIsMock = new boolean[2];

    public static Intent newIntent(Context context, String airportIata) {
        return new Intent(context, AirportBoardActivity.class).putExtra(EXTRA_AIRPORT_IATA, airportIata);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_airport_board);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mockBanner = findViewById(R.id.mock_banner);

        String iata = getIntent().getStringExtra(EXTRA_AIRPORT_IATA);
        setTitle(iata);
        AirportStore.get(this).load(new Callback<List<Airport>>() {
            @Override
            public void onSuccess(List<Airport> airports) {
                Airport airport = AirportStore.get(AirportBoardActivity.this).find(iata);
                if (airport != null && getSupportActionBar() != null) {
                    String city = airport.getCity(LocaleUtils.isVietnamese(AirportBoardActivity.this));
                    setTitle(getString(R.string.board_title, city, iata));
                    getSupportActionBar().setSubtitle(airport.getName());
                }
            }

            @Override
            public void onError(ApiException error) {
            }
        });

        pager = findViewById(R.id.board_pager);
        pager.setAdapter(new BoardPagerAdapter(this, iata));
        TabLayout tabs = findViewById(R.id.board_tabs);
        new TabLayoutMediator(tabs, pager, (tab, position) -> tab.setText(
                position == BoardPagerAdapter.PAGE_DEPARTURES ? R.string.tab_departures : R.string.tab_arrivals))
                .attach();
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateBanner();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.action_refresh) {
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                if (fragment instanceof BoardFragment) {
                    ((BoardFragment) fragment).refresh();
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBoardLoaded(boolean departures, boolean mock) {
        pageIsMock[departures ? BoardPagerAdapter.PAGE_DEPARTURES : BoardPagerAdapter.PAGE_ARRIVALS] = mock;
        updateBanner();
    }

    private void updateBanner() {
        if (pager == null) {
            return;
        }
        mockBanner.setVisibility(pageIsMock[pager.getCurrentItem()] ? View.VISIBLE : View.GONE);
    }
}
