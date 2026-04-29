package com.nanu.healthbridge.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.nanu.healthbridge.DayManager;
import com.nanu.healthbridge.HealthBridgeApp;
import com.nanu.healthbridge.R;
import com.nanu.healthbridge.db.HealthDao;
import com.nanu.healthbridge.db.HealthDatabase;
import com.nanu.healthbridge.db.HealthDayDao;
import com.topstep.fitcloud.sdk.v2.FcConnector;
import com.topstep.fitcloud.sdk.v2.model.data.FcHealthDataType;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class WorkoutFragment extends Fragment {

    private TextView tvStrainScore, tvStrainLevel;
    private RecyclerView rvWorkouts;
    private WorkoutAdapter adapter;
    private HealthDao dao;
    private HealthDayDao dayDao;
    private FcConnector connector;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStrainScore = view.findViewById(R.id.tv_strain_score);
        tvStrainLevel = view.findViewById(R.id.tv_strain_level);
        rvWorkouts = view.findViewById(R.id.rv_workouts);

        dao = HealthDatabase.getInstance(requireContext()).healthDao();
        dayDao = HealthDatabase.getInstance(requireContext()).healthDayDao();
        connector = HealthBridgeApp.getFcSDK().getConnector();

        adapter = new WorkoutAdapter();
        rvWorkouts.setAdapter(adapter);

        view.findViewById(R.id.fab_add_workout).setOnClickListener(v -> {
            new ManualWorkoutBottomSheet().show(getChildFragmentManager(), "manual_workout");
        });

        // Quick Start Buttons
        view.findViewById(R.id.btn_start_walk).setOnClickListener(v -> startWatchSport(3, "Walking"));
        view.findViewById(R.id.btn_start_run).setOnClickListener(v -> startWatchSport(1, "Running"));
        view.findViewById(R.id.btn_start_ride).setOnClickListener(v -> startWatchSport(0, "Cycling"));
        view.findViewById(R.id.btn_start_climb).setOnClickListener(v -> startWatchSport(4, "Climbing"));

        observeData();
    }

    private void observeData() {
        String today = DayManager.getInstance().getTodayDate();
        dayDao.getByDateLive(today).observe(getViewLifecycleOwner(), day -> {
            if (day != null) {
                tvStrainScore.setText(String.format(Locale.US, "%.1f", (float)day.strainScore));
                tvStrainLevel.setText(day.strainLevel);
                
                // Color coding strain
                if (day.strainScore > 70) tvStrainScore.setTextColor(requireContext().getColor(R.color.danger));
                else if (day.strainScore > 40) tvStrainScore.setTextColor(requireContext().getColor(R.color.accent_purple));
                else tvStrainScore.setTextColor(requireContext().getColor(R.color.accent_green));
            }
        });

        dao.getRecentWorkouts(20).observe(getViewLifecycleOwner(), workouts -> {
            if (workouts != null) {
                adapter.setWorkouts(workouts);
            }
        });
    }

    private void startWatchSport(int type, String name) {
        if (connector.getConnectorState() != com.topstep.wearkit.base.connector.ConnectorState.CONNECTED) {
            Toast.makeText(getContext(), "Watch not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Starting " + name + " on watch...", Toast.LENGTH_SHORT).show();
        
        // Remote sport start logic
        // We use openHealthRealTimeData to start the sensors on the watch
        // The watch will display the sport UI if Remote Sport is supported
        disposables.add(connector.dataFeature().openHealthRealTimeData(FcHealthDataType.HEART_RATE, 60)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    Log.d("Workout", "Real-time HR during sport: " + result.getHeartRate());
                }, e -> Log.e("Workout", "Sport failed", e)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}
