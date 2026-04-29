package com.nanu.healthbridge.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HealthDao {

    // ═══ Heart Rate ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertHeartRate(HeartRateEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertHeartRates(List<HeartRateEntity> entities);

    @Query("SELECT * FROM heart_rate WHERE timestamp >= :since ORDER BY timestamp DESC")
    LiveData<List<HeartRateEntity>> getHeartRatesSince(long since);

    @Query("SELECT * FROM heart_rate ORDER BY timestamp DESC LIMIT 1")
    LiveData<HeartRateEntity> getLatestHeartRate();

    @Query("SELECT * FROM heart_rate ORDER BY timestamp DESC LIMIT :limit")
    List<HeartRateEntity> getRecentHeartRates(int limit);

    /** For graph data — returns chronologically ordered HR readings since a timestamp */
    @Query("SELECT * FROM heart_rate WHERE timestamp >= :since ORDER BY timestamp ASC")
    List<HeartRateEntity> getHeartRatesSinceSync(long since);

    @Query("SELECT * FROM heart_rate WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC")
    List<HeartRateEntity> getHeartRatesBetweenSync(long start, long end);

    // ═══ Sleep Items ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSleep(SleepEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSleepRecords(List<SleepEntity> entities);

    @Query("SELECT * FROM sleep WHERE timestamp >= :since ORDER BY timestamp DESC")
    LiveData<List<SleepEntity>> getSleepSince(long since);

    @Query("SELECT * FROM sleep WHERE isNap = 1 AND timestamp >= :since ORDER BY timestamp DESC")
    LiveData<List<SleepEntity>> getNapsSince(long since);

    @Query("SELECT * FROM sleep WHERE timestamp >= :dayStart AND timestamp < :dayEnd ORDER BY startTime ASC")
    List<SleepEntity> getSleepForDay(long dayStart, long dayEnd);

    @Query("SELECT * FROM sleep WHERE timestamp = :dayTimestamp ORDER BY startTime ASC")
    LiveData<List<SleepEntity>> getSleepItemsForDay(long dayTimestamp);

    @Query("SELECT SUM(durationMinutes) FROM sleep WHERE timestamp >= :since AND isNap = 0")
    LiveData<Integer> getTotalSleepMinutes(long since);

    /** Delete sleep items for a specific day that start after a given time (for re-sync conflicts) */
    @Query("DELETE FROM sleep WHERE timestamp = :dayTimestamp AND startTime >= :afterTime")
    void deleteSleepItemsAfter(long dayTimestamp, long afterTime);

    /** Delete all sleep items for a given day */
    @Query("DELETE FROM sleep WHERE timestamp = :dayTimestamp")
    void deleteSleepItemsForDay(long dayTimestamp);

    // ═══ Sleep Records (Summary) ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSleepRecord(SleepRecordEntity record);

    @Query("SELECT * FROM sleep_records WHERE dayTimestamp = :dayTimestamp LIMIT 1")
    SleepRecordEntity getSleepRecordForDay(long dayTimestamp);

    @Query("SELECT * FROM sleep_records ORDER BY dayTimestamp DESC LIMIT 1")
    SleepRecordEntity getLatestSleepRecordSync();

    @Query("SELECT * FROM sleep_records ORDER BY dayTimestamp DESC LIMIT 1")
    LiveData<SleepRecordEntity> getLatestSleepRecord();

    @Query("SELECT * FROM sleep_records WHERE dayTimestamp >= :since ORDER BY dayTimestamp DESC")
    LiveData<List<SleepRecordEntity>> getSleepRecordsSince(long since);

    // ═══ Steps ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStep(StepEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSteps(List<StepEntity> entities);

    @Query("SELECT * FROM steps WHERE timestamp >= :since ORDER BY timestamp DESC")
    LiveData<List<StepEntity>> getStepsSince(long since);

    @Query("SELECT SUM(steps) FROM steps WHERE timestamp >= :since AND (source IS NULL OR source != 'total')")
    LiveData<Integer> getTotalStepsSince(long since);

    @Query("SELECT SUM(steps) FROM steps WHERE timestamp >= :since AND (source IS NULL OR source != 'total')")
    Integer getTotalStepsSinceSync(long since);

    @Query("SELECT * FROM steps WHERE timestamp >= :since AND source = 'total' ORDER BY timestamp DESC LIMIT 1")
    LiveData<StepEntity> getLatestTotalSteps(long since);

    @Query("DELETE FROM steps WHERE timestamp >= :since AND source = 'total'")
    void deleteTodayTotalSteps(long since);

    // ═══ Workouts ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWorkout(WorkoutEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWorkouts(List<WorkoutEntity> entities);

    @Query("SELECT * FROM workouts ORDER BY timestamp DESC")
    LiveData<List<WorkoutEntity>> getAllWorkouts();

    @Query("SELECT * FROM workouts ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<WorkoutEntity>> getRecentWorkouts(int limit);

    // ═══ Oxygen (SpO2) ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOxygen(OxygenEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOxygenRecords(List<OxygenEntity> entities);

    @Query("SELECT * FROM oxygen WHERE timestamp >= :since ORDER BY timestamp DESC")
    LiveData<List<OxygenEntity>> getOxygenSince(long since);

    @Query("SELECT * FROM oxygen ORDER BY timestamp DESC LIMIT 1")
    LiveData<OxygenEntity> getLatestOxygen();

    /** For graph data — returns chronologically ordered SpO2 readings since a timestamp */
    @Query("SELECT * FROM oxygen WHERE timestamp >= :since ORDER BY timestamp ASC")
    List<OxygenEntity> getOxygenSinceSync(long since);

    // ═══ Recovery Trend ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecoveryScore(RecoveryScoreEntity score);

    @Query("SELECT * FROM recovery_scores WHERE timestamp >= :since ORDER BY timestamp ASC")
    LiveData<List<RecoveryScoreEntity>> getRecoveryTrend(long since);

    // ═══ User Profile ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserProfile(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    UserProfile getUserProfile();

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    LiveData<UserProfile> getUserProfileLive();
}
