package com.example.data.ai

import android.util.Log
import com.example.data.importexport.ParsedTranslation
import com.example.data.model.LyricSection
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

interface TamilTranslator {
    suspend fun translateToTamil(
        hymnNumber: Int,
        englishTitle: String,
        englishSections: List<LyricSection>
    ): ParsedTranslation
}

class FirebaseTamilTranslatorService : TamilTranslator {

    private val translationSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Reverent Christian hymn title in Tamil script"),
            "sections" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "label" to Schema.string(description = "Section label in Tamil such as 'சரணம் 1' or 'பல்லவி'"),
                        "type" to Schema.enumeration(
                            values = listOf("verse", "chorus"),
                            description = "Whether this section is a verse or chorus/refrain"
                        ),
                        "lines" to Schema.array(
                            items = Schema.string(),
                            description = "Poetic Tamil lyric lines preserving the verse meaning"
                        )
                    )
                ),
                description = "All translated verses and refrains in exact section order"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = translationSchema
                temperature = 0.3f
            }
        )
    }

    override suspend fun translateToTamil(
        hymnNumber: Int,
        englishTitle: String,
        englishSections: List<LyricSection>
    ): ParsedTranslation {
        val prompt = buildPrompt(hymnNumber, englishTitle, englishSections)
        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("Translation model returned an empty response.")

            val root = JSONObject(jsonString)
            val tamilTitle = root.optString("title", "").trim()
            if (tamilTitle.isEmpty()) {
                throw IllegalStateException("Translated Tamil title was empty.")
            }

            val sectionsArray = root.getJSONArray("sections")
            val parsedSections = mutableListOf<LyricSection>()
            for (i in 0 until sectionsArray.length()) {
                val secObj = sectionsArray.getJSONObject(i)
                val type = secObj.optString("type", "verse").lowercase().let {
                    if (it == "chorus" || it == "refrain") "chorus" else "verse"
                }
                val defaultLabel = if (type == "chorus") "பல்லவி" else "சரணம் ${i + 1}"
                val label = secObj.optString("label", defaultLabel).trim().ifEmpty { defaultLabel }
                val linesArr = secObj.getJSONArray("lines")
                val lines = (0 until linesArr.length())
                    .map { idx -> linesArr.getString(idx).trim() }
                    .filter { it.isNotEmpty() }
                if (lines.isNotEmpty()) {
                    parsedSections.add(LyricSection(label = label, type = type, lines = lines))
                }
            }

            if (parsedSections.isEmpty()) {
                throw IllegalStateException("No translated lyric sections were returned.")
            }

            return ParsedTranslation(
                languageCode = "ta",
                languageName = "தமிழ்",
                title = tamilTitle,
                sections = parsedSections
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Tamil translation timed out for hymn #$hymnNumber", e)
            throw IllegalStateException("Tamil translation timed out. Check your connection and try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Tamil translation failed for hymn #$hymnNumber", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Unable to translate hymn to Tamil right now.",
                e
            )
        }
    }

    private fun buildPrompt(
        hymnNumber: Int,
        englishTitle: String,
        sections: List<LyricSection>
    ): String = buildString {
        appendLine("Translate Christian Hymn #$hymnNumber into natural, poetic, reverent Tamil (தமிழ்) suitable for congregational worship.")
        appendLine("Rules:")
        appendLine("- Preserve the exact number and order of sections.")
        appendLine("- Use 'சரணம் 1', 'சரணம் 2', etc. for verses (type: 'verse') and 'பல்லவி' for refrains/choruses (type: 'chorus').")
        appendLine("- Translate every line faithfully in clear Tamil script without transliteration or English words.")
        appendLine()
        appendLine("English Title: $englishTitle")
        sections.forEach { section ->
            appendLine("[${section.label} | ${section.type}]")
            section.lines.forEach { line -> appendLine(line) }
            appendLine()
        }
    }

    companion object {
        private const val TAG = "TamilTranslatorService"
    }
}
