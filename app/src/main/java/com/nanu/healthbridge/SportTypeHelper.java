package com.nanu.healthbridge;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps all FitCloud SDK sport type IDs to English names and categories.
 * Based on SportUiHelper from the FitCloudPro-SDK-Android sample.
 */
public class SportTypeHelper {

    // ═══ Sport type ID → Name mapping ═══
    private static final Map<Integer, String> SPORT_NAMES = new LinkedHashMap<>();
    static {
        // Common
        SPORT_NAMES.put(0, "Outdoor Cycling");
        SPORT_NAMES.put(1, "Outdoor Running");
        SPORT_NAMES.put(2, "Indoor Running");
        SPORT_NAMES.put(3, "Outdoor Walking");
        SPORT_NAMES.put(4, "Mountaineering");
        SPORT_NAMES.put(23, "Indoor Walking");
        SPORT_NAMES.put(33, "Hiking");
        SPORT_NAMES.put(67, "Stair Climbing");
        SPORT_NAMES.put(79, "Stepping");
        SPORT_NAMES.put(24, "Indoor Cycling");
        SPORT_NAMES.put(12, "Jump Rope");

        // Dance/Gymnastics
        SPORT_NAMES.put(26, "Dance");
        SPORT_NAMES.put(48, "Latin Dance");
        SPORT_NAMES.put(49, "Street Dance");
        SPORT_NAMES.put(51, "Ballet");
        SPORT_NAMES.put(63, "Aerobics");
        SPORT_NAMES.put(64, "Group Exercise");
        SPORT_NAMES.put(81, "Gymnastics");
        SPORT_NAMES.put(112, "Folk Dance");

        // Fight/Combat
        SPORT_NAMES.put(50, "Free Combat");
        SPORT_NAMES.put(65, "Kickboxing");
        SPORT_NAMES.put(66, "Fencing");
        SPORT_NAMES.put(71, "Boxing");
        SPORT_NAMES.put(72, "Taekwondo");
        SPORT_NAMES.put(73, "Karate");
        SPORT_NAMES.put(78, "Wrestling");
        SPORT_NAMES.put(80, "Tai Chi");
        SPORT_NAMES.put(83, "Martial Arts");
        SPORT_NAMES.put(97, "Judo");

        // Equipment/Bar
        SPORT_NAMES.put(9, "Elliptical");
        SPORT_NAMES.put(13, "Rowing Machine");
        SPORT_NAMES.put(40, "Glider");
        SPORT_NAMES.put(57, "Fishing");
        SPORT_NAMES.put(58, "Frisbee");
        SPORT_NAMES.put(87, "Pull-up Bar");
        SPORT_NAMES.put(88, "Parallel Bars");
        SPORT_NAMES.put(90, "Darts");
        SPORT_NAMES.put(91, "Archery");
        SPORT_NAMES.put(106, "Pull-ups");
        SPORT_NAMES.put(110, "High Jump");
        SPORT_NAMES.put(113, "Hunting");
        SPORT_NAMES.put(114, "Shooting");
        SPORT_NAMES.put(102, "Treadmill");

        // Ball Sports
        SPORT_NAMES.put(5, "Basketball");
        SPORT_NAMES.put(7, "Badminton");
        SPORT_NAMES.put(8, "Football");
        SPORT_NAMES.put(11, "Table Tennis");
        SPORT_NAMES.put(17, "Tennis");
        SPORT_NAMES.put(18, "Baseball");
        SPORT_NAMES.put(19, "Rugby");
        SPORT_NAMES.put(20, "Cricket");
        SPORT_NAMES.put(31, "Volleyball");
        SPORT_NAMES.put(34, "Hockey");
        SPORT_NAMES.put(37, "Softball");
        SPORT_NAMES.put(52, "Australian Football");
        SPORT_NAMES.put(53, "Bowling");
        SPORT_NAMES.put(54, "Squash");
        SPORT_NAMES.put(55, "Curling");
        SPORT_NAMES.put(68, "American Football");
        SPORT_NAMES.put(70, "Pickleball");
        SPORT_NAMES.put(75, "Handball");
        SPORT_NAMES.put(86, "Lacrosse");
        SPORT_NAMES.put(93, "Shuttlecock");
        SPORT_NAMES.put(94, "Ice Hockey");
        SPORT_NAMES.put(28, "Golf");

        // Water/Glide
        SPORT_NAMES.put(6, "Swimming");
        SPORT_NAMES.put(35, "Rowing");
        SPORT_NAMES.put(39, "Skiing");
        SPORT_NAMES.put(56, "Snowboarding");
        SPORT_NAMES.put(59, "Alpine Skiing");
        SPORT_NAMES.put(61, "Ice Skating");
        SPORT_NAMES.put(89, "Roller Skating");
        SPORT_NAMES.put(99, "Skateboarding");
        SPORT_NAMES.put(100, "Hoverboard");
        SPORT_NAMES.put(101, "Inline Skating");
        SPORT_NAMES.put(103, "Diving");
        SPORT_NAMES.put(104, "Surfing");
        SPORT_NAMES.put(105, "Snorkeling");

        // Core/Strength
        SPORT_NAMES.put(22, "Strength Training");
        SPORT_NAMES.put(29, "Long Jump");
        SPORT_NAMES.put(30, "Sit-ups");
        SPORT_NAMES.put(36, "HIIT");
        SPORT_NAMES.put(45, "Functional Training");
        SPORT_NAMES.put(46, "Physical Training");
        SPORT_NAMES.put(47, "Mixed Cardio");
        SPORT_NAMES.put(60, "Core Training");
        SPORT_NAMES.put(82, "Track & Field");
        SPORT_NAMES.put(95, "Ab Workout");
        SPORT_NAMES.put(107, "Push-ups");
        SPORT_NAMES.put(108, "Plank");
        SPORT_NAMES.put(109, "Rock Climbing");

        // Casual/Flexible
        SPORT_NAMES.put(10, "Yoga");
        SPORT_NAMES.put(16, "Free Training");
        SPORT_NAMES.put(27, "Hula Hoop");
        SPORT_NAMES.put(32, "Parkour");
        SPORT_NAMES.put(38, "Trail Running");
        SPORT_NAMES.put(41, "Cool Down");
        SPORT_NAMES.put(42, "Cross Training");
        SPORT_NAMES.put(43, "Pilates");
        SPORT_NAMES.put(44, "Cross Fit");
        SPORT_NAMES.put(62, "Fitness Gaming");
        SPORT_NAMES.put(69, "Foam Rolling");
        SPORT_NAMES.put(74, "Flexibility");
        SPORT_NAMES.put(76, "Hand Cycling");
        SPORT_NAMES.put(77, "Meditation");
        SPORT_NAMES.put(84, "Leisure Sports");
        SPORT_NAMES.put(85, "Snow Sports");
        SPORT_NAMES.put(92, "Horse Riding");
        SPORT_NAMES.put(96, "VO2 Max Test");
        SPORT_NAMES.put(98, "Trampoline");
        SPORT_NAMES.put(111, "Bungee Jumping");
        SPORT_NAMES.put(115, "Marathon");
    }

