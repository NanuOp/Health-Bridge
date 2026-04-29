package com.nanu.healthbridge.db;

public class FitnessProfile {
    public static class Baselines {
        public final int normalRestingHR;
        public final int hrScoreIdealMin;
        public final int hrScoreIdealMax;
        public final int maxExpectedHR;

        public Baselines(int normal, int min, int max, int expectedMax) {
            this.normalRestingHR = normal;
            this.hrScoreIdealMin = min;
            this.hrScoreIdealMax = max;
            this.maxExpectedHR = expectedMax;
        }
    }

    public static Baselines getBaselines(String level) {
        if (level == null) level = "FIT";
        switch (level) {
            case "SEDENTARY":
                return new Baselines(75, 60, 80, 185);
            case "ACTIVE":
                return new Baselines(65, 55, 72, 190);
            case "FIT":
                return new Baselines(60, 50, 65, 193);
            case "ATHLETE":
                return new Baselines(52, 42, 58, 196);
            case "ELITE":
                return new Baselines(42, 28, 50, 200);
            default:
                return new Baselines(60, 50, 65, 193);
        }
    }
}
