package com.example.export

import android.content.Context
import androidx.core.content.FileProvider
import com.example.data.local.entity.NoteEntity
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object NoteExportManager {
    private fun safeName(value: String): String =
        value.replace(Regex("[^\\p{L}\\p{N}._ -]"), "_").trim().ifBlank { "voice_note" }

    fun originalAudio(note: NoteEntity): File? {
        val path = note.audioFilePath ?: return null
        return File(path).takeIf { it.exists() }
    }

    fun createArchive(context: Context, note: NoteEntity): File? {
        val audio = originalAudio(note) ?: return null
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val base = safeName(note.title)
        val zip = File(dir, "$base.zip")
        ZipOutputStream(FileOutputStream(zip)).use { out ->
            out.putNextEntry(ZipEntry("transcription.txt"))
            out.write(note.body.toByteArray(Charsets.UTF_8))
            out.closeEntry()

            val ext = audio.extension.ifBlank { "m4a" }
            out.putNextEntry(ZipEntry("audio.$ext"))
            audio.inputStream().use { it.copyTo(out) }
            out.closeEntry()
        }
        return zip
    }

    fun uri(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
