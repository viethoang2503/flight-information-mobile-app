package vn.edu.usth.flightinfo.data.mock;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.data.parser.AirLabsParser;
import vn.edu.usth.flightinfo.data.parser.AirportParser;
import vn.edu.usth.flightinfo.data.parser.MetarParser;
import vn.edu.usth.flightinfo.data.parser.OpenSkyParser;
import vn.edu.usth.flightinfo.model.AircraftState;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.model.FlightStatus;
import vn.edu.usth.flightinfo.util.TimeUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the bundled demo data against SPEC §9 (acceptance 1). Runs with the app module as working dir. */
public class MockDataTest {

    private static final String RAW = "src/main/res/raw/";
    private static long now;
    private static List<Flight> flights;

    @BeforeClass
    public static void loadFlights() throws Exception {
        now = TimeUtils.nowSeconds();
        JSONObject root = new JSONObject(read(RAW + "mock_flights.json"));
        long offset = MockTimeShifter.offsetSeconds(root.getLong("anchor_ts"), now);
        JSONArray array = root.getJSONArray("flights");
        flights = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            flights.add(AirLabsParser.parseFlight(MockTimeShifter.shift(array.getJSONObject(i), offset)));
        }
    }

    @Test
    public void searchFindsDemoFlight() {
        Flight vn215 = MockQueries.findFlight(flights, " vn215 ", now);
        assertNotNull(vn215);
        assertEquals(FlightStatus.EN_ROUTE, vn215.getStatus());
        int progress = TimeUtils.progressPercent(vn215.getBestDepTs(), vn215.getBestArrTs(), now);
        assertTrue("VN215 should be mid-flight, was " + progress, progress > 20 && progress < 80);
        assertNull(MockQueries.findFlight(flights, "ZZ999", now));
    }

    @Test
    public void hanoiBoardHasEnoughFlightsInOrder() {
        List<Flight> departures = MockQueries.filterSchedules(flights, "HAN", true);
        List<Flight> arrivals = MockQueries.filterSchedules(flights, "han", false);
        assertTrue(departures.size() >= 5);
        assertTrue(arrivals.size() >= 5);
        for (int i = 1; i < departures.size(); i++) {
            assertTrue(departures.get(i - 1).getDepTimeTs() <= departures.get(i).getDepTimeTs());
        }
    }

    @Test
    public void demoCoversEveryStatus() {
        for (FlightStatus status : new FlightStatus[]{FlightStatus.SCHEDULED, FlightStatus.DELAYED,
                FlightStatus.EN_ROUTE, FlightStatus.LANDED, FlightStatus.CANCELLED}) {
            boolean found = false;
            for (Flight f : flights) {
                found |= f.getStatus() == status;
            }
            assertTrue("missing " + status, found);
        }
    }

    @Test
    public void mapAndWeatherAndAirportsParse() throws Exception {
        List<AircraftState> aircraft = OpenSkyParser.parse(read(RAW + "mock_opensky.json"));
        assertTrue(aircraft.size() >= 20);
        List<AircraftState> moved = MockQueries.moveAircraft(aircraft, 60);
        assertEquals(aircraft.size(), moved.size());

        assertTrue(MetarParser.parse(read(RAW + "mock_metar.json")).size() >= 10);

        List<Airport> airports = AirportParser.parse(read("src/main/assets/airports.json"));
        assertTrue(airports.size() >= 40);
        // Every airport used by a mock flight must exist in airports.json.
        for (Flight f : flights) {
            assertTrue(f.getDepIata(), containsIata(airports, f.getDepIata()));
            assertTrue(f.getArrIata(), containsIata(airports, f.getArrIata()));
        }
    }

    private static boolean containsIata(List<Airport> airports, String iata) {
        for (Airport a : airports) {
            if (a.getIata().equals(iata)) {
                return true;
            }
        }
        return false;
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
