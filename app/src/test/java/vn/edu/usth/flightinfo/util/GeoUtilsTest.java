package vn.edu.usth.flightinfo.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GeoUtilsTest {

    @Test
    public void movesNorthAndEast() {
        double[] north = GeoUtils.move(10.0, 106.0, 0, 111_320);
        assertEquals(11.0, north[0], 1e-6);
        assertEquals(106.0, north[1], 1e-6);

        double[] east = GeoUtils.move(0.0, 106.0, 90, 111_320);
        assertEquals(0.0, east[0], 1e-6);
        assertEquals(107.0, east[1], 1e-6);
    }
}
