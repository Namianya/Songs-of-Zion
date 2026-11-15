package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesRepository
import com.example.data.model.HymnListItem
import com.example.data.model.SetlistWithEntries
import com.example.data.repository.HymnalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SetlistsUiState(
    val favorites: List<HymnListItem> = emptyList(),
    val setlists: List<SetlistWithEntries> = emptyList(),
    val preferredLanguageCode: String = "en"
)

class SetlistsViewModel(
    private val repository: HymnalRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SetlistsUiState> = combine(
        repository.observeFavoriteHymns(),
        repository.observeAllSetlists(),
        preferencesRepository.preferencesFlow
    ) { favorites, setlists, prefs ->
        val favItems = favorites.mapNotNull { item ->
            if (item.translations.isEmpty()) return@mapNotNull null
            val primary = item.translations.firstOrNull {
                it.languageCode.equals(prefs.preferredLanguageCode, ignoreCase = true)
            } ?: item.translations.first()
            val secondary = item.translations.firstOrNull { it.languageCode != primary.languageCode }
            HymnListItem(
                hymn = item.hymn,
                primaryTranslation = primary,
                secondaryTranslation = secondary,
                availableLanguages = item.translations.map { it.languageCode to it.languageName }
            )
        }
        SetlistsUiState(
            favorites = favItems,
            setlists = setlists,
            preferredLanguageCode = prefs.preferredLanguageCode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SetlistsUiState()
    )

    fun removeFavorite(hymnId: Long) {
        viewModelScope.launch {
            repository.setFavorite(hymnId, false)
        }
    }

    fun createSetlist(name: String, description: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.createSetlist(trimmed, description)
        }
    }

    fun deleteSetlist(setlistId: Long) {
        viewModelScope.launch {
            repository.deleteSetlist(setlistId)
        }
    }

    fun moveEntryUp(setlistId: Long, entryId: Long) {
        viewModelScope.launch {
            repository.moveSetlistEntry(setlistId, entryId, -1)
        }
    }

    fun moveEntryDown(setlistId: Long, entryId: Long) {
        viewModelScope.launch {
            repository.moveSetlistEntry(setlistId, entryId, 1)
        }
    }

    fun removeEntry(entryId: Long) {
        viewModelScope.launch {
            repository.removeSetlistEntry(entryId)
        }
    }

    class Factory(
        private val repository: HymnalRepository,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SetlistsViewModel(repository, preferencesRepository) as T
        }
    }
}
