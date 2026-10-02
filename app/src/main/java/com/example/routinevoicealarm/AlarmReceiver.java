package com.example.routinevoicealarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

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
                new AlarmStorage(context)
                        .get(id);

        if (item == null) {
            return;
        }

        NotificationManager nm =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (nm == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 26) {

            android.net.Uri soundUri =
                    RingtoneManager.getDefaultUri(
                            RingtoneManager.TYPE_ALARM
                    );

            if (soundUri == null) {

                soundUri =
                        RingtoneManager.getDefaultUri(
                                RingtoneManager.TYPE_NOTIFICATION
                        );
            }

            AudioAttributes audioAttributes =
                    new AudioAttributes.Builder()
                            .setUsage(
                                    AudioAttributes.USAGE_ALARM
                            )
                            .setContentType(
                                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                            )
                            .build();

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL,
                            "Routine alarms",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "Routine voice alarm notifications"
            );

            channel.setSound(
                    soundUri,
                    audioAttributes
            );

            channel.enableVibration(true);

            nm.createNotificationChannel(
                    channel
            );
        }

        Intent alarm =
                new Intent(
                        context,
                        AlarmActivity.class
                );

        alarm.putExtra(
                "alarm_id",
                id
        );

        alarm.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent full =
                PendingIntent.getActivity(
                        context,
                        (int)(
                                id
                                        % Integer.MAX_VALUE
                        ),
                        alarm,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
                );

        String message;

        if (item.message == null ||
                item.message.trim().isEmpty()) {

            message =
                    item.name +
                    " করার সময় হয়ে গেছে।";

        } else {

            message =
                    item.message.trim();
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context,
                        CHANNEL
                )
                        .setSmallIcon(
                                R.drawable.ic_alarm
                        )
                        .setContentTitle(
                                item.name
                        )
                        .setContentText(
                                message
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_MAX
                        )
                        .setCategory(
                                NotificationCompat.CATEGORY_ALARM
                        )
                        .setVisibility(
                                NotificationCompat.VISIBILITY_PUBLIC
                        )
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setContentIntent(full)
                        .setFullScreenIntent(
                                full,
                                true
                        );

        nm.notify(
                (int)(
                        id
                                % Integer.MAX_VALUE
                ),
                builder.build()
        );

        /*
         * Schedule the next normal occurrence.
         */
        if (!intent.getBooleanExtra(
                "snooze",
                false
        )) {

            AlarmScheduler.scheduleNext(
                    context,
                    item
            );
        }
    }
            }
