package vn.edu.usth.flightinfo.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GeoUtilsTest {

    private static final double HAN_LAT = 21.2212;
    private static final double HAN_LON = 105.8072;
    private static final double SGN_LAT = 10.8188;
    private static final double SGN_LON = 106.6519;

    @Test
    public void movesNorthAndEast() {
        double[] north = GeoUtils.move(10.0, 106.0, 0, 111_320);
        assertEquals(11.0, north[0], 1e-6);
        assertEquals(106.0, north[1], 1e-6);

        double[] east = GeoUtils.move(0.0, 106.0, 90, 111_320);
        assertEquals(0.0, east[0], 1e-6);
        assertEquals(107.0, east[1], 1e-6);
    }

    @Test
    public void distanceKmBetweenHanoiAndSaigon() {
        double dist = GeoUtils.distanceKm(HAN_LAT, HAN_LON, SGN_LAT, SGN_LON);
        // Hanoi (HAN) to Saigon (SGN) is approximately 1,140 - 1,160 km
        assertTrue("Distance should be around 1150 km, got: " + dist, dist > 1130 && dist < 1170);
    }

    @Test
    public void bearingFromHanoiToSaigon() {
        double b = GeoUtils.bearing(HAN_LAT, HAN_LON, SGN_LAT, SGN_LON);
        // Saigon is ~10.4° further south but only ~0.85° further east: almost due south, ~175 degrees
        assertTrue("Bearing should be between 170° and 180°, got: " + b, b >= 170.0 && b <= 180.0);
    }

    @Test
    public void interpolateEndpointsAndMidpoint() {
        double[] start = GeoUtils.interpolate(HAN_LAT, HAN_LON, SGN_LAT, SGN_LON, 0.0);
        assertEquals(HAN_LAT, start[0], 1e-5);
        assertEquals(HAN_LON, start[1], 1e-5);

        double[] end = GeoUtils.interpolate(HAN_LAT, HAN_LON, SGN_LAT, SGN_LON, 1.0);
        assertEquals(SGN_LAT, end[0], 1e-5);
        assertEquals(SGN_LON, end[1], 1e-5);

        double[] mid = GeoUtils.interpolate(HAN_LAT, HAN_LON, SGN_LAT, SGN_LON, 0.5);
        // Midpoint latitude should be roughly halfway between 21.22 and 10.82 (around ~16.0)
        assertTrue(mid[0] < HAN_LAT && mid[0] > SGN_LAT);
        assertEquals(16.05, mid[0], 0.5);
    }

    @Test
    public void boundsOfDomesticRouteAreItsEnds() {
        double[] box = GeoUtils.bounds(route(HAN_LAT, HAN_LON, SGN_LAT, SGN_LON));
        assertEquals(HAN_LAT, box[0], 0.2);   // north
        assertEquals(SGN_LON, box[1], 0.2);   // east
        assertEquals(SGN_LAT, box[2], 0.2);   // south
        assertEquals(HAN_LON, box[3], 0.2);   // west
    }

    @Test
    public void boundsAcrossTheDateLineKeepWestGreaterThanEast() {
        // Saigon (SGN) to San Francisco (SFO) crosses the 180° meridian over the North Pacific.
        double[] box = GeoUtils.bounds(route(10.8188, 106.6519, 37.6213, -122.3790));
        assertEquals(-122.379, box[1], 0.01);  // east = SFO
        assertEquals(106.652, box[3], 0.01);   // west = SGN
        assertTrue("west > east marks a date-line box", box[3] > box[1]);
        // The great circle bulges well north of both airports.
        assertTrue("north should exceed SFO latitude, got " + box[0], box[0] > 45.0);
    }

    private static java.util.List<double[]> route(double lat1, double lon1, double lat2, double lon2) {
        java.util.List<double[]> points = new java.util.ArrayList<>();
        for (int i = 0; i <= 63; i++) {
            points.add(GeoUtils.interpolate(lat1, lon1, lat2, lon2, i / 63.0));
        }
        return points;
    }
}
