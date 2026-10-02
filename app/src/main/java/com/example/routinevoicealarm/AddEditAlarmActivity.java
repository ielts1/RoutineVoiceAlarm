package com.example.routinevoicealarm;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import java.util.*;

public class AddEditAlarmActivity extends Activity {
    TimePicker picker;
    EditText name, message;
    CheckBox[] checks = new CheckBox[7];
    long editId = -1;

    // UI order: Saturday, Sunday, Monday, Tuesday, Wednesday, Thursday, Friday.
    // Calendar constants: Saturday=7, Sunday=1, Monday=2 ... Friday=6.
    final int[] calendarDays = {Calendar.SATURDAY, Calendar.SUNDAY, Calendar.MONDAY,
            Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_add_edit_alarm);

        picker=findViewById(R.id.timePicker);
        name=findViewById(R.id.nameInput);
        message=findViewById(R.id.messageInput);

        int[] ids={R.id.daySat,R.id.daySun,R.id.dayMon,R.id.dayTue,R.id.dayWed,R.id.dayThu,R.id.dayFri};
        for(int i=0;i<7;i++) checks[i]=findViewById(ids[i]);

        editId=getIntent().getLongExtra("alarm_id",-1);
        if(editId!=-1) loadExisting(editId);

        findViewById(R.id.saveButton).setOnClickListener(v -> save());
        findViewById(R.id.cancelButton).setOnClickListener(v -> finish());
    }

    void loadExisting(long id) {
        AlarmItem a=new AlarmStorage(this).get(id);
        if(a==null) return;
        picker.setHour(a.hour); picker.setMinute(a.minute);
        name.setText(a.name); message.setText(a.message);
        for(int i=0;i<7;i++) checks[i].setChecked(a.days[calendarDays[i]]);
        ((TextView)findViewById(R.id.screenTitle)).setText("Edit Routine Alarm");
    }

    void save() {
        String n=name.getText().toString().trim();
        if(n.isEmpty()){ name.setError("Enter routine name"); return; }

        AlarmItem a=new AlarmItem(editId==-1 ? System.currentTimeMillis() : editId);
        a.hour=picker.getHour(); a.minute=picker.getMinute();
        a.name=n; a.message=message.getText().toString().trim(); a.enabled=true;

        boolean any=false;
        for(int i=0;i<7;i++){ a.days[calendarDays[i]]=checks[i].isChecked(); any|=checks[i].isChecked(); }
        if(!any){ Toast.makeText(this,"Select at least one day",Toast.LENGTH_SHORT).show(); return; }

        AlarmStorage s=new AlarmStorage(this);
        if(editId!=-1) {
            AlarmItem old=s.get(editId);
            if(old!=null) AlarmScheduler.cancel(this,old);
        }
        s.save(a);
        AlarmScheduler.scheduleNext(this,a);
        Toast.makeText(this,"Routine saved",Toast.LENGTH_SHORT).show();
        finish();
    }
}
