package vn.edu.usth.flightinfo.ui.myflights;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.FlightRepository;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.model.FlightStatus;
import vn.edu.usth.flightinfo.model.Result;
import vn.edu.usth.flightinfo.ui.TabFragment;
import vn.edu.usth.flightinfo.ui.common.ErrorMessages;
import vn.edu.usth.flightinfo.ui.common.FlightAdapter;
import vn.edu.usth.flightinfo.ui.detail.FlightDetailActivity;

/**
 * Tab 4: flights saved with the star (SQLite). The list is read again each time the tab is shown,
 * then flights that are not finished yet are refreshed from the network, at most once a minute.
 * Swiping a row removes it; the row is deleted from the database only once the Undo Snackbar is gone.
 */
public class MyFlightsFragment extends TabFragment {

    private static final long REFRESH_INTERVAL_MS = 60_000;

    private FlightRepository repository;
    private FlightAdapter adapter;
    private SwipeRefreshLayout refreshLayout;
    private View emptyView;
    private View statsCard;

    private long lastRefreshAt;
    /** Flight numbers covered by the last refresh: a newly saved flight triggers a new one. */
    private final Set<String> refreshedIatas = new HashSet<>();
    /** Ignores callbacks of an older refresh round (mock callbacks cannot be cancelled). */
    private int refreshRound;
    private int sentRequests;
    private int pendingRequests;
    private int failedRequests;
    private boolean anyMock;
    private ApiException lastError;

    /** Swiped away, Undo still possible: not deleted from the database yet. */
    private Flight pendingDelete;
    /** Removed rows (pending or being deleted) that a reload must not bring back. */
    private final Set<String> hiddenIatas = new HashSet<>();

