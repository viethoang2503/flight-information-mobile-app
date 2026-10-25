package vn.edu.usth.flightinfo.data.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import vn.edu.usth.flightinfo.model.AircraftState;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.util.GeoUtils;

/** Pure-Java lookups over mock data (no Android dependencies, so they are unit tested directly). */
final class MockQueries {

    private MockQueries() {
    }

    /** The flight with this number; if several match, the one departing closest to now. */
    static Flight findFlight(List<Flight> flights, String flightIata, long nowTs) {
        Flight best = null;
        String wanted = flightIata == null ? "" : flightIata.trim().toUpperCase(Locale.US);
        for (Flight f : flights) {
            if (!wanted.equals(f.getFlightIata())) {
                continue;
            }
            if (best == null || Math.abs(f.getDepTimeTs() - nowTs) < Math.abs(best.getDepTimeTs() - nowTs)) {
                best = f;
            }
        }
        return best;
    }

    /** Departures or arrivals of one airport, ordered by scheduled time. */
    static List<Flight> filterSchedules(List<Flight> flights, String airportIata, boolean departures) {
        List<Flight> result = new ArrayList<>();
        for (Flight f : flights) {
            String airport = departures ? f.getDepIata() : f.getArrIata();
            if (airport != null && airport.equalsIgnoreCase(airportIata)) {
                result.add(f);
            }
        }
        Comparator<Flight> byTime = departures
                ? Comparator.comparingLong(Flight::getDepTimeTs)
                : Comparator.comparingLong(Flight::getArrTimeTs);
        Collections.sort(result, byTime);
        return result;
    }

    /** Moves airborne aircraft along their track as if {@code elapsedSeconds} had passed. */
    static List<AircraftState> moveAircraft(List<AircraftState> states, long elapsedSeconds) {
        List<AircraftState> moved = new ArrayList<>(states.size());
        for (AircraftState s : states) {
            if (s.isOnGround() || s.getVelocity() == null || s.getTrueTrack() == null) {
                moved.add(s);
                continue;
            }
            double[] p = GeoUtils.move(s.getLat(), s.getLon(), s.getTrueTrack(),
                    s.getVelocity() * elapsedSeconds);
            moved.add(s.withPosition(p[0], p[1]));
        }
        return moved;
    }
}
