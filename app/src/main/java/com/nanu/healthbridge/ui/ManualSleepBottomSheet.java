package com.nanu.healthbridge.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.nanu.healthbridge.DayManager;
import com.nanu.healthbridge.R;
import com.nanu.healthbridge.db.HealthDatabase;

public class ManualSleepBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_manual_sleep, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText etDeep = view.findViewById(R.id.et_deep_min);
        TextInputEditText etLight = view.findViewById(R.id.et_light_min);
        TextInputEditText etRem = view.findViewById(R.id.et_rem_min);
        TextInputEditText etAwake = view.findViewById(R.id.et_awake_min);

        view.findViewById(R.id.btn_save_manual_sleep).setOnClickListener(v -> {
            try {
                int deep = Integer.parseInt(etDeep.getText().toString());
                int light = Integer.parseInt(etLight.getText().toString());
                int rem = Integer.parseInt(etRem.getText().toString());
                int awake = Integer.parseInt(etAwake.getText().toString());
                int total = deep + light + rem + awake;

                if (total == 0) return;

                DayManager.getInstance().updateSleep(
                        HealthDatabase.getInstance(requireContext()).healthDayDao(),
                        total, deep, rem, light, awake,
                        System.currentTimeMillis() - (total * 60 * 1000L), // Assume it ended now
                        System.currentTimeMillis(),
                        true
                );

                Toast.makeText(requireContext(), "Sleep data saved locally", Toast.LENGTH_SHORT).show();
                dismiss();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
