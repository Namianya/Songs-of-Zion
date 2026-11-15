package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.HymnEntity
import com.example.data.model.HymnTranslationEntity
import com.example.data.model.SetlistEntryEntity
import com.example.data.model.WorshipSetlistEntity

@Database(
    entities = [
        HymnEntity::class,
        HymnTranslationEntity::class,
        WorshipSetlistEntity::class,
        SetlistEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ZionDatabase : RoomDatabase() {
    abstract fun hymnDao(): HymnDao
    abstract fun setlistDao(): SetlistDao

    companion object {
        @Volatile
        private var INSTANCE: ZionDatabase? = null

        fun getInstance(context: Context): ZionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZionDatabase::class.java,
                    "songs_of_zion.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
