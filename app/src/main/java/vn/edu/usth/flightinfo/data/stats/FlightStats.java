package vn.edu.usth.flightinfo.data.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.model.FlightStatus;
import vn.edu.usth.flightinfo.util.GeoUtils;
import vn.edu.usth.flightinfo.util.Airlines;

/** Pure-Java summary of the user's saved flights. */
public final class FlightStats {
    private final int flightCount;
    private final double totalDistanceKm;
    private final double totalHours;
    private final double onTimePercent;
    private final int onTimeBase;
    private final List<AirlineCount> airlines;
    private final String busiestDepartureIata;
    private final int busiestDepartureCount;

    private FlightStats(int count, double distance, double hours, double onTime, int onTimeBase,
                        List<AirlineCount> airlines, String airport, int airportCount) {
        this.flightCount = count;
        this.totalDistanceKm = distance;
        this.totalHours = hours;
        this.onTimePercent = onTime;
        this.onTimeBase = onTimeBase;
        this.airlines = Collections.unmodifiableList(airlines);
        this.busiestDepartureIata = airport;
        this.busiestDepartureCount = airportCount;
    }

    public static FlightStats from(List<Flight> flights, Map<String, Airport> airports) {
        List<Flight> safeFlights = flights == null ? Collections.emptyList() : flights;
        Map<String, Airport> safeAirports = airports == null ? Collections.emptyMap() : airports;
        double distance = 0.0;
        double minutes = 0.0;
        int eligible = 0;
        int onTime = 0;
        Map<String, Integer> airlineCounts = new HashMap<>();
        Map<String, Integer> departureCounts = new HashMap<>();

        for (Flight flight : safeFlights) {
            if (flight == null) continue;
            String dep = normalize(flight.getDepIata());
            String arr = normalize(flight.getArrIata());
            if (dep != null) departureCounts.put(dep, departureCounts.getOrDefault(dep, 0) + 1);

            Airport from = dep == null ? null : safeAirports.get(dep);
            Airport to = arr == null ? null : safeAirports.get(arr);
            if (from != null && to != null) {
                distance += GeoUtils.distanceKm(from.getLat(), from.getLon(), to.getLat(), to.getLon());
            }

            Integer duration = flight.getDuration();
            if (duration != null && duration > 0) {
                minutes += duration;
            } else {
                long depTs = flight.getBestDepTs();
                long arrTs = flight.getBestArrTs();
                if (depTs > 0 && arrTs > depTs) minutes += (arrTs - depTs) / 60.0;
            }

            // Only flights that have actually departed have a known punctuality: a future
            // flight with no delay yet must not count as "on time".
            FlightStatus status = flight.getStatus();
            if (status == FlightStatus.EN_ROUTE || status == FlightStatus.LANDED) {
                eligible++;
                Integer delay = flight.getDelayMinutes();
                if (delay == null || delay < FlightStatus.DELAY_THRESHOLD_MINUTES) onTime++;
            }

            String airline = normalize(flight.getAirlineIata());
            if (airline == null) {
                String number = normalize(flight.getFlightIata());
                if (number != null) airline = number.replaceAll("[0-9].*$", "");
            }
            if (airline != null && !airline.isEmpty()) {
                airlineCounts.put(airline, airlineCounts.getOrDefault(airline, 0) + 1);
            }
        }

        List<AirlineCount> ranked = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : airlineCounts.entrySet()) {
            ranked.add(new AirlineCount(entry.getKey(), entry.getValue()));
        }
        ranked.sort((a, b) -> {
            int byCount = Integer.compare(b.count, a.count);
            return byCount != 0 ? byCount : a.code.compareTo(b.code);
        });
        if (ranked.size() > 5) {
            int other = 0;
            for (int i = 5; i < ranked.size(); i++) other += ranked.get(i).count;
            ranked = new ArrayList<>(ranked.subList(0, 5));
            ranked.add(new AirlineCount("OTHER", other));
        }

        String busiest = null;
        int busiestCount = 0;
        for (Map.Entry<String, Integer> entry : departureCounts.entrySet()) {
            if (entry.getValue() > busiestCount
                    || (entry.getValue() == busiestCount && (busiest == null || entry.getKey().compareTo(busiest) < 0))) {
                busiest = entry.getKey();
                busiestCount = entry.getValue();
            }
        }
        double punctuality = eligible == 0 ? 0.0 : 100.0 * onTime / eligible;
        return new FlightStats(safeFlights.size(), distance, minutes / 60.0, punctuality, eligible,
                ranked, busiest, busiestCount);
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public int getFlightCount() { return flightCount; }
    public double getTotalDistanceKm() { return totalDistanceKm; }
    public double getTotalHours() { return totalHours; }
    public double getOnTimePercent() { return onTimePercent; }
    /** False when no saved flight has departed yet, so the on-time rate means nothing. */
    public boolean hasOnTimeData() { return onTimeBase > 0; }
    public List<AirlineCount> getAirlines() { return airlines; }
    public String getBusiestDepartureIata() { return busiestDepartureIata; }
    public int getBusiestDepartureCount() { return busiestDepartureCount; }

    public static final class AirlineCount {
        private final String code;
        private final int count;
        public AirlineCount(String code, int count) { this.code = code; this.count = count; }
        public String getCode() { return code; }
        public int getCount() { return count; }
    }
}
