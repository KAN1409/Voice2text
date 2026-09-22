package com.example.transcription

import android.util.Base64
import android.util.Log
import com.example.data.security.SecureKeyStorage
import com.example.domain.model.TranscriptionOptions
import com.example.domain.model.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class GeminiProvider(
    private val keyStorage: SecureKeyStorage
) : TranscriptionProvider {

    override val name: String = "Gemini"

    override val isConfigured: Boolean
        get() = keyStorage.getGeminiKey().isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val modelName = "gemini-2.5-flash"

    override suspend fun transcribe(
        audioFile: File,
        mimeType: String,
        options: TranscriptionOptions,
        onStatusUpdate: (String) -> Unit
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        val apiKey = keyStorage.getGeminiKey()
        if (apiKey.isBlank()) {
            throw TranscriptionException.MissingKeyException("Gemini API key is missing. Configure it in Settings.")
        }

        if (!audioFile.exists() || audioFile.length() == 0L) {
            throw TranscriptionException.InvalidAudioException("Audio file is empty or does not exist.")
        }

        onStatusUpdate("Preparing audio for Gemini...")
        val startTime = System.currentTimeMillis()

        // Read audio bytes into Base64
        val audioBytes = audioFile.readBytes()
        val base64Data = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

        val cleanMimeType = when {
            mimeType.contains("mp4", ignoreCase = true) || audioFile.name.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
            mimeType.contains("mpeg", ignoreCase = true) || audioFile.name.endsWith(".mp3", ignoreCase = true) -> "audio/mp3"
            mimeType.contains("wav", ignoreCase = true) || audioFile.name.endsWith(".wav", ignoreCase = true) -> "audio/wav"
            mimeType.contains("ogg", ignoreCase = true) || audioFile.name.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
            mimeType.contains("aac", ignoreCase = true) || audioFile.name.endsWith(".aac", ignoreCase = true) -> "audio/aac"
            mimeType.contains("flac", ignoreCase = true) || audioFile.name.endsWith(".flac", ignoreCase = true) -> "audio/flac"
            else -> "audio/mp4"
        }

        val vocabPrompt = if (options.customVocabulary.isNotEmpty()) {
            "\nRecognize these industry / project terms and names accurately if spoken: " + options.customVocabulary.joinToString(", ")
        } else ""

        val systemPrompt = """
            You are a professional speech transcription engine.
            Transcribe the audio faithfully and verbatim.
            The speaker primarily uses Egyptian Arabic and English and may switch languages inside the same sentence.
            Preserve Arabic speech in Egyptian Arabic script without standardizing or normalizing into MSA.
            Preserve English words in English.
            Do not translate.
            Do not summarize.
            Do not paraphrase.
            Do not invent missing words.
            Preserve numbers, dates, times, dimensions, prices, names, technical terms, product names, abbreviations, and English expressions accurately.
            If a word is genuinely unclear, mark it conservatively rather than hallucinating an entire phrase.$vocabPrompt
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", cleanMimeType)
                                put("data", base64Data)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", "Please provide the exact, verbatim transcription of this audio. Do not add conversational commentary, introductory text, or markdown code blocks.")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("topP", 0.95)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        onStatusUpdate("Transcribing with Gemini...")

        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            Log.e("GeminiProvider", "Network call failed: ${e.message}")
            throw TranscriptionException.NetworkException("Connection to Gemini failed: ${e.localizedMessage}")
        }

        val responseCode = response.code
        val responseBodyString = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            Log.e("GeminiProvider", "HTTP $responseCode: $responseBodyString")
            when (responseCode) {
                429 -> throw TranscriptionException.RateLimitException("Gemini rate limit / quota reached (HTTP 429).")
                403 -> {
                    if (responseBodyString.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                        responseBodyString.contains("quota", ignoreCase = true)
                    ) {
                        throw TranscriptionException.QuotaExceededException("Gemini quota exceeded.")
                    } else {
                        throw TranscriptionException.MissingKeyException("Invalid Gemini API key or permission denied.")
                    }
                }
                500, 502, 503, 504 -> throw TranscriptionException.ServiceUnavailableException("Gemini server error (HTTP $responseCode).")
                else -> throw TranscriptionException.ProviderException("Gemini error (HTTP $responseCode)")
            }
        }

        try {
            val json = JSONObject(responseBodyString)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw TranscriptionException.ProviderException("No transcript returned by Gemini.")
            }
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textBuilder = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                textBuilder.append(part.optString("text", ""))
            }

            var transcriptText = textBuilder.toString().trim()
            transcriptText = HallucinationDetector.sanitizeTranscript(transcriptText)

            val processingTimeMs = System.currentTimeMillis() - startTime

            TranscriptionResult(
                text = transcriptText,
                providerName = name,
                modelName = modelName,
                detectedLanguages = "Egyptian Arabic + English",
                durationMs = 0L,
                processingTimeMs = processingTimeMs,
                isFallback = false
            )
        } catch (e: Exception) {
            if (e is TranscriptionException) throw e
            throw TranscriptionException.ProviderException("Failed to parse Gemini response: ${e.localizedMessage}")
        }
    }

    override suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = keyStorage.getGeminiKey()
        if (apiKey.isBlank()) return@withContext false to "API Key not set"

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName?key=$apiKey"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                true to "Connected (gemini-2.5-flash)"
            } else {
                false to "HTTP ${response.code}: ${response.message}"
            }
        } catch (e: Exception) {
            false to (e.message ?: "Connection error")
        }
    }
}
