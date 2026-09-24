package com.example.transcription

/**
 * Diagnostic-only transcript anomaly analysis.
 *
 * This object must never rewrite ASR output. The recognizer's text is evidence and remains
 * immutable; callers may use [analyze] to decide whether another decoding pass/provider is
 * warranted.
 */
object HallucinationDetector {

    private val suspiciousPhrases = listOf(
        "اشترك في القناة",
        "لا تنسوا الاشتراك",
        "تفعيل زر الجرس",
        "شكرا لمتابعتكم",
        "شكرا على المشاهدة",
        "subtitles by",
        "transcribed by",
        "amara.org",
        "thank you for watching",
        "subscribe to",
        "please like and subscribe"
    )

    data class Analysis(
        val suspiciousPhraseHits: List<String>,
        val consecutiveRepeatCount: Int,
        val isSuspicious: Boolean
    )

    /**
     * Observe anomalies without changing a single character of the transcript.
     */
    fun analyze(rawText: String): Analysis {
        val lower = rawText.lowercase()
        val hits = suspiciousPhrases.filter { lower.contains(it) }
        val repeats = countConsecutiveRepeats(rawText)
        return Analysis(
            suspiciousPhraseHits = hits,
            consecutiveRepeatCount = repeats,
            isSuspicious = hits.isNotEmpty() || repeats >= 2
        )
    }

    /**
     * Compatibility shim for older callers. Intentionally returns the original text verbatim.
     */
    @Deprecated("ASR text is immutable. Use analyze(rawText) for diagnostics.")
    fun sanitizeTranscript(rawText: String, audioDurationMs: Long = 0): String = rawText

    private fun countConsecutiveRepeats(text: String): Int {
        val sentences = text.split(Regex("(?<=[.!?،\\n])\\s*"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        var previous: String? = null
        var run = 0
        var maxRun = 0
        for (sentence in sentences) {
            val normalized = sentence.lowercase()
            if (normalized == previous) {
                run++
                maxRun = maxOf(maxRun, run)
            } else {
                previous = normalized
                run = 0
            }
        }
        return maxRun
    }

    /**
     * Legacy overlap helper retained for chunk assembly only. It never changes words; it only
     * removes an exact duplicated boundary that is known to come from overlapping audio chunks.
     */
    fun mergeWithOverlap(firstChunk: String, secondChunk: String): String {
        val text1 = firstChunk.trim()
        val text2 = secondChunk.trim()
        if (text1.isEmpty()) return text2
        if (text2.isEmpty()) return text1

        val words1 = text1.split("\\s+".toRegex())
        val words2 = text2.split("\\s+".toRegex())
        val maxOverlapWords = minOf(words1.size, words2.size, 15)

        for (overlapLen in maxOverlapWords downTo 2) {
            val endOf1 = words1.takeLast(overlapLen).joinToString(" ").lowercase()
            val startOf2 = words2.take(overlapLen).joinToString(" ").lowercase()
            if (endOf1 == startOf2) {
                val remaining = words2.drop(overlapLen).joinToString(" ")
                return if (remaining.isBlank()) text1 else "$text1 $remaining"
            }
        }
        return "$text1 $text2"
    }
}
