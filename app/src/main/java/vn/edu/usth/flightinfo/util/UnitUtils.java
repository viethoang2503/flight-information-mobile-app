package vn.edu.usth.flightinfo.util;

/** Unit conversions for the map and the settings' altitude unit. */
public final class UnitUtils {

    private static final double FEET_PER_METER = 3.28084;
    private static final double KMH_PER_MS = 3.6;

    private UnitUtils() {
    }

    public static long metersToFeet(double meters) {
        return Math.round(meters * FEET_PER_METER);
    }

    public static long msToKmh(double metersPerSecond) {
        return Math.round(metersPerSecond * KMH_PER_MS);
    }

    /** Altitude rounded to whole units, in feet or meters. */
    public static long altitude(double meters, boolean feet) {
        return feet ? metersToFeet(meters) : Math.round(meters);
    }
}
