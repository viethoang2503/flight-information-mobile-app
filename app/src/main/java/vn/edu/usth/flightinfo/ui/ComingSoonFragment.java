package vn.edu.usth.flightinfo.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import vn.edu.usth.flightinfo.R;

/** Temporary content for tabs that are built in later phases (Airports, Map, My Flights). */
public class ComingSoonFragment extends TabFragment {

    private static final String ARG_TITLE = "title";

    public static ComingSoonFragment newInstance(@StringRes int titleRes) {
        ComingSoonFragment fragment = new ComingSoonFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TITLE, titleRes);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getTitleRes() {
        return requireArguments().getInt(ARG_TITLE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_coming_soon, container, false);
    }
}
