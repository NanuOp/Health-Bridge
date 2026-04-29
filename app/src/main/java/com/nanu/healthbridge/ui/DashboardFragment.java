package com.nanu.healthbridge.ui;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Color;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nanu.healthbridge.HealthBridgeApp;
import com.nanu.healthbridge.R;
import com.nanu.healthbridge.SyncService;
import com.nanu.healthbridge.db.HealthDatabase;
import com.nanu.healthbridge.db.HealthDao;
import com.nanu.healthbridge.db.HeartRateEntity;
import com.nanu.healthbridge.db.OxygenEntity;
import com.nanu.healthbridge.db.SleepRecordEntity;
import com.nanu.healthbridge.ui.utils.AnimationHelper;
import com.nanu.healthbridge.ui.views.CircularProgressView;
import com.nanu.healthbridge.ui.views.SparklineView;
import com.topstep.fitcloud.sdk.v2.FcConnector;
import com.topstep.wearkit.base.connector.ConnectorState;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DashboardFragment extends Fragment {

    private TextView tvGreeting, tvDate, tvBattery, tvRecoveryScore, tvRecoveryZone;
    private TextView tvHrValue, tvSpo2Value, tvSleepValue, tvStepsValue, tvStrainValue, tvStrainLevelDash, tvSleepBedtime, tvSleepWarning;
    private TextView tvReadinessTitle, tvReadinessInsight;
    private CircularProgressView recoveryRing;
    private View viewRingPulse, bannerAccentBorder;
    private Button btnSyncAll;

    private com.nanu.healthbridge.db.HealthDayDao dayDao;
    private FcConnector connector;
    private HealthDao dao;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        connector = HealthBridgeApp.getFcSDK().getConnector();
        dao = HealthDatabase.getInstance(requireContext()).healthDao();
        dayDao = HealthDatabase.getInstance(requireContext()).healthDayDao();

        bindViews(view);
        updateGreeting();
        startPulseAnimation();
        observeDatabase();
        observeToday();
        setupListeners();
    }

    private void bindViews(View v) {
        tvGreeting = v.findViewById(R.id.tv_greeting);
        tvDate = v.findViewById(R.id.tv_date);
        tvBattery = v.findViewById(R.id.tv_battery);
        tvRecoveryScore = v.findViewById(R.id.tv_recovery_score);
        tvRecoveryZone = v.findViewById(R.id.tv_recovery_zone);
        
        recoveryRing = v.findViewById(R.id.recovery_ring);
        viewRingPulse = v.findViewById(R.id.view_ring_pulse);
        
        tvHrValue = v.findViewById(R.id.tv_hr_value);
        tvSpo2Value = v.findViewById(R.id.tv_spo2_value);
        tvSleepValue = v.findViewById(R.id.tv_sleep_value);
        tvStepsValue = v.findViewById(R.id.tv_steps_value);
        tvStrainValue = v.findViewById(R.id.tv_strain_value);
        tvStrainLevelDash = v.findViewById(R.id.tv_strain_level_dash);
        
        viewRingPulse = v.findViewById(R.id.view_ring_pulse);
        
        tvSleepBedtime = v.findViewById(R.id.tv_sleep_bedtime);
        tvSleepWarning = v.findViewById(R.id.tv_sleep_warning);
        
        tvReadinessTitle = v.findViewById(R.id.tv_readiness_title);
        tvReadinessInsight = v.findViewById(R.id.tv_readiness_insight);
        bannerAccentBorder = v.findViewById(R.id.banner_accent_border);

        btnSyncAll = v.findViewById(R.id.btn_sync_all);

        tvDate.setText(new SimpleDateFormat("MMMM dd · EEEE", Locale.US).format(new Date()));
    }

    private void updateGreeting() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) greeting = "Good morning, Shiv";
        else if (hour >= 12 && hour < 17) greeting = "Good afternoon, Shiv";
        else if (hour >= 17 && hour < 21) greeting = "Good evening, Shiv";
        else greeting = "Good night, Shiv";
        tvGreeting.setText(greeting);
    }

    private void startPulseAnimation() {
        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(viewRingPulse,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f),
                PropertyValuesHolder.ofFloat("alpha", 0.6f, 0.2f, 0.6f)
        );
        pulse.setDuration(3000);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setInterpolator(new LinearInterpolator());
        pulse.start();
    }

    private void setupListeners() {
        btnSyncAll.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            SyncService.enqueueSync(requireContext());
            Toast.makeText(requireContext(), "Syncing data...", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeDatabase() {
        // HR - Only update the number
        dao.getLatestHeartRate().observe(getViewLifecycleOwner(), hr -> {
            if (hr != null) {
                tvHrValue.setText(String.valueOf(hr.bpm));
            }
        });
        
        // SpO2 - Only update the number
        dao.getLatestOxygen().observe(getViewLifecycleOwner(), o2 -> {
            if (o2 != null) {
                tvSpo2Value.setText(String.valueOf((int)o2.spo2));
            }
        });

        // Sleep Graph removed for more detail

        // Battery
        if (connector.getConnectorState() == ConnectorState.CONNECTED) {
            disposables.add(HealthBridgeApp.getFcSDK().getBatteryAbility().observeBattery()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(battery -> tvBattery.setText("BAT " + battery.getPercentage() + "%"), e -> {}));
            
            // Also request once immediately
            disposables.add(connector.settingsFeature().requestBattery()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(battery -> tvBattery.setText("BAT " + battery.getPercentage() + "%"), e -> {}));
        }
    }

    private void observeToday() {
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        dayDao.getByDateLive(todayStr).observe(getViewLifecycleOwner(), day -> {
            if (day == null) return;

            // Recovery Score
            if (day.recoveryScore > 0) {
                tvRecoveryScore.setText(String.valueOf(day.recoveryScore));
                int color = Color.parseColor(day.color != null ? day.color : "#00FF88");
                recoveryRing.setProgress(day.recoveryScore, color);
                tvRecoveryZone.setText(day.recoveryZone);
                tvRecoveryZone.setTextColor(color);
                bannerAccentBorder.setBackgroundColor(color);
            }

            // Steps/Sleep/Strain
            float sleepHours = day.sleepTotalMinutes / 60f;
            tvSleepValue.setText(String.format(Locale.US, "%.1fh", sleepHours));
            
            if (day.sleepStartTime > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
                tvSleepBedtime.setText("slept at " + sdf.format(new Date(day.sleepStartTime)));
                
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(day.sleepStartTime);
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                if (hour >= 0 && hour < 5) {
                    tvSleepWarning.setVisibility(View.VISIBLE);
                } else {
                    tvSleepWarning.setVisibility(View.GONE);
                }
            } else {
                tvSleepBedtime.setText("--");
                tvSleepWarning.setVisibility(View.GONE);
            }

            tvStepsValue.setText(String.valueOf(day.totalSteps));
            tvStrainValue.setText(String.format(Locale.US, "%.1f", (float)day.strainScore));
            tvStrainLevelDash.setText(day.strainLevel != null ? day.strainLevel : "RESTORATIVE");
            
            // Insight
            tvReadinessInsight.setText(day.aiInsight != null ? day.aiInsight : "Ready for training");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}
