package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VoiceTranscriberApp
import com.example.audio.AudioImportResult
import com.example.audio.RecordingState
import com.example.data.local.entity.TranscriptionEntity
import com.example.data.local.entity.VocabularyEntity
import com.example.data.repository.ExportFormat
import com.example.domain.model.AccuracyMode
import com.example.domain.model.ProviderHealth
import com.example.domain.model.TranscriptionOptions
import com.example.domain.model.TranscriptionResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class TranscriptionUiState {
    object Idle : TranscriptionUiState()
    data class Processing(
        val providerName: String,
        val statusMessage: String,
        val fileName: String,
        val durationMs: Long
    ) : TranscriptionUiState()
    data class Success(val recordId: Long, val result: TranscriptionResult) : TranscriptionUiState()
    data class Error(val errorMessage: String, val isRetryable: Boolean = false) : TranscriptionUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as VoiceTranscriberApp).container
    private val transcriptionRepo = container.transcriptionRepository
    private val settingsRepo = container.settingsRepository
    private val keyStorage = container.secureKeyStorage
    private val transcriptionRouter = container.transcriptionRouter
    val audioRecorder = container.audioRecorder
    val audioPlayer = container.audioPlayer
    val audioImporter = container.audioImporter

    // Observables
    val recentTranscriptions: StateFlow<List<TranscriptionEntity>> = transcriptionRepo
        .getRecentTranscriptions(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTranscriptions: StateFlow<List<TranscriptionEntity>> = transcriptionRepo
        .allTranscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vocabularyList: StateFlow<List<VocabularyEntity>> = transcriptionRepo
        .allVocabulary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accuracyMode: StateFlow<AccuracyMode> = settingsRepo.accuracyMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccuracyMode.MAXIMUM_ACCURACY)

    val keepRecordings: StateFlow<Boolean> = settingsRepo.keepRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val providerHealthMap: StateFlow<Map<String, ProviderHealth>> = transcriptionRouter.providerHealthMap

    private val _transcriptionState = MutableStateFlow<TranscriptionUiState>(TranscriptionUiState.Idle)
    val transcriptionState: StateFlow<TranscriptionUiState> = _transcriptionState.asStateFlow()

    private val _selectedRecord = MutableStateFlow<TranscriptionEntity?>(null)
    val selectedRecord: StateFlow<TranscriptionEntity?> = _selectedRecord.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TranscriptionEntity>>(emptyList())
    val searchResults: StateFlow<List<TranscriptionEntity>> = _searchResults.asStateFlow()

    private val _todayGeminiMinutes = MutableStateFlow(0.0)
    val todayGeminiMinutes: StateFlow<Double> = _todayGeminiMinutes.asStateFlow()

    private val _todayGroqMinutes = MutableStateFlow(0.0)
    val todayGroqMinutes: StateFlow<Double> = _todayGroqMinutes.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Key settings state
    private val _geminiKey = MutableStateFlow(keyStorage.getGeminiKey())
    val geminiKey: StateFlow<String> = _geminiKey.asStateFlow()

    private val _groqKey = MutableStateFlow(keyStorage.getGroqKey())
    val groqKey: StateFlow<String> = _groqKey.asStateFlow()

    private val _geminiTestResult = MutableStateFlow<String?>(null)
    val geminiTestResult: StateFlow<String?> = _geminiTestResult.asStateFlow()

    private val _groqTestResult = MutableStateFlow<String?>(null)
    val groqTestResult: StateFlow<String?> = _groqTestResult.asStateFlow()

    init {
        refreshUsageStats()
        refreshKeyDisplay()
    }

    fun refreshUsageStats() {
        viewModelScope.launch {
            _todayGeminiMinutes.value = settingsRepo.getTodayMinutesForProvider("Gemini")
            _todayGroqMinutes.value = settingsRepo.getTodayMinutesForProvider("Groq Whisper")
            transcriptionRouter.updateHealthStatuses()
        }
    }

    fun refreshKeyDisplay() {
        _geminiKey.value = keyStorage.getGeminiKey()
        _groqKey.value = keyStorage.getGroqKey()
    }

    fun saveKeys(gemini: String, groq: String) {
        keyStorage.saveGeminiKey(gemini.trim())
        keyStorage.saveGroqKey(groq.trim())
        refreshKeyDisplay()
        transcriptionRouter.updateHealthStatuses()
        viewModelScope.launch {
            _toastMessage.emit("API keys saved successfully")
        }
    }

    fun testGeminiConnection() {
        viewModelScope.launch {
            _geminiTestResult.value = "Testing connection..."
            val (success, message) = container.geminiProvider.testConnection()
            _geminiTestResult.value = if (success) "✓ $message" else "✗ $message"
            transcriptionRouter.updateHealthStatuses()
        }
    }

    fun testGroqConnection() {
        viewModelScope.launch {
            _groqTestResult.value = "Testing connection..."
            val (success, message) = container.groqProvider.testConnection()
            _groqTestResult.value = if (success) "✓ $message" else "✗ $message"
            transcriptionRouter.updateHealthStatuses()
        }
    }

    fun setAccuracyMode(mode: AccuracyMode) {
        viewModelScope.launch {
            settingsRepo.setAccuracyMode(mode)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            transcriptionRepo.searchTranscriptions(query).collect { results ->
                _searchResults.value = results
            }
        }
    }

    fun selectRecord(id: Long) {
        viewModelScope.launch {
            val record = transcriptionRepo.getTranscriptionByIdDirect(id)
            _selectedRecord.value = record
            if (record != null && record.audioFilePath.isNotBlank()) {
                audioPlayer.loadAudio(record.audioFilePath)
            }
        }
    }

    fun updateRecordTitle(id: Long, newTitle: String) {
        viewModelScope.launch {
            val record = transcriptionRepo.getTranscriptionByIdDirect(id) ?: return@launch
            val updated = record.copy(title = newTitle.trim())
            transcriptionRepo.updateTranscription(updated)
            _selectedRecord.value = updated
        }
    }

    fun updateRecordTranscript(id: Long, newText: String) {
        viewModelScope.launch {
            val record = transcriptionRepo.getTranscriptionByIdDirect(id) ?: return@launch
            val wordCount = newText.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
            val updated = record.copy(transcript = newText.trim(), wordCount = wordCount)
            transcriptionRepo.updateTranscription(updated)
            _selectedRecord.value = updated
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            transcriptionRepo.deleteTranscription(id)
            if (_selectedRecord.value?.id == id) {
                _selectedRecord.value = null
                audioPlayer.release()
            }
            _toastMessage.emit("Transcript deleted")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            transcriptionRepo.deleteAllTranscriptions()
            _selectedRecord.value = null
            audioPlayer.release()
            _toastMessage.emit("All transcription history deleted")
        }
    }

    fun addVocabulary(term: String, category: String = "General") {
        if (term.isBlank()) return
        viewModelScope.launch {
            transcriptionRepo.addVocabularyTerm(term, category)
            _toastMessage.emit("Added \"$term\" to vocabulary")
        }
    }

    fun deleteVocabulary(id: Long) {
        viewModelScope.launch {
            transcriptionRepo.deleteVocabularyTerm(id)
        }
    }

    fun addPresetVocabulary(terms: List<Pair<String, String>>) {
        viewModelScope.launch {
            for ((term, cat) in terms) {
                transcriptionRepo.addVocabularyTerm(term, cat)
            }
            _toastMessage.emit("Preset terms added to vocabulary")
        }
    }

    // --- Transcription Flow ---

    fun startRecording(): Boolean {
        val file = audioRecorder.startRecording()
        return file != null
    }

    fun pauseRecording() = audioRecorder.pauseRecording()

    fun resumeRecording() = audioRecorder.resumeRecording()

    fun cancelRecording() = audioRecorder.cancelRecording()

    fun stopAndTranscribe() {
        val elapsed = audioRecorder.elapsedTimeMs.value
        val file = audioRecorder.stopRecording()
        if (file == null || !file.exists() || file.length() == 0L) {
            _transcriptionState.value = TranscriptionUiState.Error("Recording was too short or empty.")
            return
        }

        transcribeAudioFile(file, "audio/mp4", file.name, elapsed)
    }

    fun importAudio(uri: Uri) {
        viewModelScope.launch {
            _transcriptionState.value = TranscriptionUiState.Processing(
                providerName = "Local",
                statusMessage = "Importing audio file...",
                fileName = "Processing...",
                durationMs = 0L
            )

            val importResult = audioImporter.importAudioFromUri(uri)
            importResult.onSuccess { imported ->
                transcribeAudioFile(
                    audioFile = imported.file,
                    mimeType = imported.mimeType,
                    fileName = imported.originalFileName,
                    durationMs = imported.durationMs
                )
            }.onFailure { error ->
                _transcriptionState.value = TranscriptionUiState.Error(
                    "Failed to import audio: ${error.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun retranscribeRecord(record: TranscriptionEntity, forcedProviderName: String? = null) {
        val file = File(record.audioFilePath)
        if (!file.exists()) {
            viewModelScope.launch {
                _toastMessage.emit("Original audio file is no longer available.")
            }
            return
        }

        val mimeType = if (file.name.endsWith(".mp3")) "audio/mp3" else "audio/mp4"
        transcribeAudioFile(file, mimeType, record.originalFileName, record.durationMs, existingRecordId = record.id)
    }

    private fun transcribeAudioFile(
        audioFile: File,
        mimeType: String,
        fileName: String,
        durationMs: Long,
        existingRecordId: Long? = null
    ) {
        viewModelScope.launch {
            _transcriptionState.value = TranscriptionUiState.Processing(
                providerName = "Gemini",
                statusMessage = "Starting transcription...",
                fileName = fileName,
                durationMs = durationMs
            )

            val customVocab = transcriptionRepo.getVocabularyTermsDirect()
            val options = TranscriptionOptions(
                accuracyMode = accuracyMode.value,
                customVocabulary = customVocab
            )

            try {
                val result = transcriptionRouter.transcribeAudio(
                    audioFile = audioFile,
                    mimeType = mimeType,
                    options = options,
                    durationMs = durationMs,
                    onProgress = { status ->
                        val current = _transcriptionState.value
                        if (current is TranscriptionUiState.Processing) {
                            val detectedProvider = if (status.contains("Groq", ignoreCase = true)) "Groq Whisper" else "Gemini"
                            _transcriptionState.value = current.copy(
                                providerName = detectedProvider,
                                statusMessage = status
                            )
                        }
                    }
                )

                val smartTitle = transcriptionRepo.generateSmartTitle(result.text)
                val wordCount = result.text.split("\\s+".toRegex()).count { it.isNotBlank() }

                val recordId = if (existingRecordId != null) {
                    val existing = transcriptionRepo.getTranscriptionByIdDirect(existingRecordId)
                    if (existing != null) {
                        val updated = existing.copy(
                            transcript = result.text,
                            provider = result.providerName,
                            model = result.modelName,
                            detectedLanguages = result.detectedLanguages,
                            processingTimeMs = result.processingTimeMs,
                            wordCount = wordCount
                        )
                        transcriptionRepo.updateTranscription(updated)
                        existingRecordId
                    } else {
                        insertNewRecord(smartTitle, result, audioFile, fileName, durationMs, wordCount)
                    }
                } else {
                    insertNewRecord(smartTitle, result, audioFile, fileName, durationMs, wordCount)
                }

                refreshUsageStats()
                selectRecord(recordId)
                _transcriptionState.value = TranscriptionUiState.Success(recordId, result)
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Transcription failed. Please check your network and API keys."
                _transcriptionState.value = TranscriptionUiState.Error(errorMsg, isRetryable = true)
            }
        }
    }

    private suspend fun insertNewRecord(
        title: String,
        result: TranscriptionResult,
        audioFile: File,
        fileName: String,
        durationMs: Long,
        wordCount: Int
    ): Long {
        val entity = TranscriptionEntity(
            title = title,
            transcript = result.text,
            audioFilePath = audioFile.absolutePath,
            originalFileName = fileName,
            createdAt = System.currentTimeMillis(),
            durationMs = durationMs,
            provider = result.providerName,
            model = result.modelName,
            detectedLanguages = result.detectedLanguages,
            processingTimeMs = result.processingTimeMs,
            wordCount = wordCount
        )
        return transcriptionRepo.saveTranscription(entity)
    }

    fun dismissTranscriptionDialog() {
        _transcriptionState.value = TranscriptionUiState.Idle
    }

    fun getExportText(record: TranscriptionEntity, format: ExportFormat): String {
        return transcriptionRepo.formatExportContent(record, format)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
