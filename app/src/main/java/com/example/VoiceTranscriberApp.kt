package com.example

import android.app.Application
import com.example.audio.AudioImporter
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.data.local.AppDatabase
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TranscriptionRepository
import com.example.data.security.SecureKeyStorage
import com.example.transcription.GeminiProvider
import com.example.transcription.GroqProvider
import com.example.transcription.TranscriptionRouter

class AppContainer(private val application: Application) {
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(application)
    }

    val secureKeyStorage: SecureKeyStorage by lazy {
        SecureKeyStorage(application)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(application, database.usageStatDao())
    }

    val transcriptionRepository: TranscriptionRepository by lazy {
        TranscriptionRepository(database.transcriptionDao(), database.vocabularyDao())
    }

    val geminiProvider: GeminiProvider by lazy {
        GeminiProvider(secureKeyStorage)
    }

    val groqProvider: GroqProvider by lazy {
        GroqProvider(secureKeyStorage)
    }

    val transcriptionRouter: TranscriptionRouter by lazy {
        TranscriptionRouter(geminiProvider, groqProvider, settingsRepository)
    }

    val audioRecorder: AudioRecorder by lazy {
        AudioRecorder(application)
    }

    val audioPlayer: AudioPlayer by lazy {
        AudioPlayer(application)
    }

    val audioImporter: AudioImporter by lazy {
        AudioImporter(application)
    }
}

class VoiceTranscriberApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: VoiceTranscriberApp
            private set
    }
}
