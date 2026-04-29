package com.nanu.healthbridge;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nanu.healthbridge.db.HealthDay;
import com.nanu.healthbridge.db.HealthDayDao;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DayManager {
    private static final String TAG = "DayManager";
    private static DayManager instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private DayManager() {}

    public static synchronized DayManager getInstance() {
        if (instance == null) {
            instance = new DayManager();
        }
        return instance;
    }

    public String getTodayDate() {
        return LocalDate.now().toString();
    }

    public void getOrCreateToday(HealthDayDao dao, OnDayLoadedCallback callback) {
        executor.execute(() -> {
            String date = getTodayDate();
            HealthDay day = dao.getByDate(date);
            if (day == null) {
                day = new HealthDay();
                day.date = date;
                
                // Load yesterday's strain for recovery penalty
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.DATE, -1);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                String yesterdayDate = sdf.format(cal.getTime());
                HealthDay yesterday = dao.getByDate(yesterdayDate);
                if (yesterday != null) {
                    day.yesterdayStrainScore = (int)yesterday.strainScore;
                }

                day.createdAt = System.currentTimeMillis();
                day.updatedAt = System.currentTimeMillis();
                day.restingHR = 0;
                day.avgHR = 0;
                day.maxHR = 0;
                day.totalSteps = 0;
                day.recoveryCalculated = false;
                dao.insertOrUpdate(day);
                Log.d(TAG, "Created new HealthDay for " + date);
            }
            callback.onLoaded(day);
        });
    }

    public void updateHRReading(HealthDayDao dao, int bpm, float spO2) {
        executor.execute(() -> {
            String date = getTodayDate();
            HealthDay day = dao.getByDate(date);
            if (day == null) return;

            // Only update restingHR if within sleep window
            long now = System.currentTimeMillis();
            boolean inSleepWindow = false;

            if (day.sleepStartTime > 0 && day.sleepEndTime > 0) {
                if (now >= day.sleepStartTime && now <= day.sleepEndTime) {
                    inSleepWindow = true;
                }
            } else {
                // Default night window: 10:00 PM (22:00) to 8:00 AM
                Calendar cal = Calendar.getInstance();
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                if (hour >= 22 || hour <= 8) {
                    inSleepWindow = true;
                }
            }

            // Additional filter: 40-80 BPM
            if (inSleepWindow && bpm >= 40 && bpm <= 80) {
                if (day.restingHR == 0 || bpm < day.restingHR) {
                    day.restingHR = bpm;
                }
            }

            // Max HR
            if (bpm > day.maxHR) {
                day.maxHR = bpm;
            }

            // Avg HR (simple running average for the day)
            if (bpm > 0) {
                if (day.avgHR == 0) {
                    day.avgHR = bpm;
                } else {
                    day.avgHR = (day.avgHR + bpm) / 2;
                }
            }

            // SpO2
            if (spO2 > 0) {
                if (day.avgSpO2 == 0) {
                    day.avgSpO2 = spO2;
                    day.minSpO2 = spO2;
                } else {
                    day.avgSpO2 = (day.avgSpO2 + spO2) / 2;
                    if (spO2 < day.minSpO2) day.minSpO2 = spO2;
                }
            }

            day.updatedAt = System.currentTimeMillis();
            dao.insertOrUpdate(day);
        });
    }


    public void updateSteps(HealthDayDao dao, int steps, float distance, float calories) {
        executor.execute(() -> {
            String date = getTodayDate();
            HealthDay day = dao.getByDate(date);
            if (day == null) return;

            day.totalSteps = steps;
            day.totalDistanceKm = distance;
            day.totalCalories = calories;
            day.stepsManualInput = false;
            day.updatedAt = System.currentTimeMillis();
            dao.insertOrUpdate(day);
        });
    }

    public void updateSleep(HealthDayDao dao, int total, int deep, int rem, int light, int awake, long start, long end, boolean manual) {
        executor.execute(() -> {
            String date = getTodayDate();
            HealthDay day = dao.getByDate(date);
            if (day == null) return;

            day.sleepTotalMinutes = total;
            day.sleepDeepMinutes = deep;
            day.sleepRemMinutes = rem;
            day.sleepLightMinutes = light;
            day.sleepAwakeMinutes = awake;
            day.sleepStartTime = start;
            day.sleepEndTime = end;
            day.sleepManualInput = manual;
            day.updatedAt = System.currentTimeMillis();
            dao.insertOrUpdate(day);
        });
    }

    public void updateStrainFromWorkout(HealthDayDao dao, int durationMin, int avgHR, int maxHR) {
        executor.execute(() -> {
            String date = getTodayDate();
            HealthDay day = dao.getByDate(date);
            if (day == null) return;

            // Fetch profile for personalized max HR
            com.nanu.healthbridge.db.HealthDao hDao = com.nanu.healthbridge.db.HealthDatabase.getInstance(HealthBridgeApp.getInstance()).healthDao();
            com.nanu.healthbridge.db.UserProfile profile = hDao.getUserProfile();
            if (profile == null) profile = new com.nanu.healthbridge.db.UserProfile();

            int rhr = day.restingHR > 0 ? day.restingHR : profile.getNormalRestingHR();
            int maxPossible = profile.getMaxExpectedHR();
            double hrReserve = maxPossible - rhr;
            if (hrReserve <= 0) hrReserve = 100; // safety
            
            double intensity = (avgHR - rhr) / hrReserve;
            intensity = Math.max(0, intensity);
            
            int strainPoints = (int) (intensity * durationMin * 2);
            day.strainScore = Math.min(day.strainScore + strainPoints, 100);
            day.strainMinutes += durationMin;

            if (day.strainScore <= 25) day.strainLevel = "RESTORATIVE";
            else if (day.strainScore <= 50) day.strainLevel = "MODERATE";
            else if (day.strainScore <= 75) day.strainLevel = "HIGH";
            else day.strainLevel = "ALL_OUT";

            day.updatedAt = System.currentTimeMillis();
            dao.insertOrUpdate(day);
        });
    }

    public void scheduleMidnightReset(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MidnightReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 0);

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pendingIntent);
        }
    }

    public interface OnDayLoadedCallback {
        void onLoaded(HealthDay day);
    }
}
