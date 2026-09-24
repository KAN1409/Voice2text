package com.example.transcription

import android.util.Log
import com.example.data.security.SecureKeyStorage
import com.example.domain.model.AccuracyMode
import com.example.domain.model.TranscriptionOptions
import com.example.domain.model.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GroqProvider(
    private val keyStorage: SecureKeyStorage
) : TranscriptionProvider {

    override val name: String = "Groq Whisper"

    override val isConfigured: Boolean
        get() = keyStorage.getGroqKey().isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun transcribe(
        audioFile: File,
        mimeType: String,
        options: TranscriptionOptions,
        onStatusUpdate: (String) -> Unit
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        val apiKey = keyStorage.getGroqKey()
        if (apiKey.isBlank()) {
            throw TranscriptionException.MissingKeyException("Groq API key is missing. Configure it in Settings.")
        }

        if (!audioFile.exists() || audioFile.length() == 0L) {
            throw TranscriptionException.InvalidAudioException("Audio file is empty or does not exist.")
        }

        val modelName = if (options.accuracyMode == AccuracyMode.FAST) {
            "whisper-large-v3-turbo"
        } else {
            "whisper-large-v3"
        }

        onStatusUpdate("Preparing audio for Groq $modelName...")
        val startTime = System.currentTimeMillis()

        val cleanMimeType = when {
            audioFile.name.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
            audioFile.name.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
            audioFile.name.endsWith(".wav", ignoreCase = true) -> "audio/wav"
            audioFile.name.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
            audioFile.name.endsWith(".flac", ignoreCase = true) -> "audio/flac"
            audioFile.name.endsWith(".webm", ignoreCase = true) -> "audio/webm"
            else -> "audio/mp4"
        }

        val fileRequestBody = audioFile.asRequestBody(cleanMimeType.toMediaType())

        val promptHint = StringBuilder("محادثة باللهجة المصرية مع English code-switching. Terms: quotation, contractor, meeting, project, invoice, shop drawing, approval. ")
        if (options.customVocabulary.isNotEmpty()) {
            promptHint.append("Keywords: ").append(options.customVocabulary.joinToString(", "))
        }

        val multipartBodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name, fileRequestBody)
            .addFormDataPart("model", modelName)
            // Arabic is the base language; Whisper still preserves embedded English terms.
            // Explicit language prevents short Egyptian Arabic/English clips being misdetected as English and translated/romanized.
            .addFormDataPart("language", "ar")
            .addFormDataPart("prompt", promptHint.toString())
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("temperature", "0.0")
            // This endpoint is /audio/transcriptions, never /audio/translations.

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/audio/transcriptions")
            .header("Authorization", "Bearer $apiKey")
            .post(multipartBodyBuilder.build())
            .build()

        onStatusUpdate("Transcribing with Groq $modelName...")

        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            Log.e("GroqProvider", "Network call failed: ${e.message}")
            throw TranscriptionException.NetworkException("Connection to Groq failed: ${e.localizedMessage}")
        }

        val responseCode = response.code
        val responseBodyString = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            Log.e("GroqProvider", "HTTP $responseCode: $responseBodyString")
            when (responseCode) {
                429 -> throw TranscriptionException.RateLimitException("Groq daily / TPM rate limit reached (HTTP 429).")
                401, 403 -> throw TranscriptionException.MissingKeyException("Invalid Groq API key or unauthorized.")
                500, 502, 503, 504 -> throw TranscriptionException.ServiceUnavailableException("Groq service unavailable (HTTP $responseCode).")
                else -> throw TranscriptionException.ProviderException("Groq error (HTTP $responseCode): $responseBodyString")
            }
        }

        try {
            val json = JSONObject(responseBodyString)
            val rawText = json.optString("text", "").trim()
            val detectedLanguage = json.optString("language", "ar / en")

            val cleanedText = HallucinationDetector.sanitizeTranscript(rawText)
            Log.i(\n                "GroqProvider",\n                "ASR diagnostics: segments=${segments?.length() ?: 0}, " +\n                    "lowConfidence=$lowConfidenceSegments, suspicious=$suspiciousSegments"\n            )\n            val segments = json.optJSONArray("segments")\n            var lowConfidence = 0\n            var suspicious = 0\n            if (segments != null) {\n                for (i in 0 until segments.length()) {\n                    val segment = segments.optJSONObject(i) ?: continue\n                    val logProb = segment.optDouble("avg_logprob", Double.NaN)\n                    val noSpeech = segment.optDouble("no_speech_prob", Double.NaN)\n                    val compression = segment.optDouble("compression_ratio", Double.NaN)\n                    if ((!logProb.isNaN() && logProb < -1.0) || (!noSpeech.isNaN() && noSpeech > 0.6) || (!compression.isNaN() && compression > 2.4)) lowConfidence++\n                    if (HallucinationDetector.analyze(segment.optString("text", "")).isSuspicious) suspicious++\n                }\n            }\n            Log.i("GroqProvider", "ASR diagnostics segments=${segments?.length() ?: 0} lowConfidence=$lowConfidence suspicious=$suspicious")\n            val processingTimeMs = System.currentTimeMillis() - startTime

            TranscriptionResult(
                text = rawText,
                providerName = name,
                modelName = modelName,
                detectedLanguages = detectedLanguage,
                durationMs = 0L,
                processingTimeMs = processingTimeMs,
                isFallback = false
            )
        } catch (e: Exception) {
            if (e is TranscriptionException) throw e
            throw TranscriptionException.ProviderException("Failed to parse Groq response: ${e.localizedMessage}")
        }
    }

    override suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = keyStorage.getGroqKey()
        if (apiKey.isBlank()) return@withContext false to "API Key not set"

        try {
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                true to "Connected (Groq API)"
            } else {
                false to "HTTP ${response.code}: ${response.message}"
            }
        } catch (e: Exception) {
            false to (e.message ?: "Connection error")
        }
    }
}
