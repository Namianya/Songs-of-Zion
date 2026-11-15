package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.HymnEntity
import com.example.data.model.HymnTranslationEntity
import com.example.data.model.HymnWithTranslations
import kotlinx.coroutines.flow.Flow

@Dao
interface HymnDao {
    @Transaction
    @Query("SELECT * FROM hymns ORDER BY hymnNumber ASC")
    fun observeAllHymns(): Flow<List<HymnWithTranslations>>

    @Transaction
    @Query("SELECT * FROM hymns WHERE isFavorite = 1 ORDER BY hymnNumber ASC")
    fun observeFavoriteHymns(): Flow<List<HymnWithTranslations>>

    @Transaction
    @Query("SELECT * FROM hymns WHERE id = :hymnId")
    fun observeHymnById(hymnId: Long): Flow<HymnWithTranslations?>

    @Query("SELECT * FROM hymns WHERE hymnNumber = :hymnNumber LIMIT 1")
    suspend fun getHymnByNumber(hymnNumber: Int): HymnEntity?

    @Query("SELECT COUNT(*) FROM hymns")
    suspend fun getHymnCount(): Int

    @Query("SELECT id FROM hymns ORDER BY hymnNumber ASC")
    suspend fun getAllHymnIdsOrdered(): List<Long>

    @Query("SELECT DISTINCT category FROM hymns ORDER BY category ASC")
    fun observeCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHymn(hymn: HymnEntity): Long

    @Update
    suspend fun updateHymn(hymn: HymnEntity)

    @Query("UPDATE hymns SET isFavorite = :isFavorite WHERE id = :hymnId")
    suspend fun setFavorite(hymnId: Long, isFavorite: Boolean)

    @Query("SELECT * FROM hymn_translations WHERE hymnId = :hymnId AND languageCode = :languageCode LIMIT 1")
    suspend fun getTranslation(hymnId: Long, languageCode: String): HymnTranslationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(translation: HymnTranslationEntity): Long

    @Update
    suspend fun updateTranslation(translation: HymnTranslationEntity)

    @Query("DELETE FROM hymns")
    suspend fun deleteAllHymns()
}
