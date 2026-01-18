package com.example.ieeeconnect.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.ieeeconnect.db.EventEntity;
import com.example.ieeeconnect.db.EventDao;
import com.example.ieeeconnect.database.PendingEvent;
import com.example.ieeeconnect.database.PendingEventDao;

@Database(entities = {EventEntity.class, PendingEvent.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DB_NAME = "ieee_connect_db";
    private static volatile AppDatabase instance;

    public abstract EventDao eventDao();
    public abstract PendingEventDao pendingEventDao();


    // Non-destructive migration from v1 -> v2: add basic time columns if missing
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            try { database.execSQL("ALTER TABLE events ADD COLUMN startTime INTEGER NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN endTime INTEGER NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
        }
    };

    // Non-destructive migration from v2 -> v3: add banner/creator/id fields
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            try { database.execSQL("ALTER TABLE events ADD COLUMN bannerUrl TEXT"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN creatorId TEXT"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN id TEXT"); } catch (Exception ignored) {}
        }
    };

    // Non-destructive migration from v3 -> v4: add optional columns if they don't exist
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add any new columns that newer code expects. Use try/catch to make migration idempotent.
            try { database.execSQL("ALTER TABLE events ADD COLUMN eventId TEXT"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN eventTime INTEGER NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN createdByUserId TEXT"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN goingUserIds TEXT NOT NULL DEFAULT '[]'"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN interestedUserIds TEXT NOT NULL DEFAULT '[]'"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN locationName TEXT"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN location TEXT"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN category TEXT"); } catch (Exception ignored) {}
            // Ensure commonly used columns exist (id/startTime/endTime/bannerUrl/creatorId)
            try { database.execSQL("ALTER TABLE events ADD COLUMN startTime INTEGER NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN endTime INTEGER NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN bannerUrl TEXT"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN creatorId TEXT"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN id TEXT"); } catch (Exception ignored) {}
        }
    };

    // Non-destructive migration from v4 -> v5: add location latitude/longitude, address and sync flags
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Keep ALTERs inside try/catch: safe and idempotent
            try { database.execSQL("ALTER TABLE events ADD COLUMN locationLatitude REAL"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN locationLongitude REAL"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN locationAddress TEXT"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1"); } catch (Exception ignored) {}
            try { database.execSQL("ALTER TABLE events ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)
                            // register non-destructive migrations to avoid data loss
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            // If the app encounters an unknown/incompatible schema during migration, fall back to destructive
                            // migration (clears DB). This avoids crashes on upgrade paths where we can't migrate safely.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
