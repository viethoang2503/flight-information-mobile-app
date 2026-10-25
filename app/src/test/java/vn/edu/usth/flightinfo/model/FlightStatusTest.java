package vn.edu.usth.flightinfo.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FlightStatusTest {

    @Test
    public void mapsAirLabsStatuses() {
        assertEquals(FlightStatus.SCHEDULED, FlightStatus.from("scheduled", null));
        assertEquals(FlightStatus.SCHEDULED, FlightStatus.from("scheduled", 14));
        assertEquals(FlightStatus.DELAYED, FlightStatus.from("scheduled", 15));
        assertEquals(FlightStatus.EN_ROUTE, FlightStatus.from("active", 40));
        assertEquals(FlightStatus.EN_ROUTE, FlightStatus.from("en-route", null));
        assertEquals(FlightStatus.LANDED, FlightStatus.from("landed", null));
        assertEquals(FlightStatus.CANCELLED, FlightStatus.from("Cancelled", null));
        assertEquals(FlightStatus.UNKNOWN, FlightStatus.from("diverted", null));
        assertEquals(FlightStatus.UNKNOWN, FlightStatus.from(null, null));
    }
}
