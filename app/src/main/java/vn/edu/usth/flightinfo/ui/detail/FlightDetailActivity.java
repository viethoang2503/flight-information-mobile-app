package vn.edu.usth.flightinfo.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.toolbox.NetworkImageView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.FlightRepository;
import vn.edu.usth.flightinfo.data.WeatherRepository;
import vn.edu.usth.flightinfo.data.local.AirportStore;
import vn.edu.usth.flightinfo.data.local.Prefs;
import vn.edu.usth.flightinfo.data.parser.AirLabsParser;
import vn.edu.usth.flightinfo.data.remote.ApiClient;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.model.FlightStatus;
import vn.edu.usth.flightinfo.model.Result;
import vn.edu.usth.flightinfo.model.Weather;
import vn.edu.usth.flightinfo.ui.common.ErrorMessages;
import vn.edu.usth.flightinfo.ui.common.LocaleUtils;
import vn.edu.usth.flightinfo.ui.common.StatusUi;
import vn.edu.usth.flightinfo.util.Airlines;
import vn.edu.usth.flightinfo.util.TimeUtils;

/**
 * Full information about one flight: boarding-pass card, detail rows, arrival weather (METAR).
 * Toolbar actions: save to My Flights, share, refresh. While visible, the flight auto-refreshes
 * every minute (started in onResume, stopped in onPause).
 */
public class FlightDetailActivity extends AppCompatActivity {

    public static final String EXTRA_FLIGHT_IATA = "flight_iata";
    public static final String EXTRA_FLIGHT_JSON = "flight_json";
    public static final String EXTRA_IS_MOCK = "is_mock";
    public static final String EXTRA_REFRESH = "refresh";

    private static final String STATE_FLIGHT_JSON = "state_flight_json";
    private static final String STATE_IS_MOCK = "state_is_mock";
    private static final String STATE_METAR_EXPANDED = "state_metar_expanded";

    private static final long AUTO_REFRESH_MS = 60_000;

    private FlightRepository flightRepository;
    private WeatherRepository weatherRepository;
    private Prefs prefs;

    private String flightIata;
    private Flight flight;
    private boolean isMock;
    /** Null until the database answered. */
    private Boolean saved;
    private String loadedWeatherIcao;
    private boolean metarExpanded;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefresh = new Runnable() {
        @Override
        public void run() {
            if (flight != null && !isFinal(flight.getStatus())) {
                loadFlight();
            }
            handler.postDelayed(this, AUTO_REFRESH_MS);
        }
    };

    private TextView mockBanner;
    private SwipeRefreshLayout swipeRefresh;
    private NetworkImageView logo;
    private TextView flightCode;
    private TextView airline;
    private TextView status;
    private TextView depCode;
    private TextView depCity;
    private TextView depTime;
    private TextView depTimeOld;
    private TextView arrCode;
    private TextView arrCity;
    private TextView arrTime;
    private TextView arrTimeOld;
    private LinearProgressIndicator progress;
    private TextView progressLabel;
    private LinearLayout infoContainer;
    private View weatherCard;
    private TextView weatherTitle;
    private TextView weatherCategory;
    private TextView weatherSummary;
    private TextView metarRaw;
    private TextView metarHint;

    /** Opens the detail of a flight that is already loaded; {@code refresh} reloads it on open. */
    public static Intent newIntent(Context context, Flight flight, boolean isMock, boolean refresh) {
        return new Intent(context, FlightDetailActivity.class)
                .putExtra(EXTRA_FLIGHT_IATA, flight.getFlightIata())
                .putExtra(EXTRA_FLIGHT_JSON, flight.getRawJson())
                .putExtra(EXTRA_IS_MOCK, isMock)
                .putExtra(EXTRA_REFRESH, refresh);
    }

    /** Opens the detail of a flight known only by its number (e.g. from the map). */
    public static Intent newIntent(Context context, String flightIata) {
        return new Intent(context, FlightDetailActivity.class).putExtra(EXTRA_FLIGHT_IATA, flightIata);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        flightRepository = FlightRepository.get(this);
        weatherRepository = WeatherRepository.get(this);
        prefs = new Prefs(this);
        bindViews();

        swipeRefresh.setColorSchemeResources(R.color.brand_link);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface);
        swipeRefresh.setOnRefreshListener(this::loadFlight);
        metarRaw.setOnClickListener(v -> {
            metarExpanded = !metarExpanded;
            applyMetarHeight();
        });

        Intent intent = getIntent();
        flightIata = intent.getStringExtra(EXTRA_FLIGHT_IATA);
        setTitle(flightIata);

