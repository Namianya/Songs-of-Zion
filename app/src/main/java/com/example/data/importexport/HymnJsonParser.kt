package com.example.data.importexport

import com.example.data.model.LyricSection
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.json.JSONArray
import org.json.JSONObject

data class ParsedTranslation(
    val languageCode: String,
    val languageName: String,
    val title: String,
    val sections: List<LyricSection>
)

data class ParsedHymn(
    val hymnNumber: Int,
    val category: String,
    val tuneName: String,
    val meter: String,
    val musicalKey: String,
    val author: String,
    val scriptureRef: String,
    val translations: List<ParsedTranslation>
)

class HymnJsonParser {
    private val moshi: Moshi = Moshi.Builder().build()
    private val sectionListType = Types.newParameterizedType(List::class.java, LyricSection::class.java)
    private val sectionsAdapter: JsonAdapter<List<LyricSection>> = moshi.adapter(sectionListType)

    fun serializeSections(sections: List<LyricSection>): String {
        return sectionsAdapter.toJson(sections)
    }

    fun deserializeSections(json: String): List<LyricSection> {
        return runCatching { sectionsAdapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())
    }

    fun parseCatalogJson(rawJson: String): List<ParsedHymn> {
        val trimmed = rawJson.trim().removePrefix("\uFEFF")
        if (trimmed.isEmpty()) return emptyList()

        val hymnsArray: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val root = JSONObject(trimmed)
                when {
                    root.has("hymns") && root.optJSONArray("hymns") != null -> root.getJSONArray("hymns")
                    root.has("songs") && root.optJSONArray("songs") != null -> root.getJSONArray("songs")
                    root.has("songs") && root.optJSONObject("songs") != null -> {
                        val songsMap = root.getJSONObject("songs")
                        val collected = JSONArray()
                        val sortedKeys = songsMap.keys().asSequence().toList()
                            .sortedWith(compareBy({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it }))
                        for (k in sortedKeys) {
                            val item = songsMap.optJSONObject(k)
                            if (item != null && item.optString("title", "").isNotBlank()) {
                                collected.put(item)
                            }
                        }
                        collected
                    }
                    root.has("data") && root.optJSONArray("data") != null -> root.getJSONArray("data")
                    else -> JSONArray().put(root)
                }
            }
            else -> throw IllegalArgumentException("File does not start with a JSON array or object.")
        }

        val parsed = mutableListOf<ParsedHymn>()
        for (i in 0 until hymnsArray.length()) {
            val obj = hymnsArray.optJSONObject(i) ?: continue
            val number = when {
                obj.has("hymnNumber") -> obj.optInt("hymnNumber", i + 1)
                obj.has("number") -> obj.optInt("number", i + 1)
                obj.has("id") -> obj.optInt("id", i + 1)
                else -> i + 1
            }
            val category = obj.optString("category", "").ifBlank { categorizeHymnNumber(number) }
            val tuneName = obj.optString("tuneName", obj.optString("tune", ""))
            val meter = obj.optString("meter", "")
            val musicalKey = obj.optString("musicalKey", obj.optString("key", ""))
            val author = cleanSongText(obj.optString("author", ""))
            val scriptureRef = obj.optString("scriptureRef", obj.optString("scripture", ""))

            val translations = mutableListOf<ParsedTranslation>()
            val transArray = obj.optJSONArray("translations")
            if (transArray != null) {
                for (tIdx in 0 until transArray.length()) {
                    val tObj = transArray.optJSONObject(tIdx) ?: continue
                    val parsedTrans = parseSingleTranslation(tObj)
                    if (parsedTrans != null) {
                        translations.add(parsedTrans)
                    }
                }
            } else {
                val single = parseSingleTranslation(obj)
                if (single != null) {
                    translations.add(single)
                }
            }

            if (translations.isNotEmpty()) {
                parsed.add(
                    ParsedHymn(
                        hymnNumber = number,
                        category = category,
                        tuneName = tuneName,
                        meter = meter,
                        musicalKey = musicalKey,
                        author = author,
                        scriptureRef = scriptureRef,
                        translations = translations
                    )
                )
            }
        }
        return parsed
    }

    private fun parseSingleTranslation(obj: JSONObject): ParsedTranslation? {
        val rawTitle = cleanSongText(obj.optString("title", ""))
        if (rawTitle.isEmpty()) return null

        val langCode = obj.optString("languageCode", obj.optString("language", "en"))
            .trim()
            .lowercase()
            .ifEmpty { "en" }
        val langName = obj.optString("languageName", "").trim().ifEmpty {
            when (langCode) {
                "en" -> "English"
                "ta" -> "தமிழ்"
                "ta-latn" -> "Tamil (Roman)"
                "sw" -> "Kiswahili"
                "fr" -> "Français"
                "es" -> "Español"
                else -> langCode.uppercase()
            }
        }

        val sections = mutableListOf<LyricSection>()
        val secArray = obj.optJSONArray("sections")
        val stanzasMap = obj.optJSONObject("stanzas")
        val chorusesMap = obj.optJSONObject("choruses")

        if (secArray != null) {
            for (sIdx in 0 until secArray.length()) {
                val sObj = secArray.optJSONObject(sIdx) ?: continue
                val label = sObj.optString("label", "Verse ${sIdx + 1}").ifBlank { "Verse ${sIdx + 1}" }
                val type = sObj.optString("type", "verse").ifBlank { "verse" }
                val lines = mutableListOf<String>()
                val linesArray = sObj.optJSONArray("lines")
                if (linesArray != null) {
                    for (lIdx in 0 until linesArray.length()) {
                        val line = linesArray.optString(lIdx, "").trimEnd()
                        if (line.isNotEmpty()) lines.add(line)
                    }
                } else if (sObj.has("text")) {
                    sObj.optString("text", "").lines()
                        .map { it.trimEnd() }
                        .filter { it.isNotEmpty() }
                        .forEach { lines.add(it) }
                }
                if (lines.isNotEmpty()) {
                    sections.add(LyricSection(label = label, type = type, lines = lines))
                }
            }
        } else if (stanzasMap != null || chorusesMap != null) {
            parseStanzasAndChorusesMaps(stanzasMap, chorusesMap, sections)
        } else {
            val rawLyrics = obj.optString("lyrics", "").trim()
            if (rawLyrics.isNotEmpty()) {
                val blocks = rawLyrics.split(Regex("\\r?\\n\\s*\\r?\\n"))
                blocks.forEachIndexed { idx, block ->
                    val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    if (lines.isNotEmpty()) {
                        val firstLower = lines.first().lowercase()
                        val isChorus = firstLower.startsWith("chorus") ||
                            firstLower.startsWith("refrain") ||
                            firstLower.startsWith("பல்லவி")
                        val cleanedLines = if (isChorus && lines.size > 1 && lines.first().endsWith(":")) {
                            lines.drop(1)
                        } else {
                            lines
                        }
                        val label = if (isChorus) {
                            if (langCode == "ta") "பல்லவி" else "Refrain"
                        } else {
                            if (langCode == "ta") "சரணம் ${idx + 1}" else "Verse ${idx + 1}"
                        }
                        sections.add(
                            LyricSection(
                                label = label,
                                type = if (isChorus) "chorus" else "verse",
                                lines = cleanedLines
                            )
                        )
                    }
                }
            }
        }

        if (sections.isEmpty()) return null
        return ParsedTranslation(
            languageCode = langCode,
            languageName = langName,
            title = rawTitle,
            sections = sections
        )
    }

    private fun parseStanzasAndChorusesMaps(
        stanzasMap: JSONObject?,
        chorusesMap: JSONObject?,
        sections: MutableList<LyricSection>
    ) {
        var extractedRefrain = false
        var verseIndex = 0
        if (stanzasMap != null) {
            val keys = stanzasMap.keys().asSequence().toList()
                .sortedWith(compareBy({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it }))
            for (k in keys) {
                val cleaned = cleanSongText(stanzasMap.optString(k, ""))
                if (isNoiseSection(cleaned)) continue
                val refrainMatch = Regex("\\b(?:Refrain|Chorus)\\b[:\\s]+(.+)$").find(cleaned)
                if (refrainMatch != null && refrainMatch.range.first > 10) {
                    val versePart = cleaned.substring(0, refrainMatch.range.first).trim()
                    val refrainPart = refrainMatch.groupValues[1].trim()
                    verseIndex++
                    sections.add(LyricSection("Verse $verseIndex", "verse", splitLinesCleanly(versePart)))
                    if (!isNoiseSection(refrainPart)) {
                        extractedRefrain = true
                        sections.add(LyricSection("Refrain", "chorus", splitLinesCleanly(refrainPart)))
                    }
                } else {
                    verseIndex++
                    sections.add(
                        LyricSection(
                            label = "Verse $verseIndex",
                            type = "verse",
                            lines = splitLinesCleanly(cleaned.removePrefix("*").trim())
                        )
                    )
                }
            }
        }

        if (!extractedRefrain && chorusesMap != null) {
            val keys = chorusesMap.keys().asSequence().toList()
                .sortedWith(compareBy({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it }))
            for (k in keys) {
                val cleaned = cleanSongText(chorusesMap.optString(k, ""))
                if (isNoiseSection(cleaned)) continue
                val cSec = LyricSection(
                    label = if (keys.size == 1) "Refrain" else "Chorus $k",
                    type = "chorus",
                    lines = splitLinesCleanly(cleaned)
                )
                if (sections.isNotEmpty() && sections[0].type == "verse") {
                    sections.add(1, cSec)
                } else {
                    sections.add(cSec)
                }
            }
        }
    }

    private fun cleanSongText(input: String): String {
        if (input.isBlank()) return ""
        return input
            .replace("``", "'")
            .replace("`", "'")
            .replace(Regex("A'+men"), "Amen")
            .replace("Posted by HYMNS at", "")
            .replace("*Stanza", "")
            .replace(Regex("\\s*\\((Refrain|Chorus)\\)\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isNoiseSection(text: String): Boolean {
        val t = text.trim()
        return t.isEmpty() ||
            t in setOf(".", ":", "SDAH", "to") ||
            t.startsWith("comments Labels:") ||
            t.startsWith("to be sung as refrain")
    }

    private fun splitLinesCleanly(text: String): List<String> {
        val t = text.trim()
        if (t.isEmpty()) return emptyList()
        if (t.contains('\n')) return t.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val parts = t.split(Regex("(?<=[;!?\\.])\\s+(?=[A-Z'\"])"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return if (parts.isNotEmpty()) parts else listOf(t)
    }

    private fun categorizeHymnNumber(num: Int): String = when {
        num <= 38 -> "Praise & Adoration"
        num <= 58 -> "Morning & Evening"
        num <= 69 -> "Worship & Opening"
        num <= 99 -> "God & Creation"
        num <= 114 -> "Providence & Grace"
        num <= 153 -> "Advent & Nativity"
        num <= 180 -> "Cross & Passion"
        num <= 256 -> "Resurrection & Redeemer"
        num <= 270 -> "Holy Spirit"
        num <= 278 -> "Holy Scriptures"
        num <= 343 -> "Gospel Call & Salvation"
        num <= 379 -> "Church & Service"
        num <= 395 -> "Sabbath"
        num <= 412 -> "Communion & Ordinances"
        num <= 454 -> "Second Coming & Glory"
        num <= 535 -> "Faith, Trust & Prayer"
        num <= 555 -> "Guidance & Shepherd"
        num <= 566 -> "Harvest & Thanksgiving"
        num <= 591 -> "Consecration & Life"
        num <= 633 -> "Pilgrimage & Watchfulness"
        num <= 659 -> "Stewardship & Home"
        else -> "Responses & Amens"
    }

    fun buildSampleTemplateJson(): String = """
{
  "catalog": "Songs of Zion",
  "version": "1.0",
  "hymns": [
    {
      "hymnNumber": 101,
      "category": "Praise & Adoration",
      "tuneName": "SAMPLE TUNE",
      "meter": "8.7.8.7",
      "musicalKey": "G Major",
      "author": "Hymn Writer",
      "scriptureRef": "Psalm 100:1-2",
      "translations": [
        {
          "languageCode": "en",
          "languageName": "English",
          "title": "Sample English Hymn Title",
          "sections": [
            {
              "label": "Verse 1",
              "type": "verse",
              "lines": [
                "First line of the opening verse,",
                "Second line of praise and grace."
              ]
            },
            {
              "label": "Refrain",
              "type": "chorus",
              "lines": [
                "Sing hallelujah to the King,",
                "Let all the earth His praises bring."
              ]
            }
          ]
        },
        {
          "languageCode": "ta",
          "languageName": "தமிழ்",
          "title": "மாதிரி தமிழ் பாடல் தலைப்பு",
          "sections": [
            {
              "label": "சரணம் 1",
              "type": "verse",
              "lines": [
                "முதல் சரணத்தின் முதல் வரி,",
                "தேவனைப் போற்றும் இரண்டாம் வரி."
              ]
            },
            {
              "label": "பல்லவி",
              "type": "chorus",
              "lines": [
                "அல்லேலூயா பாடுவோம்,",
                "ஆண்டவரைத் துதிப்போம்."
              ]
            }
          ]
        }
      ]
    }
  ]
}
""".trimIndent()
}
