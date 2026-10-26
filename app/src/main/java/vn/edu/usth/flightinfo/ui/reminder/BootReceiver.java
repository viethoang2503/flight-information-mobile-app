package vn.edu.usth.flightinfo.ui.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import vn.edu.usth.flightinfo.data.local.ReminderStore;
import vn.edu.usth.flightinfo.util.AppExecutors;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        PendingResult pending = goAsync();
        AppExecutors.get().io(() -> {
            try {
                restoreReminders(context);
            } finally {
                pending.finish();
            }
        });
    }

    static void restoreReminders(Context context) {
        ReminderStore store = new ReminderStore(context);
        ReminderScheduler scheduler = new ReminderScheduler(context);
        long now = System.currentTimeMillis();
        for (ReminderStore.Entry entry : store.getAll().values()) {
            if (entry.triggerAt > now) {
                scheduler.scheduleStored(entry.flightIata, entry.triggerAt, entry.flightJson);
            } else {
                store.remove(entry.flightIata);
            }
        }
    }
}
