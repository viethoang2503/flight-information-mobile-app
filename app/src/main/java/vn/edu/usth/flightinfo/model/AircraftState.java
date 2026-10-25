package vn.edu.usth.flightinfo.model;

/** One aircraft position from OpenSky's /states/all. SI units: meters, m/s, degrees. */
public class AircraftState {

    private final String icao24;
    private final String callsign;
    private final String originCountry;
    private final double lat;
    private final double lon;
    private final Double baroAltitude;
    private final boolean onGround;
    private final Double velocity;
    private final Double trueTrack;
    private final Double verticalRate;

    public AircraftState(String icao24, String callsign, String originCountry, double lat, double lon,
                         Double baroAltitude, boolean onGround, Double velocity, Double trueTrack,
                         Double verticalRate) {
        this.icao24 = icao24;
        this.callsign = callsign;
        this.originCountry = originCountry;
        this.lat = lat;
        this.lon = lon;
        this.baroAltitude = baroAltitude;
        this.onGround = onGround;
        this.velocity = velocity;
        this.trueTrack = trueTrack;
        this.verticalRate = verticalRate;
    }

    /** Copy of this state at a new position (used to animate mock aircraft). */
    public AircraftState withPosition(double newLat, double newLon) {
        return new AircraftState(icao24, callsign, originCountry, newLat, newLon,
                baroAltitude, onGround, velocity, trueTrack, verticalRate);
    }

    public String getIcao24() { return icao24; }
    public String getCallsign() { return callsign; }
    public String getOriginCountry() { return originCountry; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public Double getBaroAltitude() { return baroAltitude; }
    public boolean isOnGround() { return onGround; }
    public Double getVelocity() { return velocity; }
    public Double getTrueTrack() { return trueTrack; }
    public Double getVerticalRate() { return verticalRate; }
}
