package vn.edu.usth.flightinfo.data.parser;

import org.junit.Test;

import java.util.List;

import vn.edu.usth.flightinfo.data.ApiException;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.model.FlightStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AirLabsParserTest {

    private static final String FLIGHT = "{\"request\":{},\"response\":{"
            + "\"flight_iata\":\"VN215\",\"airline_iata\":\"VN\",\"status\":\"scheduled\","
            + "\"dep_iata\":\"HAN\",\"dep_time\":\"2025-01-01 08:00\",\"dep_time_ts\":1735693200,"
            + "\"dep_estimated\":\"2025-01-01 08:30\",\"dep_estimated_ts\":1735695000,"
            + "\"dep_gate\":null,\"dep_terminal\":\"1\","
            + "\"arr_iata\":\"SGN\",\"arr_time\":\"2025-01-01 10:10\",\"arr_time_ts\":1735701000,"
            + "\"duration\":130}}";

    @Test
    public void parsesWrappedFlight() throws Exception {
        Flight f = AirLabsParser.parseFlightResponse(FLIGHT);
        assertEquals("VN215", f.getFlightIata());
        assertEquals("HAN", f.getDepIata());
        assertEquals("SGN", f.getArrIata());
        assertEquals("1", f.getDepTerminal());
        assertNull("JSON null must not become the text \"null\"", f.getDepGate());
        assertEquals(1735693200L, f.getDepTimeTs());
        assertEquals(Integer.valueOf(130), f.getDuration());
        assertNotNull(f.getRawJson());
    }

    @Test
    public void derivesDelayFromEstimatedTime() throws Exception {
        Flight f = AirLabsParser.parseFlightResponse(FLIGHT);
        assertEquals(Integer.valueOf(30), f.getDelayMinutes());
        assertEquals(FlightStatus.DELAYED, f.getStatus());
    }

    @Test
    public void derivesAirlineFromFlightNumber() throws Exception {
        Flight f = AirLabsParser.parseFlightResponse("{\"response\":{\"flight_iata\":\"VJ123\"}}");
        assertEquals("VJ", f.getAirlineIata());
    }

    @Test
    public void emptyResponseIsNotFound() {
        assertErrorType("{\"request\":{},\"response\":[]}", ApiException.Type.NOT_FOUND);
        assertErrorType("{\"request\":{},\"response\":null}", ApiException.Type.NOT_FOUND);
    }

    @Test
    public void errorBodiesAreClassified() {
        assertErrorType("{\"error\":{\"message\":\"Monthly limit\",\"code\":\"month_limit_exceeded\"}}",
                ApiException.Type.QUOTA);
        assertErrorType("{\"error\":{\"message\":\"Bad key\",\"code\":\"unknown_api_key\"}}",
                ApiException.Type.NO_KEY);
        assertErrorType("not json", ApiException.Type.PARSE);
    }

    @Test
    public void parsesSchedulesWrappedOrBare() throws Exception {
        String items = "[{\"flight_iata\":\"VN1\"},{\"flight_iata\":\"VN2\"}]";
        List<Flight> wrapped = AirLabsParser.parseSchedulesResponse("{\"response\":" + items + "}");
        List<Flight> bare = AirLabsParser.parseSchedulesResponse(items);
        assertEquals(2, wrapped.size());
        assertEquals(2, bare.size());
        assertEquals("VN2", bare.get(1).getFlightIata());
        assertTrue(AirLabsParser.parseSchedulesResponse("{\"response\":[]}").isEmpty());
    }

    private static void assertErrorType(String body, ApiException.Type expected) {
        try {
            AirLabsParser.parseFlightResponse(body);
            fail("Expected " + expected);
        } catch (ApiException e) {
            assertEquals(expected, e.getType());
        }
    }
}
