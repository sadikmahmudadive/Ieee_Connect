package com.example.ieeeconnect.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY sentAt ASC")
    List<MessageEntity> getMessagesForChat(String chatId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertMessages(List<MessageEntity> messages);
}

