package vn.edu.usth.flightinfo.ui.airports;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.FlightRepository;
import vn.edu.usth.flightinfo.data.parser.AirLabsParser;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.model.Result;
import vn.edu.usth.flightinfo.ui.common.ErrorMessages;
import vn.edu.usth.flightinfo.ui.common.FlightAdapter;
import vn.edu.usth.flightinfo.ui.detail.FlightDetailActivity;

/** One page of the airport board: the departures or the arrivals list, with pull-to-refresh. */
public class BoardFragment extends Fragment {

    /** Implemented by AirportBoardActivity to show the mock banner for the visible page. */
    public interface Host {
        void onBoardLoaded(boolean departures, boolean mock);
    }

    private static final String ARG_AIRPORT = "airport";
    private static final String ARG_DEPARTURES = "departures";
    private static final String STATE_FLIGHTS = "flights";
    private static final String STATE_MOCK = "mock";

    private FlightRepository repository;
    private String airportIata;
    private boolean departures;

    /** Null until the first successful load. */
    private List<Flight> flights;
    private boolean isMock;

    private SwipeRefreshLayout refreshLayout;
    private FlightAdapter adapter;
    private View stateView;
    private TextView stateText;
    private Button retryButton;

    public static BoardFragment newInstance(String airportIata, boolean departures) {
        BoardFragment fragment = new BoardFragment();
        Bundle args = new Bundle();
        args.putString(ARG_AIRPORT, airportIata);
        args.putBoolean(ARG_DEPARTURES, departures);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        repository = FlightRepository.get(context);
        airportIata = requireArguments().getString(ARG_AIRPORT);
        departures = requireArguments().getBoolean(ARG_DEPARTURES);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_board, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        refreshLayout = view.findViewById(R.id.board_refresh);
        stateView = view.findViewById(R.id.board_state);
        stateText = view.findViewById(R.id.board_state_text);
        retryButton = view.findViewById(R.id.board_retry);

        refreshLayout.setColorSchemeResources(R.color.brand_accent);
        refreshLayout.setOnRefreshListener(this::load);
        retryButton.setOnClickListener(v -> load());

        adapter = new FlightAdapter(!departures, flight -> startActivity(
                FlightDetailActivity.newIntent(requireContext(), flight, isMock, false)));
        RecyclerView list = view.findViewById(R.id.board_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        // After rotation, show the saved list instead of calling the API again.
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_FLIGHTS)) {
            flights = fromJson(savedInstanceState.getStringArrayList(STATE_FLIGHTS));
            isMock = savedInstanceState.getBoolean(STATE_MOCK);
            showFlights();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (flights == null) {
            load();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        repository.cancel(this);
        refreshLayout.setRefreshing(false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (flights != null) {
            ArrayList<String> json = new ArrayList<>();
            for (Flight f : flights) {
                json.add(f.getRawJson());
            }
            outState.putStringArrayList(STATE_FLIGHTS, json);
            outState.putBoolean(STATE_MOCK, isMock);
        }
    }

    /** Reload from the toolbar's Refresh action. */
    public void refresh() {
        if (getView() != null) {
            load();
        }
    }

    private void load() {
        refreshLayout.setRefreshing(true);
        stateView.setVisibility(View.GONE);
        repository.getSchedules(airportIata, departures, this, new Callback<Result<List<Flight>>>() {
            @Override
            public void onSuccess(Result<List<Flight>> result) {
                if (getView() == null) {
                    return;
                }
                refreshLayout.setRefreshing(false);
                flights = sortByTime(result.getData());
                isMock = result.isMock();
                showFlights();
            }

            @Override
            public void onError(ApiException error) {
                if (getView() == null) {
                    return;
                }
                refreshLayout.setRefreshing(false);
                if (flights == null || flights.isEmpty()) {
                    stateText.setText(ErrorMessages.of(error));
                    retryButton.setVisibility(View.VISIBLE);
                    stateView.setVisibility(View.VISIBLE);
                } else {
                    Snackbar.make(refreshLayout, ErrorMessages.of(error), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showFlights() {
        adapter.submit(flights);
        boolean empty = flights.isEmpty();
        stateText.setText(departures ? R.string.board_empty_departures : R.string.board_empty_arrivals);
        retryButton.setVisibility(View.GONE);
        stateView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (getActivity() instanceof Host) {
            ((Host) getActivity()).onBoardLoaded(departures, isMock);
        }
    }

    private List<Flight> sortByTime(List<Flight> input) {
        List<Flight> sorted = new ArrayList<>(input);
        Collections.sort(sorted, departures
                ? Comparator.comparingLong(Flight::getDepTimeTs)
                : Comparator.comparingLong(Flight::getArrTimeTs));
        return sorted;
    }

    private static List<Flight> fromJson(List<String> jsonList) {
        List<Flight> result = new ArrayList<>();
        if (jsonList == null) {
            return result;
        }
        for (String json : jsonList) {
            try {
                result.add(AirLabsParser.parseFlight(new JSONObject(json)));
            } catch (JSONException ignored) {
                // Skip a broken entry.
            }
        }
        return result;
    }
}
