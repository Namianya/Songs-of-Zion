package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.FirebaseTamilTranslatorService
import com.example.data.ai.TamilTranslator
import com.example.data.local.PreferencesRepository
import com.example.data.local.ReaderPreferences
import com.example.data.model.HymnEntity
import com.example.data.model.HymnTranslationEntity
import com.example.data.model.LyricSection
import com.example.data.model.WorshipSetlistEntity
import com.example.data.repository.HymnalRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SetlistMembership(
    val setlist: WorshipSetlistEntity,
    val containsHymn: Boolean,
    val hymnCount: Int
)

data class ReaderUiState(
    val isLoading: Boolean = true,
    val hymn: HymnEntity? = null,
    val translations: List<HymnTranslationEntity> = emptyList(),
    val activeTranslation: HymnTranslationEntity? = null,
    val selectedLanguageCode: String = "en",
    val hasTamilTranslation: Boolean = false,
    val isTranslatingTamil: Boolean = false,
    val translationError: String? = null,
    val parallelTitle: String? = null,
    val sections: List<LyricSection> = emptyList(),
    val preferences: ReaderPreferences = ReaderPreferences(),
    val setlistMemberships: List<SetlistMembership> = emptyList(),
    val previousHymnId: Long? = null,
    val nextHymnId: Long? = null,
    val navigationContextLabel: String? = null
)

