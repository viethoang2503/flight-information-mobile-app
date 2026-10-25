package vn.edu.usth.flightinfo.ui.airports;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/** Two pages: departures (0) and arrivals (1), each a BoardFragment. */
public class BoardPagerAdapter extends FragmentStateAdapter {

    public static final int PAGE_DEPARTURES = 0;
    public static final int PAGE_ARRIVALS = 1;
    private static final int PAGE_COUNT = 2;

    private final String airportIata;

    public BoardPagerAdapter(@NonNull FragmentActivity activity, String airportIata) {
        super(activity);
        this.airportIata = airportIata;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return BoardFragment.newInstance(airportIata, position == PAGE_DEPARTURES);
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
