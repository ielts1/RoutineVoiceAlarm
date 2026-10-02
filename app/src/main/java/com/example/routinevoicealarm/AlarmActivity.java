package com.example.routinevoicealarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

public class AlarmActivity extends Activity {

    private AlarmItem item;

    @Override
    protected void onCreate(Bundle b) {

        super.onCreate(b);

        setContentView(
                R.layout.activity_alarm
        );

        long id =
                getIntent().getLongExtra(
                        "alarm_id",
                        -1
                );

        item =
                new AlarmStorage(this)
                        .get(id);

        if (item == null) {
            finish();
            return;
        }

        TextView time =
                findViewById(R.id.alarmTime);

        TextView title =
                findViewById(R.id.alarmTitle);

        TextView message =
                findViewById(R.id.alarmMessage);

        java.text.SimpleDateFormat format =
                new java.text.SimpleDateFormat(
                        "hh:mm a",
                        java.util.Locale.getDefault()
                );

        time.setText(
                format.format(
                        new java.util.Date()
                )
        );

        title.setText(item.name);

        String spoken =
                item.message == null ||
                item.message.trim().isEmpty()
                        ? item.name +
                          " করার সময় হয়ে গেছে।"
                        : item.message.trim();

        message.setText(spoken);

        findViewById(
                R.id.stopButton
        ).setOnClickListener(
                v -> stopAlarm()
        );

        findViewById(
                R.id.snoozeButton
        ).setOnClickListener(
                v -> snooze()
        );
    }

    private void stopAlarm() {

        Intent i =
                new Intent(
                        this,
                        AlarmService.class
                );

        i.setAction(
                AlarmService.ACTION_STOP
        );

        startService(i);

        finishAndRemoveTask();
    }

    private void snooze() {

        Intent i =
                new Intent(
                        this,
                        AlarmService.class
                );

        i.setAction(
                AlarmService.ACTION_SNOOZE
        );

        startService(i);

        finishAndRemoveTask();
    }
}
