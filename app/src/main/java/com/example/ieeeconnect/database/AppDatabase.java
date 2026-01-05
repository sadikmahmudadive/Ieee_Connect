package com.example.ieeeconnect.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.ieeeconnect.database.converters.ListToStringConverter;
import com.example.ieeeconnect.domain.model.Event;

@Database(entities = {Event.class, PendingEvent.class}, version = 12, exportSchema = false)
@TypeConverters(ListToStringConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract EventDao eventDao();
    public abstract PendingEventDao pendingEventDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "ieee_connect_database_v4")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
