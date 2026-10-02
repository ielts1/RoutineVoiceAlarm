package com.example.routinevoicealarm;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class AlarmActivity extends Activity {
    private Ringtone ringtone;
    private TextToSpeech tts;
    private AlarmItem item;
    private Handler repeatHandler = new Handler(Looper.getMainLooper());
    private boolean stopped = false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        setContentView(R.layout.activity_alarm);

        long id = getIntent().getLongExtra("alarm_id", -1);
        item = new AlarmStorage(this).get(id);
        if (item == null) { finish(); return; }

        TextView time = findViewById(R.id.alarmTime);
        TextView title = findViewById(R.id.alarmTitle);
        TextView msg = findViewById(R.id.alarmMessage);
        time.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));
        title.setText(item.name);
        String spoken = item.message == null || item.message.trim().isEmpty()
                ? item.name + " করার সময় হয়ে গেছে।"
                : item.message;
        msg.setText(spoken);

        findViewById(R.id.stopButton).setOnClickListener(v -> stopAlarm());
        findViewById(R.id.snoozeButton).setOnClickListener(v -> snooze());

        startRingtone();
        startSpeech(spoken);
    }

    private void startRingtone() {
        try {
            android.net.Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (Build.VERSION.SDK_INT >= 21)
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            ringtone.play();
        } catch (Exception ignored) {}
    }

    private void startSpeech(String text) {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new Locale("bn", "BD"));
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED)
                    tts.setLanguage(Locale.getDefault());
                speakRepeated(text);
            }
        });
    }

    private void speakRepeated(String text) {
        if (stopped || tts == null) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "routine_voice");
        repeatHandler.postDelayed(() -> speakRepeated(text), 15000);
    }

    private void snooze() {
        if (item != null) AlarmScheduler.scheduleSnooze(this, item,
                System.currentTimeMillis() + 10 * 60 * 1000L);
        stopAlarm();
    }

    private void stopAlarm() {
        stopped = true;
        repeatHandler.removeCallbacksAndMessages(null);
        if (ringtone != null) ringtone.stop();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (nm != null && item != null) nm.cancel((int)(item.id % Integer.MAX_VALUE));
        finishAndRemoveTask();
    }

    @Override protected void onDestroy() {
        stopped = true;
        repeatHandler.removeCallbacksAndMessages(null);
        if (ringtone != null) try { ringtone.stop(); } catch(Exception ignored) {}
        if (tts != null) { try { tts.stop(); } catch(Exception ignored) {} tts.shutdown(); }
        super.onDestroy();
    }
}
