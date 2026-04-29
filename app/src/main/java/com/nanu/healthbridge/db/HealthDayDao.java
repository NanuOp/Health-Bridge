package com.nanu.healthbridge.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HealthDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(HealthDay day);

    @Query("SELECT * FROM health_days WHERE date = :date")
    HealthDay getByDate(String date);

    @Query("SELECT * FROM health_days WHERE date = :date")
    LiveData<HealthDay> getByDateLive(String date);

    @Query("SELECT * FROM health_days ORDER BY date DESC LIMIT 30")
    List<HealthDay> getLast30Days();

    @Query("SELECT AVG(restingHR) FROM health_days WHERE date >= :fromDate AND restingHR > 0")
    double getAvgRestingHR(String fromDate);


    @Query("SELECT recoveryScore FROM health_days ORDER BY date DESC LIMIT 7")
    List<Integer> getLast7RecoveryScores();
    
    @Query("SELECT * FROM health_days ORDER BY date DESC LIMIT 7")
    LiveData<List<HealthDay>> getLast7DaysLive();
}
