package com.nanu.healthbridge.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        HeartRateEntity.class,
        SleepEntity.class,
        SleepRecordEntity.class,
        StepEntity.class,
        WorkoutEntity.class,
        OxygenEntity.class,
        RecoveryScoreEntity.class,
        HealthDay.class,
        UserProfile.class
}, version = 8, exportSchema = false)
public abstract class HealthDatabase extends RoomDatabase {

    private static volatile HealthDatabase INSTANCE;

    public abstract HealthDao healthDao();
    public abstract HealthDayDao healthDayDao();

    public static HealthDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (HealthDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            HealthDatabase.class,
                            "healthbridge_db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
