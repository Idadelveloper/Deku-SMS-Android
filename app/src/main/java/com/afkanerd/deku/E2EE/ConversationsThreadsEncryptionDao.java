package com.afkanerd.deku.E2EE;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ConversationsThreadsEncryptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ConversationsThreadsEncryption conversationsThreadsEncryption);

    @Query("SELECT * FROM ConversationsThreadsEncryption WHERE keystoreAlias = :keystoreAlias")
    ConversationsThreadsEncryption findByKeystoreAlias(String keystoreAlias);
}
