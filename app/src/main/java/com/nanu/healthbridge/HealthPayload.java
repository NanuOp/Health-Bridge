package com.nanu.healthbridge;

import java.util.List;

public class HealthPayload {
    public String device = "FireBoltt-Crusader";
    public String mac = "2B:D6:78:56:CC:AA";
    public String syncTime;
    public Integer battery;

    // Real-time health snapshot from the watch
    public RealTimeHealth realTimeHealth;

    // Historical data (from full sync, if available)
    public List<StepRecord> steps;
    public List<HRRecord> heartRate;
    public List<SleepRecord> sleep;
    public TodayTotal todayTotal;

    /**
     * Real-time health data from requestLatestHealthData().
     * This is the current/latest measurement from the watch sensors.
     */
    public static class RealTimeHealth {
        public int heartRate;      // bpm
        public int oxygen;         // SpO2 %
        public int systolicBP;     // mmHg
        public int diastolicBP;    // mmHg
        public float bodyTemp;     // °C
        public int respiratoryRate;
        public int stressLevel;    // pressure/stress

        public RealTimeHealth(int heartRate, int oxygen, int systolicBP, int diastolicBP,
                              float bodyTemp, int respiratoryRate, int stressLevel) {
            this.heartRate = heartRate;
            this.oxygen = oxygen;
            this.systolicBP = systolicBP;
            this.diastolicBP = diastolicBP;
            this.bodyTemp = bodyTemp;
            this.respiratoryRate = respiratoryRate;
            this.stressLevel = stressLevel;
        }
    }

    public static class StepRecord {
        public long timestamp;
        public int steps;
        public float distanceKm;
        public float caloriesKcal;
        public int durationSec;

        public StepRecord(long timestamp, int steps, float distanceKm, float caloriesKcal, int durationSec) {
            this.timestamp = timestamp;
            this.steps = steps;
            this.distanceKm = distanceKm;
            this.caloriesKcal = caloriesKcal;
            this.durationSec = durationSec;
        }
    }

    public static class HRRecord {
        public long timestamp;
        public int bpm;

        public HRRecord(long timestamp, int bpm) {
            this.timestamp = timestamp;
            this.bpm = bpm;
        }
    }

    public static class SleepRecord {
        public long timestamp;
        public int state;
        public String stateName;
        public int durationMinutes;

        public SleepRecord(long timestamp, int state, int durationMinutes) {
            this.timestamp = timestamp;
            this.state = state;
            this.durationMinutes = durationMinutes;
            switch (state) {
                case 1:  this.stateName = "Deep";  break;
                case 2:  this.stateName = "Light"; break;
                case 4:  this.stateName = "REM";   break;
                case 3:
                default: this.stateName = "Awake"; break;
            }
        }
    }

    public static class TodayTotal {
        public int totalSteps;
        public int totalDistanceM;
        public int totalCalories;
        public int currentHR;

        public TodayTotal(int totalSteps, int totalDistanceM, int totalCalories, int currentHR) {
            this.totalSteps = totalSteps;
            this.totalDistanceM = totalDistanceM;
            this.totalCalories = totalCalories;
            this.currentHR = currentHR;
        }
    }
}
