package vn.edu.usth.flightinfo.data.stats;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.model.Flight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FlightStatsTest {
    private Flight flight(String number, String airline, String dep, String arr,
                          int duration, String status, Integer delay) {
        Flight f = new Flight();
        f.setFlightIata(number);
        f.setAirlineIata(airline);
        f.setDepIata(dep);
        f.setArrIata(arr);
        f.setDuration(duration);
        f.setRawStatus(status);
        f.setDepDelayed(delay);
        return f;
    }

    private Map<String, Airport> airports() {
        Map<String, Airport> map = new HashMap<>();
        map.put("AAA", new Airport("AAA", null, "Airport A", "City A", null, "Country", 0, 0));
        map.put("BBB", new Airport("BBB", null, "Airport B", "City B", null, "Country", 0, 1));
        map.put("CCC", new Airport("CCC", null, "Airport C", "City C", null, "Country", 1, 1));
        return map;
    }

    @Test public void aggregatesCountDistanceDurationAndPunctuality() {
        Flight a = flight("VN1", "VN", "AAA", "BBB", 60, "scheduled", 0);
        Flight b = flight("VJ2", "VJ", "AAA", "CCC", 90, "scheduled", 20);
        Flight c = flight("VN3", "VN", "BBB", "CCC", 30, "cancelled", null);

        FlightStats stats = FlightStats.from(Arrays.asList(a, b, c), airports());

        assertEquals(3, stats.getFlightCount());
        assertEquals(3.0, stats.getTotalHours(), 0.001);
        assertEquals(50.0, stats.getOnTimePercent(), 0.001);
        assertEquals("AAA", stats.getBusiestDepartureIata());
        assertEquals(2, stats.getBusiestDepartureCount());
        assertEquals(2, stats.getAirlines().size());
        assertEquals("VN", stats.getAirlines().get(0).getCode());
        assertEquals(2, stats.getAirlines().get(0).getCount());
        assertEquals(379.6, stats.getTotalDistanceKm(), 2.0);
    }

    @Test public void emptyInputReturnsZeroesAndNoAirport() {
        FlightStats stats = FlightStats.from(Collections.emptyList(), Collections.emptyMap());
        assertEquals(0, stats.getFlightCount());
        assertEquals(0.0, stats.getTotalDistanceKm(), 0.0);
        assertEquals(0.0, stats.getTotalHours(), 0.0);
        assertEquals(0.0, stats.getOnTimePercent(), 0.0);
        assertNull(stats.getBusiestDepartureIata());
        assertEquals(0, stats.getAirlines().size());
    }

    @Test public void moreThanFiveAirlinesAreGroupedAsOther() {
        FlightStats stats = FlightStats.from(Arrays.asList(
                flight("VN1", "VN", "AAA", "BBB", 10, "scheduled", 0),
                flight("VJ1", "VJ", "AAA", "BBB", 10, "scheduled", 0),
                flight("QH1", "QH", "AAA", "BBB", 10, "scheduled", 0),
                flight("SQ1", "SQ", "AAA", "BBB", 10, "scheduled", 0),
                flight("TG1", "TG", "AAA", "BBB", 10, "scheduled", 0),
                flight("JL1", "JL", "AAA", "BBB", 10, "scheduled", 0),
                flight("AF1", "AF", "AAA", "BBB", 10, "scheduled", 0)
        ), airports());
        assertEquals(6, stats.getAirlines().size());
        assertEquals("OTHER", stats.getAirlines().get(5).getCode());
        assertEquals(2, stats.getAirlines().get(5).getCount());
    }
}
