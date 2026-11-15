package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.SetlistEntryEntity
import com.example.data.model.SetlistWithEntries
import com.example.data.model.WorshipSetlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetlistDao {
    @Transaction
    @Query("SELECT * FROM worship_setlists ORDER BY createdAt DESC")
    fun observeAllSetlists(): Flow<List<SetlistWithEntries>>

    @Transaction
    @Query("SELECT * FROM worship_setlists WHERE id = :setlistId")
    fun observeSetlistById(setlistId: Long): Flow<SetlistWithEntries?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlist(setlist: WorshipSetlistEntity): Long

    @Update
    suspend fun updateSetlist(setlist: WorshipSetlistEntity)

    @Query("DELETE FROM worship_setlists WHERE id = :setlistId")
    suspend fun deleteSetlist(setlistId: Long)

    @Query("SELECT * FROM setlist_entries WHERE setlistId = :setlistId ORDER BY position ASC")
    suspend fun getEntriesForSetlist(setlistId: Long): List<SetlistEntryEntity>

    @Query("SELECT COUNT(*) FROM setlist_entries WHERE setlistId = :setlistId AND hymnId = :hymnId")
    suspend fun countHymnInSetlist(setlistId: Long, hymnId: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM setlist_entries WHERE setlistId = :setlistId")
    suspend fun getMaxPosition(setlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: SetlistEntryEntity): Long

    @Update
    suspend fun updateEntries(entries: List<SetlistEntryEntity>)

    @Query("DELETE FROM setlist_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("DELETE FROM setlist_entries WHERE setlistId = :setlistId AND hymnId = :hymnId")
    suspend fun removeHymnFromSetlist(setlistId: Long, hymnId: Long)
}
