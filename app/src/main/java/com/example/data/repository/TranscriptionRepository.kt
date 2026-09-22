package com.example.data.repository

import com.example.data.local.dao.TranscriptionDao
import com.example.data.local.dao.VocabularyDao
import com.example.data.local.entity.TranscriptionEntity
import com.example.data.local.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val extension: String, val mimeType: String) {
    TXT("txt", "text/plain"),
    MARKDOWN("md", "text/markdown"),
    SRT("srt", "text/plain"),
    VTT("vtt", "text/vtt")
}

class TranscriptionRepository(
    private val transcriptionDao: TranscriptionDao,
    private val vocabularyDao: VocabularyDao
) {
    val allTranscriptions: Flow<List<TranscriptionEntity>> = transcriptionDao.getAllTranscriptions()
    val allVocabulary: Flow<List<VocabularyEntity>> = vocabularyDao.getAllVocabulary()

    fun getRecentTranscriptions(limit: Int = 5): Flow<List<TranscriptionEntity>> =
        transcriptionDao.getRecentTranscriptions(limit)

    fun getTranscriptionById(id: Long): Flow<TranscriptionEntity?> =
        transcriptionDao.getTranscriptionById(id)

    suspend fun getTranscriptionByIdDirect(id: Long): TranscriptionEntity? =
        transcriptionDao.getTranscriptionByIdDirect(id)

    fun searchTranscriptions(query: String): Flow<List<TranscriptionEntity>> =
        transcriptionDao.searchTranscriptions(query)

    suspend fun saveTranscription(item: TranscriptionEntity): Long =
        transcriptionDao.insertTranscription(item)

    suspend fun updateTranscription(item: TranscriptionEntity) =
        transcriptionDao.updateTranscription(item)

    suspend fun deleteTranscription(id: Long) {
        val item = transcriptionDao.getTranscriptionByIdDirect(id)
        if (item != null && item.audioFilePath.isNotBlank()) {
            try {
                val file = File(item.audioFilePath)
                if (file.exists() && file.absolutePath.contains("recordings")) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        transcriptionDao.deleteTranscriptionById(id)
    }

    suspend fun deleteAllTranscriptions() = transcriptionDao.deleteAllTranscriptions()

    suspend fun getVocabularyTermsDirect(): List<String> = vocabularyDao.getAllTermsDirect()

    suspend fun addVocabularyTerm(term: String, category: String = "General"): Long =
        vocabularyDao.insertTerm(VocabularyEntity(term = term.trim(), category = category))

    suspend fun deleteVocabularyTerm(id: Long) = vocabularyDao.deleteTermById(id)

    fun generateSmartTitle(transcript: String): String {
        val trimmed = transcript.trim()
        if (trimmed.isBlank()) return "Voice Note"

        // First clean lines and get the first sentence or chunk
        val firstLine = trimmed.lines().firstOrNull { it.isNotBlank() } ?: trimmed
        val sentences = firstLine.split(Regex("[.!?،\n]")).filter { it.isNotBlank() }
        val candidate = sentences.firstOrNull()?.trim() ?: firstLine

        // Limit to 6-8 words or 45 chars
        val words = candidate.split("\\s+".toRegex())
        val titleCandidate = if (words.size > 7) {
            words.take(7).joinToString(" ") + "..."
        } else {
            candidate
        }

        return if (titleCandidate.length > 50) {
            titleCandidate.take(47) + "..."
        } else {
            titleCandidate
        }
    }

    fun formatExportContent(item: TranscriptionEntity, format: ExportFormat): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(item.createdAt))
        val durationFormatted = formatDuration(item.durationMs)

        return when (format) {
            ExportFormat.TXT -> {
                """
                Title: ${item.title}
                Date: $dateStr
                Duration: $durationFormatted
                Provider: ${item.provider} (${item.model})
                Language: ${item.detectedLanguages}
                --------------------------------------------------

                ${item.transcript}
                """.trimIndent()
            }
            ExportFormat.MARKDOWN -> {
                """
                # ${item.title}

                - **Date**: $dateStr
                - **Duration**: $durationFormatted
                - **Provider**: ${item.provider} (`${item.model}`)
                - **Language**: ${item.detectedLanguages}

                ---

                ${item.transcript}
                """.trimIndent()
            }
            ExportFormat.SRT -> {
                // Generate standard subtitle block
                """
                1
                00:00:00,000 --> ${formatSrtTimestamp(item.durationMs)}
                ${item.transcript}
                """.trimIndent()
            }
            ExportFormat.VTT -> {
                """
                WEBVTT

                00:00:00.000 --> ${formatVttTimestamp(item.durationMs)}
                ${item.transcript}
                """.trimIndent()
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    private fun formatSrtTimestamp(ms: Long): String {
        val totalSecs = (ms / 1000).coerceAtLeast(1)
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, mins, secs, millis)
    }

    private fun formatVttTimestamp(ms: Long): String {
        val totalSecs = (ms / 1000).coerceAtLeast(1)
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, mins, secs, millis)
    }
}