    @Override
    public int getTitleRes() {
        return R.string.tab_my_flights;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        repository = FlightRepository.get(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_flights, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        refreshLayout = view.findViewById(R.id.my_flights_refresh);
        refreshLayout.setColorSchemeResources(R.color.brand_link);
        refreshLayout.setProgressBackgroundColorSchemeResource(R.color.surface);
        refreshLayout.setOnRefreshListener(() -> loadSaved(true));
        emptyView = view.findViewById(R.id.my_flights_empty);
        statsCard = view.findViewById(R.id.my_flights_stats_card);
        statsCard.setOnClickListener(v -> startActivity(
                new android.content.Intent(requireContext(),
                        vn.edu.usth.flightinfo.ui.stats.StatsActivity.class)));

        adapter = new FlightAdapter(false, flight -> startActivity(
                FlightDetailActivity.newIntent(requireContext(), flight, isShowingMockData(), false)));
        RecyclerView list = view.findViewById(R.id.my_flights_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder from,
                                  @NonNull RecyclerView.ViewHolder to) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
                removeWithUndo(holder.getBindingAdapterPosition());
            }
        }).attachToRecyclerView(list);
    }

    // Tabs are shown/hidden, not replaced: reload both when resumed and when shown again.
    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            loadSaved(false);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isResumed()) {
            loadSaved(false);
        }
    }

    @Override
    public void onRefreshRequested() {
        if (refreshLayout != null) {
            refreshLayout.setRefreshing(true);
            loadSaved(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        repository.cancel(this);
        refreshRound++;
        refreshLayout.setRefreshing(false);
        commitPendingDelete();
    }

    /** Reads the saved flights, then refreshes them if forced or if the last refresh is old. */
    private void loadSaved(boolean forceRefresh) {
        repository.getSavedFlights(new Callback<List<Flight>>() {
            @Override
            public void onSuccess(List<Flight> flights) {
                if (getView() == null) {
                    return;
                }
                List<Flight> visible = new ArrayList<>();
                boolean hasNewFlight = false;
                for (Flight flight : flights) {
                    if (!hiddenIatas.contains(flight.getFlightIata())) {
                        visible.add(flight);
                        hasNewFlight |= !refreshedIatas.contains(flight.getFlightIata());
                    }
                }
                adapter.submit(visible);
                updateEmptyState();

                boolean due = SystemClock.elapsedRealtime() - lastRefreshAt >= REFRESH_INTERVAL_MS;
                if (forceRefresh || hasNewFlight || lastRefreshAt == 0 || due) {
                    refreshStatuses(visible);
                } else {
                    refreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onError(ApiException error) {
                // Reading SQLite does not report errors through this callback.
            }
        });
    }

    private void refreshStatuses(List<Flight> flights) {
        lastRefreshAt = SystemClock.elapsedRealtime();
        repository.cancel(this);
        int round = ++refreshRound;
        pendingRequests = 0;
        failedRequests = 0;
        anyMock = false;
        lastError = null;
        refreshedIatas.clear();

        for (Flight saved : flights) {
            refreshedIatas.add(saved.getFlightIata());
            // A landed or cancelled flight will not change: save the API quota.
            if (isFinal(saved.getStatus())) {
                continue;
            }
            pendingRequests++;
            repository.getFlight(saved.getFlightIata(), this, new Callback<Result<Flight>>() {
                @Override
                public void onSuccess(Result<Flight> result) {
                    if (round != refreshRound || getView() == null) {
                        return;
                    }
                    Flight fresh = result.getData();
                    repository.updateSaved(fresh);
                    if (!hiddenIatas.contains(fresh.getFlightIata())) {
                        adapter.replace(fresh);
                    }
                    anyMock |= result.isMock();
                    onRequestDone();
                }

                @Override
                public void onError(ApiException error) {
                    if (round != refreshRound || getView() == null) {
                        return;
                    }
                    failedRequests++;
                    lastError = error;
                    onRequestDone();
                }
            });
        }
        sentRequests = pendingRequests;
        if (pendingRequests == 0) {
            refreshLayout.setRefreshing(false);
        }
    }

    private void onRequestDone() {
        if (--pendingRequests > 0) {
            return;
        }
        refreshLayout.setRefreshing(false);
        setShowingMockData(anyMock);
        // The saved copies stay on screen; only say something if nothing could be updated.
        if (lastError != null && failedRequests == sentRequests) {
            showSnackbar(Snackbar.make(requireView(), ErrorMessages.of(lastError), Snackbar.LENGTH_LONG));
        }
    }

    private void removeWithUndo(int position) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        // Only one Undo at a time: the previous removal becomes final.
        commitPendingDelete();

        Flight flight = adapter.getItem(position);
        String iata = flight.getFlightIata();
        adapter.removeAt(position);
        hiddenIatas.add(iata);
        pendingDelete = flight;
        updateEmptyState();

        Snackbar snackbar = Snackbar.make(requireView(),
                        getString(R.string.my_flights_removed, iata), Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo, v -> {
                    if (pendingDelete != flight) {
                        return;
                    }
                    pendingDelete = null;
                    hiddenIatas.remove(iata);
                    adapter.insertAt(Math.min(position, adapter.getItemCount()), flight);
                    updateEmptyState();
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar bar, int event) {
                        if (event != DISMISS_EVENT_ACTION && pendingDelete == flight) {
                            commitPendingDelete();
                        }
                    }
                });
        showSnackbar(snackbar);
    }

    /** Writes the pending removal to the database; the row stays hidden until it is done. */
    private void commitPendingDelete() {
        if (pendingDelete == null) {
            return;
        }
        Flight flight = pendingDelete;
        pendingDelete = null;
        repository.setSaved(flight, false, () -> hiddenIatas.remove(flight.getFlightIata()));
    }

    private void showSnackbar(Snackbar snackbar) {
        // Float above the glass tab bar instead of hiding under it.
        snackbar.setAnchorView(requireActivity().findViewById(R.id.nav_bar)).show();
    }

    private void updateEmptyState() {
        boolean empty = adapter.getItemCount() == 0;
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (statsCard != null) statsCard.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private static boolean isFinal(FlightStatus status) {
        return status == FlightStatus.LANDED || status == FlightStatus.CANCELLED;
    }
}
