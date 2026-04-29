package com.nanu.healthbridge.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "health_days")
public class HealthDay {
    @PrimaryKey 
    @NonNull
    public String date; // "YYYY-MM-DD"

    // Sleep
    public int sleepTotalMinutes;
    public int sleepDeepMinutes;
    public int sleepRemMinutes;
    public int sleepLightMinutes;
    public int sleepAwakeMinutes;
    public long sleepStartTime;
    public long sleepEndTime;
    public boolean sleepManualInput;

    // Steps
    public int totalSteps;
    public float totalDistanceKm;
    public float totalCalories;
    public boolean stepsManualInput;

    // Heart Rate
    public int restingHR;
    public int avgHR;
    public int maxHR;
    public float avgSpO2;
    public float minSpO2;

    // Recovery
    public int recoveryScore;
    public int restingHRScore;
    public int sleepScore;
    public int spO2Score;
    public String recoveryZone;
    public String color;
    public String aiInsight;
    public boolean recoveryCalculated;

    // Strain
    public int strainScore;
    public float strainCalories;
    public int strainMinutes;
    public String strainLevel;
    public int yesterdayStrainScore;

    // Meta
    public long createdAt;
    public long updatedAt;
}
