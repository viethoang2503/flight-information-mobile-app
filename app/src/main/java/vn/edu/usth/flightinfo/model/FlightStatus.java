package vn.edu.usth.flightinfo.model;

/**
 * App-level flight status. AirLabs reports
 * {@code scheduled | active | en-route | landed | cancelled}; a scheduled flight
 * departing {@link #DELAY_THRESHOLD_MINUTES} or more late is shown as DELAYED.
 */
public enum FlightStatus {
    SCHEDULED,
    DELAYED,
    EN_ROUTE,
    LANDED,
    CANCELLED,
    UNKNOWN;

    public static final int DELAY_THRESHOLD_MINUTES = 15;

    public static FlightStatus from(String rawStatus, Integer delayMinutes) {
        if (rawStatus == null) {
            return UNKNOWN;
        }
        switch (rawStatus.trim().toLowerCase()) {
            case "cancelled":
                return CANCELLED;
            case "landed":
                return LANDED;
            case "active":
            case "en-route":
                return EN_ROUTE;
            case "scheduled":
                return delayMinutes != null && delayMinutes >= DELAY_THRESHOLD_MINUTES
                        ? DELAYED : SCHEDULED;
            default:
                return UNKNOWN;
        }
    }
}
