package vn.edu.usth.flightinfo.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.ui.common.StatusUi;
import vn.edu.usth.flightinfo.util.Airlines;

/** Builds a pre-filled Calendar editor. No calendar permission or direct insertion needed. */
public final class CalendarHelper {
    private static final int DEFAULT_DURATION_MINUTES = 60;

    private CalendarHelper() { }

    /**
     * @throws IllegalArgumentException if the flight or its departure time is unavailable.
     * The caller should display a Snackbar instead of opening a broken event.
     */
    public static Intent buildIntent(Context context, Flight flight, Airport dep) {
        if (flight == null || flight.getBestDepTs() <= 0) {
            throw new IllegalArgumentException("Flight departure time is unavailable");
        }

        long depSeconds = flight.getBestDepTs();
        long arrSeconds = flight.getBestArrTs();
        if (arrSeconds <= depSeconds) {
            Integer durationMinutes = flight.getDuration();
            long minutes = durationMinutes != null && durationMinutes > 0
                    ? durationMinutes : DEFAULT_DURATION_MINUTES;
            arrSeconds = depSeconds + TimeUnit.MINUTES.toSeconds(minutes);
        }

        String title = context.getString(
                R.string.f9_calendar_title,
                valueOrEmpty(flight.getFlightIata()),
                valueOrEmpty(flight.getDepIata()),
                valueOrEmpty(flight.getArrIata()));

        List<String> details = new ArrayList<>();
        addIfPresent(details, Airlines.name(flight.getAirlineIata()));
        if (!TextUtils.isEmpty(flight.getDepTerminal())) {
            details.add(context.getString(R.string.f9_calendar_terminal, flight.getDepTerminal()));
        }
        if (!TextUtils.isEmpty(flight.getDepGate())) {
            details.add(context.getString(R.string.f9_calendar_gate, flight.getDepGate()));
        }
        if (flight.getStatus() != null) {
            details.add(context.getString(StatusUi.label(flight.getStatus())));
        }
        String description = TextUtils.join(
                context.getString(R.string.f9_calendar_separator), details);

        String location = dep != null ? valueOrEmpty(dep.getName())
                : valueOrEmpty(flight.getDepIata());

        return new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, TimeUnit.SECONDS.toMillis(depSeconds))
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, TimeUnit.SECONDS.toMillis(arrSeconds))
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                .putExtra(CalendarContract.Events.DESCRIPTION, description);
    }

    private static void addIfPresent(List<String> parts, String text) {
        if (!TextUtils.isEmpty(text)) {
            parts.add(text);
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
