package com.nanu.healthbridge.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {
    @PrimaryKey
    public int id = 1; // single row

    public String name = "Shiv";
    public int age = 22;
    public int heightCm = 170;
    public int weightKg = 70;
    public String fitnessLevel = "FIT"; // Default
    public String sport = "Basketball";
    public int manualBaselineHR = 0; // 0 = auto calculate
    
    public long createdAt;
    public long updatedAt;

    // Helper to get baselines
    public int getNormalRestingHR() { return FitnessProfile.getBaselines(fitnessLevel).normalRestingHR; }
    public int getHrScoreIdealMin() { return FitnessProfile.getBaselines(fitnessLevel).hrScoreIdealMin; }
    public int getHrScoreIdealMax() { return FitnessProfile.getBaselines(fitnessLevel).hrScoreIdealMax; }
    public int getMaxExpectedHR() { return FitnessProfile.getBaselines(fitnessLevel).maxExpectedHR; }
}
