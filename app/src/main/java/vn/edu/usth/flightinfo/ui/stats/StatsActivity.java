package vn.edu.usth.flightinfo.ui.stats;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.FlightRepository;
import vn.edu.usth.flightinfo.data.local.AirportStore;
import vn.edu.usth.flightinfo.data.stats.FlightStats;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.ui.common.LocaleUtils;
import vn.edu.usth.flightinfo.util.AppExecutors;

/** Displays statistics derived from this account's locally saved My Flights. */
public class StatsActivity extends AppCompatActivity {
    private TextView countView;
    private TextView distanceView;
    private TextView hoursView;
    private TextView punctualityView;
    private TextView busiestAirportView;
    private BarChartView chart;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        countView = findViewById(R.id.stats_count);
        distanceView = findViewById(R.id.stats_distance_value);
        hoursView = findViewById(R.id.stats_hours_value);
        punctualityView = findViewById(R.id.stats_punctuality_value);
        busiestAirportView = findViewById(R.id.stats_busiest_airport_value);
        chart = findViewById(R.id.stats_airline_chart);

        findViewById(R.id.stats_toolbar).setOnClickListener(v -> finish());
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.stats_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        loadStats();
    }

    private void loadStats() {
        AirportStore.get(this).load(new Callback<List<Airport>>() {
            @Override public void onSuccess(List<Airport> airports) {
                Map<String, Airport> byIata = new HashMap<>();
                for (Airport airport : airports) byIata.put(airport.getIata(), airport);
                FlightRepository.get(StatsActivity.this).getSavedFlights(new Callback<List<Flight>>() {
                    @Override public void onSuccess(List<Flight> flights) {
                        AppExecutors.get().io(() -> {
                            FlightStats stats = FlightStats.from(flights, byIata);
                            AppExecutors.get().main(() -> {
                                if (!isFinishing() && !isDestroyed()) render(stats, byIata);
                            });
                        });
                    }
                    @Override public void onError(vn.edu.usth.flightinfo.data.ApiException error) {
                        showEmpty();
                    }
                });
            }
            @Override public void onError(vn.edu.usth.flightinfo.data.ApiException error) {
                FlightRepository.get(StatsActivity.this).getSavedFlights(new Callback<List<Flight>>() {
                    @Override public void onSuccess(List<Flight> flights) {
                        Map<String, Airport> noAirports = new HashMap<>();
                        AppExecutors.get().io(() -> {
                            FlightStats stats = FlightStats.from(flights, noAirports);
                            AppExecutors.get().main(() -> {
                                if (!isFinishing() && !isDestroyed()) render(stats, noAirports);
                            });
                        });
                    }
                    @Override public void onError(vn.edu.usth.flightinfo.data.ApiException ignored) { showEmpty(); }
                });
            }
        });
    }

    private void render(FlightStats stats, Map<String, Airport> airports) {
        countView.setText(String.valueOf(stats.getFlightCount()));
        distanceView.setText(getString(R.string.stats_value_km, Math.round(stats.getTotalDistanceKm())));
        hoursView.setText(getString(R.string.stats_value_hours, formatHours(stats.getTotalHours())));
        punctualityView.setText(getString(R.string.stats_value_percent, Math.round(stats.getOnTimePercent())));

        List<BarChartView.Bar> bars = new ArrayList<>();
        for (FlightStats.AirlineCount airline : stats.getAirlines()) {
            String label = "OTHER".equals(airline.getCode())
                    ? getString(R.string.stats_other_airline) : airline.getCode();
            bars.add(new BarChartView.Bar(label, airline.getCount()));
        }
        chart.setData(bars);

        String code = stats.getBusiestDepartureIata();
        if (code == null) {
            busiestAirportView.setText(getString(R.string.stats_no_airport));
        } else {
            Airport airport = airports.get(code);
            String city = airport == null ? "" : airport.getCity(LocaleUtils.isVietnamese(this));
            String place = city.isEmpty() ? code : code + " · " + city;
            busiestAirportView.setText(getString(R.string.stats_airport_count, place,
                    stats.getBusiestDepartureCount()));
        }
    }

    private String formatHours(double hours) {
        if (hours <= 0) return "0";
        if (hours < 10) return String.format(Locale.getDefault(), "%.1f", hours);
        return String.format(Locale.getDefault(), "%.0f", hours);
    }

    private void showEmpty() {
        countView.setText("0");
        distanceView.setText(getString(R.string.stats_value_km, 0));
        hoursView.setText(getString(R.string.stats_value_hours, "0"));
        punctualityView.setText(getString(R.string.stats_value_percent, 0));
        busiestAirportView.setText(getString(R.string.stats_no_airport));
        chart.setData(new ArrayList<>());
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
