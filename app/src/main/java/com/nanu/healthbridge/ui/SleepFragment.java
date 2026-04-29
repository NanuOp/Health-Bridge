package com.nanu.healthbridge.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nanu.healthbridge.R;
import com.nanu.healthbridge.db.HealthDao;
import com.nanu.healthbridge.db.HealthDatabase;
import com.nanu.healthbridge.db.SleepEntity;
import com.nanu.healthbridge.ui.utils.AnimationHelper;
import com.nanu.healthbridge.ui.views.CircularProgressView;
import com.nanu.healthbridge.ui.views.SleepArchitectureView;

import java.util.Calendar;
import java.util.List;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import com.nanu.healthbridge.db.SleepRecordEntity;

public class SleepFragment extends Fragment {
    private TextView tvDuration, tvTimes, tvDeep, tvLight, tvRem, tvAwake;
    private CircularProgressView scoreRing;
    private SleepArchitectureView architectureView;
    private BarChart chartSleep;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabManual;

    private HealthDao dao;
    private com.nanu.healthbridge.db.HealthDayDao dayDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sleep, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dao = HealthDatabase.getInstance(requireContext()).healthDao();
        dayDao = HealthDatabase.getInstance(requireContext()).healthDayDao();

        bindViews(view);
        setupChart();
        observeSleep();
        observeToday();
        
        fabManual.setOnClickListener(v -> showManualSleepBottomSheet());
    }

    private void bindViews(View v) {
        tvDuration = v.findViewById(R.id.tv_sleep_duration);
        tvTimes = v.findViewById(R.id.tv_sleep_times);
        tvDeep = v.findViewById(R.id.tv_deep_time);
        tvLight = v.findViewById(R.id.tv_light_time);
        tvRem = v.findViewById(R.id.tv_rem_time);
        tvAwake = v.findViewById(R.id.tv_awake_time);
        scoreRing = v.findViewById(R.id.sleep_score_ring);
        architectureView = v.findViewById(R.id.sleep_architecture_view);
        chartSleep = v.findViewById(R.id.sleep_bar_chart);
        fabManual = v.findViewById(R.id.fab_manual_sleep);
    }

    private void observeSleep() {
        dao.getLatestSleepRecord().observe(getViewLifecycleOwner(), record -> {
            if (record == null) return;
            
            int totalMins = (record.deepSeconds + record.lightSeconds + record.remSeconds) / 60;
            tvDuration.setText(String.format(Locale.US, "%dh %dm", totalMins / 60, totalMins % 60));
            
            scoreRing.setProgress(record.score, Color.parseColor("#7B61FF"));
            
            tvDeep.setText(String.format(Locale.US, "%dh %dm", record.deepSeconds / 3600, (record.deepSeconds % 3600) / 60));
            tvLight.setText(String.format(Locale.US, "%dh %dm", record.lightSeconds / 3600, (record.lightSeconds % 3600) / 60));
            tvRem.setText(String.format(Locale.US, "%dh %dm", record.remSeconds / 3600, (record.remSeconds % 3600) / 60));
            tvAwake.setText(String.format(Locale.US, "%dh %dm", record.soberSeconds / 3600, (record.soberSeconds % 3600) / 60));

            // Fetch segments for architecture
            dao.getSleepItemsForDay(record.dayTimestamp).observe(getViewLifecycleOwner(), items -> {
                if (items != null && !items.isEmpty()) {
                    List<SleepArchitectureView.SleepSegment> segments = new ArrayList<>();
                    for (SleepEntity item : items) {
                        segments.add(new SleepArchitectureView.SleepSegment(item.status, item.durationMinutes));
                    }
                    architectureView.setSegments(segments);
                    
                    if (!items.isEmpty()) {
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
                        String start = sdf.format(new Date(items.get(0).startTime));
                        String end = sdf.format(new Date(items.get(items.size() - 1).endTime));
                        tvTimes.setText(start + " → " + end);
                    }
                }
            });
        });

        long todayStart = getTodayStartMillis();
        dao.getSleepRecordsSince(todayStart - 7 * 86400000L).observe(getViewLifecycleOwner(), this::updateChart);
    }

    private void observeToday() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        dayDao.getByDateLive(today).observe(getViewLifecycleOwner(), day -> {
            if (day == null) return;
            // Additional updates if needed
        });
    }

    private void setupChart() {
        chartSleep.getDescription().setEnabled(false);
        chartSleep.getLegend().setEnabled(false);
        chartSleep.getAxisRight().setEnabled(false);
        chartSleep.getAxisLeft().setTextColor(0xFF8888AA);
        chartSleep.getAxisLeft().setDrawGridLines(false);
        
        XAxis xAxis = chartSleep.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(0xFF8888AA);
        xAxis.setDrawGridLines(false);
    }

    private void updateChart(List<SleepRecordEntity> records) {
        if (records == null || records.isEmpty()) return;
        List<BarEntry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        
        int index = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("E", Locale.US);
        for (int i = Math.max(0, records.size() - 7); i < records.size(); i++) {
            SleepRecordEntity r = records.get(i);
            float hours = (r.deepSeconds + r.lightSeconds + r.remSeconds) / 3600f;
            entries.add(new BarEntry(index++, hours));
            labels.add(sdf.format(new Date(r.dayTimestamp)));
        }

        chartSleep.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                return (i >= 0 && i < labels.size()) ? labels.get(i) : "";
            }
        });

        BarDataSet set = new BarDataSet(entries, "Sleep Hours");
        set.setColor(Color.parseColor("#7B61FF"));
        set.setValueTextColor(Color.WHITE);
        
        chartSleep.setData(new BarData(set));
        chartSleep.animateY(1000);
        chartSleep.invalidate();
    }

    private void showManualSleepBottomSheet() {
        new ManualSleepBottomSheet().show(getChildFragmentManager(), "ManualSleep");
    }

    private long getTodayStartMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
