package com.example.transcription

import android.util.Log
import com.example.data.repository.SettingsRepository
import com.example.domain.model.ProviderHealth
import com.example.domain.model.ProviderStatus
import com.example.domain.model.TranscriptionOptions
import com.example.domain.model.TranscriptionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class TranscriptionRouter(
    private val geminiProvider: GeminiProvider,
    private val groqProvider: GroqProvider,
    private val settingsRepository: SettingsRepository
) {
    private val _providerHealthMap = MutableStateFlow<Map<String, ProviderHealth>>(emptyMap())
    val providerHealthMap: StateFlow<Map<String, ProviderHealth>> = _providerHealthMap.asStateFlow()

    private val rateLimitCooldownMs = 2 * 60 * 1000L // 2 minutes cooldown for rate-limited providers
    private val rateLimitTimestamps = mutableMapOf<String, Long>()

    init {
        updateHealthStatuses()
    }

    fun updateHealthStatuses() {
        val map = mutableMapOf<String, ProviderHealth>()

        val geminiKeyConfigured = geminiProvider.isConfigured
        val geminiRateLimited = isRateLimited(geminiProvider.name)
        map[geminiProvider.name] = ProviderHealth(
            providerName = geminiProvider.name,
            status = when {
                !geminiKeyConfigured -> ProviderStatus.MISSING_KEY
                geminiRateLimited -> ProviderStatus.RATE_LIMITED
                else -> ProviderStatus.AVAILABLE
            }
        )

        val groqKeyConfigured = groqProvider.isConfigured
        val groqRateLimited = isRateLimited(groqProvider.name)
        map[groqProvider.name] = ProviderHealth(
            providerName = groqProvider.name,
            status = when {
                !groqKeyConfigured -> ProviderStatus.MISSING_KEY
                groqRateLimited -> ProviderStatus.RATE_LIMITED
                else -> ProviderStatus.AVAILABLE
            }
        )

        _providerHealthMap.value = map
    }

    private fun isRateLimited(providerName: String): Boolean {
        val lastRateLimit = rateLimitTimestamps[providerName] ?: return false
        val elapsed = System.currentTimeMillis() - lastRateLimit
        return elapsed < rateLimitCooldownMs
    }

    private fun markRateLimited(providerName: String) {
        rateLimitTimestamps[providerName] = System.currentTimeMillis()
        updateHealthStatuses()
    }

    suspend fun transcribeAudio(
        audioFile: File,
        mimeType: String,
        options: TranscriptionOptions,
        durationMs: Long,
        onProgress: (String) -> Unit
    ): TranscriptionResult {
        updateHealthStatuses()

        val providers = determineProviderOrder()

        if (providers.isEmpty()) {
            throw TranscriptionException.MissingKeyException(
                "No transcription providers configured. Please set your Gemini or Groq API key in Settings."
            )
        }

        var lastException: Exception? = null

        for (i in providers.indices) {
            val provider = providers[i]
            val isPrimary = (i == 0)

            try {
                onProgress("Connecting to ${provider.name}...")
                val result = provider.transcribe(audioFile, mimeType, options, onProgress)

                // Record usage for today
                settingsRepository.recordUsage(provider.name, durationMs)

                // Success! Mark available
                rateLimitTimestamps.remove(provider.name)
                updateHealthStatuses()

                return if (!isPrimary) {
                    result.copy(
                        isFallback = true,
                        fallbackReason = "Switched to ${provider.name} due to primary provider limitation.",
                        durationMs = durationMs
                    )
                } else {
                    result.copy(durationMs = durationMs)
                }
            } catch (e: TranscriptionException.RateLimitException) {
                Log.w("TranscriptionRouter", "${provider.name} rate limited: ${e.message}")
                markRateLimited(provider.name)
                lastException = e
                if (i < providers.size - 1) {
                    val nextProvider = providers[i + 1]
                    onProgress("${provider.name} rate limit reached.\nSwitching to ${nextProvider.name}...")
                }
            } catch (e: TranscriptionException.QuotaExceededException) {
                Log.w("TranscriptionRouter", "${provider.name} quota exceeded: ${e.message}")
                markRateLimited(provider.name)
                lastException = e
                if (i < providers.size - 1) {
                    val nextProvider = providers[i + 1]
                    onProgress("${provider.name} quota exceeded.\nSwitching to ${nextProvider.name}...")
                }
            } catch (e: TranscriptionException.ServiceUnavailableException) {
                Log.w("TranscriptionRouter", "${provider.name} unavailable: ${e.message}")
                lastException = e
                if (i < providers.size - 1) {
                    val nextProvider = providers[i + 1]
                    onProgress("${provider.name} temporarily unavailable.\nSwitching to ${nextProvider.name}...")
                }
            } catch (e: TranscriptionException.NetworkException) {
                Log.w("TranscriptionRouter", "${provider.name} network error: ${e.message}")
                lastException = e
                if (i < providers.size - 1) {
                    val nextProvider = providers[i + 1]
                    onProgress("Network issue with ${provider.name}.\nTrying ${nextProvider.name}...")
                }
            } catch (e: TranscriptionException.InvalidAudioException) {
                // Non-retryable
                throw e
            } catch (e: TranscriptionException.MissingKeyException) {
                lastException = e
                if (i < providers.size - 1) {
                    val nextProvider = providers[i + 1]
                    onProgress("${provider.name} key missing. Trying ${nextProvider.name}...")
                }
            } catch (e: Exception) {
                Log.e("TranscriptionRouter", "Unexpected error on ${provider.name}: ${e.message}")
                lastException = e
                if (i < providers.size - 1) {
                    val nextProvider = providers[i + 1]
                    onProgress("Switching to ${nextProvider.name}...")
                }
            }
        }

        throw lastException ?: TranscriptionException.ProviderException("Transcription failed with all available providers.")
    }

    private fun determineProviderOrder(): List<TranscriptionProvider> {
        val configured = mutableListOf<TranscriptionProvider>()

        val groqConfigured = groqProvider.isConfigured
        val geminiConfigured = geminiProvider.isConfigured

        val groqRateLimited = isRateLimited(groqProvider.name)
        val geminiRateLimited = isRateLimited(geminiProvider.name)

        // IMPORTANT: Groq Whisper is the verified verbatim transcription path.
        // Keep it primary whenever it is configured and not temporarily rate-limited.
        // Gemini is fallback only. Do not change provider prompts/models here.
        if (groqConfigured && !groqRateLimited) {
            configured.add(groqProvider)
        }

        if (geminiConfigured && !geminiRateLimited) {
            configured.add(geminiProvider)
        }

        // If every configured provider is temporarily rate-limited, preserve the same
        // preference order as a last resort: Groq first, then Gemini.
        if (configured.isEmpty()) {
            if (groqConfigured) configured.add(groqProvider)
            if (geminiConfigured) configured.add(geminiProvider)
        }

        return configured
    }
}
