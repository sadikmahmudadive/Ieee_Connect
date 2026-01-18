package com.example.ieeeconnect.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.ieeeconnect.domain.model.Event;

import java.util.List;

@Dao
public interface EventDao {
    @Query("SELECT * FROM events ORDER BY startTime DESC")
    List<Event> getAllEvents();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Event> events);

    @Query("DELETE FROM events")
    void deleteAll();

    @Transaction
    default void refreshEvents(List<Event> events) {
        deleteAll();
        if (events != null && !events.isEmpty()) {
            insertAll(events);
        }
    }

    @Query("SELECT * FROM events WHERE startTime > :currentTime ORDER BY startTime ASC")
    List<Event> getUpcomingEvents(long currentTime);
}
