package vn.edu.usth.flightinfo.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/** Persists one reminder per flight IATA, including the original flight JSON. */
public final class ReminderStore {
    private static final String PREFS = "flight_reminders";
    private final SharedPreferences prefs;

    public ReminderStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static final class Entry {
        public final String flightIata;
        public final long triggerAt;
        public final String flightJson;
        /** AuthManager owner id of whoever set the reminder; null for entries saved before F8. */
        public final String owner;
        public Entry(String flightIata, long triggerAt, String flightJson, String owner) {
            this.flightIata = flightIata;
            this.triggerAt = triggerAt;
            this.flightJson = flightJson;
            this.owner = owner;
        }

        /** Old entries without an owner are kept visible to everyone rather than lost. */
        public boolean belongsTo(String ownerId) {
            return owner == null || owner.equals(ownerId);
        }
    }

    public void save(String flightIata, long triggerAt, String flightJson) {
        save(flightIata, triggerAt, flightJson, null);
    }

    public void save(String flightIata, long triggerAt, String flightJson, String owner) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("triggerAt", triggerAt);
            obj.put("flightJson", flightJson);
            if (owner != null) obj.put("owner", owner);
            prefs.edit().putString(flightIata, obj.toString()).apply();
        } catch (Exception ignored) { }
    }

    public Entry get(String flightIata) {
        String serialized = prefs.getString(flightIata, null);
        if (serialized == null) return null;
        try {
            JSONObject obj = new JSONObject(serialized);
            return new Entry(flightIata, obj.getLong("triggerAt"), obj.optString("flightJson", ""),
                    obj.has("owner") ? obj.getString("owner") : null);
        } catch (Exception ignored) {
            return null;
        }
    }

    public Map<String, Entry> getAll() {
        Map<String, Entry> entries = new HashMap<>();
        for (String key : prefs.getAll().keySet()) {
            Entry entry = get(key);
            if (entry != null) entries.put(key, entry);
        }
        return entries;
    }

    public void remove(String flightIata) {
        prefs.edit().remove(flightIata).apply();
    }
}
