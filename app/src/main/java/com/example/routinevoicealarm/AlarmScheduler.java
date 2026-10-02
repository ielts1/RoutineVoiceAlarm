package com.example.routinevoicealarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;
import java.util.List;

public class AlarmScheduler {
    private static final int BASE = 9000;

    public static void rescheduleAll(Context c) {
        AlarmStorage s = new AlarmStorage(c);
        for (AlarmItem a : s.getAll()) {
            cancel(c, a);
            if (a.enabled) scheduleNext(c, a);
        }
    }

    public static void scheduleNext(Context c, AlarmItem a) {
        if (!a.enabled) return;
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return;

        Calendar now = Calendar.getInstance();
        Calendar best = null;

        for (int add = 0; add <= 7; add++) {
            Calendar t = (Calendar) now.clone();
            t.add(Calendar.DAY_OF_YEAR, add);
            t.set(Calendar.HOUR_OF_DAY, a.hour);
            t.set(Calendar.MINUTE, a.minute);
            t.set(Calendar.SECOND, 0);
            t.set(Calendar.MILLISECOND, 0);

            int dow = t.get(Calendar.DAY_OF_WEEK);
            if (a.days[dow] && t.after(now) && (best == null || t.before(best))) best = t;
        }

        if (best == null) return;

        Intent i = new Intent(c, AlarmReceiver.class);
        i.putExtra("alarm_id", a.id);
        PendingIntent pi = PendingIntent.getBroadcast(
                c, requestCode(a.id), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo info =
                    new AlarmManager.AlarmClockInfo(best.getTimeInMillis(), pi);
            am.setAlarmClock(info, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, best.getTimeInMillis(), pi);
        }
    }

    public static void scheduleSnooze(Context c, AlarmItem a, long whenMillis) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, AlarmReceiver.class);
        i.putExtra("alarm_id", a.id);
        i.putExtra("snooze", true);
        PendingIntent pi = PendingIntent.getBroadcast(
                c, requestCode(a.id) + 500000,
                i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return;
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi);
    }

    public static void cancel(Context c, AlarmItem a) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, AlarmReceiver.class);
        i.putExtra("alarm_id", a.id);
        PendingIntent pi = PendingIntent.getBroadcast(
                c, requestCode(a.id), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        PendingIntent snooze = PendingIntent.getBroadcast(
                c, requestCode(a.id) + 500000, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(snooze);
    }

    private static int requestCode(long id) {
        return BASE + (int)(id % 1000000);
    }
}
