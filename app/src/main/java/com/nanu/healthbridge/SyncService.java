package com.nanu.healthbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.nanu.healthbridge.db.HealthDao;
import com.nanu.healthbridge.db.HealthDatabase;
import com.nanu.healthbridge.db.HeartRateEntity;
import com.nanu.healthbridge.db.OxygenEntity;
import com.nanu.healthbridge.db.SleepEntity;
import com.nanu.healthbridge.db.SleepRecordEntity;
import com.nanu.healthbridge.db.StepEntity;
import com.nanu.healthbridge.db.WorkoutEntity;
import com.topstep.fitcloud.sdk.v2.FcConnector;
import com.topstep.fitcloud.sdk.v2.FcSDK;
import com.topstep.fitcloud.sdk.v2.features.FcDataFeature;
import com.topstep.fitcloud.sdk.v2.features.FcSettingsFeature;
import com.topstep.fitcloud.sdk.v2.model.data.FcHeartRateData;
import com.topstep.fitcloud.sdk.v2.model.data.FcOxygenData;
import com.topstep.fitcloud.sdk.v2.model.data.FcSleepData;
import com.topstep.fitcloud.sdk.v2.model.data.FcSleepItem;
import com.topstep.fitcloud.sdk.v2.model.data.FcSportData;
import com.topstep.fitcloud.sdk.v2.model.data.FcStepData;
import com.topstep.fitcloud.sdk.v2.model.data.FcSyncData;
import com.topstep.fitcloud.sdk.v2.model.data.FcSyncDataType;
import com.topstep.fitcloud.sdk.v2.model.data.FcTodayTotalData;
import com.topstep.fitcloud.sdk.v2.utils.SleepCalculateHelper;
import com.topstep.fitcloud.sdk.v2.utils.SleepSummary;
import com.topstep.wearkit.base.connector.ConnectorState;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * Background service that periodically grabs health data from watch, 
 * saves it to Room DB, and sends to Ubuntu PC.
 */
public class SyncService extends Service {
    private static final String TAG = "SyncService";
    private static final String CHANNEL_ID = "healthbridge_sync";
    private static final long SYNC_INTERVAL = 30 * 60 * 1000; // 30 minutes

    private String serverIp;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private FcSDK sdk;
    private FcConnector connector;
    private HealthDao dao;
    private com.nanu.healthbridge.db.HealthDayDao dayDao;
    private HealthPayload pendingPayload;

    @Override
    public void onCreate() {
        super.onCreate();
        sdk = HealthBridgeApp.getFcSDK();
        connector = sdk.getConnector();
        dao = HealthDatabase.getInstance(this).healthDao();
        dayDao = HealthDatabase.getInstance(this).healthDayDao();
        createNotificationChannel();
        startForeground(1, getNotification("Waiting for watch connection..."));
    }

