package com.example

import android.app.Application
import com.example.data.ai.FirebaseTamilTranslatorService
import com.example.data.ai.TamilTranslator
import com.example.data.local.PreferencesRepository
import com.example.data.local.ZionDatabase
import com.example.data.repository.HymnalRepository

class SongsOfZionApplication : Application() {
    val database: ZionDatabase by lazy { ZionDatabase.getInstance(this) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }
    val hymnalRepository: HymnalRepository by lazy {
        HymnalRepository(this, database, preferencesRepository)
    }
    val tamilTranslator: TamilTranslator by lazy { FirebaseTamilTranslatorService() }
}