class ReaderViewModel(
    private val hymnId: Long,
    private val setlistId: Long?,
    private val repository: HymnalRepository,
    private val preferencesRepository: PreferencesRepository,
    private val tamilTranslator: TamilTranslator = FirebaseTamilTranslatorService()
) : ViewModel() {

    private val manualLanguageCode = MutableStateFlow<String?>(null)
    private val isTranslatingTamil = MutableStateFlow(false)
    private val translationError = MutableStateFlow<String?>(null)

    private data class TranslationUiFlags(
        val manualLang: String?,
        val isTranslating: Boolean,
        val error: String?
    )

    private val translationFlagsFlow = combine(
        manualLanguageCode,
        isTranslatingTamil,
        translationError
    ) { lang, translating, error ->
        TranslationUiFlags(lang, translating, error)
    }

    val uiState: StateFlow<ReaderUiState> = combine(
        repository.observeHymnById(hymnId),
        repository.observeAllHymns(),
        repository.observeAllSetlists(),
        preferencesRepository.preferencesFlow,
        translationFlagsFlow
    ) { hymnWithTrans, allHymns, allSetlists, prefs, flags ->
        if (hymnWithTrans == null) {
            return@combine ReaderUiState(isLoading = false, preferences = prefs)
        }

        val translations = hymnWithTrans.translations
        val desiredLang = flags.manualLang ?: prefs.preferredLanguageCode
        val hasTamil = translations.any { it.languageCode.equals("ta", ignoreCase = true) }
        val activeTranslation = translations.firstOrNull {
            it.languageCode.equals(desiredLang, ignoreCase = true)
        } ?: translations.firstOrNull()

        val parallelTitle = translations.firstOrNull {
            it.languageCode != activeTranslation?.languageCode
        }?.title

        val sections = activeTranslation?.let { repository.decodeSections(it.sectionsJson) }.orEmpty()

        val memberships = allSetlists.map { setlistWithEntries ->
            SetlistMembership(
                setlist = setlistWithEntries.setlist,
                containsHymn = setlistWithEntries.entries.any { it.entry.hymnId == hymnId },
                hymnCount = setlistWithEntries.entries.size
            )
        }

        val activeSetlist = setlistId?.let { id -> allSetlists.firstOrNull { it.setlist.id == id } }
        val (prevId, nextId, contextLabel) = if (activeSetlist != null) {
            val orderedIds = activeSetlist.entries.sortedBy { it.entry.position }.map { it.entry.hymnId }
            val idx = orderedIds.indexOf(hymnId)
            Triple(
                if (idx > 0) orderedIds[idx - 1] else null,
                if (idx >= 0 && idx < orderedIds.lastIndex) orderedIds[idx + 1] else null,
                if (idx >= 0) "${activeSetlist.setlist.name} • ${idx + 1}/${orderedIds.size}" else activeSetlist.setlist.name
            )
        } else {
            val orderedIds = allHymns.sortedBy { it.hymn.hymnNumber }.map { it.hymn.id }
            val idx = orderedIds.indexOf(hymnId)
            Triple(
                if (idx > 0) orderedIds[idx - 1] else null,
                if (idx >= 0 && idx < orderedIds.lastIndex) orderedIds[idx + 1] else null,
                null
            )
        }

        ReaderUiState(
            isLoading = false,
            hymn = hymnWithTrans.hymn,
            translations = translations,
            activeTranslation = activeTranslation,
            selectedLanguageCode = desiredLang,
            hasTamilTranslation = hasTamil,
            isTranslatingTamil = flags.isTranslating,
            translationError = flags.error,
            parallelTitle = parallelTitle,
            sections = sections,
            preferences = prefs,
            setlistMemberships = memberships,
            previousHymnId = prevId,
            nextHymnId = nextId,
            navigationContextLabel = contextLabel
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReaderUiState()
    )

    fun selectLanguage(languageCode: String) {
        val normalized = languageCode.lowercase()
        manualLanguageCode.value = normalized
        translationError.value = null
        if (normalized == "ta" && !uiState.value.hasTamilTranslation && !isTranslatingTamil.value) {
            translateCurrentHymnToTamil()
        }
    }

    fun translateCurrentHymnToTamil() {
        val current = uiState.value
        val hymn = current.hymn ?: return
        if (current.hasTamilTranslation || isTranslatingTamil.value) return

        val englishTranslation = current.translations.firstOrNull {
            it.languageCode.equals("en", ignoreCase = true)
        } ?: current.translations.firstOrNull() ?: return

        val englishSections = repository.decodeSections(englishTranslation.sectionsJson)
        if (englishSections.isEmpty()) return

        manualLanguageCode.value = "ta"
        viewModelScope.launch {
            isTranslatingTamil.value = true
            translationError.value = null
            try {
                val translated = tamilTranslator.translateToTamil(
                    hymnNumber = hymn.hymnNumber,
                    englishTitle = englishTranslation.title,
                    englishSections = englishSections
                )
                repository.saveTranslation(hymn.id, translated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to translate hymn #${hymn.hymnNumber} to Tamil", e)
                translationError.value = e.localizedMessage
                    ?: "Could not translate to Tamil. Check your internet connection and try again."
            } finally {
                isTranslatingTamil.value = false
            }
        }
    }

    fun dismissTranslationError() {
        translationError.value = null
    }

    fun toggleFavorite() {
        val current = uiState.value.hymn ?: return
        viewModelScope.launch {
            repository.setFavorite(current.id, !current.isFavorite)
        }
    }

    fun setFontSize(sizeSp: Float) {
        viewModelScope.launch {
            preferencesRepository.setFontSize(sizeSp)
        }
    }

    fun setReaderNightMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReaderNightMode(enabled)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun toggleHymnInSetlist(targetSetlistId: Long) {
        viewModelScope.launch {
            repository.toggleHymnInSetlist(targetSetlistId, hymnId)
        }
    }

    fun createSetlistAndAddHymn(name: String, description: String = "") {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val newId = repository.createSetlist(trimmed, description)
            repository.toggleHymnInSetlist(newId, hymnId)
        }
    }

    class Factory(
        private val hymnId: Long,
        private val setlistId: Long?,
        private val repository: HymnalRepository,
        private val preferencesRepository: PreferencesRepository,
        private val tamilTranslator: TamilTranslator = FirebaseTamilTranslatorService()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReaderViewModel(
                hymnId = hymnId,
                setlistId = setlistId,
                repository = repository,
                preferencesRepository = preferencesRepository,
                tamilTranslator = tamilTranslator
            ) as T
        }
    }

    companion object {
        private const val TAG = "ReaderViewModel"
    }
}
