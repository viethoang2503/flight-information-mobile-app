package vn.edu.usth.flightinfo.ui.search;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.data.Callback;
import vn.edu.usth.flightinfo.data.FlightRepository;
import vn.edu.usth.flightinfo.data.local.RecentSearches;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.model.Result;
import vn.edu.usth.flightinfo.ui.TabFragment;
import vn.edu.usth.flightinfo.ui.common.ErrorMessages;
import vn.edu.usth.flightinfo.ui.common.FlightAdapter;
import vn.edu.usth.flightinfo.ui.detail.FlightDetailActivity;

/** Tab 1: search a flight by number, plus the list of recent searches. */
public class SearchFragment extends TabFragment {

    /** Two-character airline code + 1 to 4 digits, e.g. VN215, VJ1234, 9G55. */
    private static final Pattern FLIGHT_NUMBER = Pattern.compile("^[A-Z0-9]{2}\\d{1,4}$");

    private FlightRepository repository;
    private RecentSearches recentSearches;
    private FlightAdapter recentAdapter;

    private TextInputLayout inputLayout;
    private EditText input;
    private Button searchButton;
    private ProgressBar progress;
    private TextView emptyView;
    private TextView clearButton;

    @Override
    public int getTitleRes() {
        return R.string.tab_search;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        repository = FlightRepository.get(context);
        recentSearches = new RecentSearches(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        inputLayout = view.findViewById(R.id.search_input_layout);
        input = view.findViewById(R.id.search_input);
        searchButton = view.findViewById(R.id.search_button);
        progress = view.findViewById(R.id.search_progress);
        emptyView = view.findViewById(R.id.recent_empty);
        clearButton = view.findViewById(R.id.clear_recent_button);

        searchButton.setOnClickListener(v -> search());
        input.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_SEARCH || enter) {
                search();
                return true;
            }
            return false;
        });
        clearButton.setOnClickListener(v -> {
            recentSearches.clear();
            showRecent();
        });

        // Opening a recent flight shows the saved copy right away, then refreshes it.
        recentAdapter = new FlightAdapter(false, flight -> startActivity(
                FlightDetailActivity.newIntent(requireContext(), flight, isShowingMockData(), true)));
        RecyclerView list = view.findViewById(R.id.recent_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(recentAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        showRecent();
    }

    @Override
    public void onStop() {
        super.onStop();
        repository.cancel(this);
        setLoading(false);
    }

    private void showRecent() {
        List<Flight> recent = recentSearches.getAll();
        recentAdapter.submit(recent);
        emptyView.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
        clearButton.setVisibility(recent.isEmpty() ? View.INVISIBLE : View.VISIBLE);
    }

    private void search() {
        String code = input.getText().toString().replaceAll("\\s", "").toUpperCase(Locale.US);
        if (!FLIGHT_NUMBER.matcher(code).matches()) {
            inputLayout.setError(getString(R.string.error_invalid_flight));
            return;
        }
        inputLayout.setError(null);
        hideKeyboard();
        setLoading(true);

        repository.getFlight(code, this, new Callback<Result<Flight>>() {
            @Override
            public void onSuccess(Result<Flight> result) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                setShowingMockData(result.isMock());
                recentSearches.add(result.getData());
                startActivity(FlightDetailActivity.newIntent(requireContext(), result.getData(),
                        result.isMock(), false));
            }

            @Override
            public void onError(ApiException error) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                Snackbar.make(requireView(), ErrorMessages.of(error), Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_retry, v -> search())
                        // Float above the glass tab bar instead of hiding under it.
                        .setAnchorView(requireActivity().findViewById(R.id.nav_bar))
                        .show();
            }
        });
    }

    private void setLoading(boolean loading) {
        if (progress == null) {
            return;
        }
        progress.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
        searchButton.setEnabled(!loading);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
    }
}
