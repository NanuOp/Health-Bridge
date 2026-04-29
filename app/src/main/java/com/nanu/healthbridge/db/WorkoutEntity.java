package com.nanu.healthbridge.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workouts")
public class WorkoutEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long timestamp;
    public int type;          // SDK sport type ID (see SportTypeHelper)
    public String typeName;
    public int durationSec;
    public float distanceKm;
    public int distanceM;     // Raw meters from SDK
    public float caloriesKcal;
    public int steps;
    public int avgHeartRate;
    public int maxHeartRate;
    public int minHeartRate;
    public String source;     // "app_realtime" or "sync"
}
