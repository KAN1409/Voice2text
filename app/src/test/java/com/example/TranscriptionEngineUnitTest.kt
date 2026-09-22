package com.example

import com.example.data.local.entity.TranscriptionEntity
import com.example.data.repository.ExportFormat
import com.example.transcription.HallucinationDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionEngineUnitTest {

    @Test
    fun testHallucinationDetector_removesSubtitlesArtifacts() {
        val raw = "يا باشمهندس الـ quotation وصل من المقاول\nاشترك في القناة ليصلك كل جديد\nهنبدا الـ execution الأسبوع الجاي"
        val cleaned = HallucinationDetector.sanitizeTranscript(raw)

        assertFalse(cleaned.contains("اشترك في القناة"))
        assertTrue(cleaned.contains("quotation"))
        assertTrue(cleaned.contains("execution"))
    }

    @Test
    fun testHallucinationDetector_removesEnglishYoutubeArtifacts() {
        val raw = "Meeting tomorrow at 10 AM regarding the BOQ\nSubtitles by Amara.org\nThank you for watching"
        val cleaned = HallucinationDetector.sanitizeTranscript(raw)

        assertFalse(cleaned.contains("Amara.org"))
        assertFalse(cleaned.contains("Thank you for watching"))
        assertTrue(cleaned.contains("Meeting tomorrow at 10 AM regarding the BOQ"))
    }

    @Test
    fun testHallucinationDetector_removesRepetitiveLoops() {
        val loop = "تمام يا فندم. تمام يا فندم. تمام يا فندم. تمام يا فندم."
        val cleaned = HallucinationDetector.removeRepeatedPhrases(loop)

        val count = cleaned.split("تمام يا فندم").size - 1
        assertTrue("Repeated loops should be collapsed", count <= 2)
    }

    @Test
    fun testOverlapMerging() {
        val chunk1 = "المهندس استلم الـ shop drawings قبل meeting بكرة"
        val chunk2 = "meeting بكرة في الموقع مع الاستشاري"

        val merged = HallucinationDetector.mergeWithOverlap(chunk1, chunk2)
        assertEquals("المهندس استلم الـ shop drawings قبل meeting بكرة في الموقع مع الاستشاري", merged)
    }
}
