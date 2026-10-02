package com.example.routinevoicealarm;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    LinearLayout list;
    AlarmStorage storage;

    TextView totalCount;
    TextView activeCount;
    TextView disabledCount;
    TextView todayCount;
    TextView todayLabel;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        storage = new AlarmStorage(this);

        setContentView(R.layout.activity_main);

        findViewById(R.id.addButton)
                .setOnClickListener(v -> {
                    startActivity(
                            new Intent(
                                    this,
                                    AddEditAlarmActivity.class
                            )
                    );
                });

        findViewById(R.id.settingsButton)
                .setOnClickListener(
                        v -> openPermissionSettings()
                );

        totalCount = findViewById(R.id.totalCount);
        activeCount = findViewById(R.id.activeCount);
        disabledCount = findViewById(R.id.disabledCount);
        todayCount = findViewById(R.id.todayCount);
        todayLabel = findViewById(R.id.todayLabel);

        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        render();

        if (Build.VERSION.SDK_INT >= 31) {

            AlarmManager am =
                    (AlarmManager)getSystemService(
                            ALARM_SERVICE
                    );

            if (am != null &&
                    am.canScheduleExactAlarms()) {

                AlarmScheduler.rescheduleAll(this);
            }
        }
    }

    void render() {

        list = findViewById(R.id.alarmList);

        list.removeAllViews();

        TextView today =
                findViewById(R.id.today);

        today.setText(
                new SimpleDateFormat(
                        "EEEE, dd MMMM yyyy",
                        Locale.getDefault()
                ).format(new Date())
        );

        List<AlarmItem> items =
                storage.getAll();

        items.sort((a, b) -> {

            int x =
                    a.hour * 60 +
                    a.minute;

            int y =
                    b.hour * 60 +
                    b.minute;

            return Integer.compare(x, y);
        });

        int total = items.size();

        int active = 0;

        int disabled = 0;

        int todayAlarms = 0;

        Calendar now =
                Calendar.getInstance();

        int currentDay =
                now.get(Calendar.DAY_OF_WEEK);

        for (AlarmItem a : items) {

            if (a.enabled) {
                active++;
            } else {
                disabled++;
            }

            if (a.enabled &&
                    a.days[currentDay]) {

                todayAlarms++;
            }
        }

        totalCount.setText(
                String.valueOf(total)
        );

        activeCount.setText(
                String.valueOf(active)
        );

        disabledCount.setText(
                String.valueOf(disabled)
        );

        todayCount.setText(
                String.valueOf(todayAlarms)
        );

        todayLabel.setText(
                String.valueOf(todayAlarms)
        );

        if (items.isEmpty()) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "No routine alarms yet.\n\n" +
                    "Tap “+ Add Routine Alarm” " +
                    "to create your first routine."
            );

            empty.setTextSize(18);
            empty.setTextColor(
                    Color.DKGRAY
            );

            empty.setGravity(
                    Gravity.CENTER
            );

            empty.setPadding(
                    20,
                    40,
                    20,
                    40
            );

            list.addView(
                    empty,
                    new LinearLayout.LayoutParams(
                            -1,
                            -2
                    )
            );

            return;
        }

        for (AlarmItem a : items) {

            addCard(a);
        }
    }

    void addCard(AlarmItem a) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                20,
                18,
                20,
                18
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.WHITE
        );

        background.setCornerRadius(
                28
        );

        card.setBackground(
                background
        );

        TextView top =
                new TextView(this);

        String ampm =
                a.hour < 12
                        ? "AM"
                        : "PM";

        int displayHour =
                a.hour == 0
                        ? 12
                        : (a.hour > 12
                        ? a.hour - 12
                        : a.hour);

        top.setText(
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d %s",
                        displayHour,
                        a.minute,
                        ampm
                )
        );

        top.setTextSize(27);
        top.setTextColor(
                Color.rgb(
                        37,
                        99,
                        235
                )
        );

        top.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        card.addView(top);


        LinearLayout titleRow =
                new LinearLayout(this);

        titleRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        titleRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView n =
                new TextView(this);

        n.setText(a.name);
        n.setTextSize(20);
        n.setTextColor(
                Color.rgb(
                        17,
                        24,
                        39
                )
        );

        n.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        titleRow.addView(
                n,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );


        Switch toggle =
                new Switch(this);

        toggle.setChecked(
                a.enabled
        );

        toggle.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    a.enabled =
                            isChecked;

                    storage.save(a);

                    if (isChecked) {

                        AlarmScheduler.scheduleNext(
                                this,
                                a
                        );

                    } else {

                        AlarmScheduler.cancel(
                                this,
                                a
                        );
                    }

                    render();
                }
        );

        titleRow.addView(
                toggle
        );

        card.addView(
                titleRow
        );


        String message =
                a.message == null
                        ? ""
                        : a.message.trim();

        if (!message.isEmpty()) {

            TextView m =
                    new TextView(this);

            m.setText(message);
            m.setTextSize(16);
            m.setTextColor(
                    Color.rgb(
                            75,
                            85,
                            99
                    )
            );

            m.setMaxLines(2);

            LinearLayout.LayoutParams mp =
                    new LinearLayout.LayoutParams(
                            -1,
                            -2
                    );

            mp.topMargin = 5;

            card.addView(
                    m,
                    mp
            );
        }


        TextView d =
                new TextView(this);

        d.setText(
                daysText(a)
        );

        d.setTextSize(14);
        d.setTextColor(
                Color.rgb(
                        107,
                        114,
                        128
                )
        );

        LinearLayout.LayoutParams dp =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        dp.topMargin = 8;

        card.addView(
                d,
                dp
        );


        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button edit =
                new Button(this);

        edit.setText("Edit");

        edit.setOnClickListener(
                v -> {

                    Intent i =
                            new Intent(
                                    this,
                                    AddEditAlarmActivity.class
                            );

                    i.putExtra(
                            "alarm_id",
                            a.id
                    );

                    startActivity(i);
                }
        );


        Button del =
                new Button(this);

        del.setText("Delete");

        del.setOnClickListener(
                v -> new AlertDialog.Builder(
                        this
                )
                        .setTitle(
                                "Delete routine?"
                        )
                        .setMessage(
                                a.name
                        )
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Delete",
                                (d1, w) -> {

                                    AlarmScheduler.cancel(
                                            this,
                                            a
                                    );

                                    storage.delete(
                                            a.id
                                    );

                                    render();
                                }
                        )
                        .show()
        );


        buttons.addView(
                edit,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        buttons.addView(
                del,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        bp.topMargin = 5;

        card.addView(
                buttons,
                bp
        );


        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        cp.setMargins(
                0,
                0,
                0,
                14
        );

        list.addView(
                card,
                cp
        );
    }

    String daysText(AlarmItem a) {

        String[] names = {
                "",
                "Sun",
                "Mon",
                "Tue",
                "Wed",
                "Thu",
                "Fri",
                "Sat"
        };

        StringBuilder s =
                new StringBuilder(
                        "Days: "
                );

        boolean first = true;

        for (int i = 1; i <= 7; i++) {

            if (a.days[i]) {

                if (!first) {
                    s.append(", ");
                }

                s.append(
                        names[i]
                );

                first = false;
            }
        }

        return s.toString();
    }

    void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    40
            );
        }
    }

    void openPermissionSettings() {

        if (Build.VERSION.SDK_INT >= 34) {

            NotificationManager nm =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE
                            );

            if (nm != null &&
                    !nm.canUseFullScreenIntent()) {

                try {

                    startActivity(
                            new Intent(
                                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                    Uri.parse(
                                            "package:" +
                                                    getPackageName()
                                    )
                            )
                    );

                    return;

                } catch (Exception ignored) {
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 31) {

            AlarmManager am =
                    (AlarmManager)
                            getSystemService(
                                    ALARM_SERVICE
                            );

            if (am != null &&
                    !am.canScheduleExactAlarms()) {

                try {

                    startActivity(
                            new Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse(
                                            "package:" +
                                                    getPackageName()
                                    )
                            )
                    );

                    return;

                } catch (Exception ignored) {
                }
            }
        }

        try {

            Intent i =
                    new Intent(
                            Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    );

            i.putExtra(
                    Settings.EXTRA_APP_PACKAGE,
                    getPackageName()
            );

            startActivity(i);

        } catch (Exception ignored) {
        }
    }
            }
