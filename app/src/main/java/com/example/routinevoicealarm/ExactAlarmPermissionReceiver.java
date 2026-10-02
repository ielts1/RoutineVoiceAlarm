package com.example.routinevoicealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ExactAlarmPermissionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        AlarmScheduler.rescheduleAll(context);
    }
}
