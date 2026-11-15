package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zion_preferences")

data class ReaderPreferences(
    val fontSizeSp: Float = 18f,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val readerNightMode: Boolean = false,
    val preferredLanguageCode: String = "en",
    val lastImportTimestamp: Long = 0L,
    val lastImportCount: Int = 0
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val READER_NIGHT_MODE = booleanPreferencesKey("reader_night_mode")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val LAST_IMPORT_TIMESTAMP = longPreferencesKey("last_import_timestamp")
        val LAST_IMPORT_COUNT = intPreferencesKey("last_import_count")
        val CATALOG_SEED_VERSION = intPreferencesKey("catalog_seed_version")
    }

    val preferencesFlow: Flow<ReaderPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)
        ReaderPreferences(
            fontSizeSp = (prefs[Keys.FONT_SIZE_SP] ?: 18f).coerceIn(14f, 28f),
            themeMode = themeMode,
            readerNightMode = prefs[Keys.READER_NIGHT_MODE] ?: false,
            preferredLanguageCode = prefs[Keys.PREFERRED_LANGUAGE] ?: "en",
            lastImportTimestamp = prefs[Keys.LAST_IMPORT_TIMESTAMP] ?: 0L,
            lastImportCount = prefs[Keys.LAST_IMPORT_COUNT] ?: 0
        )
    }

    suspend fun getSeedVersion(): Int {
        return context.dataStore.data.first()[Keys.CATALOG_SEED_VERSION] ?: 0
    }

    suspend fun setSeedVersion(version: Int) {
        context.dataStore.edit { it[Keys.CATALOG_SEED_VERSION] = version }
    }

    suspend fun setFontSize(sizeSp: Float) {
        context.dataStore.edit { it[Keys.FONT_SIZE_SP] = sizeSp.coerceIn(14f, 28f) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setReaderNightMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.READER_NIGHT_MODE] = enabled }
    }

    suspend fun setPreferredLanguage(languageCode: String) {
        context.dataStore.edit { it[Keys.PREFERRED_LANGUAGE] = languageCode }
    }

    suspend fun recordImport(timestamp: Long, count: Int) {
        context.dataStore.edit {
            it[Keys.LAST_IMPORT_TIMESTAMP] = timestamp
            it[Keys.LAST_IMPORT_COUNT] = count
        }
    }
}
