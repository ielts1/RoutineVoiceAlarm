package com.example.routinevoicealarm;

import org.json.JSONArray;
import org.json.JSONObject;

public class AlarmItem {
    public long id;
    public int hour, minute;
    public String name, message;
    // Java Calendar: Sunday=1 ... Saturday=7.
    public boolean[] days = new boolean[8];
    public boolean enabled = true;

    public AlarmItem(long id) { this.id = id; }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("hour", hour);
            o.put("minute", minute);
            o.put("name", name);
            o.put("message", message == null ? "" : message);
            o.put("enabled", enabled);
            JSONArray d = new JSONArray();
            for (int i = 1; i <= 7; i++) d.put(days[i]);
            o.put("days", d);
        } catch (Exception ignored) {}
        return o;
    }

    public static AlarmItem fromJson(JSONObject o) {
        AlarmItem a = new AlarmItem(o.optLong("id"));
        a.hour = o.optInt("hour");
        a.minute = o.optInt("minute");
        a.name = o.optString("name", "");
        a.message = o.optString("message", "");
        a.enabled = o.optBoolean("enabled", true);
        JSONArray d = o.optJSONArray("days");
        if (d != null) for (int i=1; i<=7; i++) a.days[i] = d.optBoolean(i-1, false);
        return a;
    }
}
