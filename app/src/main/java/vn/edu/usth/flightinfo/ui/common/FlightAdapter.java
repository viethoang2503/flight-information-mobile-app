package vn.edu.usth.flightinfo.ui.common;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.NetworkImageView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.remote.ApiClient;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.util.Airlines;
import vn.edu.usth.flightinfo.util.TimeUtils;

/** RecyclerView adapter for flight rows (item_flight.xml). */
public class FlightAdapter extends RecyclerView.Adapter<FlightAdapter.ViewHolder> {

    public interface OnFlightClickListener {
        void onFlightClick(Flight flight);
    }

    private final List<Flight> flights = new ArrayList<>();
    private final boolean showArrivalTime;
    private final OnFlightClickListener listener;

    /**
     * @param showArrivalTime true to show the arrival time (arrivals board), false for departure time
     */
    public FlightAdapter(boolean showArrivalTime, OnFlightClickListener listener) {
        this.showArrivalTime = showArrivalTime;
        this.listener = listener;
    }

    public void submit(List<Flight> items) {
        flights.clear();
        flights.addAll(items);
        notifyDataSetChanged();
    }

    public Flight getItem(int position) {
        return flights.get(position);
    }

    public void removeAt(int position) {
        flights.remove(position);
        notifyItemRemoved(position);
    }

    public void insertAt(int position, Flight flight) {
        flights.add(position, flight);
        notifyItemInserted(position);
    }

    /** Replaces the row showing the same flight number (e.g. after a refresh), if there is one. */
    public void replace(Flight flight) {
        for (int i = 0; i < flights.size(); i++) {
            if (flights.get(i).getFlightIata().equals(flight.getFlightIata())) {
                flights.set(i, flight);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @Override
    public int getItemCount() {
        return flights.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flight, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Flight flight = flights.get(position);
        holder.code.setText(flight.getFlightIata());
        holder.airline.setText(Airlines.name(flight.getAirlineIata()));
        holder.route.setText(holder.itemView.getContext().getString(R.string.route_format,
                flight.getDepIata(), flight.getArrIata()));

        String scheduled = TimeUtils.hhmm(showArrivalTime ? flight.getArrTime() : flight.getDepTime());
        String best = TimeUtils.hhmm(showArrivalTime ? flight.getBestArrTime() : flight.getBestDepTime());
        holder.time.setText(best != null ? best : scheduled);
        if (scheduled != null && best != null && !scheduled.equals(best)) {
            holder.oldTime.setText(scheduled);
            holder.oldTime.setPaintFlags(holder.oldTime.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.oldTime.setVisibility(View.VISIBLE);
        } else {
            holder.oldTime.setVisibility(View.GONE);
        }

        StatusUi.bind(holder.status, flight.getStatus());
        holder.logo.setDefaultImageResId(R.drawable.ic_flight);
        holder.logo.setErrorImageResId(R.drawable.ic_flight);
        holder.logo.setImageUrl(Airlines.logoUrl(flight.getAirlineIata()), ApiClient.get().getImageLoader());

        holder.itemView.setOnClickListener(v -> listener.onFlightClick(flight));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final NetworkImageView logo;
        final TextView code;
        final TextView airline;
        final TextView route;
        final TextView time;
        final TextView oldTime;
        final TextView status;

        ViewHolder(View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.flight_logo);
            code = itemView.findViewById(R.id.flight_code);
            airline = itemView.findViewById(R.id.flight_airline);
            route = itemView.findViewById(R.id.flight_route);
            time = itemView.findViewById(R.id.flight_time);
            oldTime = itemView.findViewById(R.id.flight_time_old);
            status = itemView.findViewById(R.id.flight_status);
        }
    }
}
