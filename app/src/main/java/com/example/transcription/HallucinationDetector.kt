package com.example.transcription

object HallucinationDetector {

    private val SUSPICIOUS_PHRASES = listOf(
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

    fun sanitizeTranscript(rawText: String, audioDurationMs: Long = 0): String {
        var text = rawText.trim()
        if (text.isBlank()) return ""

        // Check for common silence hallucination lines
        val lines = text.split("\n").filter { line ->
            val lower = line.trim().lowercase()
            !SUSPICIOUS_PHRASES.any { lower.contains(it) }
        }
        text = lines.joinToString("\n").trim()

        // Remove excessive repeated consecutive sentences
        text = removeRepeatedPhrases(text)

        return text
    }

    fun removeRepeatedPhrases(text: String): String {
        val sentences = text.split(Regex("(?<=[.!?،\n])\\s*")).filter { it.isNotBlank() }
        if (sentences.size <= 2) return text

        val deduplicated = mutableListOf<String>()
        var repeatCount = 0
        var lastSentence = ""

        for (sentence in sentences) {
            val normalized = sentence.trim().lowercase()
            if (normalized == lastSentence) {
                repeatCount++
                if (repeatCount < 2) {
                    deduplicated.add(sentence.trim())
                }
            } else {
                repeatCount = 0
                lastSentence = normalized
                deduplicated.add(sentence.trim())
            }
        }

        return deduplicated.joinToString(" ")
    }

    /**
     * Deduplicate overlap between two consecutive chunks.
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
                val remainingOf2 = words2.drop(overlapLen).joinToString(" ")
                return if (remainingOf2.isNotBlank()) "$text1 $remainingOf2" else text1
            }
        }

        return "$text1 $text2"
    }
}
