package vn.edu.usth.flightinfo.ui.reminder;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import vn.edu.usth.flightinfo.data.local.ReminderStore;
import vn.edu.usth.flightinfo.data.parser.AirLabsParser;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.ui.detail.FlightDetailActivity;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class ReminderTest {
    private Context context;
    private ReminderStore store;
    private ReminderScheduler scheduler;
    private AlarmManager alarms;
    private NotificationManager notifications;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("flight_reminders", Context.MODE_PRIVATE).edit().clear().commit();
        store = new ReminderStore(context);
        scheduler = new ReminderScheduler(context);
        alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        notifications = context.getSystemService(NotificationManager.class);
    }

    @Test public void schedulesAtDepartureMinusLeadAndPersistsPayload() throws Exception {
        Flight flight = flight("VN215");
        assertTrue(scheduler.schedule(flight, 30));
        long expected = flight.getBestDepTs() * 1000L - 30 * 60_000L;
        assertEquals(expected, store.get("VN215").triggerAt);
        assertEquals(flight.getRawJson(), new ReminderStore(context).get("VN215").flightJson);
        assertEquals(AlarmManager.RTC_WAKEUP, shadowOf(alarms).getScheduledAlarms().get(0).type);
        assertEquals(expected, shadowOf(alarms).getScheduledAlarms().get(0).triggerAtTime);
    }

    @Test @Config(sdk = 33)
    public void usesAnExactIdleAlarmWhenAllowed() throws Exception {
        ShadowAlarmManager.setCanScheduleExactAlarms(true);
        assertTrue(scheduler.canScheduleExact());
        assertTrue(scheduler.schedule(flight("VN215"), 30));
        ShadowAlarmManager.ScheduledAlarm alarm = shadowOf(alarms).getScheduledAlarms().get(0);
        assertEquals(ShadowAlarmManager.WINDOW_EXACT, alarm.getWindowLengthMs());
        assertTrue(alarm.isAllowWhileIdle());
    }

    @Test @Config(sdk = 31)
    public void fallsBackToATenMinuteWindowWithoutExactAlarms() throws Exception {
        ShadowAlarmManager.setCanScheduleExactAlarms(false);
        try {
            assertFalse(scheduler.canScheduleExact());
            Flight flight = flight("VN215");
            assertTrue(scheduler.schedule(flight, 30));
            ShadowAlarmManager.ScheduledAlarm alarm = shadowOf(alarms).getScheduledAlarms().get(0);
            assertEquals(flight.getBestDepTs() * 1000L - 30 * 60_000L, alarm.getTriggerAtMs());
            assertEquals(10 * 60_000L, alarm.getWindowLengthMs());
        } finally {
            ShadowAlarmManager.setCanScheduleExactAlarms(true);
        }
    }

    @Test public void replacingOneFlightDoesNotReplaceAnotherFlight() throws Exception {
        scheduler.schedule(flight("VN215"), 30);
        scheduler.schedule(flight("VJ123"), 30);
        scheduler.schedule(flight("VN215"), 60);
        assertEquals(2, store.getAll().size());
        assertEquals(2, shadowOf(alarms).getScheduledAlarms().size());
        scheduler.cancel("VN215");
        assertNull(store.get("VN215"));
        assertNotNull(store.get("VJ123"));
        assertEquals(1, shadowOf(alarms).getScheduledAlarms().size());
    }

    @Test public void rejectsPastTimeAndMissingDetailsWithoutOverwritingReminder() throws Exception {
        Flight flight = flight("VN215");
        assertTrue(scheduler.schedule(flight, 30));
        long original = store.get("VN215").triggerAt;
        flight.setDepTimeTs(System.currentTimeMillis() / 1000);
        assertFalse(scheduler.schedule(flight, 30));
        assertEquals(original, store.get("VN215").triggerAt);
        assertFalse(scheduler.schedule(null, 30));
        assertFalse(scheduler.schedule(flight("VN215"), 0));
        Flight missing = flight("VN215");
        missing.setRawJson(null);
        assertFalse(scheduler.schedule(missing, 30));
        missing.setRawJson("invalid json");
        assertFalse(scheduler.schedule(missing, 30));
        missing.setRawJson("{\"flight_iata\":\"VJ123\"}");
        assertFalse(scheduler.schedule(missing, 30));
    }

    @Test public void notificationOpensStoredFlightAndConsumesReminder() throws Exception {
        scheduler.schedule(flight("VN215"), 30);
        new ReminderReceiver().onReceive(context, alarmIntent("VN215"));
        Notification notification = shadowOf(notifications).getNotification("VN215".hashCode());
        assertNotNull(notification);
        assertNotNull(notifications.getNotificationChannel(ReminderReceiver.CHANNEL_ID));
        Intent detail = shadowOf(notification.contentIntent).getSavedIntent();
        assertEquals(FlightDetailActivity.class.getName(), detail.getComponent().getClassName());
        assertEquals("VN215", detail.getStringExtra(FlightDetailActivity.EXTRA_FLIGHT_IATA));
        assertNull(store.get("VN215"));
        scheduler.cancel("VN215");
        assertNull(shadowOf(notifications).getNotification("VN215".hashCode()));
    }

    @Test public void cancelledAndCorruptRemindersDoNotNotifyOrCrash() throws Exception {
        scheduler.schedule(flight("VN215"), 30);
        scheduler.cancel("VN215");
        new ReminderReceiver().onReceive(context, alarmIntent("VN215"));
        assertNull(shadowOf(notifications).getNotification("VN215".hashCode()));
        store.save("VN215", System.currentTimeMillis(), "invalid json");
        new ReminderReceiver().onReceive(context, alarmIntent("VN215"));
        assertNull(store.get("VN215"));
        assertNull(shadowOf(notifications).getNotification("VN215".hashCode()));
    }

    @Test public void reminderOfAnotherAccountIsHiddenAndDoesNotNotify() throws Exception {
        // No Firebase in unit tests, so the current owner is the guest.
        store.save("VN215", System.currentTimeMillis() + 60_000L, flight("VN215").getRawJson(), "other-uid");
        assertNull(scheduler.find("VN215"));
        new ReminderReceiver().onReceive(context, alarmIntent("VN215"));
        assertNull(shadowOf(notifications).getNotification("VN215".hashCode()));
        assertNull(store.get("VN215"));

        assertTrue(scheduler.schedule(flight("VN215"), 30));
        assertNotNull(scheduler.find("VN215"));
    }

    @Test public void notificationRetainsMockMode() throws Exception {
        assertTrue(scheduler.schedule(flight("VN215"), 30, true));
        new ReminderReceiver().onReceive(context, alarmIntent("VN215"));
        Notification notification = shadowOf(notifications).getNotification("VN215".hashCode());
        assertTrue(shadowOf(notification.contentIntent).getSavedIntent()
                .getBooleanExtra(FlightDetailActivity.EXTRA_IS_MOCK, false));
    }

    @Test public void rebootRestoresFutureRemindersAndRemovesExpiredOnes() throws Exception {
        long future = System.currentTimeMillis() + 60_000L;
        store.save("VN215", future, flight("VN215").getRawJson());
        store.save("VJ123", System.currentTimeMillis() - 1, flight("VJ123").getRawJson());
        BootReceiver.restoreReminders(context);
        assertNotNull(store.get("VN215"));
        assertNull(store.get("VJ123"));
        assertEquals(1, shadowOf(alarms).getScheduledAlarms().size());
        assertEquals(future, shadowOf(alarms).getNextScheduledAlarm().triggerAtTime);
    }

    @Test @Config(sdk = 33)
    public void deniedNotificationPermissionConsumesReminderWithoutCrash() throws Exception {
        shadowOf((Application) context).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS);
        scheduler.schedule(flight("VN215"), 30);
        new ReminderReceiver().onReceive(context, alarmIntent("VN215"));
        assertNull(shadowOf(notifications).getNotification("VN215".hashCode()));
        assertNull(store.get("VN215"));
    }

    private Intent alarmIntent(String iata) {
        return new Intent(context, ReminderReceiver.class).putExtra("flightIata", iata);
    }

    private Flight flight(String iata) throws Exception {
        return AirLabsParser.parseFlight(new JSONObject().put("flight_iata", iata)
                .put("dep_iata", "HAN").put("arr_iata", "SGN").put("dep_gate", "11")
                .put("dep_time", "2026-10-26 13:35").put("dep_time_ts", System.currentTimeMillis() / 1000 + 14400));
    }
}
