package vn.edu.usth.flightinfo.ui.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.model.Flight;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class CalendarHelperTest {
    private static final long DEP_TS = 1_790_000_000L;

    private final Context context = RuntimeEnvironment.getApplication();

    @Test public void prefillsTitleTimesLocationAndDetails() {
        Flight flight = flight();
        flight.setArrTimeTs(DEP_TS + 2 * 3600);
        Airport han = new Airport("HAN", "VVNB", "Noi Bai International Airport", "Hanoi", null,
                "Vietnam", 21.22, 105.81);

        Intent intent = CalendarHelper.buildIntent(context, flight, han);

        assertEquals(Intent.ACTION_INSERT, intent.getAction());
        assertEquals(CalendarContract.Events.CONTENT_URI, intent.getData());
        assertEquals("VN215 HAN → SGN", intent.getStringExtra(CalendarContract.Events.TITLE));
        assertEquals(DEP_TS * 1000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0));
        assertEquals((DEP_TS + 7200) * 1000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0));
        assertEquals("Noi Bai International Airport",
                intent.getStringExtra(CalendarContract.Events.EVENT_LOCATION));
        String description = intent.getStringExtra(CalendarContract.Events.DESCRIPTION);
        assertTrue(description, description.contains("Terminal 1"));
        assertTrue(description, description.contains("Gate 11"));
    }

    @Test public void updatedDepartureTimeIsUsed() {
        Flight flight = flight();
        flight.setDepEstimatedTs(DEP_TS + 1800);
        flight.setArrTimeTs(DEP_TS + 2 * 3600);

        Intent intent = CalendarHelper.buildIntent(context, flight, null);

        assertEquals((DEP_TS + 1800) * 1000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0));
        assertEquals("HAN", intent.getStringExtra(CalendarContract.Events.EVENT_LOCATION));
    }

    @Test public void missingArrivalUsesDurationOrOneHour() {
        Flight flight = flight();
        flight.setDuration(130);
        Intent withDuration = CalendarHelper.buildIntent(context, flight, null);
        assertEquals((DEP_TS + 130 * 60) * 1000L,
                withDuration.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0));

        flight.setDuration(null);
        Intent withoutDuration = CalendarHelper.buildIntent(context, flight, null);
        assertEquals((DEP_TS + 3600) * 1000L,
                withoutDuration.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0));
    }

    @Test public void missingDepartureTimeIsRejected() {
        Flight flight = flight();
        flight.setDepTimeTs(0);
        assertThrows(IllegalArgumentException.class,
                () -> CalendarHelper.buildIntent(context, flight, null));
    }

    private static Flight flight() {
        Flight flight = new Flight();
        flight.setFlightIata("VN215");
        flight.setAirlineIata("VN");
        flight.setDepIata("HAN");
        flight.setArrIata("SGN");
        flight.setDepTerminal("1");
        flight.setDepGate("11");
        flight.setRawStatus("scheduled");
        flight.setDepTimeTs(DEP_TS);
        return flight;
    }
}
