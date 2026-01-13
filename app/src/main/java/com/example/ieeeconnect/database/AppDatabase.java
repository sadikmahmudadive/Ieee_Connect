package com.example.ieeeconnect.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.ieeeconnect.database.converters.ListToStringConverter;
import com.example.ieeeconnect.domain.model.Event;

// Bumped version to 21 to reflect schema changes and allow non-destructive migration for 'category' column.
// Enable exportSchema so the annotation processor will write schema JSON files to the configured kapt schemaLocation.
@Database(entities = {Event.class, PendingEvent.class}, version = 21, exportSchema = true)
@TypeConverters(ListToStringConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract EventDao eventDao();
    public abstract PendingEventDao pendingEventDao();

    private static volatile AppDatabase INSTANCE;

    // Migration from v19 -> v20: add new columns introduced in Event model without dropping data.
    public static final Migration MIGRATION_19_20 = new Migration(19, 20) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Try to add columns if they don't exist. If column already exists, the ALTER will throw
            // an exception; we catch and ignore to make migration idempotent for different starting schemas.
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN eventId TEXT");
            } catch (Exception ignored) {}

            try {
                database.execSQL("ALTER TABLE events ADD COLUMN eventTime INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) {}

            try {
                database.execSQL("ALTER TABLE events ADD COLUMN createdByUserId TEXT");
            } catch (Exception ignored) {}

            try {
                database.execSQL("ALTER TABLE events ADD COLUMN goingUserIds TEXT NOT NULL DEFAULT '[]'");
            } catch (Exception ignored) {}

            try {
                database.execSQL("ALTER TABLE events ADD COLUMN interestedUserIds TEXT NOT NULL DEFAULT '[]'");
            } catch (Exception ignored) {}

            try {
                database.execSQL("ALTER TABLE events ADD COLUMN locationName TEXT");
            } catch (Exception ignored) {}

            try {
                database.execSQL("ALTER TABLE events ADD COLUMN location TEXT");
            } catch (Exception ignored) {}

            try {
                database.execSQL("ALTER TABLE events ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) {}

            // Ensure startTime/endTime and bannerUrl/id columns exist (they likely already do)
            try { database.execSQL("ALTER TABLE events ADD COLUMN startTime INTEGER NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN endTime INTEGER NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN bannerUrl TEXT"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN id TEXT"); } catch (Exception ignored) {}
        }
    };

    // Migration from v20 -> v21: add 'category' column if it does not exist (non-destructive)
    public static final Migration MIGRATION_20_21 = new Migration(20, 21) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN category TEXT");
            } catch (Exception ignored) {}
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "ieee_connect_database_v8")
                            // Register migrations so Room can upgrade without destroying user data.
                            .addMigrations(MIGRATION_19_20, MIGRATION_20_21)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
