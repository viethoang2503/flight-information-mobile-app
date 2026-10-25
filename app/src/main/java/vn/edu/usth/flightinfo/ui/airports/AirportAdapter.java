package vn.edu.usth.flightinfo.ui.airports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.ui.common.LocaleUtils;
import vn.edu.usth.flightinfo.ui.common.StatusUi;

/** Rows of item_airport.xml. */
public class AirportAdapter extends RecyclerView.Adapter<AirportAdapter.ViewHolder> {

    public interface OnAirportClickListener {
        void onAirportClick(Airport airport);
    }

    private final List<Airport> airports = new ArrayList<>();
    private final OnAirportClickListener listener;
    private String defaultIata;

    public AirportAdapter(OnAirportClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Airport> items, String defaultIata) {
        airports.clear();
        airports.addAll(items);
        this.defaultIata = defaultIata;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return airports.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_airport, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Airport airport = airports.get(position);
        boolean vietnamese = LocaleUtils.isVietnamese(holder.itemView.getContext());
        // Country name localized from its ISO code, e.g. "VN" -> "Vietnam" / "Việt Nam".
        Locale uiLocale = holder.itemView.getResources().getConfiguration().getLocales().get(0);
        String country = new Locale.Builder().setRegion(airport.getCountry()).build()
                .getDisplayCountry(uiLocale);

        holder.code.setText(airport.getIata());
        holder.city.setText(holder.itemView.getContext().getString(R.string.board_city_country,
                airport.getCity(vietnamese), country));
        holder.name.setText(airport.getName());

        boolean isDefault = airport.getIata().equals(defaultIata);
        holder.defaultChip.setVisibility(isDefault ? View.VISIBLE : View.GONE);
        if (isDefault) {
            StatusUi.tint(holder.defaultChip, R.color.brand_link);
        }
        holder.itemView.setOnClickListener(v -> listener.onAirportClick(airport));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView code;
        final TextView city;
        final TextView name;
        final TextView defaultChip;

        ViewHolder(View itemView) {
            super(itemView);
            code = itemView.findViewById(R.id.airport_code);
            city = itemView.findViewById(R.id.airport_city);
            name = itemView.findViewById(R.id.airport_name);
            defaultChip = itemView.findViewById(R.id.airport_default);
        }
    }
}
