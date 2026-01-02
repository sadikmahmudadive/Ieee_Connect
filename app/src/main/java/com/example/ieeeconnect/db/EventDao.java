package com.example.ieeeconnect.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EventDao {
    @Query("SELECT * FROM events ORDER BY startTime DESC")
    List<EventEntity> getAllEvents();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEvents(List<EventEntity> events);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEvent(EventEntity event);

    @Query("DELETE FROM events")
    void clearAll();
}

