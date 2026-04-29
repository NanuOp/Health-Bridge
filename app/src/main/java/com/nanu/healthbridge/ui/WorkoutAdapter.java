package com.nanu.healthbridge.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nanu.healthbridge.R;
import com.nanu.healthbridge.db.WorkoutEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    private List<WorkoutEntity> workouts = new ArrayList<>();

    public void setWorkouts(List<WorkoutEntity> workouts) {
        this.workouts = workouts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutEntity w = workouts.get(position);
        holder.tvType.setText(w.typeName);
        holder.tvDate.setText(new SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(new Date(w.timestamp)));
        holder.tvDuration.setText(String.format(Locale.US, "%dm", w.durationSec / 60));
        holder.tvHr.setText(String.format(Locale.US, "%d bpm", w.avgHeartRate));
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDate, tvDuration, tvHr;

        ViewHolder(View v) {
            super(v);
            tvType = v.findViewById(R.id.tv_workout_type);
            tvDate = v.findViewById(R.id.tv_workout_date);
            tvDuration = v.findViewById(R.id.tv_workout_duration);
            tvHr = v.findViewById(R.id.tv_workout_hr);
        }
    }
}
