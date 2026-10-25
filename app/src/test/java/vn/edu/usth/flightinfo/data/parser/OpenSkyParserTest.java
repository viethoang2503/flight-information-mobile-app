package vn.edu.usth.flightinfo.data.parser;

import org.junit.Test;

import java.util.List;

import vn.edu.usth.flightinfo.model.AircraftState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OpenSkyParserTest {

    @Test
    public void parsesStatesAndSkipsAircraftWithoutPosition() throws Exception {
        String body = "{\"time\":1,\"states\":["
                + "[\"888011\",\"HVN215  \",\"Viet Nam\",1,1,106.2,16.4,10668.0,false,230.5,175.4,0.0,null,null,null,false,0],"
                + "[\"888022\",\"VJC199  \",\"Viet Nam\",1,1,106.6,10.8,null,true,0.0,250.0,null,null,null,null,false,0],"
                + "[\"888033\",\"NOPOS   \",\"Viet Nam\",null,1,null,null,null,false,null,null,null,null,null,null,false,0]"
                + "]}";
        List<AircraftState> states = OpenSkyParser.parse(body);
        assertEquals(2, states.size());

        AircraftState first = states.get(0);
        assertEquals("HVN215", first.getCallsign());
        assertEquals(16.4, first.getLat(), 1e-9);
        assertEquals(106.2, first.getLon(), 1e-9);
        assertEquals(175.4, first.getTrueTrack(), 1e-9);
        assertFalse(first.isOnGround());

        AircraftState second = states.get(1);
        assertTrue(second.isOnGround());
        assertNull(second.getBaroAltitude());
    }

    @Test
    public void nullStatesMeansNoAircraft() throws Exception {
        assertTrue(OpenSkyParser.parse("{\"time\":1,\"states\":null}").isEmpty());
    }
}
