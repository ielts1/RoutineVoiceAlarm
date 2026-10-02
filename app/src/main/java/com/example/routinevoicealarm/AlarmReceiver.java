package com.example.routinevoicealarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL = "routine_alarm_channel";

    @Override public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra("alarm_id", -1);
        AlarmItem item = new AlarmStorage(context).get(id);
        if (item == null) return;

        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "Routine alarms", NotificationManager.IMPORTANCE_HIGH);
            ch.setSound(null, null);
            ch.setDescription("Routine voice alarm notifications");
            nm.createNotificationChannel(ch);
        }

        Intent alarm = new Intent(context, AlarmActivity.class);
        alarm.putExtra("alarm_id", id);
        alarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent full = PendingIntent.getActivity(
                context, (int)(id % Integer.MAX_VALUE), alarm,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(com.example.routinevoicealarm.R.drawable.ic_alarm)
                .setContentTitle(item.name)
                .setContentText(item.message == null || item.message.isEmpty()
                        ? item.name + " করার সময় হয়ে গেছে।" : item.message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .setFullScreenIntent(full, true);

        nm.notify((int)(id % Integer.MAX_VALUE), b.build());

        if (!intent.getBooleanExtra("snooze", false)) {
            AlarmScheduler.scheduleNext(context, item);
        }
    }
}
