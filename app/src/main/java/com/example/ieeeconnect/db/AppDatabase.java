package com.example.ieeeconnect.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.ieeeconnect.db.EventEntity;
import com.example.ieeeconnect.db.EventDao;
import com.example.ieeeconnect.database.PendingEvent;
import com.example.ieeeconnect.database.PendingEventDao;

@Database(entities = {EventEntity.class, PendingEvent.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DB_NAME = "ieee_connect_db";
    private static volatile AppDatabase instance;

    public abstract EventDao eventDao();
    public abstract PendingEventDao pendingEventDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
