package com.nanu.healthbridge;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RecoveryCalculator {

    public static class RecoveryInput {
        public int restingHR;
        public boolean hasValidRestingHR;
        public int totalSleepMinutes;
        public long sleepStartTime; // unix ms
        public double avgSpO2;
        public int yesterdayStrainScore;
        public com.nanu.healthbridge.db.UserProfile profile;
    }

    public static class RecoveryResult {
        public int finalScore;
        public String zone;
        public String zoneColor;
        public String recommendation;
        
        // Breakdown
        public int restingHRScore;
        public int sleepScore;
        public int spO2Score;
        
        // Sleep breakdown
        public int sleepDurationScore;
        public int sleepConsistencyScore;
        public int sleepTimingScore;
        public String sleepStartTimeFormatted;
        public boolean lateSleepWarning;

        // Strain impact
        public int strainPenaltyApplied;
        public int sleepMitigationApplied;
        public String strainImpactLabel;
        public String strainImpactColor;

        public String restingHRTrend;
        public String sleepSummary;
        public String spO2Summary;

        public com.nanu.healthbridge.db.UserProfile profile;
    }

    public static RecoveryResult calculate(RecoveryInput input) {
        RecoveryResult result = new RecoveryResult();
        result.profile = input.profile;
        if (result.profile == null) result.profile = new com.nanu.healthbridge.db.UserProfile();
        
        // 1. Resting HR Score (Fitness Level Personalized)
        if (!input.hasValidRestingHR || input.restingHR == 0) {
            result.restingHRScore = 50; // Neutral
            result.restingHRTrend = "-- BPM · wear watch while sleeping";
        } else {
            com.nanu.healthbridge.db.UserProfile p = result.profile;
            int idealMin = p.getHrScoreIdealMin();
            int idealMax = p.getHrScoreIdealMax();
            int normalHR = p.getNormalRestingHR();
            int rhr = input.restingHR;

            if (rhr <= idealMin) {
                result.restingHRScore = 100;
            } else if (rhr <= idealMax) {
                result.restingHRScore = (int)(90 + ((float)(idealMax - rhr) / (idealMax - idealMin)) * 10);
            } else if (rhr <= normalHR + 10) {
                result.restingHRScore = (int)(70 + ((float)(normalHR + 10 - rhr) / 10f) * 20);
            } else if (rhr <= normalHR + 20) {
                result.restingHRScore = (int)(40 + ((float)(normalHR + 20 - rhr) / 10f) * 30);
            } else {
                result.restingHRScore = Math.max(0, 40 - (rhr - normalHR - 20) * 2);
            }
            
            String status = rhr <= idealMax ? "optimal" : (rhr <= normalHR + 5 ? "normal" : "elevated");
            result.restingHRTrend = rhr + " BPM · " + status + " for " + p.fitnessLevel;
        }

        // 2. Sleep Score (Weighted: 50% Duration, 30% Consistency, 20% Timing)
        // Duration (50%)
        float durationScore = 0;
        if (input.totalSleepMinutes >= 420) durationScore = 100;
        else if (input.totalSleepMinutes >= 240) {
            durationScore = ((input.totalSleepMinutes - 240) / 180f) * 100;
        }
        result.sleepDurationScore = (int)durationScore;

        // Consistency (30%) - how close to 8 hours (480 mins)
        int deficit = Math.abs(480 - input.totalSleepMinutes);
        float consistencyScore = Math.max(0, 100 - (deficit / 4.8f));
        result.sleepConsistencyScore = (int)consistencyScore;

        // Timing (20%)
        float timingScore = 0;
        if (input.sleepStartTime > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(input.sleepStartTime);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            
            if (hour >= 21 && hour < 23) timingScore = 100; // 9-11 PM
            else if (hour == 23) timingScore = 90; // 11-12 PM
            else if (hour == 0) timingScore = 70; // 12-1 AM
            else if (hour == 1) timingScore = 50;
            else if (hour >= 2 && hour < 5) timingScore = 30;
            else timingScore = 60; // fallback/early
            
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
            result.sleepStartTimeFormatted = sdf.format(new Date(input.sleepStartTime));
            result.lateSleepWarning = (hour >= 0 && hour < 5);
        } else {
            result.sleepStartTimeFormatted = "--";
            timingScore = 50; // Neutral
        }
        result.sleepTimingScore = (int)timingScore;

        result.sleepScore = (int)(durationScore * 0.5f + consistencyScore * 0.3f + timingScore * 0.2f);
        
        int h = input.totalSleepMinutes / 60;
        int m = input.totalSleepMinutes % 60;
        result.sleepSummary = String.format(Locale.US, "%dh %dm · slept %s · consistency %d%%", 
                h, m, result.sleepStartTimeFormatted, result.sleepConsistencyScore);

        // 3. SpO2 Score
        result.spO2Score = (int)clamp((int) Math.round((input.avgSpO2 - 80) * 5), 0, 100);
        result.spO2Summary = String.format(Locale.US, "%.1f%% avg · optimal saturation", input.avgSpO2);

        // 4. Base Recovery Score
        int finalScore = (int) (result.restingHRScore * 0.4 + result.sleepScore * 0.4 + result.spO2Score * 0.2);

        // 5. Strain Impact Penalty
        float strainPenalty = (input.yesterdayStrainScore / 100f) * 40f;
        float sleepMitigation = (input.totalSleepMinutes / 480f) * 25f;
        sleepMitigation = Math.min(sleepMitigation, 25f);
        
        float netPenalty = Math.max(0, strainPenalty - sleepMitigation);
        result.strainPenaltyApplied = (int)strainPenalty;
        result.sleepMitigationApplied = (int)sleepMitigation;
        
        if (netPenalty >= 30) {
            result.strainImpactLabel = String.format(Locale.US, "🏀 Heavy strain cost · -%d pts", (int)netPenalty);
            result.strainImpactColor = "#FF3B5C";
        } else if (netPenalty >= 15) {
            result.strainImpactLabel = String.format(Locale.US, "⚡ Moderate strain cost · -%d pts", (int)netPenalty);
            result.strainImpactColor = "#FF6B35";
        } else if (netPenalty >= 5) {
            result.strainImpactLabel = String.format(Locale.US, "💪 Light strain cost · -%d pts", (int)netPenalty);
            result.strainImpactColor = "#FFD700";
        } else {
            result.strainImpactLabel = "✅ Strain well recovered";
            result.strainImpactColor = "#00FF88";
        }

        finalScore = (int)(finalScore - netPenalty);
        result.finalScore = (int)clamp(finalScore, 0, 100);

        // Zone Logic
        if (result.finalScore >= 80) {
            result.zone = "OPTIMAL";
            result.zoneColor = "#00FF88";
            result.recommendation = "Body is primed for high intensity.";
        } else if (result.finalScore >= 50) {
            result.zone = "STEADY";
            result.zoneColor = "#00D4FF";
            result.recommendation = "Maintain regular training volume.";
        } else {
            result.zone = "RECOVER";
            result.zoneColor = "#FF3B5C";
            result.recommendation = "Focus on active recovery and rest.";
        }

        return result;
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
