package vn.edu.usth.flightinfo.ui.reminder;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.data.local.ReminderStore;
import vn.edu.usth.flightinfo.data.parser.AirLabsParser;
import vn.edu.usth.flightinfo.model.Flight;
import vn.edu.usth.flightinfo.ui.detail.FlightDetailActivity;
import vn.edu.usth.flightinfo.util.TimeUtils;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "flight_reminders";

    @Override public void onReceive(Context context, Intent intent) {
        String iata = intent.getStringExtra("flightIata");
        if (iata == null) return;
        ReminderStore store = new ReminderStore(context);
        ReminderStore.Entry entry = store.get(iata);
        if (entry == null) return; // Cancelled or already consumed.
        String json = entry.flightJson;
        try {
            JSONObject payload = new JSONObject(json);
            Flight flight = AirLabsParser.parseFlight(payload);
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager manager = context.getSystemService(NotificationManager.class);
                manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                        context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT));
            }
            Intent detail = FlightDetailActivity.newIntent(context, flight,
                    payload.optBoolean("reminder_is_mock", false), false);
            detail.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent click = PendingIntent.getActivity(context, iata.hashCode(), detail,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            String time = TimeUtils.hhmm(flight.getBestDepTime());
            if (time == null) time = "--:--";
            String gate = flight.getDepGate() == null ? "--" : flight.getDepGate();
            String body = context.getString(R.string.reminder_notification_body, time,
                    flight.getDepIata() == null ? "---" : flight.getDepIata(),
                    flight.getArrIata() == null ? "---" : flight.getArrIata(), gate);
            NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_flight)
                    .setContentTitle(context.getString(R.string.reminder_notification_title, iata))
                    .setContentText(body).setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true).setContentIntent(click).setPriority(NotificationCompat.PRIORITY_DEFAULT);
            if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(iata.hashCode(), notification.build());
            }
        } catch (Exception ignored) {
            // Keep the receiver safe even if an old stored JSON payload is malformed.
        } finally {
            store.remove(iata);
        }
    }
}
