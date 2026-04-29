package com.nanu.healthbridge.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "heart_rate")
public class HeartRateEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long timestamp;
    public int bpm;
    public String source; // "realtime", "sync", "resting"
}
