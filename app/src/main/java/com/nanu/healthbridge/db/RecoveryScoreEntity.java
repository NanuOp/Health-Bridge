package com.nanu.healthbridge.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recovery_scores")
public class RecoveryScoreEntity {
    @PrimaryKey public long timestamp; // Start of day (00:00:00)
    public int score;
    public String zone;
    public String color;
    public int hrvScore;
    public int sleepScore;
    public int rhrScore;
    public int spo2Score;
}
