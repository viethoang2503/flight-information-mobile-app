package vn.edu.usth.flightinfo.ui.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import androidx.core.app.NotificationManagerCompat;
import org.json.JSONException;
import org.json.JSONObject;
import vn.edu.usth.flightinfo.data.auth.AuthManager;
import vn.edu.usth.flightinfo.data.local.ReminderStore;
import vn.edu.usth.flightinfo.model.Flight;

/** Schedules inexact, idle-tolerant alarms; no exact-alarm permission required. */
public final class ReminderScheduler {
    private final Context context;
    private final ReminderStore store;
    private final AlarmManager alarms;

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.store = new ReminderStore(this.context);
        this.alarms = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    public boolean schedule(Flight flight, int leadMinutes) {
        return schedule(flight, leadMinutes, false);
    }

    public boolean schedule(Flight flight, int leadMinutes, boolean isMock) {
        if (flight == null || TextUtils.isEmpty(flight.getFlightIata()) || flight.getBestDepTs() <= 0 || leadMinutes <= 0) return false;
        long triggerAt = flight.getBestDepTs() * 1000L - leadMinutes * 60_000L;
        if (triggerAt <= System.currentTimeMillis()) return false;
        String json = flight.getRawJson();
        if (TextUtils.isEmpty(json)) return false;
        try {
            JSONObject payload = new JSONObject(json);
            if (!flight.getFlightIata().equals(payload.optString("flight_iata"))) return false;
            if (isMock) json = payload.put("reminder_is_mock", true).toString();
        } catch (JSONException invalidJson) {
            return false;
        }
        scheduleStored(flight.getFlightIata(), triggerAt, json);
        store.save(flight.getFlightIata(), triggerAt, json, AuthManager.get(context).ownerId());
        return true;
    }

    /** The reminder for this flight set by the signed-in user (or guest), or null. */
    public ReminderStore.Entry find(String flightIata) {
        ReminderStore.Entry entry = store.get(flightIata);
        return entry != null && entry.belongsTo(AuthManager.get(context).ownerId()) ? entry : null;
    }

    public void scheduleStored(String flightIata, long triggerAt, String json) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .putExtra("flightIata", flightIata);
        PendingIntent pending = PendingIntent.getBroadcast(context, flightIata.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending);
    }

    public void cancel(String flightIata) {
        if (TextUtils.isEmpty(flightIata)) return;
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, flightIata.hashCode(), intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending != null) {
            alarms.cancel(pending);
            pending.cancel();
        }
        store.remove(flightIata);
        NotificationManagerCompat.from(context).cancel(flightIata.hashCode());
    }
}
