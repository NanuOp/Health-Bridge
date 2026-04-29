package com.nanu.healthbridge.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "steps")
public class StepEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long timestamp;
    public int steps;
    public float distanceKm;
    public float caloriesKcal;
    public int durationSec;
    public String source; // "sync" or "total"
}
