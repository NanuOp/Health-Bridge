package com.nanu.healthbridge.ui;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nanu.healthbridge.R;
import com.nanu.healthbridge.RecoveryApiService;
import com.nanu.healthbridge.RecoveryCalculator;
import com.nanu.healthbridge.db.HealthDao;
import com.nanu.healthbridge.db.HealthDatabase;
import com.nanu.healthbridge.db.HeartRateEntity;
import com.nanu.healthbridge.db.OxygenEntity;
import com.nanu.healthbridge.ui.utils.AnimationHelper;
import com.nanu.healthbridge.ui.views.CircularProgressView;
import com.nanu.healthbridge.ui.views.SparklineView;
import com.nanu.healthbridge.ui.views.TrendBarView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VitalsFragment extends Fragment {

    private TextView tvScoreHero, tvZoneHero, tvRecommendationHero;
    private TextView tvRhrScore, tvSleepScore, tvSpo2Score;
    private TextView tvRhrSummary, tvSleepSummary, tvSpo2Summary;
    private TextView tvStrainImpactLabel, tvStrainImpactYesterday, tvStrainPenalty, tvSleepMitigation;
    private com.google.android.material.card.MaterialCardView cardStrainImpact;
    private View viewStrainImpactAccent;
    private ProgressBar pbRhr, pbSleep, pbSpo2;
    private TextView tvAiInsight;
    private CircularProgressView ringHero;
    private TrendBarView trendBarView;
    private SparklineView hrGraph, spo2Graph;
    private View viewPulse;

    private com.nanu.healthbridge.db.HealthDayDao dayDao;
    private HealthDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private RecoveryApiService apiService;
    private long lastApiCallTime = 0;
    private static final long API_COOLDOWN = 5 * 60 * 1000L; // 5 mins cooldown

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vitals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dayDao = HealthDatabase.getInstance(requireContext()).healthDayDao();
        dao = HealthDatabase.getInstance(requireContext()).healthDao();
        apiService = new RecoveryApiService("#");

        bindViews(view);
        startPulseAnimation();
        loadTrend();
        loadVitalsGraphs();
        observeTodayAndCalculate();
    }

    private void bindViews(View v) {
        tvScoreHero = v.findViewById(R.id.tv_recovery_score_hero);
        tvZoneHero = v.findViewById(R.id.tv_zone_name_hero);
        tvRecommendationHero = v.findViewById(R.id.tv_recommendation_hero);
        ringHero = v.findViewById(R.id.recovery_ring_hero);
        viewPulse = v.findViewById(R.id.view_ring_pulse_large);

        tvRhrScore = v.findViewById(R.id.tv_rhr_score);
        tvSleepScore = v.findViewById(R.id.tv_sleep_score);
        tvSpo2Score = v.findViewById(R.id.tv_spo2_score);
        tvRhrSummary = v.findViewById(R.id.tv_rhr_summary);
        tvSleepSummary = v.findViewById(R.id.tv_sleep_summary);
        tvSpo2Summary = v.findViewById(R.id.tv_spo2_summary);
        pbRhr = v.findViewById(R.id.pb_rhr);
        pbSleep = v.findViewById(R.id.pb_sleep);
        pbSpo2 = v.findViewById(R.id.pb_spo2);

        tvStrainImpactLabel = v.findViewById(R.id.tv_strain_impact_label);
        tvStrainImpactYesterday = v.findViewById(R.id.tv_strain_impact_yesterday);
        tvStrainPenalty = v.findViewById(R.id.tv_strain_penalty);
        tvSleepMitigation = v.findViewById(R.id.tv_sleep_mitigation);
        cardStrainImpact = v.findViewById(R.id.card_strain_impact);
        viewStrainImpactAccent = v.findViewById(R.id.view_strain_impact_accent);

        tvAiInsight = v.findViewById(R.id.tv_ai_insight);
        trendBarView = v.findViewById(R.id.recovery_trend_view);
        hrGraph = v.findViewById(R.id.detailed_hr_graph);
        spo2Graph = v.findViewById(R.id.detailed_spo2_graph);
    }

    private void startPulseAnimation() {
        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(viewPulse,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f),
                PropertyValuesHolder.ofFloat("alpha", 0.6f, 0.2f, 0.6f));
        pulse.setDuration(3000);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setInterpolator(new LinearInterpolator());
        pulse.start();
    }

    private void loadTrend() {
        dayDao.getLast7DaysLive().observe(getViewLifecycleOwner(), days -> {
            if (days == null || days.isEmpty())
                return;
            List<TrendBarView.TrendData> trend = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("E", Locale.US);
            for (com.nanu.healthbridge.db.HealthDay d : days) {
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d.date);
                    int color = Color.parseColor(d.color != null ? d.color : "#00FF88");
                    trend.add(new TrendBarView.TrendData(d.recoveryScore, sdf.format(date), color));
                } catch (Exception e) {
                }
            }
            trendBarView.setData(trend);
        });
    }

    private void loadVitalsGraphs() {
        long last24h = System.currentTimeMillis() - (24 * 60 * 60 * 1000L);

        dao.getHeartRatesSince(last24h).observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty())
                return;
            List<Integer> values = new ArrayList<>();
            for (HeartRateEntity e : list)
                values.add(e.bpm);
            hrGraph.setData(values, Color.parseColor("#FF6B6B"));
        });

        dao.getOxygenSince(last24h).observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty())
                return;
            List<Integer> values = new ArrayList<>();
            for (OxygenEntity e : list)
                values.add((int) e.spo2);
            spo2Graph.setData(values, Color.parseColor("#00D4FF"));
        });
    }

    private void observeTodayAndCalculate() {
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        dayDao.getByDateLive(todayStr).observe(getViewLifecycleOwner(), day -> {
            if (day != null) {
                executor.execute(() -> runCalculationLoop(day));
            }
        });
    }

    private void runCalculationLoop(com.nanu.healthbridge.db.HealthDay today) {
        RecoveryCalculator.RecoveryInput input = new RecoveryCalculator.RecoveryInput();

        com.nanu.healthbridge.db.UserProfile profile = dao.getUserProfile();
        if (profile == null) {
            profile = new com.nanu.healthbridge.db.UserProfile();
            profile.createdAt = System.currentTimeMillis();
            profile.updatedAt = System.currentTimeMillis();
            dao.insertUserProfile(profile);
        }
        input.profile = profile;

        input.restingHR = today.restingHR;
        input.hasValidRestingHR = true; // For now
        input.totalSleepMinutes = today.sleepTotalMinutes;
        input.avgSpO2 = today.avgSpO2;
        input.yesterdayStrainScore = today.yesterdayStrainScore;
        input.profile = profile;

        RecoveryCalculator.RecoveryResult result = RecoveryCalculator.calculate(input);

        // Update database with new score
        today.recoveryScore = result.finalScore;
        today.recoveryZone = result.zone;
        today.color = result.zoneColor;
        today.restingHR = input.restingHR; // Sync detected RHR back to day
        dayDao.insertOrUpdate(today);

        if (getActivity() != null) {
            requireActivity().runOnUiThread(() -> updateUI(result));
            fetchAIInsight(today, result);
        }
    }

    private void updateUI(RecoveryCalculator.RecoveryResult result) {
        tvScoreHero.setText(String.valueOf(result.finalScore));
        int color = Color.parseColor(result.zoneColor);
        ringHero.setProgress(result.finalScore, color);
        tvZoneHero.setText(result.zone);
        tvZoneHero.setTextColor(color);
        tvRecommendationHero.setText(result.recommendation);

        pbRhr.setProgress(result.restingHRScore);
        tvRhrScore.setText(result.restingHRScore + "/100");
        tvRhrSummary.setText(result.restingHRTrend);

        pbSleep.setProgress(result.sleepScore);
        tvSleepScore.setText(result.sleepScore + "/100");
        tvSleepSummary.setText(result.sleepSummary);

        pbSpo2.setProgress(result.spO2Score);
        tvSpo2Score.setText(result.spO2Score + "/100");
        tvSpo2Summary.setText(result.spO2Summary);

        // Strain Impact
        tvStrainImpactLabel.setText(result.strainImpactLabel);
        int impactColor = Color.parseColor(result.strainImpactColor);
        tvStrainImpactLabel.setTextColor(impactColor);
        viewStrainImpactAccent.setBackgroundColor(impactColor);
        cardStrainImpact.setStrokeColor(impactColor);

        tvStrainPenalty.setText("Penalty: -" + result.strainPenaltyApplied + " pts");
        tvSleepMitigation.setText("Sleep offset: +" + result.sleepMitigationApplied + " pts");

        // Find today to get yesterday score (a bit redundant but for simplicity)
        new Thread(() -> {
            com.nanu.healthbridge.db.HealthDay today = com.nanu.healthbridge.db.HealthDatabase.getInstance(getContext())
                    .healthDayDao()
                    .getByDate(new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
            if (today != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvStrainImpactYesterday.setText("Yesterday: " + today.yesterdayStrainScore + "/100");
                });
            }
        }).start();
    }

    private void fetchAIInsight(com.nanu.healthbridge.db.HealthDay today, RecoveryCalculator.RecoveryResult result) {
        long now = System.currentTimeMillis();
        if (now - lastApiCallTime < API_COOLDOWN) {
            if (today.aiInsight != null && tvAiInsight.getText().length() == 0)
                typeText(today.aiInsight);
            return;
        }

        lastApiCallTime = now;
        apiService.getRecoveryInsight(today, result,
                new RecoveryApiService.InsightCallback() {
                    @Override
                    public void onInsight(String insight) {
                        if (getActivity() != null) {
                            requireActivity().runOnUiThread(() -> {
                                typeText(insight);
                                today.aiInsight = insight;
                                executor.execute(() -> dayDao.insertOrUpdate(today));
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            requireActivity()
                                    .runOnUiThread(() -> tvAiInsight.setText("Coach status: Waiting for more data..."));
                        }
                    }
                });
    }

    private void typeText(String text) {
        tvAiInsight.setText("");
        Handler handler = new Handler(Looper.getMainLooper());
        final int[] index = { 0 };
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < text.length()) {
                    tvAiInsight.append(String.valueOf(text.charAt(index[0]++)));
                    handler.postDelayed(this, 30);
                }
            }
        };
        handler.post(runnable);
    }
}
