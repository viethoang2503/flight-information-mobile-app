package vn.edu.usth.flightinfo.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AirlinesTest {

    @Test
    public void convertsCallsignToFlightNumber() {
        assertEquals("VN215", Airlines.callsignToFlightIata("HVN215  "));
        assertEquals("VJ124", Airlines.callsignToFlightIata("vjc124"));
        assertEquals("QH201", Airlines.callsignToFlightIata("BAV0201"));
        assertNull(Airlines.callsignToFlightIata("XYZ123"));
        assertNull(Airlines.callsignToFlightIata("HVN"));
        assertNull(Airlines.callsignToFlightIata(null));
    }

    @Test
    public void namesAndLogos() {
        assertEquals("Vietnam Airlines", Airlines.name("VN"));
        assertEquals("ZZ", Airlines.name("ZZ"));
        assertEquals("https://pics.avs.io/200/200/VJ.png", Airlines.logoUrl("vj"));
    }
}
