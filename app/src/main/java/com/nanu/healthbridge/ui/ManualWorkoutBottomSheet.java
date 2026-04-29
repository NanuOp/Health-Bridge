package com.nanu.healthbridge.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.nanu.healthbridge.DayManager;
import com.nanu.healthbridge.R;
import com.nanu.healthbridge.SportTypeHelper;
import com.nanu.healthbridge.db.HealthDatabase;
import com.nanu.healthbridge.db.WorkoutEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManualWorkoutBottomSheet extends BottomSheetDialogFragment {

    private Spinner spinnerSportType;
    private TextInputEditText etDuration, etAvgHr;
    private Button btnSave;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_manual_workout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerSportType = view.findViewById(R.id.spinner_sport_type);
        etDuration = view.findViewById(R.id.et_duration);
        etAvgHr = view.findViewById(R.id.et_avg_hr);
        btnSave = view.findViewById(R.id.btn_save_workout);

        setupSportSpinner();

        btnSave.setOnClickListener(v -> saveWorkout());
    }

    private void setupSportSpinner() {
        Map<Integer, String> sportsMap = SportTypeHelper.getAllSportNames();
        List<String> names = new ArrayList<>(sportsMap.values());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSportType.setAdapter(adapter);
        
        // Default to Run
        spinnerSportType.setSelection(names.indexOf("Outdoor Running"));
    }

    private void saveWorkout() {
        String durationStr = etDuration.getText().toString();
        String hrStr = etAvgHr.getText().toString();

        if (durationStr.isEmpty() || hrStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int durationMin = Integer.parseInt(durationStr);
        int avgHr = Integer.parseInt(hrStr);
        String selectedSport = (String) spinnerSportType.getSelectedItem();
        
        // Find ID
        int sportId = 1; // Default
        Map<Integer, String> sportsMap = SportTypeHelper.getAllSportNames();
        for (Map.Entry<Integer, String> entry : sportsMap.entrySet()) {
            if (entry.getValue().equals(selectedSport)) {
                sportId = entry.getKey();
                break;
            }
        }

        WorkoutEntity we = new WorkoutEntity();
        we.timestamp = System.currentTimeMillis();
        we.durationSec = durationMin * 60;
        we.avgHeartRate = avgHr;
        we.type = sportId;
        we.typeName = selectedSport;
        we.source = "manual";

        new Thread(() -> {
            HealthDatabase.getInstance(requireContext()).healthDao().insertWorkout(we);
            DayManager.getInstance().updateStrainFromWorkout(
                    HealthDatabase.getInstance(requireContext()).healthDayDao(),
                    durationMin, avgHr, avgHr + 20 // Estimate max HR for strain calc
            );
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Workout saved", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            }
        }).start();
    }
}
