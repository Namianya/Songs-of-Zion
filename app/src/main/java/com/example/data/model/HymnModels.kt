package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "hymns",
    indices = [Index(value = ["hymnNumber"], unique = true)]
)
data class HymnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hymnNumber: Int,
    val category: String,
    val tuneName: String = "",
    val meter: String = "",
    val musicalKey: String = "",
    val author: String = "",
    val scriptureRef: String = "",
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "hymn_translations",
    foreignKeys = [
        ForeignKey(
            entity = HymnEntity::class,
            parentColumns = ["id"],
            childColumns = ["hymnId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["hymnId", "languageCode"], unique = true),
        Index(value = ["languageCode"])
    ]
)
data class HymnTranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hymnId: Long,
    val languageCode: String,
    val languageName: String,
    val title: String,
    val lyricsText: String,
    val sectionsJson: String
)

@Entity(tableName = "worship_setlists")
data class WorshipSetlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "setlist_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorshipSetlistEntity::class,
            parentColumns = ["id"],
            childColumns = ["setlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HymnEntity::class,
            parentColumns = ["id"],
            childColumns = ["hymnId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["setlistId"]),
        Index(value = ["hymnId"])
    ]
)
data class SetlistEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setlistId: Long,
    val hymnId: Long,
    val position: Int
)

data class HymnWithTranslations(
    @Embedded val hymn: HymnEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "hymnId"
    )
    val translations: List<HymnTranslationEntity>
)

data class SetlistEntryWithHymn(
    @Embedded val entry: SetlistEntryEntity,
    @Relation(
        entity = HymnEntity::class,
        parentColumn = "hymnId",
        entityColumn = "id"
    )
    val hymnWithTranslations: HymnWithTranslations
)

data class SetlistWithEntries(
    @Embedded val setlist: WorshipSetlistEntity,
    @Relation(
        entity = SetlistEntryEntity::class,
        parentColumn = "id",
        entityColumn = "setlistId"
    )
    val entries: List<SetlistEntryWithHymn>
)

@JsonClass(generateAdapter = true)
data class LyricSection(
    val label: String,
    val type: String = "verse", // "verse" or "chorus"
    val lines: List<String>
) {
    val isChorus: Boolean
        get() = type.equals("chorus", ignoreCase = true) ||
            type.equals("refrain", ignoreCase = true) ||
            label.contains("chorus", ignoreCase = true) ||
            label.contains("refrain", ignoreCase = true) ||
            label.contains("பல்லவி", ignoreCase = true)
}

data class LanguageCount(
    val languageCode: String,
    val languageName: String,
    val count: Int
)

data class DatabaseStats(
    val totalHymns: Int = 0,
    val totalTranslations: Int = 0,
    val categoryCount: Int = 0,
    val languages: List<LanguageCount> = emptyList()
)

data class ImportSummary(
    val hymnsAdded: Int,
    val hymnsUpdated: Int,
    val translationsSaved: Int,
    val sourceName: String
)

data class HymnListItem(
    val hymn: HymnEntity,
    val primaryTranslation: HymnTranslationEntity,
    val secondaryTranslation: HymnTranslationEntity?,
    val availableLanguages: List<Pair<String, String>>,
    val matchedSnippet: String? = null
)
