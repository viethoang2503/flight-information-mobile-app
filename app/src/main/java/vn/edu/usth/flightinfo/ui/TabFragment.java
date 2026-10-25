package vn.edu.usth.flightinfo.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

/**
 * Base class of the four bottom-navigation tabs. A tab reports whether it currently shows mock data
 * (so MainActivity can show the banner) and can react to the toolbar's Refresh action.
 */
public abstract class TabFragment extends Fragment {

    /** Implemented by the activity hosting the tabs. */
    public interface Host {
        void onTabDataSourceChanged(TabFragment tab);
    }

    private static final String STATE_MOCK = "showing_mock";

    private boolean showingMockData;

    @StringRes
    public abstract int getTitleRes();

    /** Called when the user taps Refresh in the toolbar while this tab is visible. */
    public void onRefreshRequested() {
    }

    public boolean isShowingMockData() {
        return showingMockData;
    }

    protected void setShowingMockData(boolean mock) {
        showingMockData = mock;
        if (getActivity() instanceof Host) {
            ((Host) getActivity()).onTabDataSourceChanged(this);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            showingMockData = savedInstanceState.getBoolean(STATE_MOCK);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MOCK, showingMockData);
    }
}
