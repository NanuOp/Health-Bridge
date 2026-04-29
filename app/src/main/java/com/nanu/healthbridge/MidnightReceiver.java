package com.nanu.healthbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nanu.healthbridge.db.HealthDatabase;
import com.nanu.healthbridge.db.HealthDayDao;

public class MidnightReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MidnightReceiver", "Finalizing health day...");
        HealthDayDao dao = HealthDatabase.getInstance(context).healthDayDao();
        // DayManager logic handles creation of new day on first access after midnight
        // Any specific end-of-day logic can be added here
    }
}
