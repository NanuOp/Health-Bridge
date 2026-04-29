package com.nanu.healthbridge.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Summary record for a single sleep session (one night or nap period).
 * Calculated using SDK's SleepCalculateHelper from raw FcSleepItem data.
 */
@Entity(tableName = "sleep_records")
public class SleepRecordEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long dayTimestamp;      // The "date" of this sleep (day start millis)
    public int deepSeconds;
    public int lightSeconds;
    public int remSeconds;
    public int soberSeconds;       // Awake time
    public int napSeconds;
    public boolean isSupportSleepNap;
    public int score;              // Sleep quality score from watch
    public int efficiency;         // Sleep efficiency from watch
}
