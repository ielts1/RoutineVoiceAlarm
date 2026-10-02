package com.example.routinevoicealarm;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout list;
    AlarmStorage storage;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        storage=new AlarmStorage(this);
        setContentView(R.layout.activity_main);
        findViewById(R.id.addButton).setOnClickListener(v -> {
            startActivity(new Intent(this,AddEditAlarmActivity.class));
        });
        findViewById(R.id.settingsButton).setOnClickListener(v -> openPermissionSettings());
        requestNotificationPermission();
    }

    @Override protected void onResume(){
        super.onResume();
        render();
        if(Build.VERSION.SDK_INT>=31){
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            if(am!=null && am.canScheduleExactAlarms()) AlarmScheduler.rescheduleAll(this);
        }
    }

    void render(){
        list=findViewById(R.id.alarmList);
        list.removeAllViews();
        TextView today=findViewById(R.id.today);
        today.setText(new SimpleDateFormat("EEEE, dd MMMM",Locale.getDefault()).format(new Date()));

        List<AlarmItem> items=storage.getAll();
        items.sort((a,b)->{
            int x=a.hour*60+a.minute, y=b.hour*60+b.minute;
            return Integer.compare(x,y);
        });

        if(items.isEmpty()){
            TextView empty=new TextView(this);
            empty.setText("No routine alarms yet.\\nTap “+ Add Routine Alarm” to create your first one.");
            empty.setTextSize(18);
            empty.setPadding(8,30,8,30);
            list.addView(empty);
            return;
        }

        for(AlarmItem a:items) addCard(a);
    }

    void addCard(AlarmItem a){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20,16,20,16);
        card.setBackgroundColor(Color.WHITE);

        TextView top=new TextView(this);
        top.setText(String.format(Locale.getDefault(),"%02d:%02d  •  %s",
                a.hour==0?12:(a.hour>12?a.hour-12:a.hour),
                a.minute, a.hour<12?"AM":"PM"));
        top.setTextSize(25); top.setTextColor(Color.rgb(79,70,229));
        card.addView(top);

        TextView n=new TextView(this);
        n.setText(a.name);
        n.setTextSize(20); n.setTextColor(Color.DKGRAY);
        card.addView(n);

        TextView d=new TextView(this);
        d.setText(daysText(a));
        d.setTextSize(15); d.setTextColor(Color.GRAY);
        card.addView(d);

        LinearLayout buttons=new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button toggle=new Button(this);
        toggle.setText(a.enabled?"ON":"OFF");
        toggle.setOnClickListener(v->{
            a.enabled=!a.enabled; storage.save(a);
            if(a.enabled) AlarmScheduler.scheduleNext(this,a); else AlarmScheduler.cancel(this,a);
            render();
        });

        Button edit=new Button(this);
        edit.setText("Edit");
        edit.setOnClickListener(v->{
            Intent i=new Intent(this,AddEditAlarmActivity.class);
            i.putExtra("alarm_id",a.id); startActivity(i);
        });

        Button del=new Button(this);
        del.setText("Delete");
        del.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("Delete routine?")
                .setMessage(a.name)
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Delete",(d1,w)->{
                    AlarmScheduler.cancel(this,a); storage.delete(a.id); render();
                }).show());

        buttons.addView(toggle,new LinearLayout.LayoutParams(0,-2,1));
        buttons.addView(edit,new LinearLayout.LayoutParams(0,-2,1));
        buttons.addView(del,new LinearLayout.LayoutParams(0,-2,1));
        card.addView(buttons);

        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);
        cp.setMargins(0,0,0,14);
        list.addView(card,cp);
    }

    String daysText(AlarmItem a){
        String[] names={"","Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        StringBuilder s=new StringBuilder("Days: ");
        boolean first=true;
        for(int i=1;i<=7;i++) if(a.days[i]){
            if(!first)s.append(", ");
            s.append(names[i]); first=false;
        }
        return s.toString();
    }

    void requestNotificationPermission(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},40);
        }
    }

    void openPermissionSettings(){
        if(Build.VERSION.SDK_INT>=31){
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            if(am!=null && !am.canScheduleExactAlarms()){
                try{
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:"+getPackageName())));
                    return;
                }catch(Exception ignored){}
            }
        }
        try{
            Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());
            startActivity(i);
        }catch(Exception ignored){}
    }
}
