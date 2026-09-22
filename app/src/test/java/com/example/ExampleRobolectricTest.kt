package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.TranscriptionEntity
import com.example.data.repository.ExportFormat
import com.example.transcription.HallucinationDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VoiceTranscriber", appName)
    }

    @Test
    fun `test export formatting for SRT and Markdown`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.local.AppDatabase.getDatabase(context)
        val repo = com.example.data.repository.TranscriptionRepository(db.transcriptionDao(), db.vocabularyDao())

        val sample = TranscriptionEntity(
            id = 1,
            title = "Site Meeting Discussion",
            transcript = "تمت مراجعة الـ BOQ والـ variation order مع الكلاينت.",
            audioFilePath = "/path/to/rec.m4a",
            originalFileName = "rec.m4a",
            createdAt = 1726992000000L,
            durationMs = 65000L,
            provider = "Gemini",
            model = "gemini-2.5-flash",
            detectedLanguages = "ar-EG + en",
            processingTimeMs = 1200L,
            wordCount = 9
        )

        val srtOutput = repo.formatExportContent(sample, ExportFormat.SRT)
        assertTrue(srtOutput.contains("00:00:00,000 --> 00:01:05,000"))
        assertTrue(srtOutput.contains("BOQ"))
        assertTrue(srtOutput.contains("variation order"))

        val mdOutput = repo.formatExportContent(sample, ExportFormat.MARKDOWN)
        assertTrue(mdOutput.contains("# Site Meeting Discussion"))
        assertTrue(mdOutput.contains("Gemini"))
    }
}
