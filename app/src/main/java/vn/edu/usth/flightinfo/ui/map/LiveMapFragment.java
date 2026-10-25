package vn.edu.usth.flightinfo.ui.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.android.volley.toolbox.NetworkImageView;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;
import vn.edu.usth.flightinfo.BuildConfig;
import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.MapRepository;
import vn.edu.usth.flightinfo.data.local.AirportStore;
import vn.edu.usth.flightinfo.data.local.Prefs;
import vn.edu.usth.flightinfo.data.remote.ApiClient;
import vn.edu.usth.flightinfo.model.AircraftState;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.model.Result;
import vn.edu.usth.flightinfo.ui.TabFragment;
import vn.edu.usth.flightinfo.ui.airports.AirportBoardActivity;
import vn.edu.usth.flightinfo.ui.common.ErrorMessages;
import vn.edu.usth.flightinfo.ui.detail.FlightDetailActivity;
import vn.edu.usth.flightinfo.util.Airlines;
import vn.edu.usth.flightinfo.util.AppExecutors;
import vn.edu.usth.flightinfo.util.UnitUtils;

/**
 * Tab 3: aircraft over Vietnam on an osmdroid map (OpenSky, or mock data that keeps moving).
 * Planes point in their direction of flight; tapping one opens a glass sheet with its numbers and
 * a "View flight" button. Positions refresh every 30 s, only while the tab is on screen.
 */
public class LiveMapFragment extends TabFragment {

    private static final long REFRESH_INTERVAL_MS = 30_000;
    private static final double START_LAT = 16.0;
    private static final double START_LON = 106.5;
    private static final double START_ZOOM = 5.6;
    private static final double MIN_ZOOM = 4.5;
    private static final double MAX_ZOOM = 12.0;
    private static final float BLUR_RADIUS = 20f;
    /** Vertical speed (m/s) under which the aircraft is shown as flying level. */
    private static final double LEVEL_FLIGHT_MS = 0.5;

    private static final String STATE_SELECTED = "selected_icao24";
    private static final String STATE_LAT = "map_lat";
    private static final String STATE_LON = "map_lon";
    private static final String STATE_ZOOM = "map_zoom";


    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            loadAircraft();
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    private MapRepository repository;
    private Prefs prefs;
    private MapView mapView;
    private final Map<String, Marker> aircraftMarkers = new HashMap<>();
    private final Map<String, AircraftState> aircraft = new HashMap<>();
    private Drawable planeIcon;
    private Drawable planeIconSelected;
    private String selectedIcao24;
    private long lastLoadAt;
    private Bundle pendingMapState;

    private TextView countView;
    private TextView updatedView;
    private BlurView sheet;
    private NetworkImageView sheetLogo;
    private TextView sheetTitle;
    private TextView sheetSubtitle;
    private TextView sheetAltitude;
    private TextView sheetSpeed;
    private TextView sheetHeading;
    private Button viewFlightButton;

