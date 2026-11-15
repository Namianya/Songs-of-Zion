package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesRepository
import com.example.data.local.ReaderPreferences
import com.example.data.model.HymnListItem
import com.example.data.model.HymnWithTranslations
import com.example.data.repository.HymnalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HymnSortOrder(val label: String) {
    NUMBER("By Number"),
    TITLE("A–Z Title")
}

data class HymnsUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedLanguageFilter: String = "all",
    val sortOrder: HymnSortOrder = HymnSortOrder.NUMBER,
    val categories: List<String> = listOf("All"),
    val availableLanguages: List<Pair<String, String>> = listOf("en" to "English", "ta" to "தமிழ்"),
    val hymns: List<HymnListItem> = emptyList(),
    val totalCatalogCount: Int = 0,
    val preferredLanguageCode: String = "en",
    val quickJumpDigits: String = "",
    val quickJumpMatches: List<HymnListItem> = emptyList()
)

class HymnsViewModel(
    private val repository: HymnalRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("All")
    private val selectedLanguageFilter = MutableStateFlow("all")
    private val sortOrder = MutableStateFlow(HymnSortOrder.NUMBER)
    private val quickJumpDigits = MutableStateFlow("")
    private val isSeeding = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            runCatching { repository.ensureSeeded() }
            isSeeding.value = false
        }
    }

    private data class FilterInputs(
        val query: String,
        val category: String,
        val languageFilter: String,
        val sort: HymnSortOrder,
        val jumpDigits: String
    )

    private val filterFlow = combine(
        searchQuery,
        selectedCategory,
        selectedLanguageFilter,
        sortOrder,
        quickJumpDigits
    ) { query, cat, lang, sort, jump ->
        FilterInputs(query, cat, lang, sort, jump)
    }

    val uiState: StateFlow<HymnsUiState> = combine(
        repository.observeAllHymns(),
        repository.observeCategories(),
        preferencesRepository.preferencesFlow,
        filterFlow,
        isSeeding
    ) { allHymns, categories, prefs, filters, seeding ->
        val langOrder = listOf("en", "ta", "ta-latn")
        val langs = allHymns
            .flatMap { it.translations }
            .distinctBy { it.languageCode }
            .map { it.languageCode to it.languageName }
            .sortedWith(
                compareBy<Pair<String, String>>(
                    { langOrder.indexOf(it.first.lowercase()).let { idx -> if (idx == -1) Int.MAX_VALUE else idx } },
                    { it.second }
                )
            )
            .ifEmpty { listOf("en" to "English", "ta" to "தமிழ்") }

        val displayLang = if (filters.languageFilter != "all") filters.languageFilter else prefs.preferredLanguageCode
        val trimmedQuery = filters.query.trim()

        val filtered = allHymns
            .asSequence()
            .filter { item ->
                filters.category == "All" || item.hymn.category.equals(filters.category, ignoreCase = true)
            }
            .filter { item ->
                filters.languageFilter == "all" || item.translations.any {
                    it.languageCode.equals(filters.languageFilter, ignoreCase = true)
                }
            }
            .mapNotNull { item ->
                matchAndBuildListItem(item, trimmedQuery, displayLang)
            }
            .toList()

        val sorted = when (filters.sort) {
            HymnSortOrder.NUMBER -> filtered.sortedBy { it.hymn.hymnNumber }
            HymnSortOrder.TITLE -> filtered.sortedBy { it.primaryTranslation.title.lowercase() }
        }

        val jumpMatches = buildQuickJumpMatches(allHymns, filters.jumpDigits, prefs)

        HymnsUiState(
            isLoading = seeding && allHymns.isEmpty(),
            searchQuery = filters.query,
            selectedCategory = filters.category,
            selectedLanguageFilter = filters.languageFilter,
            sortOrder = filters.sort,
            categories = listOf("All") + categories,
            availableLanguages = langs,
            hymns = sorted,
            totalCatalogCount = allHymns.size,
            preferredLanguageCode = prefs.preferredLanguageCode,
            quickJumpDigits = filters.jumpDigits,
            quickJumpMatches = jumpMatches
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HymnsUiState()
    )

    private fun matchAndBuildListItem(
        item: HymnWithTranslations,
        query: String,
        preferredLang: String
    ): HymnListItem? {
        if (item.translations.isEmpty()) return null
        val availableLangs = item.translations.map { it.languageCode to it.languageName }
        val defaultPrimary = item.translations.firstOrNull {
            it.languageCode.equals(preferredLang, ignoreCase = true)
        } ?: item.translations.first()

        if (query.isEmpty()) {
            val secondary = item.translations.firstOrNull { it.languageCode != defaultPrimary.languageCode }
            return HymnListItem(
                hymn = item.hymn,
                primaryTranslation = defaultPrimary,
                secondaryTranslation = secondary,
                availableLanguages = availableLangs,
                matchedSnippet = null
            )
        }

        val normalizedQuery = query.lowercase()
        val numberStr = item.hymn.hymnNumber.toString()
        val metadataMatches = numberStr == normalizedQuery ||
            numberStr.startsWith(normalizedQuery) ||
            "#$numberStr".contains(normalizedQuery) ||
            item.hymn.category.lowercase().contains(normalizedQuery) ||
            item.hymn.tuneName.lowercase().contains(normalizedQuery) ||
            item.hymn.author.lowercase().contains(normalizedQuery) ||
            item.hymn.scriptureRef.lowercase().contains(normalizedQuery)

        // Check translations for title or lyric match, preferring preferredLang first
        val orderedTranslations = listOfNotNull(defaultPrimary) +
            item.translations.filter { it.id != defaultPrimary.id }

        val matchedTranslation = orderedTranslations.firstOrNull { trans ->
            trans.title.lowercase().contains(normalizedQuery) ||
                trans.lyricsText.lowercase().contains(normalizedQuery)
        }

        if (!metadataMatches && matchedTranslation == null) {
            return null
        }

        val activeTranslation = matchedTranslation ?: defaultPrimary
        val secondary = item.translations.firstOrNull { it.languageCode != activeTranslation.languageCode }
        val snippet = matchedTranslation?.let { trans ->
            trans.lyricsText.lines().firstOrNull { line ->
                line.lowercase().contains(normalizedQuery)
            }?.trim()
        }

        return HymnListItem(
            hymn = item.hymn,
            primaryTranslation = activeTranslation,
            secondaryTranslation = secondary,
            availableLanguages = availableLangs,
            matchedSnippet = snippet
        )
    }

    private fun buildQuickJumpMatches(
        allHymns: List<HymnWithTranslations>,
        digits: String,
        prefs: ReaderPreferences
    ): List<HymnListItem> {
        val cleaned = digits.filter { it.isDigit() }
        val matches = if (cleaned.isEmpty()) {
            allHymns.take(6)
        } else {
            val exact = allHymns.filter { it.hymn.hymnNumber.toString() == cleaned }
            val prefix = allHymns.filter {
                it.hymn.hymnNumber.toString().startsWith(cleaned) && it.hymn.hymnNumber.toString() != cleaned
            }
            (exact + prefix).take(8)
        }
        return matches.mapNotNull { matchAndBuildListItem(it, "", prefs.preferredLanguageCode) }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        selectedCategory.value = category
    }

    fun onLanguageFilterSelected(languageCode: String) {
        selectedLanguageFilter.value = languageCode
    }

    fun onSortOrderChange(order: HymnSortOrder) {
        sortOrder.value = order
    }

    fun onToggleFavorite(hymnId: Long, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(hymnId, !currentlyFavorite)
        }
    }

    fun appendJumpDigit(digit: String) {
        if (quickJumpDigits.value.length < 5) {
            quickJumpDigits.value += digit
        }
    }

    fun backspaceJumpDigit() {
        if (quickJumpDigits.value.isNotEmpty()) {
            quickJumpDigits.value = quickJumpDigits.value.dropLast(1)
        }
    }

    fun clearJumpDigits() {
        quickJumpDigits.value = ""
    }

    class Factory(
        private val repository: HymnalRepository,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HymnsViewModel(repository, preferencesRepository) as T
        }
    }
}
