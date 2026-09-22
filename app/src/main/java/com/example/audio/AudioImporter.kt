package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class AudioImportResult(
    val file: File,
    val originalFileName: String,
    val mimeType: String,
    val durationMs: Long,
    val fileSize: Long
)

class AudioImporter(private val context: Context) {

    suspend fun importAudioFromUri(uri: Uri): Result<AudioImportResult> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val originalFileName = getFileNameFromUri(uri) ?: "imported_audio_${System.currentTimeMillis()}.m4a"
            val extension = getExtension(originalFileName)

            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromExtension(extension)

            val cacheDir = File(context.cacheDir, "imported_audio").apply { mkdirs() }
            val tempFile = File(cacheDir, "imported_${System.currentTimeMillis()}.$extension")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Could not open input stream for audio file"))

            val durationMs = extractDurationMs(tempFile)

            Result.success(
                AudioImportResult(
                    file = tempFile,
                    originalFileName = originalFileName,
                    mimeType = mimeType,
                    durationMs = durationMs,
                    fileSize = tempFile.length()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback
            }
        }
        return uri.lastPathSegment
    }

    private fun getExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1).lowercase()
        } else {
            "m4a"
        }
    }

    private fun getMimeTypeFromExtension(extension: String): String {
        return when (extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "wav" -> "audio/wav"
            "ogg", "opus" -> "audio/ogg"
            "flac" -> "audio/flac"
            "webm" -> "audio/webm"
            "mp4" -> "audio/mp4"
            else -> "audio/m4a"
        }
    }

    fun extractDurationMs(file: File): Long {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
