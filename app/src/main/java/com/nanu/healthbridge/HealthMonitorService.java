package com.nanu.healthbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import com.topstep.fitcloud.sdk.v2.FcConnector;
import com.topstep.fitcloud.sdk.v2.features.FcDataFeature;
import com.topstep.fitcloud.sdk.v2.features.FcSettingsFeature;
import com.topstep.fitcloud.sdk.v2.model.data.FcHealthDataType;
import com.topstep.wearkit.base.connector.ConnectorState;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 24x7 Background health monitoring service.
 * Loops through 1-minute measurement brackets for HR and SpO2.
 * Data is stored to Room DB and updates the daily recovery metrics.
 */
public class HealthMonitorService extends Service {
    private static final String TAG = "HealthMonitor";
    private static final String CHANNEL_ID = "health_monitor_channel";
    private static final int NOTIFICATION_ID = 2;
    private static final long PAUSE_BETWEEN_CYCLES = 5000; // 5s pause to let watch breathe

    public static final String ACTION_START = "com.nanu.healthbridge.MONITOR_START";
    public static final String ACTION_STOP = "com.nanu.healthbridge.MONITOR_STOP";

    private FcConnector connector;
    private HealthDao dao;
    private com.nanu.healthbridge.db.HealthDayDao dayDao;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private Disposable measureDisposable;
    private boolean isRunning = false;

    private int lastHR = 0;
    private int lastSpO2 = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        connector = HealthBridgeApp.getFcSDK().getConnector();
        dao = HealthDatabase.getInstance(this).healthDao();
        dayDao = HealthDatabase.getInstance(this).healthDayDao();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopMonitoring();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!isRunning) {
            isRunning = true;
            startForeground(NOTIFICATION_ID, buildNotification("Starting health monitoring..."));
            startMeasurementLoop();
            observeConnection();
        }
        return START_STICKY;
    }

    private void observeConnection() {
        disposables.add(connector.observerConnectorState()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    if (state == ConnectorState.CONNECTED && isRunning) {
                        Log.d(TAG, "Watch reconnected, restarting measurement");
                        handler.removeCallbacksAndMessages(null);
                        handler.postDelayed(this::startMeasurementLoop, 3000);
                    } else if (state == ConnectorState.DISCONNECTED) {
                        Log.d(TAG, "Watch disconnected");
                        updateNotification("Waiting for watch reconnection...");
                        if (measureDisposable != null && !measureDisposable.isDisposed()) {
                            measureDisposable.dispose();
                        }
                    }
                }));
    }

    private void startMeasurementLoop() {
        if (!isRunning) return;

        if (connector.getConnectorState() != ConnectorState.CONNECTED) {
            updateNotification("Waiting for watch connection...");
            handler.postDelayed(this::startMeasurementLoop, 10000);
            return;
        }

        FcDataFeature dataFeature = connector.dataFeature();
        if (dataFeature.isSyncing()) {
            Log.d(TAG, "Sync in progress, waiting...");
            handler.postDelayed(this::startMeasurementLoop, 10000);
            return;
        }

        Log.d(TAG, "Starting 1-minute HR+SpO2 measurement bracket");
        updateNotification("Measuring HR & SpO2 (1-min bracket)...");

        // Combine HR and SpO2 in one bracket
        int healthType = FcHealthDataType.HEART_RATE | FcHealthDataType.OXYGEN;

        // Use 1 for a 1-minute measurement bracket as requested
        measureDisposable = dataFeature.openHealthRealTimeData(healthType, 1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            long now = System.currentTimeMillis();
                            int hr = result.getHeartRate();
                            int spo2 = result.getOxygen();

                            if (hr > 0) {
                                lastHR = hr;
                                dbExecutor.execute(() -> {
                                    HeartRateEntity entity = new HeartRateEntity();
                                    entity.timestamp = now;
                                    entity.bpm = hr;
                                    entity.source = "monitor_loop";
                                    dao.insertHeartRate(entity);
                                });
                            }

                            if (spo2 > 0) {
                                lastSpO2 = spo2;
                                dbExecutor.execute(() -> {
                                    OxygenEntity entity = new OxygenEntity();
                                    entity.timestamp = now;
                                    entity.spo2 = spo2;
                                    entity.source = "monitor_loop";
                                    dao.insertOxygen(entity);
                                });
                            }

                            if (hr > 0 || spo2 > 0) {
                                DayManager.getInstance().updateHRReading(dayDao, hr, (float) spo2);
                                updateNotification(String.format(Locale.US, "Monitoring: %d bpm | %d%% SpO2", lastHR, lastSpO2));
                            }
                        },
                        error -> {
                            Log.e(TAG, "Measurement bracket failed: " + error.getMessage());
                            handler.postDelayed(this::startMeasurementLoop, PAUSE_BETWEEN_CYCLES);
                        },
                        () -> {
                            // Bracket complete
                            Log.d(TAG, "1-minute bracket complete, restarting...");
                            if (isRunning) {
                                handler.postDelayed(this::startMeasurementLoop, PAUSE_BETWEEN_CYCLES);
                            }
                        }
                );
        disposables.add(measureDisposable);
    }

    private void stopMonitoring() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        if (measureDisposable != null && !measureDisposable.isDisposed()) {
            measureDisposable.dispose();
        }
        disposables.clear();
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    private Notification buildNotification(String text) {
        Intent stopIntent = new Intent(this, HealthMonitorService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Biopunk Health Monitor")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(openPending)
                .addAction(android.R.drawable.ic_delete, "Stop", stopPending)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Health Monitor", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoring();
        dbExecutor.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
