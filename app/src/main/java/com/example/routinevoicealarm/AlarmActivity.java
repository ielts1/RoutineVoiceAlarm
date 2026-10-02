package com.example.routinevoicealarm;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
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

    private MediaPlayer mediaPlayer;
    private TextToSpeech tts;
    private AlarmItem item;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean stopped = false;

    private int cycleCount = 0;

    private static final int MAX_CYCLES = 3;

    private static final long NEXT_CYCLE_DELAY = 1000L;

    private int speechCount = 0;

    private static final long SPEECH_REPEAT_DELAY = 1500L;

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

        long id =
                getIntent().getLongExtra(
                        "alarm_id",
                        -1
                );

        item =
                new AlarmStorage(this).get(id);

        if (item == null) {
            finish();
            return;
        }

        TextView time =
                findViewById(R.id.alarmTime);

        TextView title =
                findViewById(R.id.alarmTitle);

        TextView msg =
                findViewById(R.id.alarmMessage);

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

            spoken =
                    item.name +
                    " করার সময় হয়ে গেছে।";

        } else {

            spoken =
                    cleanSpeechText(
                            item.message
                    );
        }

        msg.setText(spoken);

        findViewById(R.id.stopButton)
                .setOnClickListener(
                        v -> stopAlarm()
                );

        findViewById(R.id.snoozeButton)
                .setOnClickListener(
                        v -> snooze()
                );

        startAlarmCycle(spoken);
    }

    private String cleanSpeechText(String text) {

        if (text == null) {
            return "";
        }

        String cleaned =
                text
                        .replace('\n', ' ')
                        .replace('\r', ' ')
                        .replace('\t', ' ');

        cleaned =
                cleaned.replaceAll(
                        "\\s+",
                        " "
                );

        return cleaned.trim();
    }

    private void startAlarmCycle(String text) {

        if (stopped) {
            return;
        }

        if (cycleCount >= MAX_CYCLES) {
            return;
        }

        cycleCount++;

        startRingtoneOnce();

        /*
         * Say the user's message once for
         * each alarm cycle.
         */
        startSpeechOnce(text);
    }

    private void startRingtoneOnce() {

        try {

            android.net.Uri uri =
                    RingtoneManager.getDefaultUri(
                            RingtoneManager.TYPE_ALARM
                    );

            if (uri == null) {
                uri =
                        RingtoneManager.getDefaultUri(
                                RingtoneManager.TYPE_NOTIFICATION
                        );
            }

            mediaPlayer =
                    MediaPlayer.create(
                            this,
                            uri
                    );

            if (mediaPlayer == null) {
                return;
            }

            if (Build.VERSION.SDK_INT >= 21) {

                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(
                                        AudioAttributes.USAGE_ALARM
                                )
                                .setContentType(
                                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                                )
                                .build()
                );
            }

            mediaPlayer.setLooping(false);

            mediaPlayer.setOnCompletionListener(
                    mp -> {

                        if (stopped) {
                            return;
                        }

                        if (cycleCount < MAX_CYCLES) {

                            handler.postDelayed(
                                    () -> {

                                        if (!stopped) {
                                            startAlarmCycle(
                                                    getSpokenText()
                                            );
                                        }

                                    },
                                    NEXT_CYCLE_DELAY
                            );
                        }
                    }
            );

            mediaPlayer.start();

        } catch (Exception ignored) {
        }
    }

    private String getSpokenText() {

        if (item == null) {
            return "";
        }

        if (item.message == null ||
                item.message.trim().isEmpty()) {

            return item.name +
                    " করার সময় হয়ে গেছে।";
        }

        return cleanSpeechText(
                item.message
        );
    }

    private void startSpeechOnce(String text) {

        if (stopped) {
            return;
        }

        speechCount = 0;

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }
        }

        tts =
                new TextToSpeech(
                        this,
                        status -> {

                            if (status !=
                                    TextToSpeech.SUCCESS) {
                                return;
                            }

                            if (Build.VERSION.SDK_INT >= 21) {

                                tts.setAudioAttributes(
                                        new AudioAttributes.Builder()
                                                .setUsage(
                                                        AudioAttributes.USAGE_ALARM
                                                )
                                                .setContentType(
                                                        AudioAttributes.CONTENT_TYPE_SPEECH
                                                )
                                                .build()
                                );
                            }

                            Locale bengali =
                                    new Locale(
                                            "bn",
                                            "BD"
                                    );

                            int result =
                                    tts.setLanguage(
                                            bengali
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
                             * Normal speaking speed.
                             * No artificial slow speech.
                             */
                            tts.setSpeechRate(
                                    1.0f
                            );

                            tts.setPitch(
                                    1.0f
                            );

                            tts.setOnUtteranceProgressListener(
                                    new UtteranceProgressListener() {

                                        @Override
                                        public void onStart(
                                                String id) {
                                        }

                                        @Override
                                        public void onDone(
                                                String id) {
                                        }

                                        @Override
                                        public void onError(
                                                String id) {
                                        }
                                    }
                            );

                            /*
                             * Entire sentence is sent
                             * as ONE utterance.
                             */
                            tts.speak(
                                    text,
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "alarm_speech_" +
                                            cycleCount
                            );
                        }
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

        handler.removeCallbacksAndMessages(
                null
        );

        if (mediaPlayer != null) {

            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception ignored) {
            }

            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }

            mediaPlayer = null;
        }

        if (tts != null) {

            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }

            tts = null;
        }

        NotificationManager nm =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (nm != null &&
                item != null) {

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

        handler.removeCallbacksAndMessages(
                null
        );

        if (mediaPlayer != null) {

            try {
                mediaPlayer.stop();
            } catch (Exception ignored) {
            }

            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }

            mediaPlayer = null;
        }

        if (tts != null) {

            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }

            tts = null;
        }

        super.onDestroy();
    }
                    }
