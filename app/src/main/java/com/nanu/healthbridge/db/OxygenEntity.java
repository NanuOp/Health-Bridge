package com.nanu.healthbridge.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "oxygen")
public class OxygenEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long timestamp;
    public int spo2;
    public String source; // "realtime", "sync", "monitor"
}
