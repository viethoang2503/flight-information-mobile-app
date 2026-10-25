package vn.edu.usth.flightinfo.util;

/** Small-distance geometry, good enough to move mock aircraft along their heading. */
public final class GeoUtils {

    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    private GeoUtils() {
    }

    /** Returns {lat, lon} after moving {@code distanceMeters} along {@code bearingDeg} (0 = north). */
    public static double[] move(double lat, double lon, double bearingDeg, double distanceMeters) {
        double bearing = Math.toRadians(bearingDeg);
        double dLat = distanceMeters * Math.cos(bearing) / METERS_PER_DEGREE_LAT;
        double dLon = distanceMeters * Math.sin(bearing)
                / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
        return new double[]{lat + dLat, lon + dLon};
    }
}
