package com.example.domain.model

enum class AccuracyMode(val displayName: String, val description: String) {
    MAXIMUM_ACCURACY("Maximum Accuracy", "Gemini dedicated transcription, fallback to Groq Whisper Large V3"),
    BALANCED("Balanced", "Fast and accurate code-switching transcription"),
    FAST("Fast", "Optimized for quick turnaround using Whisper Turbo")
}

enum class ProviderStatus {
    AVAILABLE,
    RATE_LIMITED,
    UNAVAILABLE,
    MISSING_KEY
}

data class TranscriptionOptions(
    val accuracyMode: AccuracyMode = AccuracyMode.MAXIMUM_ACCURACY,
    val preferredLanguageHint: String = "ar-EG",
    val customVocabulary: List<String> = emptyList(),
    val preserveDialect: Boolean = true,
    val codeSwitching: Boolean = true
)

data class TranscriptionResult(
    val text: String,
    val providerName: String,
    val modelName: String,
    val detectedLanguages: String,
    val durationMs: Long,
    val processingTimeMs: Long,
    val isFallback: Boolean = false,
    val fallbackReason: String? = null,
    val warnings: List<String> = emptyList()
)

data class ProviderHealth(
    val providerName: String,
    val status: ProviderStatus,
    val message: String? = null,
    val lastChecked: Long = System.currentTimeMillis()
)
