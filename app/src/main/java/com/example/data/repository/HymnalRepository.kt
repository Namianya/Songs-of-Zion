package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.example.data.importexport.HymnJsonParser
import com.example.data.importexport.ParsedHymn
import com.example.data.importexport.ParsedTranslation
import com.example.data.local.PreferencesRepository
import com.example.data.local.ZionDatabase
import com.example.data.model.DatabaseStats
import com.example.data.model.HymnEntity
import com.example.data.model.HymnTranslationEntity
import com.example.data.model.HymnWithTranslations
import com.example.data.model.ImportSummary
import com.example.data.model.LanguageCount
import com.example.data.model.LyricSection
import com.example.data.model.SetlistEntryEntity
import com.example.data.model.SetlistWithEntries
import com.example.data.model.WorshipSetlistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class HymnalRepository(
    private val context: Context,
    private val database: ZionDatabase,
    private val preferencesRepository: PreferencesRepository? = null,
    private val parser: HymnJsonParser = HymnJsonParser()
) {
    private val hymnDao = database.hymnDao()
    private val setlistDao = database.setlistDao()
    private val seedMutex = Mutex()

    fun observeAllHymns(): Flow<List<HymnWithTranslations>> = hymnDao.observeAllHymns()

    fun observeFavoriteHymns(): Flow<List<HymnWithTranslations>> = hymnDao.observeFavoriteHymns()

    fun observeHymnById(hymnId: Long): Flow<HymnWithTranslations?> = hymnDao.observeHymnById(hymnId)

    fun observeCategories(): Flow<List<String>> = hymnDao.observeCategories()

    fun observeAllSetlists(): Flow<List<SetlistWithEntries>> = setlistDao.observeAllSetlists()

    fun observeSetlistById(setlistId: Long): Flow<SetlistWithEntries?> = setlistDao.observeSetlistById(setlistId)

    fun observeDatabaseStats(): Flow<DatabaseStats> = hymnDao.observeAllHymns().map { hymns ->
        val allTranslations = hymns.flatMap { it.translations }
        val categories = hymns.map { it.hymn.category }.distinct()
        val langBreakdown = allTranslations
            .groupBy { it.languageCode }
            .map { (code, list) ->
                LanguageCount(
                    languageCode = code,
                    languageName = list.firstOrNull()?.languageName ?: code.uppercase(),
                    count = list.size
                )
            }
            .sortedByDescending { it.count }

        DatabaseStats(
            totalHymns = hymns.size,
            totalTranslations = allTranslations.size,
            categoryCount = categories.size,
            languages = langBreakdown
        )
    }

    fun decodeSections(sectionsJson: String): List<LyricSection> {
        return parser.deserializeSections(sectionsJson)
    }

    fun getSampleTemplateJson(): String = parser.buildSampleTemplateJson()

    suspend fun ensureSeeded() {
        seedMutex.withLock {
            withContext(Dispatchers.IO) {
                val existingCount = hymnDao.getHymnCount()
                val seedVersion = preferencesRepository?.getSeedVersion() ?: 0
                if (existingCount >= 1100 && seedVersion >= CURRENT_SEED_VERSION) {
                    return@withContext
                }

                // Replace old 12-hymn MVP starter seed cleanly with the 695-hymn catalog
                if (seedVersion < CURRENT_SEED_VERSION && existingCount in 1..25) {
                    hymnDao.deleteAllHymns()
                }

                val seedJson = context.assets.open("hymns_seed.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val parsed = parser.parseCatalogJson(seedJson)
                upsertHymns(parsed, sourceName = "Bundled Seed")
                preferencesRepository?.setSeedVersion(CURRENT_SEED_VERSION)
            }
        }
    }

    suspend fun saveTranslation(hymnId: Long, translation: ParsedTranslation): HymnTranslationEntity {
        return withContext(Dispatchers.IO) {
            val flatLyrics = translation.sections.joinToString("\n") { section ->
                section.lines.joinToString("\n")
            }
            val sectionsJson = parser.serializeSections(translation.sections)
            val existing = hymnDao.getTranslation(hymnId, translation.languageCode)
            if (existing == null) {
                val entity = HymnTranslationEntity(
                    hymnId = hymnId,
                    languageCode = translation.languageCode,
                    languageName = translation.languageName,
                    title = translation.title,
                    lyricsText = flatLyrics,
                    sectionsJson = sectionsJson
                )
                val id = hymnDao.insertTranslation(entity)
                entity.copy(id = id)
            } else {
                val updated = existing.copy(
                    languageName = translation.languageName,
                    title = translation.title,
                    lyricsText = flatLyrics,
                    sectionsJson = sectionsJson
                )
                hymnDao.updateTranslation(updated)
                updated
            }
        }
    }

    suspend fun importFromUri(uri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        val rawJson = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalStateException("Could not open the selected file.")

        val parsed = parser.parseCatalogJson(rawJson)
        if (parsed.isEmpty()) {
            throw IllegalArgumentException("No valid hymns were found in that JSON file.")
        }
        val fileName = resolveDisplayName(uri) ?: "Imported JSON"
        upsertHymns(parsed, sourceName = fileName)
    }

    suspend fun exportTemplateToUri(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(parser.buildSampleTemplateJson())
            }
        } ?: throw IllegalStateException("Unable to write to the chosen location.")
    }

    private suspend fun upsertHymns(parsedHymns: List<ParsedHymn>, sourceName: String): ImportSummary {
        var added = 0
        var updated = 0
        var translationsCount = 0

        database.withTransaction {
            val now = System.currentTimeMillis()
            for (item in parsedHymns) {
                val existing = hymnDao.getHymnByNumber(item.hymnNumber)
                val hymnId = if (existing == null) {
                    added++
                    hymnDao.insertHymn(
                        HymnEntity(
                            hymnNumber = item.hymnNumber,
                            category = item.category,
                            tuneName = item.tuneName,
                            meter = item.meter,
                            musicalKey = item.musicalKey,
                            author = item.author,
                            scriptureRef = item.scriptureRef,
                            isFavorite = false,
                            updatedAt = now
                        )
                    )
                } else {
                    updated++
                    hymnDao.updateHymn(
                        existing.copy(
                            category = item.category.ifBlank { existing.category },
                            tuneName = item.tuneName.ifBlank { existing.tuneName },
                            meter = item.meter.ifBlank { existing.meter },
                            musicalKey = item.musicalKey.ifBlank { existing.musicalKey },
                            author = item.author.ifBlank { existing.author },
                            scriptureRef = item.scriptureRef.ifBlank { existing.scriptureRef },
                            updatedAt = now
                        )
                    )
                    existing.id
                }

                for (trans in item.translations) {
                    val flatLyrics = trans.sections.joinToString("\n") { section ->
                        section.lines.joinToString("\n")
                    }
                    val sectionsJson = parser.serializeSections(trans.sections)
                    val existingTrans = hymnDao.getTranslation(hymnId, trans.languageCode)
                    if (existingTrans == null) {
                        hymnDao.insertTranslation(
                            HymnTranslationEntity(
                                hymnId = hymnId,
                                languageCode = trans.languageCode,
                                languageName = trans.languageName,
                                title = trans.title,
                                lyricsText = flatLyrics,
                                sectionsJson = sectionsJson
                            )
                        )
                    } else {
                        hymnDao.updateTranslation(
                            existingTrans.copy(
                                languageName = trans.languageName,
                                title = trans.title,
                                lyricsText = flatLyrics,
                                sectionsJson = sectionsJson
                            )
                        )
                    }
                    translationsCount++
                }
            }
        }

        return ImportSummary(
            hymnsAdded = added,
            hymnsUpdated = updated,
            translationsSaved = translationsCount,
            sourceName = sourceName
        )
    }

    suspend fun setFavorite(hymnId: Long, isFavorite: Boolean) {
        hymnDao.setFavorite(hymnId, isFavorite)
    }

    suspend fun getAllHymnIdsOrdered(): List<Long> = withContext(Dispatchers.IO) {
        hymnDao.getAllHymnIdsOrdered()
    }

    suspend fun createSetlist(name: String, description: String = ""): Long {
        return setlistDao.insertSetlist(
            WorshipSetlistEntity(
                name = name.trim(),
                description = description.trim()
            )
        )
    }

    suspend fun deleteSetlist(setlistId: Long) {
        setlistDao.deleteSetlist(setlistId)
    }

    suspend fun toggleHymnInSetlist(setlistId: Long, hymnId: Long): Boolean {
        val count = setlistDao.countHymnInSetlist(setlistId, hymnId)
        return if (count > 0) {
            setlistDao.removeHymnFromSetlist(setlistId, hymnId)
            false
        } else {
            val nextPos = setlistDao.getMaxPosition(setlistId) + 1
            setlistDao.insertEntry(
                SetlistEntryEntity(
                    setlistId = setlistId,
                    hymnId = hymnId,
                    position = nextPos
                )
            )
            true
        }
    }

    suspend fun moveSetlistEntry(setlistId: Long, entryId: Long, direction: Int) {
        val entries = setlistDao.getEntriesForSetlist(setlistId).sortedBy { it.position }.toMutableList()
        val index = entries.indexOfFirst { it.id == entryId }
        val targetIndex = index + direction
        if (index == -1 || targetIndex !in entries.indices) return

        val item = entries.removeAt(index)
        entries.add(targetIndex, item)
        val reindexed = entries.mapIndexed { pos, entry -> entry.copy(position = pos) }
        setlistDao.updateEntries(reindexed)
    }

    suspend fun removeSetlistEntry(entryId: Long) {
        setlistDao.deleteEntry(entryId)
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }

    companion object {
        private const val CURRENT_SEED_VERSION = 3
    }
}
