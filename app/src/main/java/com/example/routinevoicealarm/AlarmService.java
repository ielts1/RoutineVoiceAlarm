package com.example.routinevoicealarm;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.speech.tts.TextToSpeech;

import java.util.*;

public class AlarmService extends Service {

    public static final String ACTION_STOP =
            "com.example.routinevoicealarm.STOP";

    public static final String ACTION_SNOOZE =
            "com.example.routinevoicealarm.SNOOZE";

    private MediaPlayer player;
    private TextToSpeech tts;
    private AlarmItem item;
    private int cycle = 0;
    private boolean ringtoneDone = false;
    private boolean speechDone = false;
    private boolean stopped = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_STOP.equals(action)) {
                stopAlarm();
                return START_NOT_STICKY;
            }

            if (ACTION_SNOOZE.equals(action)) {
                if (item != null) {
                    AlarmScheduler.scheduleSnooze(
                            this,
                            item,
                            System.currentTimeMillis()
                                    + 10 * 60 * 1000L
                    );
                }
                stopAlarm();
                return START_NOT_STICKY;
            }

            long id = intent.getLongExtra("alarm_id", -1);

            if (id != -1 && item == null) {
                item = new AlarmStorage(this).get(id);

                if (item != null) {
                    startForegroundNotification();
                    startAlarm();
                }
            }
        }

        return START_NOT_STICKY;
    }

    private void startForegroundNotification() {

        String channelId = "routine_alarm_playback";

        NotificationManager nm =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                    new NotificationChannel(
                            channelId,
                            "Routine alarm playback",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setSound(null, null);
            nm.createNotificationChannel(channel);
        }

        Notification notification =
                new Notification.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_alarm)
                        .setContentTitle(
                                item.name
                        )
                        .setContentText(
                                "Routine alarm is running"
                        )
                        .setOngoing(true)
                        .build();

        startForeground(
                10001,
                notification
        );
    }

    private void startAlarm() {

        stopped = false;
        cycle = 0;

        String text =
                item.message == null ||
                item.message.trim().isEmpty()
                        ? item.name +
                          " করার সময় হয়ে গেছে।"
                        : item.message.trim();

        text = text
                .replaceAll("\\s+", " ")
                .trim();

        startCycle(text);
    }

    private void startCycle(String text) {

        if (stopped || cycle >= 3) {
            stopAlarm();
            return;
        }

        cycle++;

        ringtoneDone = false;
        speechDone = false;

        playRingtone();
        speak(text);
    }

    private void playRingtone() {

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

            player =
                    MediaPlayer.create(
                            this,
                            uri
                    );

            if (player == null) {
                ringtoneDone = true;
                checkCycleFinished();
                return;
            }

            if (Build.VERSION.SDK_INT >= 21) {

                player.setAudioAttributes(
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

            player.setOnCompletionListener(
                    mp -> {

                        ringtoneDone = true;

                        try {
                            mp.release();
                        } catch (Exception ignored) {}

                        player = null;

                        checkCycleFinished();
                    }
            );

            player.start();

        } catch (Exception e) {

            ringtoneDone = true;
            checkCycleFinished();
        }
    }

    private void speak(String text) {

        tts =
                new TextToSpeech(
                        this,
                        status -> {

                            if (status != TextToSpeech.SUCCESS) {
                                speechDone = true;
                                checkCycleFinished();
                                return;
                            }

                            try {

                                int result =
                                        tts.setLanguage(
                                                new Locale(
                                                        "bn",
                                                        "BD"
                                                )
                                        );

                                if (result ==
                                        TextToSpeech.LANG_MISSING_DATA ||
                                    result ==
                                        TextToSpeech.LANG_NOT_SUPPORTED) {

                                    tts.setLanguage(
                                            Locale.getDefault()
                                    );
                                }

                                tts.setSpeechRate(1.0f);

                                tts.setOnUtteranceProgressListener(
                                        new android.speech.tts.UtteranceProgressListener() {

                                            @Override
                                            public void onStart(
                                                    String utteranceId) {
                                            }

                                            @Override
                                            public void onDone(
                                                    String utteranceId) {

                                                speechDone = true;
                                                checkCycleFinished();
                                            }

                                            @Override
                                            public void onError(
                                                    String utteranceId) {

                                                speechDone = true;
                                                checkCycleFinished();
                                            }
                                        }
                                );

                                tts.speak(
                                        text,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "routine_alarm"
                                                + cycle
                                );

                            } catch (Exception e) {

                                speechDone = true;
                                checkCycleFinished();
                            }
                        }
                );
    }

    private void checkCycleFinished() {

        if (stopped) return;

        if (ringtoneDone && speechDone) {

            new Handler(
                    Looper.getMainLooper()
            ).postDelayed(
                    () -> {

                        if (!stopped) {

                            if (cycle < 3) {

                                String text =
                                        item.message == null ||
                                        item.message.trim().isEmpty()
                                                ? item.name +
                                                  " করার সময় হয়ে গেছে।"
                                                : item.message.trim();

                                text = text
                                        .replaceAll(
                                                "\\s+",
                                                " "
                                        )
                                        .trim();

                                startCycle(text);

                            } else {

                                stopAlarm();
                            }
                        }

                    },
                    500
            );
        }
    }

    private void stopAlarm() {

        stopped = true;

        if (player != null) {

            try {
                if (player.isPlaying())
                    player.stop();
            } catch (Exception ignored) {}

            try {
                player.release();
            } catch (Exception ignored) {}

            player = null;
        }

        if (tts != null) {

            try {
                tts.stop();
            } catch (Exception ignored) {}

            try {
                tts.shutdown();
            } catch (Exception ignored) {}

            tts = null;
        }

        NotificationManager nm =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        if (nm != null && item != null) {

            nm.cancel(
                    (int)(
                            item.id %
                            Integer.MAX_VALUE
                    )
            );
        }

        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {

        stopAlarm();
        super.onDestroy();
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }
                      }