        String json;
        boolean needsLoad;
        if (savedInstanceState != null) {
            json = savedInstanceState.getString(STATE_FLIGHT_JSON);
            isMock = savedInstanceState.getBoolean(STATE_IS_MOCK);
            metarExpanded = savedInstanceState.getBoolean(STATE_METAR_EXPANDED);
            needsLoad = json == null;
        } else {
            json = intent.getStringExtra(EXTRA_FLIGHT_JSON);
            isMock = intent.getBooleanExtra(EXTRA_IS_MOCK, false);
            needsLoad = json == null || intent.getBooleanExtra(EXTRA_REFRESH, false);
        }
        flight = parse(json);
        if (flight != null) {
            render();
        }
        if (needsLoad) {
            loadFlight();
        }

        // City names come from assets/airports.json; re-render once it is loaded.
        AirportStore.get(this).load(new Callback<List<Airport>>() {
            @Override
            public void onSuccess(List<Airport> airports) {
                if (flight != null && !isDestroyed()) {
                    render();
                }
            }

            @Override
            public void onError(ApiException error) {
                // Codes are still shown without city names.
            }
        });
        refreshSavedState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(autoRefresh);
        if (prefs.isAutoRefresh()) {
            handler.postDelayed(autoRefresh, AUTO_REFRESH_MS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(autoRefresh);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Pending callbacks are dropped by Volley, so also stop the spinner.
        flightRepository.cancel(this);
        weatherRepository.cancel(this);
        swipeRefresh.setRefreshing(false);
        loadedWeatherIcao = null;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (flight != null) {
            outState.putString(STATE_FLIGHT_JSON, flight.getRawJson());
        }
        outState.putBoolean(STATE_IS_MOCK, isMock);
        outState.putBoolean(STATE_METAR_EXPANDED, metarExpanded);
    }

    // --- Menu ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_flight_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem save = menu.findItem(R.id.action_save);
        boolean isSaved = Boolean.TRUE.equals(saved);
        save.setIcon(isSaved ? R.drawable.ic_star_saved : R.drawable.ic_star_border);
        save.setTitle(isSaved ? R.string.action_unsave : R.string.action_save);
        save.setEnabled(flight != null && saved != null);
        menu.findItem(R.id.action_share).setEnabled(flight != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.action_save) {
            toggleSaved();
            return true;
        }
        if (id == R.id.action_share) {
            share();
            return true;
        }
        if (id == R.id.action_refresh) {
            loadFlight();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- Loading ---

    private void loadFlight() {
        swipeRefresh.setRefreshing(true);
        flightRepository.getFlight(flightIata, this, new Callback<Result<Flight>>() {
            @Override
            public void onSuccess(Result<Flight> result) {
                if (isDestroyed()) {
                    return;
                }
                swipeRefresh.setRefreshing(false);
                flight = result.getData();
                isMock = result.isMock();
                render();
                if (Boolean.TRUE.equals(saved)) {
                    flightRepository.updateSaved(flight);
                }
            }

            @Override
            public void onError(ApiException error) {
                if (isDestroyed()) {
                    return;
                }
                swipeRefresh.setRefreshing(false);
                Snackbar.make(swipeRefresh, ErrorMessages.of(error),
                                flight == null ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_retry, v -> loadFlight())
                        .show();
            }
        });
    }

    private void loadWeather(String icao) {
        if (icao == null || icao.equals(loadedWeatherIcao)) {
            return;
        }
        loadedWeatherIcao = icao;
        weatherRepository.getMetar(icao, this, new Callback<Result<Weather>>() {
            @Override
            public void onSuccess(Result<Weather> result) {
                if (!isDestroyed()) {
                    bindWeather(result.getData());
                }
            }

            @Override
            public void onError(ApiException error) {
                if (!isDestroyed()) {
                    weatherCard.setVisibility(View.GONE);
                }
            }
        });
    }

    private void refreshSavedState() {
        flightRepository.isSaved(flightIata, new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                saved = result;
                invalidateOptionsMenu();
            }

            @Override
            public void onError(ApiException error) {
            }
        });
    }

