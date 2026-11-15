package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.importexport.HymnJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HymnJsonParserTest {
    private val parser = HymnJsonParser()

    @Test
    fun `parses bundled SDAH and Tamil hymnal catalog with transliterations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val seedJson = context.assets.open("hymns_seed.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val hymns = parser.parseCatalogJson(seedJson)

        assertTrue("Expected at least 1100 hymns (695 SDAH + Tamil hymnal)", hymns.size >= 1100)
        val sdahHymns = hymns.filter { it.hymnNumber in 1..695 }
        assertEquals(695, sdahHymns.size)
        sdahHymns.forEach { hymn ->
            val en = hymn.translations.firstOrNull { it.languageCode == "en" }
            assertTrue("Hymn #${hymn.hymnNumber} should have EN", en != null && en.sections.isNotEmpty())
        }

        val tamilCount = hymns.count { hymn -> hymn.translations.any { it.languageCode == "ta" } }
        val romanCount = hymns.count { hymn -> hymn.translations.any { it.languageCode == "ta-latn" } }
        assertTrue("Expected at least 600 Tamil translations, got $tamilCount", tamilCount >= 600)
        assertTrue("Expected at least 595 Tamil transliterations, got $romanCount", romanCount >= 595)

        val hymn73 = hymns.first { it.hymnNumber == 73 }
        val ta73 = hymn73.translations.first { it.languageCode == "ta" }
        val roman73 = hymn73.translations.first { it.languageCode == "ta-latn" }
        assertTrue(ta73.title.contains("தூய"))
        assertTrue(roman73.title.contains("Thooya"))
    }

    @Test
    fun `parses sample bilingual english and tamil template`() {
        val sample = parser.buildSampleTemplateJson()
        val hymns = parser.parseCatalogJson(sample)

        assertEquals(1, hymns.size)
        val hymn = hymns.first()
        assertEquals(101, hymn.hymnNumber)
        assertEquals(2, hymn.translations.size)
        assertEquals("en", hymn.translations[0].languageCode)
        assertEquals("ta", hymn.translations[1].languageCode)
        assertTrue(hymn.translations[1].title.contains("தமிழ்"))
    }

    @Test
    fun `parses songs map format with stanzas and choruses`() {
        val raw = """
            {
              "songs": {
                "0": {
                  "id": 100,
                  "title": "Great Is Thy Faithfulness",
                  "author": "Thomas Chisholm",
                  "key": "D",
                  "stanzas": {
                    "1": "Great is Thy faithfulness, O God my Father. Refrain Great is Thy faithfulness!"
                  },
                  "choruses": {}
                },
                "1": {
                  "id": 101,
                  "title": "",
                  "stanzas": {},
                  "choruses": {}
                }
              }
            }
        """.trimIndent()

        val hymns = parser.parseCatalogJson(raw)
        assertEquals(1, hymns.size)
        assertEquals(100, hymns[0].hymnNumber)
        assertEquals("D", hymns[0].musicalKey)
        assertEquals(2, hymns[0].translations[0].sections.size)
        assertTrue(hymns[0].translations[0].sections[1].isChorus)
    }

    @Test
    fun `parses flat single language hymn array`() {
        val raw = """
            [
              {
                "number": 55,
                "category": "Prayer & Devotion",
                "languageCode": "ta",
                "title": "இயேசுவே என் தேவா",
                "lyrics": "முதல் வரி\nஇரண்டாம் வரி\n\nபல்லவி:\nதுதி பாடுவோம்"
              }
            ]
        """.trimIndent()

        val hymns = parser.parseCatalogJson(raw)
        assertEquals(1, hymns.size)
        assertEquals(55, hymns[0].hymnNumber)
        assertEquals("ta", hymns[0].translations[0].languageCode)
        assertEquals(2, hymns[0].translations[0].sections.size)
        assertTrue(hymns[0].translations[0].sections[1].isChorus)
    }
}
