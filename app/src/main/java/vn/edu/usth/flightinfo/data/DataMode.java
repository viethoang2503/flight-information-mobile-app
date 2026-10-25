package vn.edu.usth.flightinfo.data;

/** The "Data source" setting. */
public enum DataMode {
    /** Live API, falling back to mock data when the API fails. */
    AUTO,
    /** Live API only; errors are shown to the user. */
    LIVE,
    /** Mock data only, no network (for demos). */
    MOCK;

    /** Maps the preference value ("auto" / "live" / "mock") to a mode, defaulting to AUTO. */
    public static DataMode fromPreference(String value) {
        if ("live".equals(value)) return LIVE;
        if ("mock".equals(value)) return MOCK;
        return AUTO;
    }
}