    private void toggleSaved() {
        if (flight == null || saved == null) {
            return;
        }
        boolean newSaved = !saved;
        flightRepository.setSaved(flight, newSaved, () -> {
            saved = newSaved;
            invalidateOptionsMenu();
            Snackbar.make(swipeRefresh, newSaved ? R.string.saved_message : R.string.removed_message,
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    private void share() {
        if (flight == null) {
            return;
        }
        String text = getString(R.string.share_text,
                flight.getFlightIata(),
                flight.getDepIata(),
                flight.getArrIata(),
                orPlaceholder(TimeUtils.hhmm(flight.getBestDepTime())),
                orPlaceholder(TimeUtils.hhmm(flight.getBestArrTime())),
                getString(StatusUi.label(flight.getStatus())));
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser)));
    }

    // --- Rendering ---

    private void render() {
        mockBanner.setVisibility(isMock ? View.VISIBLE : View.GONE);

        logo.setDefaultImageResId(R.drawable.ic_flight);
        logo.setErrorImageResId(R.drawable.ic_flight);
        logo.setImageUrl(Airlines.logoUrl(flight.getAirlineIata()), ApiClient.get().getImageLoader());
        flightCode.setText(flight.getFlightIata());
        airline.setText(Airlines.name(flight.getAirlineIata()));
        StatusUi.bind(status, flight.getStatus());

        AirportStore airports = AirportStore.get(this);
        Airport dep = airports.find(flight.getDepIata());
        Airport arr = airports.find(flight.getArrIata());
        boolean vietnamese = LocaleUtils.isVietnamese(this);
        depCode.setText(flight.getDepIata());
        arrCode.setText(flight.getArrIata());
        depCity.setText(dep != null ? dep.getCity(vietnamese) : "");
        arrCity.setText(arr != null ? arr.getCity(vietnamese) : "");

        boolean delayed = flight.isDelayed();
        bindTime(depTime, depTimeOld, flight.getDepTime(), flight.getBestDepTime(), delayed);
        bindTime(arrTime, arrTimeOld, flight.getArrTime(), flight.getBestArrTime(), delayed);
        bindProgress();
        bindInfoRows();

        String arrIcao = flight.getArrIcao() != null ? flight.getArrIcao() : arr != null ? arr.getIcao() : null;
        loadWeather(arrIcao);
        invalidateOptionsMenu();
    }

    /** Shows the newest known time; if it differs from the schedule, the schedule is struck through. */
    private void bindTime(TextView main, TextView old, String scheduled, String best, boolean delayed) {
        String scheduledHm = TimeUtils.hhmm(scheduled);
        String bestHm = TimeUtils.hhmm(best);
        main.setText(orPlaceholder(bestHm != null ? bestHm : scheduledHm));
        boolean changed = scheduledHm != null && bestHm != null && !scheduledHm.equals(bestHm);
        if (changed) {
            old.setText(scheduledHm);
            old.setPaintFlags(old.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            old.setVisibility(View.VISIBLE);
        } else {
            old.setVisibility(View.GONE);
        }
        @ColorRes int color = changed && delayed ? R.color.status_delayed : R.color.text_primary;
        main.setTextColor(ContextCompat.getColor(this, color));
    }

    private void bindProgress() {
        FlightStatus s = flight.getStatus();
        int percent = TimeUtils.progressPercent(flight.getBestDepTs(), flight.getBestArrTs(),
                TimeUtils.nowSeconds());
        if (s == FlightStatus.LANDED) {
            percent = 100;
        } else if (s == FlightStatus.SCHEDULED || s == FlightStatus.DELAYED || s == FlightStatus.CANCELLED) {
            percent = 0;
        }
        progress.setProgressCompat(Math.max(percent, 0), true);

        if (s == FlightStatus.LANDED) {
            progressLabel.setText(R.string.progress_arrived);
        } else if (s == FlightStatus.EN_ROUTE && percent >= 0) {
            progressLabel.setText(getString(R.string.format_progress, percent));
        } else if (s == FlightStatus.CANCELLED) {
            progressLabel.setText(R.string.status_cancelled);
        } else {
            progressLabel.setText(R.string.progress_not_departed);
        }
    }

    /** Detail rows are created from code into info_container (dynamic views). */
    private void bindInfoRows() {
        infoContainer.removeAllViews();
        Integer delay = flight.getDelayMinutes();
        if (delay != null && delay > 0) {
            addRow(R.string.label_delay, getString(R.string.format_delay_minutes, delay));
        }
        addRow(R.string.label_departure, terminalAndGate(flight.getDepTerminal(), flight.getDepGate()));
        addRow(R.string.label_arrival, terminalAndGate(flight.getArrTerminal(), flight.getArrGate()));
        addRow(R.string.label_baggage, orPlaceholder(flight.getArrBaggage()));
        addRow(R.string.label_aircraft, orPlaceholder(flight.getAircraftIcao()));
        addRow(R.string.label_registration, orPlaceholder(flight.getRegNumber()));
        Integer duration = flight.getDuration();
        addRow(R.string.label_duration, duration == null ? getString(R.string.placeholder_empty)
                : getString(R.string.format_duration, duration / 60, duration % 60));
    }

    private void addRow(@StringRes int label, String value) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_info_row, infoContainer, false);
        ((TextView) row.findViewById(R.id.info_label)).setText(label);
        ((TextView) row.findViewById(R.id.info_value)).setText(value);
        infoContainer.addView(row);
    }

