package vn.edu.usth.flightinfo.model;

/** METAR observation for one airport (aviationweather.gov). Nullable fields are missing in the report. */
public class Weather {

    private final String icao;
    private final String stationName;
    private final Double temperatureC;
    private final Double dewPointC;
    /** Degrees, or null when variable / unknown. */
    private final Integer windDirection;
    private final Integer windSpeedKt;
    private final Integer windGustKt;
    /** Statute miles as reported, e.g. "6+" or "10". */
    private final String visibility;
    /** VFR / MVFR / IFR / LIFR, may be null. */
    private final String flightCategory;
    private final String rawText;

    public Weather(String icao, String stationName, Double temperatureC, Double dewPointC,
                   Integer windDirection, Integer windSpeedKt, Integer windGustKt,
                   String visibility, String flightCategory, String rawText) {
        this.icao = icao;
        this.stationName = stationName;
        this.temperatureC = temperatureC;
        this.dewPointC = dewPointC;
        this.windDirection = windDirection;
        this.windSpeedKt = windSpeedKt;
        this.windGustKt = windGustKt;
        this.visibility = visibility;
        this.flightCategory = flightCategory;
        this.rawText = rawText;
    }

    public String getIcao() { return icao; }
    public String getStationName() { return stationName; }
    public Double getTemperatureC() { return temperatureC; }
    public Double getDewPointC() { return dewPointC; }
    public Integer getWindDirection() { return windDirection; }
    public Integer getWindSpeedKt() { return windSpeedKt; }
    public Integer getWindGustKt() { return windGustKt; }
    public String getVisibility() { return visibility; }
    public String getFlightCategory() { return flightCategory; }
    public String getRawText() { return rawText; }
}
