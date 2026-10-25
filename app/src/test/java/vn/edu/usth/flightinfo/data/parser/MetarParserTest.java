package vn.edu.usth.flightinfo.data.parser;

import org.junit.Test;

import java.util.List;

import vn.edu.usth.flightinfo.model.Weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MetarParserTest {

    @Test
    public void parsesReportAndVariableWind() throws Exception {
        String body = "[{\"icaoId\":\"VVTS\",\"name\":\"Tan Son Nhat\",\"temp\":32,\"dewp\":24,"
                + "\"wdir\":\"VRB\",\"wspd\":3,\"visib\":\"6+\",\"fltCat\":\"VFR\",\"rawOb\":\"METAR VVTS ...\"}]";
        List<Weather> reports = MetarParser.parse(body);
        assertEquals(1, reports.size());
        Weather w = reports.get(0);
        assertEquals("VVTS", w.getIcao());
        assertEquals(32.0, w.getTemperatureC(), 1e-9);
        assertNull("VRB wind has no direction", w.getWindDirection());
        assertEquals(Integer.valueOf(3), w.getWindSpeedKt());
        assertEquals("6+", w.getVisibility());
        assertEquals("VFR", w.getFlightCategory());
    }

    @Test
    public void emptyBodyMeansNoReport() throws Exception {
        assertTrue(MetarParser.parse("").isEmpty());
        assertTrue(MetarParser.parse("[]").isEmpty());
    }
}
