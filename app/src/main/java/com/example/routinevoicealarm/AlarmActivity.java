package com.example.routinevoicealarm;

import android.app.NotificationManager;
import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmActivity extends Activity {

    private Ringtone ringtone;
    private TextToSpeech tts;
    private AlarmItem item;

    private final Handler speechHandler =
            new Handler(Looper.getMainLooper());

    private boolean stopped = false;

    private static final long REPEAT_DELAY = 5000L;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        setContentView(R.layout.activity_alarm);

        long id = getIntent().getLongExtra("alarm_id", -1);

        item = new AlarmStorage(this).get(id);

        if (item == null) {
            finish();
            return;
        }

        TextView time = findViewById(R.id.alarmTime);
        TextView title = findViewById(R.id.alarmTitle);
        TextView msg = findViewById(R.id.alarmMessage);

        time.setText(
                new SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                ).format(new Date())
        );

        title.setText(item.name);

        String spoken;

        if (item.message == null ||
                item.message.trim().isEmpty()) {

            spoken = item.name + " করার সময় হয়ে গেছে।";

        } else {

            spoken = item.message.trim();
        }

        msg.setText(spoken);

        findViewById(R.id.stopButton)
                .setOnClickListener(v -> stopAlarm());

        findViewById(R.id.snoozeButton)
                .setOnClickListener(v -> snooze());

        startRingtone();
        startSpeech(spoken);
    }

    private void startRingtone() {
        try {
            android.net.Uri uri =
                    RingtoneManager.getDefaultUri(
                            RingtoneManager.TYPE_ALARM
                    );

            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_NOTIFICATION
                );
            }

            ringtone =
                    RingtoneManager.getRingtone(
                            this,
                            uri
                    );

            if (ringtone != null) {

                ringtone.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(
                                        AudioAttributes.USAGE_ALARM
                                )
                                .setContentType(
                                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                                )
                                .build()
                );

                ringtone.play();
            }

        } catch (Exception ignored) {
        }
    }

    private void startSpeech(String text) {

        tts = new TextToSpeech(
                this,
                status -> {

                    if (status != TextToSpeech.SUCCESS) {
                        return;
                    }

                    int result =
                            tts.setLanguage(
                                    new Locale("bn", "BD")
                            );

                    if (result ==
                            TextToSpeech.LANG_MISSING_DATA
                            ||
                            result ==
                            TextToSpeech.LANG_NOT_SUPPORTED) {

                        tts.setLanguage(
                                Locale.getDefault()
                        );
                    }

                    /*
                     * 1.0 = normal
                     * 0.65 = slower
                     */
                    tts.setSpeechRate(0.65f);

                    tts.setPitch(1.0f);

                    tts.setOnUtteranceProgressListener(
                            new UtteranceProgressListener() {

                                @Override
                                public void onStart(
                                        String id) {
                                }

                                @Override
                                public void onDone(
                                        String id) {

                                    if (!stopped) {

                                        speechHandler.postDelayed(
                                                () -> {

                                                    if (!stopped) {
                                                        speakText(text);
                                                    }

                                                },
                                                REPEAT_DELAY
                                        );
                                    }
                                }

                                @Override
                                public void onError(
                                        String id) {
                                }
                            }
                    );

                    speakText(text);
                }
        );
    }

    private void speakText(String text) {

        if (stopped || tts == null) {
            return;
        }

        tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "routine_voice_alarm"
        );
    }

    private void snooze() {

        if (item != null) {

            AlarmScheduler.scheduleSnooze(
                    this,
                    item,
                    System.currentTimeMillis()
                            + 10 * 60 * 1000L
            );
        }

        stopAlarm();
    }

    private void stopAlarm() {

        stopped = true;

        speechHandler.removeCallbacksAndMessages(null);

        if (ringtone != null) {
            try {
                ringtone.stop();
            } catch (Exception ignored) {
            }
        }

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }
        }

        NotificationManager nm =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (nm != null && item != null) {

            nm.cancel(
                    (int)(
                            item.id
                                    % Integer.MAX_VALUE
                    )
            );
        }

        finishAndRemoveTask();
    }

    @Override
    protected void onDestroy() {

        stopped = true;

        speechHandler.removeCallbacksAndMessages(null);

        if (ringtone != null) {
            try {
                ringtone.stop();
            } catch (Exception ignored) {
            }
        }

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }
                }