    @Override
    public int getTitleRes() {
        return R.string.tab_map;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        repository = MapRepository.get(context);
        prefs = new Prefs(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        countView = view.findViewById(R.id.map_count);
        updatedView = view.findViewById(R.id.map_updated);
        sheet = view.findViewById(R.id.map_sheet);
        sheetLogo = view.findViewById(R.id.sheet_logo);
        sheetTitle = view.findViewById(R.id.sheet_title);
        sheetSubtitle = view.findViewById(R.id.sheet_subtitle);
        sheetAltitude = view.findViewById(R.id.sheet_altitude);
        sheetSpeed = view.findViewById(R.id.sheet_speed);
        sheetHeading = view.findViewById(R.id.sheet_heading);
        viewFlightButton = view.findViewById(R.id.sheet_view_flight);
        view.findViewById(R.id.sheet_close).setOnClickListener(v -> clearSelection());

        BlurTarget target = view.findViewById(R.id.map_blur_target);
        setUpGlass(view.findViewById(R.id.map_status), target);
        setUpGlass(sheet, target);

        planeIcon = tintedPlane(R.color.brand_link);
        planeIconSelected = tintedPlane(R.color.brand_accent);

        if (savedInstanceState != null) {
            selectedIcao24 = savedInstanceState.getString(STATE_SELECTED);
            pendingMapState = savedInstanceState;
        }
        MapStyle.loadConfigThen(requireContext(), () -> {
            if (getView() != null) {
                createMap();
            }
        });
    }

    /** Real blur on Android 12+, a near-opaque fill below (software blur over a map is too slow). */
    private void setUpGlass(BlurView glass, BlurTarget target) {
        glass.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        glass.setClipToOutline(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            glass.setupWith(target).setBlurRadius(BLUR_RADIUS);
        } else {
            glass.setBackgroundResource(R.drawable.bg_blur_glass_fallback);
        }
    }

    private void createMap() {
        Context context = requireContext();
        mapView = new MapView(context);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setTilesScaledToDpi(true);
        MapStyle.applyNightStyle(mapView);

        Bundle state = pendingMapState;
        pendingMapState = null;
        if (state != null && state.containsKey(STATE_ZOOM)) {
            mapView.getController().setZoom(state.getDouble(STATE_ZOOM));
            mapView.getController().setCenter(new GeoPoint(state.getDouble(STATE_LAT), state.getDouble(STATE_LON)));
        } else {
            mapView.getController().setZoom(START_ZOOM);
            mapView.getController().setCenter(new GeoPoint(START_LAT, START_LON));
        }

        // Tapping empty map closes the aircraft sheet.
        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                clearSelection();
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
        // OpenStreetMap attribution, bottom right just above the floating tab bar.
        CopyrightOverlay copyright = new CopyrightOverlay(context);
        copyright.setTextColor(ContextCompat.getColor(context, R.color.text_hint));
        copyright.setAlignRight(true);
        copyright.setOffset((int) getResources().getDimension(R.dimen.space_m),
                (int) getResources().getDimension(R.dimen.nav_bar_clearance));
        mapView.getOverlays().add(copyright);

        FrameLayout container = requireView().findViewById(R.id.map_container);
        container.addView(mapView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addAirportMarkers();

        if (isResumed()) {
            mapView.onResume();
            if (!isHidden()) {
                startRefreshing();
            }
        }
    }

    // --- Lifecycle: refresh only while the tab is visible ---

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
            if (!isHidden()) {
                startRefreshing();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRefreshing();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            stopRefreshing();
        } else if (isResumed()) {
            startRefreshing();
        }
    }

    @Override
    public void onRefreshRequested() {
        loadAircraft();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED, selectedIcao24);
        if (mapView != null) {
            outState.putDouble(STATE_LAT, mapView.getMapCenter().getLatitude());
            outState.putDouble(STATE_LON, mapView.getMapCenter().getLongitude());
            outState.putDouble(STATE_ZOOM, mapView.getZoomLevelDouble());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopRefreshing();
        if (mapView != null) {
            mapView.onDetach();
            mapView = null;
        }
        aircraftMarkers.clear();
    }

    /** Refreshes now if the last data is older than the interval, then every 30 s. */
    private void startRefreshing() {
        if (mapView == null) {
            return;
        }
        handler.removeCallbacks(refreshTask);
        long sinceLast = SystemClock.elapsedRealtime() - lastLoadAt;
        handler.postDelayed(refreshTask, lastLoadAt == 0 ? 0 : Math.max(0, REFRESH_INTERVAL_MS - sinceLast));
    }

    private void stopRefreshing() {
        handler.removeCallbacks(refreshTask);
        repository.cancel(this);
    }

    // --- Data ---

    private void loadAircraft() {
        repository.getAircraft(this, new Callback<Result<List<AircraftState>>>() {
            @Override
            public void onSuccess(Result<List<AircraftState>> result) {
                if (mapView == null) {
                    return;
                }
                lastLoadAt = SystemClock.elapsedRealtime();
                setShowingMockData(result.isMock());
                showAircraft(result.getData());
            }

            @Override
            public void onError(ApiException error) {
                if (mapView == null) {
                    return;
                }
                Snackbar.make(requireView(), ErrorMessages.of(error), Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_retry, v -> loadAircraft())
                        .setAnchorView(requireActivity().findViewById(R.id.nav_bar))
                        .show();
            }
        });
    }

    /** Moves existing markers, adds new aircraft and removes those that left the area. */
    private void showAircraft(List<AircraftState> states) {
        Set<String> seen = new HashSet<>();
        aircraft.clear();
        for (AircraftState state : states) {
            String id = state.getIcao24();
            seen.add(id);
            aircraft.put(id, state);

            Marker marker = aircraftMarkers.get(id);
            if (marker == null) {
                marker = new Marker(mapView);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                marker.setFlat(true);
                marker.setInfoWindow(null);
                marker.setOnMarkerClickListener((m, map) -> {
                    select(id);
                    return true;
                });
                mapView.getOverlays().add(marker);
                aircraftMarkers.put(id, marker);
            }
            marker.setPosition(new GeoPoint(state.getLat(), state.getLon()));
            // The icon's nose points north; true_track is clockwise from north.
            marker.setRotation(state.getTrueTrack() != null ? state.getTrueTrack().floatValue() : 0f);
            marker.setIcon(id.equals(selectedIcao24) ? planeIconSelected : planeIcon);
            marker.setAlpha(state.isOnGround() ? 0.5f : 1f);
        }
        for (String id : new ArrayList<>(aircraftMarkers.keySet())) {
            if (!seen.contains(id)) {
                mapView.getOverlays().remove(aircraftMarkers.remove(id));
            }
        }

        countView.setText(getResources().getQuantityString(R.plurals.map_aircraft_count,
                states.size(), states.size()));
        updatedView.setText(getString(R.string.map_updated_at,
                DateFormat.getTimeFormat(requireContext()).format(new Date())));

        if (selectedIcao24 != null) {
            AircraftState selected = aircraft.get(selectedIcao24);
            if (selected != null) {
                bindSheet(selected);
                showSheet();
            } else {
                clearSelection();
            }
        }
        mapView.invalidate();
    }

    /** Small labelled dots for the airports; tapping one opens its departures board. */
    private void addAirportMarkers() {
        AirportStore.get(requireContext()).load(new Callback<List<Airport>>() {
            @Override
            public void onSuccess(List<Airport> airports) {
                if (mapView == null) {
                    return;
                }
                for (Airport airport : airports) {
                    Marker marker = new Marker(mapView);
                    marker.setPosition(new GeoPoint(airport.getLat(), airport.getLon()));
                    marker.setIcon(MapStyle.airportIcon(requireContext(), airport.getIata()));
                    // Anchor on the dot, at the left of the label.
                    marker.setAnchor(MapStyle.dotAnchorU(marker.getIcon(), requireContext()), Marker.ANCHOR_CENTER);
                    marker.setInfoWindow(null);
                    marker.setOnMarkerClickListener((m, map) -> {
                        startActivity(AirportBoardActivity.newIntent(requireContext(), airport.getIata()));
                        return true;
                    });
                    // Index 1: above the tap catcher, below the aircraft.
                    mapView.getOverlays().add(1, marker);
                }
                mapView.invalidate();
            }

            @Override
            public void onError(ApiException error) {
                // The map still works without airport dots.
            }
        });
    }

    // --- Selection sheet ---

    private void select(String icao24) {
        String previous = selectedIcao24;
        selectedIcao24 = icao24;
        setMarkerIcon(previous, planeIcon);
        setMarkerIcon(icao24, planeIconSelected);
        AircraftState state = aircraft.get(icao24);
        if (state != null) {
            bindSheet(state);
            showSheet();
        }
        mapView.invalidate();
    }

    private void clearSelection() {
        if (selectedIcao24 == null && sheet.getVisibility() != View.VISIBLE) {
            return;
        }
        setMarkerIcon(selectedIcao24, planeIcon);
        selectedIcao24 = null;
        hideSheet();
        if (mapView != null) {
            mapView.invalidate();
        }
    }

    private void setMarkerIcon(String icao24, Drawable icon) {
        Marker marker = icao24 != null ? aircraftMarkers.get(icao24) : null;
        if (marker != null) {
            marker.setIcon(icon);
        }
    }

    private void bindSheet(AircraftState state) {
        String callsign = state.getCallsign() != null ? state.getCallsign().trim() : "";
        if (callsign.isEmpty()) {
            callsign = state.getIcao24().toUpperCase(Locale.US);
        }
        String flightIata = Airlines.callsignToFlightIata(callsign);
        String airlineIata = flightIata != null ? flightIata.substring(0, 2) : null;

        sheetTitle.setText(flightIata != null && !flightIata.equals(callsign)
                ? flightIata + getString(R.string.separator_dot) + callsign : callsign);

        List<String> details = new ArrayList<>();
        if (airlineIata != null) {
            details.add(Airlines.name(airlineIata));
        }
        if (state.getOriginCountry() != null) {
            details.add(state.getOriginCountry());
        }
        Double vertical = state.getVerticalRate();
        if (vertical != null && Math.abs(vertical) >= LEVEL_FLIGHT_MS) {
            String rate = getString(R.string.format_vertical_rate, Math.abs(vertical));
            details.add(getString(vertical > 0 ? R.string.map_vertical_climb : R.string.map_vertical_descent, rate));
        }
        sheetSubtitle.setText(TextUtils.join(getString(R.string.separator_dot), details));

        sheetAltitude.setText(altitudeText(state));
        sheetSpeed.setText(state.getVelocity() != null
                ? getString(R.string.format_speed_kmh, UnitUtils.msToKmh(state.getVelocity()))
                : getString(R.string.placeholder_empty));
        sheetHeading.setText(state.getTrueTrack() != null
                ? getString(R.string.format_heading, Math.round(state.getTrueTrack()))
                : getString(R.string.placeholder_empty));

        sheetLogo.setDefaultImageResId(R.drawable.ic_flight);
        sheetLogo.setErrorImageResId(R.drawable.ic_flight);
        sheetLogo.setImageUrl(Airlines.logoUrl(airlineIata), ApiClient.get().getImageLoader());

        // "View flight" needs an IATA flight number, derived from the callsign's airline prefix.
        viewFlightButton.setEnabled(flightIata != null);
        viewFlightButton.setText(flightIata != null ? R.string.map_view_flight : R.string.map_no_flight_number);
        viewFlightButton.setOnClickListener(v ->
                startActivity(FlightDetailActivity.newIntent(requireContext(), flightIata)));
    }

    private String altitudeText(AircraftState state) {
        if (state.isOnGround()) {
            return getString(R.string.map_on_ground);
        }
        if (state.getBaroAltitude() == null) {
            return getString(R.string.placeholder_empty);
        }
        boolean feet = prefs.useFeet();
        return getString(feet ? R.string.format_altitude_ft : R.string.format_altitude_m,
                UnitUtils.altitude(state.getBaroAltitude(), feet));
    }

    private void showSheet() {
        if (sheet.getVisibility() == View.VISIBLE) {
            return;
        }
        float offset = getResources().getDimension(R.dimen.space_xl);
        sheet.setVisibility(View.VISIBLE);
        sheet.setAlpha(0f);
        sheet.setTranslationY(offset);
        sheet.animate().alpha(1f).translationY(0f).setDuration(200).start();
    }

    private void hideSheet() {
        if (sheet.getVisibility() != View.VISIBLE) {
            return;
        }
        float offset = getResources().getDimension(R.dimen.space_xl);
        sheet.animate().alpha(0f).translationY(offset).setDuration(150)
                .withEndAction(() -> sheet.setVisibility(View.GONE)).start();
    }

    // --- Icons ---

    private Drawable tintedPlane(int colorRes) {
        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_flight).mutate();
        icon.setTint(ContextCompat.getColor(requireContext(), colorRes));
        return icon;
    }
}
