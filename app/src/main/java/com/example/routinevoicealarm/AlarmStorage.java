package com.example.routinevoicealarm;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class AlarmStorage {
    private final SharedPreferences prefs;
    private static final String KEY = "alarm_list";

    public AlarmStorage(Context c) {
        prefs = c.getSharedPreferences("routine_voice_alarm", Context.MODE_PRIVATE);
    }

    public synchronized List<AlarmItem> getAll() {
        List<AlarmItem> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i=0; i<a.length(); i++) out.add(AlarmItem.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }

    public synchronized AlarmItem get(long id) {
        for (AlarmItem a : getAll()) if (a.id == id) return a;
        return null;
    }

    public synchronized void save(AlarmItem item) {
        List<AlarmItem> list = getAll();
        boolean replaced = false;
        for (int i=0; i<list.size(); i++) {
            if (list.get(i).id == item.id) { list.set(i, item); replaced = true; break; }
        }
        if (!replaced) list.add(item);
        write(list);
    }

    public synchronized void delete(long id) {
        List<AlarmItem> list = getAll();
        list.removeIf(a -> a.id == id);
        write(list);
    }

    private void write(List<AlarmItem> list) {
        JSONArray a = new JSONArray();
        for (AlarmItem item : list) a.put(item.toJson());
        prefs.edit().putString(KEY, a.toString()).apply();
    }
}
