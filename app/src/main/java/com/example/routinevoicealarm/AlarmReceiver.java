package com.example.routinevoicealarm;

import android.app.*;
import android.content.*;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL =
            "routine_alarm_channel_v3";

    @Override
    public void onReceive(
            Context context,
            Intent intent) {

        long id =
                intent.getLongExtra(
                        "alarm_id",
                        -1
                );

        AlarmItem item =
                new AlarmStorage(context).get(id);

        if (item == null) {
            return;
        }

        if (!intent.getBooleanExtra(
                "snooze",
                false
        )) {

            AlarmScheduler.scheduleNext(
                    context,
                    item
            );
        }

        Intent service =
                new Intent(
                        context,
                        AlarmService.class
                );

        service.putExtra(
                "alarm_id",
                id
        );

        if (Build.VERSION.SDK_INT >= 26) {

            context.startForegroundService(
                    service
            );

        } else {

            context.startService(
                    service
            );
        }
    }
}
