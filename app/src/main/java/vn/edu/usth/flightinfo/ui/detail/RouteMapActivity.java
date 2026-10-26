package vn.edu.usth.flightinfo.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.local.AirportStore;
import vn.edu.usth.flightinfo.data.parser.AirLabsParser;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.model.FlightStatus;
import vn.edu.usth.flightinfo.ui.common.LocaleUtils;
import vn.edu.usth.flightinfo.ui.common.StatusUi;
import vn.edu.usth.flightinfo.ui.map.MapStyle;
import vn.edu.usth.flightinfo.util.TimeUtils;

import org.osmdroid.views.MapView;

/**
 * Full-screen interactive map displaying the great-circle flight trajectory,
 * departure/arrival airports, and aircraft position.
 */
public class RouteMapActivity extends AppCompatActivity {

    public static final String EXTRA_FLIGHT_JSON = "flight_json";
    private static final String STATE_FLIGHT_JSON = "state_flight_json";

    private String flightJson;
    private Flight flight;
    private MapView mapView;
    private FrameLayout mapContainer;

    public static Intent newIntent(Context context, String flightJson) {
        return new Intent(context, RouteMapActivity.class)
                .putExtra(EXTRA_FLIGHT_JSON, flightJson);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        mapContainer = findViewById(R.id.map_container);

        if (savedInstanceState != null) {
            flightJson = savedInstanceState.getString(STATE_FLIGHT_JSON);
        } else {
            flightJson = getIntent().getStringExtra(EXTRA_FLIGHT_JSON);
        }

        flight = parseFlight(flightJson);
        if (flight == null) {
            finish();
            return;
        }

        String title = getString(R.string.route_map_full_title, flight.getFlightIata());
        setTitle(title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        bindBottomCard();
        loadAndDisplayRoute();
    }

    private void bindBottomCard() {
        TextView flightCode = findViewById(R.id.flight_code);
        TextView flightStatus = findViewById(R.id.flight_status);
        TextView depIata = findViewById(R.id.dep_iata);
        TextView depCity = findViewById(R.id.dep_city);
        TextView arrIata = findViewById(R.id.arr_iata);
        TextView arrCity = findViewById(R.id.arr_city);
        TextView progressText = findViewById(R.id.progress_text);

        flightCode.setText(flight.getFlightIata());
        StatusUi.bind(flightStatus, flight.getStatus());
        depIata.setText(flight.getDepIata());
        arrIata.setText(flight.getArrIata());

        AirportStore airports = AirportStore.get(this);
        boolean isVi = LocaleUtils.isVietnamese(this);
        Airport dep = airports.find(flight.getDepIata());
        Airport arr = airports.find(flight.getArrIata());

        depCity.setText(dep != null ? dep.getCity(isVi) : "");
        arrCity.setText(arr != null ? arr.getCity(isVi) : "");

        int percent = computeProgressPercent();
        if (percent >= 0) {
            progressText.setText(getString(R.string.route_map_progress, percent));
            progressText.setVisibility(View.VISIBLE);
        } else {
            progressText.setVisibility(View.GONE);
        }
    }

    private int computeProgressPercent() {
        FlightStatus s = flight.getStatus();
        if (s == FlightStatus.LANDED) {
            return 100;
        }
        if (s == FlightStatus.SCHEDULED || s == FlightStatus.DELAYED || s == FlightStatus.CANCELLED) {
            return 0;
        }
        return TimeUtils.progressPercent(flight.getBestDepTs(), flight.getBestArrTs(), TimeUtils.nowSeconds());
    }

    private void loadAndDisplayRoute() {
        AirportStore store = AirportStore.get(this);
        store.load(new Callback<List<Airport>>() {
            @Override
            public void onSuccess(List<Airport> airports) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Airport dep = store.find(flight.getDepIata());
                Airport arr = store.find(flight.getArrIata());
                if (dep == null || arr == null) {
                    return;
                }

                // Update city names if they were missing on initial render
                boolean isVi = LocaleUtils.isVietnamese(RouteMapActivity.this);
                ((TextView) findViewById(R.id.dep_city)).setText(dep.getCity(isVi));
                ((TextView) findViewById(R.id.arr_city)).setText(arr.getCity(isVi));

                setupMapView(dep, arr);
            }

            @Override
            public void onError(ApiException error) {
                // Airport coordinates unavailable
            }
        });
    }

    private void setupMapView(Airport dep, Airport arr) {
        MapStyle.loadConfigThen(this, () -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (mapView == null) {
                mapView = new MapView(this);
                mapView.setMultiTouchControls(true);
                mapView.setTilesScaledToDpi(true);
                mapContainer.addView(mapView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            RouteMapHelper.setupRoute(mapView, dep, arr, computeProgressPercent());
            mapView.onResume();
        });
    }

    private Flight parseFlight(String json) {
        if (json == null) {
            return null;
        }
        try {
            return AirLabsParser.parseFlight(new JSONObject(json));
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (flightJson != null) {
            outState.putString(STATE_FLIGHT_JSON, flightJson);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) {
            mapView.onDetach();
        }
        super.onDestroy();
    }
}
