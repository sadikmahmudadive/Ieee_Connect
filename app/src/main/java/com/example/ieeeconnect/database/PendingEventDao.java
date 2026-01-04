package com.example.ieeeconnect.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PendingEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPending(PendingEvent event);

    @Query("SELECT * FROM pending_events")
    List<PendingEvent> getAllPending();

    @Query("DELETE FROM pending_events WHERE localId = :localId")
    void deleteById(String localId);
}