    private String terminalAndGate(String terminal, String gate) {
        List<String> parts = new ArrayList<>();
        if (terminal != null) {
            parts.add(getString(R.string.format_terminal, terminal));
        }
        if (gate != null) {
            parts.add(getString(R.string.format_gate, gate));
        }
        return parts.isEmpty() ? getString(R.string.placeholder_empty)
                : TextUtils.join(getString(R.string.separator_dot), parts);
    }

    private void bindWeather(Weather weather) {
        Airport arr = AirportStore.get(this).find(flight != null ? flight.getArrIata() : null);
        String place = arr != null ? arr.getCity(LocaleUtils.isVietnamese(this)) : weather.getIcao();
        weatherTitle.setText(getString(R.string.weather_title, place));

        List<String> parts = new ArrayList<>();
        if (weather.getTemperatureC() != null) {
            parts.add(getString(R.string.format_temperature, weather.getTemperatureC()));
        }
        if (weather.getWindSpeedKt() != null) {
            parts.add(weather.getWindDirection() != null
                    ? getString(R.string.format_wind, weather.getWindDirection(), weather.getWindSpeedKt())
                    : getString(R.string.format_wind_variable, weather.getWindSpeedKt()));
        }
        if (weather.getVisibility() != null) {
            parts.add(getString(R.string.format_visibility, weather.getVisibility()));
        }
        weatherSummary.setText(TextUtils.join(getString(R.string.separator_dot), parts));

        String category = weather.getFlightCategory();
        if (category != null) {
            weatherCategory.setText(category);
            StatusUi.tint(weatherCategory, categoryColor(category));
            weatherCategory.setVisibility(View.VISIBLE);
        } else {
            weatherCategory.setVisibility(View.GONE);
        }

        metarRaw.setText(weather.getRawText());
        boolean hasRaw = weather.getRawText() != null;
        metarRaw.setVisibility(hasRaw ? View.VISIBLE : View.GONE);
        metarHint.setVisibility(hasRaw ? View.VISIBLE : View.GONE);
        applyMetarHeight();
        weatherCard.setVisibility(View.VISIBLE);
    }

    /**
     * Expands or collapses the raw METAR by changing the view's LayoutParams at runtime
     * (one text line when collapsed, WRAP_CONTENT when expanded).
     */
    private void applyMetarHeight() {
        ViewGroup.LayoutParams lp = metarRaw.getLayoutParams();
        lp.height = metarExpanded
                ? ViewGroup.LayoutParams.WRAP_CONTENT
                : metarRaw.getLineHeight() + metarRaw.getPaddingTop() + metarRaw.getPaddingBottom();
        metarRaw.setLayoutParams(lp);
        metarHint.setText(metarExpanded ? R.string.metar_collapse_hint : R.string.metar_expand_hint);
    }

    @ColorRes
    private static int categoryColor(String category) {
        switch (category) {
            case "VFR": return R.color.status_on_time;
            case "MVFR": return R.color.status_en_route;
            case "IFR": return R.color.status_cancelled;
            default: return R.color.status_landed;
        }
    }

    private static boolean isFinal(FlightStatus status) {
        return status == FlightStatus.LANDED || status == FlightStatus.CANCELLED;
    }

    private String orPlaceholder(String value) {
        return value != null ? value : getString(R.string.placeholder_empty);
    }

    private static Flight parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            return AirLabsParser.parseFlight(new JSONObject(json));
        } catch (JSONException e) {
            return null;
        }
    }

    private void bindViews() {
        mockBanner = findViewById(R.id.mock_banner);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        logo = findViewById(R.id.detail_logo);
        flightCode = findViewById(R.id.detail_flight_code);
        airline = findViewById(R.id.detail_airline);
        status = findViewById(R.id.detail_status);
        depCode = findViewById(R.id.dep_code);
        depCity = findViewById(R.id.dep_city);
        depTime = findViewById(R.id.dep_time);
        depTimeOld = findViewById(R.id.dep_time_old);
        arrCode = findViewById(R.id.arr_code);
        arrCity = findViewById(R.id.arr_city);
        arrTime = findViewById(R.id.arr_time);
        arrTimeOld = findViewById(R.id.arr_time_old);
        progress = findViewById(R.id.detail_progress);
        progressLabel = findViewById(R.id.detail_progress_label);
        infoContainer = findViewById(R.id.info_container);
        weatherCard = findViewById(R.id.weather_card);
        weatherTitle = findViewById(R.id.weather_title);
        weatherCategory = findViewById(R.id.weather_category);
        weatherSummary = findViewById(R.id.weather_summary);
        metarRaw = findViewById(R.id.metar_raw);
        metarHint = findViewById(R.id.metar_hint);
    }
}
