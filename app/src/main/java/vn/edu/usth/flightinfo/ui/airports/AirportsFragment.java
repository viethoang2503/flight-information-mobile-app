package vn.edu.usth.flightinfo.ui.airports;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.local.AirportStore;
import vn.edu.usth.flightinfo.data.local.Prefs;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.ui.TabFragment;

/**
 * Tab 2: airports from assets/airports.json (read on a worker thread), with a filter box.
 * The default airport chosen in Settings is pinned to the top.
 */
public class AirportsFragment extends TabFragment {

    private Prefs prefs;
    private AirportAdapter adapter;
    private EditText filter;
    private TextView emptyView;
    private List<Airport> allAirports = new ArrayList<>();

    @Override
    public int getTitleRes() {
        return R.string.tab_airports;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        prefs = new Prefs(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_airports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        filter = view.findViewById(R.id.airport_filter);
        emptyView = view.findViewById(R.id.airport_empty);

        adapter = new AirportAdapter(airport -> startActivity(
                AirportBoardActivity.newIntent(requireContext(), airport.getIata())));
        RecyclerView list = view.findViewById(R.id.airport_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        AirportStore.get(requireContext()).load(new Callback<List<Airport>>() {
            @Override
            public void onSuccess(List<Airport> airports) {
                allAirports = airports;
                if (isAdded()) {
                    applyFilter();
                }
            }

            @Override
            public void onError(ApiException error) {
                if (isAdded()) {
                    emptyView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // The default airport may have changed in Settings.
        applyFilter();
    }

    private void applyFilter() {
        if (adapter == null) {
            return;
        }
        String query = filter.getText().toString();
        String defaultIata = prefs.getDefaultAirport();
        List<Airport> shown = new ArrayList<>();
        for (Airport airport : allAirports) {
            if (airport.matches(query)) {
                if (airport.getIata().equals(defaultIata)) {
                    shown.add(0, airport);
                } else {
                    shown.add(airport);
                }
            }
        }
        adapter.submit(shown, defaultIata);
        emptyView.setVisibility(shown.isEmpty() && !allAirports.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
