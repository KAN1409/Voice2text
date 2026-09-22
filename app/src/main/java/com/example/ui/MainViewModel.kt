package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VoiceTranscriberApp
import com.example.audio.AudioImportResult
import com.example.audio.RecordingState
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TranscriptionEntity
import com.example.data.local.entity.VocabularyEntity
import com.example.data.repository.ExportFormat
import com.example.domain.model.AccuracyMode
import com.example.domain.model.NoteSourceType
import com.example.domain.model.ProviderHealth
import com.example.domain.model.TranscriptionOptions
import com.example.domain.model.TranscriptionResult
import com.example.notes.naming.CaptureTitleSuggester
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
    data class Success(val noteId: Long, val result: TranscriptionResult) : TranscriptionUiState()
    data class Error(val errorMessage: String, val isRetryable: Boolean = false) : TranscriptionUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as VoiceTranscriberApp).container
    private val noteRepo = container.noteRepository
    private val transcriptionRepo = container.transcriptionRepository
    private val settingsRepo = container.settingsRepository
    private val keyStorage = container.secureKeyStorage
    private val transcriptionRouter = container.transcriptionRouter
    val audioRecorder = container.audioRecorder
    val audioPlayer = container.audioPlayer
    val audioImporter = container.audioImporter

    // Observables - Notes
    val allActiveNotes: StateFlow<List<NoteEntity>> = noteRepo
        .allActiveNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedNotes: StateFlow<List<NoteEntity>> = noteRepo
        .pinnedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = noteRepo
        .archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vocabularyList: StateFlow<List<VocabularyEntity>> = noteRepo
        .allVocabulary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accuracyMode: StateFlow<AccuracyMode> = settingsRepo.accuracyMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccuracyMode.MAXIMUM_ACCURACY)

    val keepRecordings: StateFlow<Boolean> = settingsRepo.keepRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val providerHealthMap: StateFlow<Map<String, ProviderHealth>> = transcriptionRouter.providerHealthMap

    private val _transcriptionState = MutableStateFlow<TranscriptionUiState>(TranscriptionUiState.Idle)
    val transcriptionState: StateFlow<TranscriptionUiState> = _transcriptionState.asStateFlow()

    private val _selectedNote = MutableStateFlow<NoteEntity?>(null)
    val selectedNote: StateFlow<NoteEntity?> = _selectedNote.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<NoteEntity>>(emptyList())
    val searchResults: StateFlow<List<NoteEntity>> = _searchResults.asStateFlow()

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

    // --- Search ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            noteRepo.searchNotes(query).collect { results ->
                _searchResults.value = results
            }
        }
    }

    // --- Note Selection & Actions ---
    fun selectNote(id: Long) {
        viewModelScope.launch {
            val note = noteRepo.getNoteByIdDirect(id)
            _selectedNote.value = note
            if (note != null && !note.audioFilePath.isNullOrBlank()) {
                audioPlayer.loadAudio(note.audioFilePath)
            } else {
                audioPlayer.release()
            }
        }
    }

    fun createTextNote(userTitle: String, body: String, onCreated: (Long) -> Unit) {
        if (body.isBlank() && userTitle.isBlank()) return
        viewModelScope.launch {
            val customVocab = noteRepo.getVocabularyTermsDirect()
            val finalTitle: String
            val isAutoTitle: Boolean
            val confidence: Float
            val keywords: String

            if (userTitle.isNotBlank()) {
                finalTitle = userTitle.trim()
                isAutoTitle = false
                confidence = 1.0f
                val suggestion = CaptureTitleSuggester.suggest(body, customVocab)
                keywords = suggestion.keywords.joinToString(",")
            } else {
                val suggestion = CaptureTitleSuggester.suggest(body, customVocab)
                finalTitle = suggestion.title
                isAutoTitle = true
                confidence = suggestion.confidence
                keywords = suggestion.keywords.joinToString(",")
            }

            val wordCount = body.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
            val note = NoteEntity(
                title = finalTitle,
                body = body.trim(),
                sourceType = NoteSourceType.TEXT.name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                durationMs = 0L,
                wordCount = wordCount,
                titleWasAutoGenerated = isAutoTitle,
                titleConfidence = confidence,
                suggestedKeywords = keywords
            )

            val newId = noteRepo.saveNote(note)
            selectNote(newId)
            _toastMessage.emit("Note saved")
            onCreated(newId)
        }
    }

    fun updateNoteTitle(id: Long, newTitle: String) {
        viewModelScope.launch {
            noteRepo.updateNoteTitle(id, newTitle)
            val updated = noteRepo.getNoteByIdDirect(id)
            _selectedNote.value = updated
        }
    }

    fun updateNoteBody(id: Long, newBody: String) {
        viewModelScope.launch {
            noteRepo.updateNoteBody(id, newBody)
            val updated = noteRepo.getNoteByIdDirect(id)
            _selectedNote.value = updated
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            val newPinState = !note.isPinned
            noteRepo.togglePin(note.id, note.isPinned)
            val updated = noteRepo.getNoteByIdDirect(note.id)
            _selectedNote.value = updated
            _toastMessage.emit(if (newPinState) "Note pinned to top" else "Note unpinned")
        }
    }

    fun toggleArchive(note: NoteEntity) {
        viewModelScope.launch {
            val newArchiveState = !note.isArchived
            noteRepo.toggleArchive(note.id, note.isArchived)
            val updated = noteRepo.getNoteByIdDirect(note.id)
            _selectedNote.value = updated
            _toastMessage.emit(if (newArchiveState) "Note moved to archive" else "Note restored from archive")
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            noteRepo.deleteNote(id)
            if (_selectedNote.value?.id == id) {
                _selectedNote.value = null
                audioPlayer.release()
            }
            _toastMessage.emit("Note deleted")
        }
    }

    fun addVocabulary(term: String, category: String = "General") {
        if (term.isBlank()) return
        viewModelScope.launch {
            noteRepo.addVocabularyTerm(term, category)
            _toastMessage.emit("Added \"$term\" to vocabulary")
        }
    }

    fun deleteVocabulary(id: Long) {
        viewModelScope.launch {
            noteRepo.deleteVocabularyTerm(id)
        }
    }

    fun addPresetVocabulary(terms: List<Pair<String, String>>) {
        viewModelScope.launch {
            for ((term, cat) in terms) {
                noteRepo.addVocabularyTerm(term, cat)
            }
            _toastMessage.emit("Preset terms added to vocabulary")
        }
    }

    // --- Audio Recording & Transcription Capture Flow ---

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

        transcribeAudioFile(
            audioFile = file,
            mimeType = "audio/mp4",
            fileName = file.name,
            durationMs = elapsed,
            sourceType = NoteSourceType.VOICE
        )
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
                    durationMs = imported.durationMs,
                    sourceType = NoteSourceType.IMPORTED_AUDIO
                )
            }.onFailure { error ->
                _transcriptionState.value = TranscriptionUiState.Error(
                    "Failed to import audio: ${error.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun retranscribeNote(note: NoteEntity) {
        val path = note.audioFilePath
        if (path.isNullOrBlank()) {
            viewModelScope.launch {
                _toastMessage.emit("This note does not have an audio recording.")
            }
            return
        }

        val file = File(path)
        if (!file.exists()) {
            viewModelScope.launch {
                _toastMessage.emit("Original audio file is no longer available.")
            }
            return
        }

        val mimeType = if (file.name.endsWith(".mp3")) "audio/mp3" else "audio/mp4"
        transcribeAudioFile(
            audioFile = file,
            mimeType = mimeType,
            fileName = note.originalFileName ?: file.name,
            durationMs = note.durationMs,
            sourceType = note.getSourceTypeEnum(),
            existingNote = note
        )
    }

    private fun transcribeAudioFile(
        audioFile: File,
        mimeType: String,
        fileName: String,
        durationMs: Long,
        sourceType: NoteSourceType,
        existingNote: NoteEntity? = null
    ) {
        viewModelScope.launch {
            _transcriptionState.value = TranscriptionUiState.Processing(
                providerName = "Gemini",
                statusMessage = "Starting transcription...",
                fileName = fileName,
                durationMs = durationMs
            )

            val customVocab = noteRepo.getVocabularyTermsDirect()
            val options = TranscriptionOptions(
                accuracyMode = accuracyMode.value,
                customVocabulary = customVocab
            )

            try {
                // RUN FROZEN TRANSCRIPTION ROUTER
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

                // RUN LOCAL CAPTURE TITLE SUGGESTER (Deterministic, fast, zero API quota)
                val suggestion = CaptureTitleSuggester.suggest(result.text, customVocab)
                val wordCount = result.text.split("\\s+".toRegex()).count { it.isNotBlank() }

                val noteId: Long
                if (existingNote != null) {
                    // If user manually edited the title previously, preserve their manual title!
                    val finalTitle = if (existingNote.titleWasAutoGenerated) suggestion.title else existingNote.title
                    val isAutoTitle = existingNote.titleWasAutoGenerated

                    val updated = existingNote.copy(
                        title = finalTitle,
                        body = result.text,
                        provider = result.providerName,
                        model = result.modelName,
                        detectedLanguages = result.detectedLanguages,
                        processingTimeMs = result.processingTimeMs,
                        wordCount = wordCount,
                        titleWasAutoGenerated = isAutoTitle,
                        titleConfidence = suggestion.confidence,
                        suggestedKeywords = suggestion.keywords.joinToString(","),
                        updatedAt = System.currentTimeMillis()
                    )
                    noteRepo.updateNote(updated)
                    noteId = existingNote.id
                } else {
                    val newNote = NoteEntity(
                        title = suggestion.title,
                        body = result.text,
                        sourceType = sourceType.name,
                        audioFilePath = audioFile.absolutePath,
                        originalFileName = fileName,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        durationMs = durationMs,
                        provider = result.providerName,
                        model = result.modelName,
                        detectedLanguages = result.detectedLanguages,
                        processingTimeMs = result.processingTimeMs,
                        wordCount = wordCount,
                        isPinned = false,
                        isArchived = false,
                        isFavorite = false,
                        titleWasAutoGenerated = true,
                        titleConfidence = suggestion.confidence,
                        suggestedKeywords = suggestion.keywords.joinToString(",")
                    )
                    noteId = noteRepo.saveNote(newNote)
                }

                refreshUsageStats()
                selectNote(noteId)
                _transcriptionState.value = TranscriptionUiState.Success(noteId, result)
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Transcription failed. Please check your network and API keys."
                _transcriptionState.value = TranscriptionUiState.Error(errorMsg, isRetryable = true)
            }
        }
    }

    fun dismissTranscriptionDialog() {
        _transcriptionState.value = TranscriptionUiState.Idle
    }

    fun getExportText(note: NoteEntity, format: ExportFormat): String {
        return noteRepo.formatExportContent(note, format)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
