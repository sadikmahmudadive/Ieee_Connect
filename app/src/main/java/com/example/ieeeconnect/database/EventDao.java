package com.example.ieeeconnect.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.ieeeconnect.domain.model.Event;

import java.util.List;

@Dao
public interface EventDao {
    @Query("SELECT * FROM events ORDER BY eventTime DESC")
    List<Event> getAllEvents();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Event> events);
}