    // ═══ Category definitions ═══
    public static final String[] CATEGORY_NAMES = {
            "Common", "Dance & Gymnastics", "Combat", "Equipment",
            "Ball Sports", "Water & Glide", "Core & Strength", "Casual & Flexible"
    };

    public static final int[][] CATEGORY_SPORT_IDS = {
            {0, 1, 2, 3, 4, 23, 33, 67, 79, 24, 12},                          // Common
            {26, 48, 49, 51, 63, 64, 81, 112},                                  // Dance
            {50, 65, 66, 71, 72, 73, 78, 80, 83, 97},                           // Combat
            {9, 13, 40, 57, 58, 87, 88, 90, 91, 106, 110, 113, 114, 102},       // Equipment
            {5, 7, 8, 11, 17, 18, 19, 20, 31, 34, 37, 52, 53, 54, 55, 68, 70, 75, 86, 93, 94, 28}, // Ball
            {6, 35, 39, 56, 59, 61, 89, 99, 100, 101, 103, 104, 105},           // Water/Glide
            {22, 29, 30, 36, 45, 46, 47, 60, 82, 95, 107, 108, 109},            // Core
            {10, 16, 27, 32, 38, 41, 42, 43, 44, 62, 69, 74, 76, 77, 84, 85, 92, 96, 98, 111, 115} // Casual
    };

    /**
     * Get the sport type name for a given type ID.
     * The SDK uses a sport mask system: the raw sportType encodes both the sport AND launch type.
     * Use getSportMask(sportType) to get the base sport identifier.
     */
    public static String getSportName(int sportType) {
        // First try direct lookup
        String name = SPORT_NAMES.get(sportType);
        if (name != null) return name;

        // Try mask lookup (strip launch type bits)
        int mask = getSportMask(sportType);
        name = SPORT_NAMES.get(mask);
        if (name != null) return name;

        return "Sport #" + sportType;
    }

    /**
     * Get the sport "mask" which identifies the actual sport, stripping the launch-type info.
     * Sports are identified by (sportType - 1) / 4 * 4 + 1 pattern in the SDK.
     */
    public static int getSportMask(int sportType) {
        if (sportType <= 0) return 0;
        return sportType - getLaunchType(sportType);
    }

    /**
     * Get the launch type: 0=Device, 1=Device→App, 2=App, 3=App→Device
     */
    public static int getLaunchType(int sportType) {
        if (sportType <= 0) return 0;
        return (sportType - 1) % 4;
    }

    /**
     * Calculate distance in km from steps and step length (meters).
     */
    public static float step2Km(int step, float stepLengthM) {
        return stepLengthM * step / 1000f;
    }

    /**
     * Calculate calories in kcal from distance (km) and weight (kg).
     */
    public static float km2Calories(float km, float weightKg) {
        return 0.78f * weightKg * km;
    }

    /**
     * Get the FcSportRealTimeType value for the given sport UI type.
     * The real-time sport types use a different numbering than the UI types.
     * Maps UI sport IDs to FcSportRealTimeType constants.
     */
    public static int toRealTimeType(int uiSportId) {
        // FcSportRealTimeType constants based on SDK:
        // RIDE=0, OUTDOOR=1, INDOOR=2, WALK=3, CLIMB=4, etc.
        // The UI sport IDs from SportUiHelper map directly to the sport type
        // but the real-time types need the (type * 4 + 1) encoding for APP_DEVICE launch
        return uiSportId * 4 + 3; // APP→Device launch type
    }

    public static Map<Integer, String> getAllSportNames() {
        return new LinkedHashMap<>(SPORT_NAMES);
    }
}
