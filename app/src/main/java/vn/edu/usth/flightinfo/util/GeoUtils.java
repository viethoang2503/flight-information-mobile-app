package vn.edu.usth.flightinfo.util;

import java.util.List;

/** Geometry and great-circle calculations for flight tracking and routes. */
public final class GeoUtils {

    private static final double METERS_PER_DEGREE_LAT = 111_320.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /**
     * Bounding box of a path of {lat, lon} points, as {north, east, south, west}.
     * Longitudes are followed from point to point the shorter way round, so a path crossing the
     * 180° meridian gets west &gt; east (the date-line convention of osmdroid's BoundingBox)
     * instead of a box spanning the whole world.
     */
    public static double[] bounds(List<double[]> points) {
        double north = -90.0;
        double south = 90.0;
        double lon = 0.0;
        double minLon = 0.0;
        double maxLon = 0.0;
        double previousLon = 0.0;
        for (int i = 0; i < points.size(); i++) {
            double[] point = points.get(i);
            north = Math.max(north, point[0]);
            south = Math.min(south, point[0]);
            if (i == 0) {
                lon = minLon = maxLon = point[1];
            } else {
                // Step in (-180, 180]: the shorter way from the previous point.
                lon += ((point[1] - previousLon) + 540.0) % 360.0 - 180.0;
                minLon = Math.min(minLon, lon);
                maxLon = Math.max(maxLon, lon);
            }
            previousLon = point[1];
        }
        return new double[]{north, wrapLongitude(maxLon), south, wrapLongitude(minLon)};
    }

    /** Brings a longitude back into [-180, 180). */
    static double wrapLongitude(double lon) {
        return ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
    }

    /** Returns {lat, lon} after moving {@code distanceMeters} along {@code bearingDeg} (0 = north). */
    public static double[] move(double lat, double lon, double bearingDeg, double distanceMeters) {
        double bearing = Math.toRadians(bearingDeg);
        double dLat = distanceMeters * Math.cos(bearing) / METERS_PER_DEGREE_LAT;
        double dLon = distanceMeters * Math.sin(bearing)
                / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
        return new double[]{lat + dLat, lon + dLon};
    }

    /**
     * Great-circle distance in kilometers using the Haversine formula.
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Initial bearing (forward azimuth) from point 1 to point 2 on a great-circle path,
     * in degrees in the range [0, 360). 0 = North, 90 = East, 180 = South, 270 = West.
     */
    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);
        double theta = Math.atan2(y, x);
        double deg = Math.toDegrees(theta);
        return (deg + 360.0) % 360.0;
    }

    /**
     * Intermediate point on the great-circle path between two points.
     *
     * @param fraction 0.0 = point 1, 1.0 = point 2
     * @return {lat, lon} in degrees
     */
    public static double[] interpolate(double lat1, double lon1, double lat2, double lon2, double fraction) {
        if (fraction <= 0.0) {
            return new double[]{lat1, lon1};
        }
        if (fraction >= 1.0) {
            return new double[]{lat2, lon2};
        }

        double phi1 = Math.toRadians(lat1);
        double lambda1 = Math.toRadians(lon1);
        double phi2 = Math.toRadians(lat2);
        double lambda2 = Math.toRadians(lon2);

        double deltaPhi = phi2 - phi1;
        double deltaLambda = lambda2 - lambda1;
        double a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0);
        double d = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        if (d < 1e-9) {
            return new double[]{lat1, lon1};
        }

        double sinD = Math.sin(d);
        double weightA = Math.sin((1.0 - fraction) * d) / sinD;
        double weightB = Math.sin(fraction * d) / sinD;

        double x = weightA * Math.cos(phi1) * Math.cos(lambda1)
                + weightB * Math.cos(phi2) * Math.cos(lambda2);
        double y = weightA * Math.cos(phi1) * Math.sin(lambda1)
                + weightB * Math.cos(phi2) * Math.sin(lambda2);
        double z = weightA * Math.sin(phi1)
                + weightB * Math.sin(phi2);

        double phi = Math.atan2(z, Math.sqrt(x * x + y * y));
        double lambda = Math.atan2(y, x);

        return new double[]{Math.toDegrees(phi), Math.toDegrees(lambda)};
    }
}
