package com.nanu.healthbridge.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sleep")
public class SleepEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long timestamp;  // day timestamp
    public long startTime;
    public long endTime;
    public int status;       // 1=Deep, 2=Light, 3=Awake, 4=REM
    public String statusName;
    public int durationMinutes;
    public boolean isNap;
}