    public static void enqueueSync(android.content.Context context) {
        Intent intent = new Intent(context, SyncService.class);
        intent.putExtra("force_sync", true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra("server_ip")) {
                serverIp = intent.getStringExtra("server_ip");
                startAutoSync();
            }
            if (intent.getBooleanExtra("force_sync", false)) {
                runSyncCycle();
            }
        }
        return START_STICKY;
    }

    private void startAutoSync() {
        handler.removeCallbacksAndMessages(null);
        runSyncCycle();
    }

    private void runSyncCycle() {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        Log.d(TAG, "[" + time + "] Checking connection for sync...");

        // Check if watch is currently connected
        if (connector.getConnectorState() == ConnectorState.CONNECTED) {
            Log.d(TAG, "Watch connected, syncing data...");
            updateNotification("Syncing health data...");
            fetchAndSendData();
        } else {
            Log.d(TAG, "Watch not connected, will retry in 30 min");
            updateNotification("Waiting for watch connection...");
        }

        // Schedule next cycle
        handler.postDelayed(this::runSyncCycle, SYNC_INTERVAL);
    }

    private void fetchAndSendData() {
        pendingPayload = new HealthPayload();
        pendingPayload.syncTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        FcSettingsFeature settings = connector.settingsFeature();

        // Get real-time health data
        disposables.add(settings.requestLatestHealthData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    pendingPayload.realTimeHealth = new HealthPayload.RealTimeHealth(
                            result.getHeartRate(), result.getOxygen(),
                            result.getSystolicPressure(), result.getDiastolicPressure(),
                            result.getTemperatureBody(), result.getRespiratoryRate(),
                            0 // pressure placeholder
                    );
                    Log.d(TAG, "Got real-time health: HR=" + result.getHeartRate());
                }, error -> {
                    Log.e(TAG, "Real-time health failed: " + error.getMessage());
                }));

        // Get battery
        disposables.add(settings.requestBattery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(battery -> {
                    pendingPayload.battery = battery.getPercentage();
                    Log.d(TAG, "Battery: " + battery.getPercentage() + "%");
                }, error -> Log.e(TAG, "Battery request failed: " + error.getMessage())));

        FcDataFeature dataFeature = connector.dataFeature();
        // Perform data sync
        trySyncData();
        
        // Explicitly request historical steps and sleep (often needed for older data)
        disposables.add(dataFeature.syncData() // Trigger again if needed
                .subscribeOn(Schedulers.io())
                .subscribe(data -> Log.d(TAG, "History sync type: " + data.getType()), 
                           err -> Log.e(TAG, "History sync failed: " + err.getMessage())));
    }

    private void trySyncData() {
        Log.d(TAG, "Starting full data sync...");
        FcDataFeature dataFeature = connector.dataFeature();
        disposables.add(dataFeature.syncData()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io()) // Run processing on IO thread
                .subscribe(syncData -> {
                    Log.d(TAG, "Received sync data type: " + syncData.getType());
                    processSyncData(syncData);
                }, error -> {
                    Log.e(TAG, "Sync data failed: " + error.getMessage());
                    sendToPC();
                }, () -> {
                    Log.d(TAG, "Sync data completed");
                    sendToPC();
                }));
    }

    private void processSyncData(FcSyncData syncData) {
        if (syncData.getType() == FcSyncDataType.STEP) {
            List<FcStepData> steps = syncData.toStep();
            if (steps != null && !steps.isEmpty()) {
                if (pendingPayload.steps == null) pendingPayload.steps = new ArrayList<>();
                List<StepEntity> stepEntities = new ArrayList<>();
                for (FcStepData data : steps) {
                    pendingPayload.steps.add(new HealthPayload.StepRecord(
                            data.getTimestamp(), data.getStep(), data.getDistance(),
                            data.getCalories(), data.getSportDuration()
                    ));
                    StepEntity se = new StepEntity();
                    se.timestamp = data.getTimestamp();
                    se.steps = data.getStep();
                    se.distanceKm = data.getDistance();
                    se.caloriesKcal = data.getCalories();
                    se.source = "sync";
                    stepEntities.add(se);
                }
                dao.insertSteps(stepEntities);
                // Aggregator
                StepEntity latest = stepEntities.get(stepEntities.size() - 1);
                DayManager.getInstance().updateSteps(dayDao, latest.steps, latest.distanceKm, latest.caloriesKcal);
            }
        } 
        else if (syncData.getType() == FcSyncDataType.HEART_RATE) {
            List<FcHeartRateData> hrList = syncData.toHeartRate();
            if (hrList != null && !hrList.isEmpty()) {
                if (pendingPayload.heartRate == null) pendingPayload.heartRate = new ArrayList<>();
                List<HeartRateEntity> hrEntities = new ArrayList<>();
                for (FcHeartRateData data : hrList) {
                    pendingPayload.heartRate.add(new HealthPayload.HRRecord(
                            data.getTimestamp(), data.getHeartRate()
                    ));
                    HeartRateEntity he = new HeartRateEntity();
                    he.timestamp = data.getTimestamp();
                    he.bpm = data.getHeartRate();
                    he.source = "sync";
                    hrEntities.add(he);
                }
                dao.insertHeartRates(hrEntities);
            }
        }
        else if (syncData.getType() == FcSyncDataType.SLEEP) {
            List<FcSleepData> sleepList = syncData.toSleep();
            if (sleepList != null && !sleepList.isEmpty()) {
                if (pendingPayload.sleep == null) pendingPayload.sleep = new ArrayList<>();
                
                for (FcSleepData sleepData : sleepList) {
                    long dayTimestamp = sleepData.getTimestamp();
                    
                    // Handle conflicts: delete existing sleep for this day from DB
                    dao.deleteSleepItemsForDay(dayTimestamp);
                    
                    List<FcSleepItem> items = sleepData.getItems();
                    if (items != null) {
                        List<SleepEntity> sleepEntities = new ArrayList<>();
                        for (FcSleepItem item : items) {
                            long durationMs = item.getEndTime() - item.getStartTime();
                            int durationMinutes = (int) (durationMs / 60000);
                            
                            pendingPayload.sleep.add(new HealthPayload.SleepRecord(
                                    item.getStartTime(), item.getStatus(), durationMinutes
                            ));
                            
                            SleepEntity se = new SleepEntity();
                            se.timestamp = dayTimestamp;
                            se.startTime = item.getStartTime();
                            se.endTime = item.getEndTime();
                            se.status = item.getStatus();
                            se.durationMinutes = durationMinutes;
                            se.isNap = sleepData.isSupportSleepNap(); // true if watch supports nap feature
                            
                            switch (item.getStatus()) {
                                case 1: se.statusName = "Deep"; break;
                                case 2: se.statusName = "Light"; break;
                                case 4: se.statusName = "REM"; break;
                                default: se.statusName = "Awake"; break;
                            }
                            sleepEntities.add(se);
                        }
                        dao.insertSleepRecords(sleepEntities);
                    }
                    
                    // Calculate sleep summaries manually since SleepCalculateHelper often returns null
                    SleepRecordEntity record = new SleepRecordEntity();
                    record.dayTimestamp = dayTimestamp;
                    record.isSupportSleepNap = sleepData.isSupportSleepNap();
                    
                    int deepSecs = 0, lightSecs = 0, remSecs = 0, soberSecs = 0;
                    for (FcSleepItem item : items) {
                        long durationSec = (item.getEndTime() - item.getStartTime()) / 1000;
                        if (item.getStatus() == 1) deepSecs += durationSec;
                        else if (item.getStatus() == 2) lightSecs += durationSec;
                        else if (item.getStatus() == 4) remSecs += durationSec;
                        else soberSecs += durationSec;
                    }
                    
                    record.deepSeconds = deepSecs;
                    record.lightSeconds = lightSecs;
                    record.remSeconds = remSecs;
                    record.soberSeconds = soberSecs;
                    record.napSeconds = 0; // Only tracking main sleep for now

                    // Calculate basic score and efficiency
                    int totalSleep = deepSecs + lightSecs + remSecs;
                    int totalTime = totalSleep + soberSecs;
                    if (totalTime > 0) {
                        record.efficiency = (int) ((totalSleep * 100L) / totalTime);
                    }
                    long startTime = 0, endTime = 0;
                    if (items != null && !items.isEmpty()) {
                        startTime = items.get(0).getStartTime();
                        endTime = items.get(items.size() - 1).getEndTime();
                    }
                    
                    dao.insertSleepRecord(record);
                    DayManager.getInstance().updateSleep(dayDao, totalSleep / 60, deepSecs / 60, remSecs / 60, lightSecs / 60, soberSecs / 60, startTime, endTime, false);
                }
            }
        }
        else if (syncData.getType() == FcSyncDataType.SPORT) {
            List<FcSportData> sportList = syncData.toSport();
            if (sportList != null && !sportList.isEmpty()) {
                List<WorkoutEntity> sportEntities = new ArrayList<>();
                for (FcSportData data : sportList) {
                    WorkoutEntity we = new WorkoutEntity();
                    we.timestamp = data.getTimestamp();
                    we.durationSec = data.getDuration();
                    we.distanceM = (int) data.getDistance();
                    we.distanceKm = data.getDistance() / 1000f;
                    we.caloriesKcal = data.getCalories();
                    we.steps = data.getSteps();
                    we.type = data.getType();
                    we.typeName = SportTypeHelper.getSportName(data.getType());
                    we.source = "sync";
                    
                    if (data.getHeartRateItems() != null && !data.getHeartRateItems().isEmpty()) {
                        int min = 999, max = 0, sum = 0;
                        for (com.topstep.fitcloud.sdk.v2.model.data.FcSportHeartRateItem item : data.getHeartRateItems()) {
                            int hr = item.getHeartRate();
                            if (hr > 0) {
                                if (hr < min) min = hr;
                                if (hr > max) max = hr;
                                sum += hr;
                            }
                        }
                        we.minHeartRate = min == 999 ? 0 : min;
                        we.maxHeartRate = max;
                        if (data.getHeartRateItems().size() > 0) {
                            we.avgHeartRate = sum / data.getHeartRateItems().size();
                        }
                    }
                    sportEntities.add(we);
                }
                dao.insertWorkouts(sportEntities);
                for (WorkoutEntity we : sportEntities) {
                    DayManager.getInstance().updateStrainFromWorkout(dayDao, (int)(we.durationSec / 60), we.avgHeartRate, we.maxHeartRate);
                }
            }
        }
        else if (syncData.getType() == FcSyncDataType.OXYGEN) {
            List<FcOxygenData> oxygenList = syncData.toOxygen();
            if (oxygenList != null && !oxygenList.isEmpty()) {
                List<OxygenEntity> oxEntities = new ArrayList<>();
                for (FcOxygenData data : oxygenList) {
                    OxygenEntity ox = new OxygenEntity();
                    ox.timestamp = data.getTimestamp();
                    ox.spo2 = data.getOxygen();
                    ox.source = "sync";
                    oxEntities.add(ox);
                }
                dao.insertOxygenRecords(oxEntities);
            }
        }
            // Oxygen processed above
        else if (syncData.getType() == FcSyncDataType.TODAY_TOTAL_DATA) {
            FcTodayTotalData todayTotal = syncData.toTodayTotal();
            if (todayTotal != null) {
                pendingPayload.todayTotal = new HealthPayload.TodayTotal(
                        todayTotal.getStep(), todayTotal.getDistance(),
                        todayTotal.getCalorie(), todayTotal.getHeartRate()
                );
                
                dbExecutor.execute(() -> {
                    long startOfDay = getTodayStartMillis();
                    dao.deleteTodayTotalSteps(startOfDay);
                    
                    StepEntity se = new StepEntity();
                    se.timestamp = System.currentTimeMillis();
                    se.steps = todayTotal.getStep();
                    se.distanceKm = todayTotal.getDistance();
                    se.caloriesKcal = todayTotal.getCalorie();
                    se.source = "total";
                    dao.insertStep(se);
                    DayManager.getInstance().updateSteps(dayDao, se.steps, se.distanceKm, se.caloriesKcal);
                    Log.d(TAG, "Saved today total steps to DB: " + todayTotal.getStep());
                });
            }
        }
    }

    private void sendToPC() {
        if (serverIp == null || pendingPayload == null) return;
        
        final HealthPayload payloadToSend = pendingPayload;
        pendingPayload = null; // Clear it for next sync
        
        DataSender sender = new DataSender(serverIp, 8765);
        networkExecutor.execute(() -> sender.sendHealthData(payloadToSend, new DataSender.SendCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Data sent to Ubuntu PC");
                handler.post(() -> updateNotification("Last sync: " +
                        new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date())));
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Send failed: " + error);
                handler.post(() -> updateNotification("Send failed: " + error));
            }
        }));
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(1, getNotification(text));
        }
    }

    private Notification getNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("HealthBridge Sync")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private long getTodayStartMillis() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Health Sync", NotificationManager.IMPORTANCE_LOW);
            serviceChannel.setDescription("Background health data sync from watch");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        disposables.clear();
        networkExecutor.shutdown();
        dbExecutor.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
