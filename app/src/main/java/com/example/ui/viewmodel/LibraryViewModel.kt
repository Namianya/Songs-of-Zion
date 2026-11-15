package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesRepository
import com.example.data.local.ReaderPreferences
import com.example.data.model.DatabaseStats
import com.example.data.model.ImportSummary
import com.example.data.repository.HymnalRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Importing : ImportUiState
    data class Success(val summary: ImportSummary) : ImportUiState
    data class Error(val message: String) : ImportUiState
}

data class LibraryUiState(
    val stats: DatabaseStats = DatabaseStats(),
    val preferences: ReaderPreferences = ReaderPreferences(),
    val importState: ImportUiState = ImportUiState.Idle,
    val templateMessage: String? = null,
    val sampleJsonPreview: String = ""
)

class LibraryViewModel(
    private val repository: HymnalRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    private val templateMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.observeDatabaseStats(),
        preferencesRepository.preferencesFlow,
        importState,
        templateMessage
    ) { stats, prefs, importStatus, templateMsg ->
        LibraryUiState(
            stats = stats,
            preferences = prefs,
            importState = importStatus,
            templateMessage = templateMsg,
            sampleJsonPreview = repository.getSampleTemplateJson()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(sampleJsonPreview = repository.getSampleTemplateJson())
    )

    fun importDatabase(uri: Uri) {
        viewModelScope.launch {
            importState.value = ImportUiState.Importing
            runCatching { repository.importFromUri(uri) }
                .onSuccess { summary ->
                    preferencesRepository.recordImport(
                        timestamp = System.currentTimeMillis(),
                        count = summary.hymnsAdded + summary.hymnsUpdated
                    )
                    importState.value = ImportUiState.Success(summary)
                }
                .onFailure { error ->
                    importState.value = ImportUiState.Error(
                        error.message ?: "Failed to parse hymn JSON file."
                    )
                }
        }
    }

    fun exportSampleTemplate(uri: Uri) {
        viewModelScope.launch {
            runCatching { repository.exportTemplateToUri(uri) }
                .onSuccess {
                    templateMessage.value = "Sample JSON template saved."
                }
                .onFailure { err ->
                    templateMessage.value = err.message ?: "Could not save template."
                }
        }
    }

    fun dismissImportStatus() {
        importState.value = ImportUiState.Idle
    }

    fun dismissTemplateMessage() {
        templateMessage.value = null
    }

    fun setPreferredLanguage(languageCode: String) {
        viewModelScope.launch {
            preferencesRepository.setPreferredLanguage(languageCode)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setReaderNightMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReaderNightMode(enabled)
        }
    }

    fun setFontSize(sizeSp: Float) {
        viewModelScope.launch {
            preferencesRepository.setFontSize(sizeSp)
        }
    }

    class Factory(
        private val repository: HymnalRepository,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository, preferencesRepository) as T
        }
    }
}
