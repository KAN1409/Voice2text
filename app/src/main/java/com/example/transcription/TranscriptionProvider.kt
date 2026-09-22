package com.example.transcription

import com.example.domain.model.TranscriptionOptions
import com.example.domain.model.TranscriptionResult
import java.io.File

sealed class TranscriptionException(message: String, val isRetryable: Boolean = false) : Exception(message) {
    class RateLimitException(message: String = "Rate limit reached") : TranscriptionException(message, isRetryable = true)
    class QuotaExceededException(message: String = "Quota exceeded") : TranscriptionException(message, isRetryable = true)
    class ServiceUnavailableException(message: String = "Service temporarily unavailable") : TranscriptionException(message, isRetryable = true)
    class NetworkException(message: String = "Connection interrupted") : TranscriptionException(message, isRetryable = true)
    class MissingKeyException(message: String = "API key is missing") : TranscriptionException(message, isRetryable = false)
    class InvalidAudioException(message: String = "Selected audio file could not be read") : TranscriptionException(message, isRetryable = false)
    class ProviderException(message: String) : TranscriptionException(message, isRetryable = false)
}

interface TranscriptionProvider {
    val name: String
    val isConfigured: Boolean
    suspend fun transcribe(
        audioFile: File,
        mimeType: String,
        options: TranscriptionOptions,
        onStatusUpdate: (String) -> Unit
    ): TranscriptionResult

    suspend fun testConnection(): Pair<Boolean, String>
}
