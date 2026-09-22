package com.example.notes.naming

import java.util.Locale

enum class TitleStrategy {
    VOCABULARY_AND_ACTION,
    KEYPHRASE,
    ACTION_OBJECT,
    SUBJECT_EXTRACT,
    FIRST_MEANINGFUL_PHRASE,
    FALLBACK
}

data class TitleSuggestion(
    val title: String,
    val confidence: Float,
    val keywords: List<String>,
    val strategy: TitleStrategy
)

object CaptureTitleSuggester {

    private val ARABIC_FILLERS = listOf(
        "بص يا باشا", "بص يا فندم", "بص يا سيدي", "يا باشمهندس", "يا باشا", "يا فندم", "يا سيدي",
        "على فكرة", "علي فكرة", "بقولك ايه", "بقولك إيه", "بقولك", "عايز أقول", "عايز اقول",
        "كنت بفكر", "كنت عايز", "أنا كنت عايز", "انا كنت عايز", "أنا عايز", "انا عايز",
        "أنا كنت", "انا كنت", "علشان بس", "عشان بس", "في حاجة", "والله",
        "يعني", "بص", "طيب", "أيوه", "ايوه", "آه", "اه", "امم", "مثلاً", "مثلا",
        "خلينا", "المهم", "شوف", "معلش", "طب", "فا", "علشان", "عشان", "كده", "كدة",
        "بس", "ممكن", "برضه", "برضو", "خلاص", "تقريباً", "تقريبا"
    )

    private val ENGLISH_FILLERS = listOf(
        "i was thinking", "i wanted to", "i need to", "i want to", "remember to",
        "make sure to", "don't forget to", "you know", "kind of", "sort of",
        "could you", "would you", "let's", "basically", "actually", "i think",
        "um", "uh", "like", "so", "well", "just", "please", "hey", "hello"
    )

    private val ARABIC_ACTION_MAP = mapOf(
        "أكلم" to "مكالمة",
        "اكلم" to "مكالمة",
        "اتصل" to "اتصال",
        "أشيك" to "فحص",
        "اشيك" to "فحص",
        "أراجع" to "مراجعة",
        "اراجع" to "مراجعة",
        "راجع" to "مراجعة",
        "مراجعة" to "مراجعة",
        "أشوف" to "متابعة",
        "اشوف" to "متابعة",
        "أبعت" to "إرسال",
        "ابعت" to "إرسال",
        "أشتري" to "شراء",
        "اشتري" to "شراء",
        "شراء" to "شراء",
        "فكرني" to "تذكير",
        "لازم" to "متابعة",
        "اجتماع" to "اجتماع",
        "ميتنج" to "اجتماع",
        "فكرة" to "فكرة",
        "أسعار" to "أسعار",
        "اسعار" to "أسعار",
        "عرض سعر" to "عرض سعر",
        "تأمين" to "تأمين",
        "تامين" to "تأمين"
    )

    private val COMMON_VOCAB_EN = listOf(
        "PR", "BOQ", "Variation Order", "RFI", "Shop Drawing", "Shop Drawings",
        "Mockup", "Gypsum Board", "Piatra", "Galala", "Palmariva", "Negma",
        "New Cairo", "Quotation", "Contractor", "Meeting", "Approval", "Client",
        "Invoice", "Down Payment", "Milestone", "Sheikh Zayed", "Indirect Lighting",
        "Ceiling", "Marble", "Tiles", "Inspection", "Submittal", "Consultant", "Delivery"
    )

    /**
     * Main entry point to suggest a clean, compact title (3-7 words, ~45 chars)
     * from any Arabic, English, or Code-Switching note.
     */
    fun suggest(
        text: String,
        customVocabulary: List<String> = emptyList()
    ): TitleSuggestion {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return TitleSuggestion(
                title = "New Note",
                confidence = 0.5f,
                keywords = emptyList(),
                strategy = TitleStrategy.FALLBACK
            )
        }

        // Bounded analysis window
        val sample = if (trimmed.length > 2500) trimmed.substring(0, 2500) else trimmed

        // Extract potential custom vocabulary matches
        val allVocab = (customVocabulary + COMMON_VOCAB_EN).distinct()
        val matchedVocab = mutableListOf<String>()
        for (vocab in allVocab) {
            if (sample.contains(vocab, ignoreCase = true)) {
                matchedVocab.add(vocab)
            }
        }

        // Detect keywords from text
        val keywords = extractKeywords(sample, matchedVocab)

        // Try Strategy 1: Action + Subject / Vocabulary match
        val actionResult = tryExtractActionAndSubject(sample, matchedVocab)
        if (actionResult != null && actionResult.length in 4..60) {
            return TitleSuggestion(
                title = sanitizeFinalTitle(actionResult),
                confidence = 0.90f,
                keywords = keywords,
                strategy = TitleStrategy.ACTION_OBJECT
            )
        }

        // Try Strategy 2: Strong Vocabulary Match with Context
        if (matchedVocab.isNotEmpty()) {
            val vocabTitle = buildVocabLeadingTitle(sample, matchedVocab)
            if (vocabTitle.isNotBlank() && vocabTitle.length in 4..60) {
                return TitleSuggestion(
                    title = sanitizeFinalTitle(vocabTitle),
                    confidence = 0.85f,
                    keywords = keywords,
                    strategy = TitleStrategy.VOCABULARY_AND_ACTION
                )
            }
        }

        // Try Strategy 3: Cleaned First Meaningful Clause
        val firstClauseTitle = extractCleanFirstClause(sample)
        if (firstClauseTitle.isNotBlank()) {
            return TitleSuggestion(
                title = sanitizeFinalTitle(firstClauseTitle),
                confidence = 0.70f,
                keywords = keywords,
                strategy = TitleStrategy.FIRST_MEANINGFUL_PHRASE
            )
        }

        // Strategy 4: Safe Fallback
        val safeWords = cleanClause(trimmed).split("\\s+".toRegex()).take(5).joinToString(" ")
        val fallback = if (safeWords.length > 45) safeWords.take(42) + "..." else safeWords
        return TitleSuggestion(
            title = sanitizeFinalTitle(fallback.ifBlank { "New Note" }),
            confidence = 0.55f,
            keywords = keywords,
            strategy = TitleStrategy.FALLBACK
        )
    }

    private fun tryExtractActionAndSubject(sample: String, matchedVocab: List<String>): String? {
        // 1. English patterns: e.g. "I need to review the bathroom shop drawings before Thursday"
        val enPatterns = listOf(
            Regex("""(?:need to|want to|please|make sure to|remember to|have to)?\s*(review|check|submit|approve|send|call|buy|prepare|inspect)\s+(?:the\s+|a\s+|our\s+)?([A-Za-z0-9\s\-]+?)(?:\s+(?:before|by|on|for|with|at|\.|\n|,|until)|$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:meeting|call|discussion)\s+with\s+([A-Za-z0-9\s]+?)\s+(?:about|regarding|for|on)\s+([A-Za-z0-9\s\-]+?)(?:\.|\n|,|$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:idea|thought)\s+(?:about|for|to)\s+([A-Za-z0-9\s\-]+?)(?:\.|\n|,|$)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in enPatterns) {
            val match = pattern.find(sample)
            if (match != null) {
                val groupValues = match.groupValues
                if (groupValues.size >= 3) {
                    val action = groupValues[1].replaceFirstChar { it.uppercase() }
                    val subject = cleanEnglishPhrase(groupValues[2])
                    if (subject.isNotBlank()) {
                        return formatEnglishTitle("$subject $action")
                    }
                } else if (groupValues.size == 2) {
                    val phrase = cleanEnglishPhrase(groupValues[1])
                    if (phrase.isNotBlank()) {
                        return formatEnglishTitle(phrase)
                    }
                }
            }
        }

        // 2. Arabic patterns:
        // Check for person / subject + topic: "بكرة لازم أكلم أحمد بتاع الرخام بخصوص أسعار الجلالة للمشروع"
        val arabicPersonTopic = Regex("""(?:أكلم|اكلم|اتصل بـ?|مع)\s+([أ-يA-Za-z]+)\s+(?:بتاع\s+[أ-يA-Za-z]+\s+)?(?:بخصوص|عشان|على|في موضوع)\s+([أ-يA-Za-z0-9\s\-]+?)(?:\.|\n|،|$)""")
        val personMatch = arabicPersonTopic.find(sample)
        if (personMatch != null) {
            val person = personMatch.groupValues[1].trim()
            val topic = cleanArabicPhrase(personMatch.groupValues[2])
            if (topic.isNotBlank()) {
                return "$person – $topic"
            }
        }

        // Check for "فكرني / لازم أشوف / موضوع X"
        val arabicSubjectAction = Regex("""(?:فكرني|لازم|عايز|محتاج|مهم)\s+(?:أشوف|اشوف|أراجع|اراجع|أشيك|اشيك|أتابع|اتابع|أخلص|اخلص|موضوع)\s+([أ-يA-Za-z0-9\s\-]+?)(?:\s+(?:بتاع|الأسبوع|الاسبوع|بكرة|النهاردة|قبل)|$|\.|\n|،)""")
        val subjectMatch = arabicSubjectAction.find(sample)
        if (subjectMatch != null) {
            val target = cleanArabicPhrase(subjectMatch.groupValues[1])
            if (target.isNotBlank()) {
                val actionWord = findArabicActionPrefix(sample)
                return if (actionWord != null) "$actionWord $target" else target
            }
        }

        // Check for idea "كنت بفكر / فكرة"
        if (sample.contains("بفكر") || sample.contains("فكرة")) {
            val ideaRegex = Regex("""(?:بفكر|فكرة)\s+(?:إننا|اننا|في|لو|إن|ان)?\s*(?:ممكن)?\s*([أ-يA-Za-z0-9\s\-]+?)(?:\s+(?:بدل|عشان|علشان)|$|\.|\n|،)""")
            val match = ideaRegex.find(sample)
            if (match != null) {
                val cleanIdea = cleanArabicPhrase(match.groupValues[1])
                if (cleanIdea.isNotBlank()) {
                    return "فكرة $cleanIdea"
                }
            }
        }

        return null
    }

    private fun buildVocabLeadingTitle(sample: String, matchedVocab: List<String>): String {
        // Check if there is an action or context adjacent to the vocab term
        for (vocab in matchedVocab.take(2)) {
            val vocabIndex = sample.indexOf(vocab, ignoreCase = true)
            if (vocabIndex != -1) {
                val start = (vocabIndex - 30).coerceAtLeast(0)
                val end = (vocabIndex + vocab.length + 40).coerceAtMost(sample.length)
                val window = sample.substring(start, end)

                val cleanedWindow = cleanClause(window)
                val words = cleanedWindow.split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (words.size in 2..7) {
                    return words.joinToString(" ")
                }
            }
        }

        return matchedVocab.take(2).joinToString(" & ")
    }

    private fun extractCleanFirstClause(sample: String): String {
        val clauses = sample.split(Regex("[.!?،\n]")).map { it.trim() }.filter { it.isNotBlank() }
        for (rawClause in clauses) {
            val cleaned = cleanClause(rawClause)
            val words = cleaned.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (words.isNotEmpty()) {
                val titleWords = words.take(6)
                val candidate = titleWords.joinToString(" ")
                if (candidate.length in 3..50) {
                    return candidate
                }
            }
        }
        return ""
    }

    fun cleanClause(text: String): String {
        var result = text

        // Remove fillers using unicode boundary matching
        for (filler in ARABIC_FILLERS) {
            val pattern = Regex("(?<![\\p{L}\\p{Nd}])${Regex.escape(filler)}(?![\\p{L}\\p{Nd}])", RegexOption.IGNORE_CASE)
            result = result.replace(pattern, " ")
        }

        for (filler in ENGLISH_FILLERS) {
            val pattern = Regex("(?<![\\p{L}\\p{Nd}])${Regex.escape(filler)}(?![\\p{L}\\p{Nd}])", RegexOption.IGNORE_CASE)
            result = result.replace(pattern, " ")
        }

        // Normalize spaces and clean leading connectors
        result = result.replace(Regex("""\s+"""), " ").trim()
        result = result.removePrefix("و ").removePrefix("ف ").removePrefix("أن ").removePrefix("ان ").removePrefix("إن ")
        return result.trim()
    }

    private fun cleanArabicPhrase(phrase: String): String {
        return cleanClause(phrase)
            .replace(Regex("""^(بتاع|عن|في|على|علي|إننا|اننا|ممكن|نغير|نعمل)\s+"""), "")
            .trim()
    }

    private fun cleanEnglishPhrase(phrase: String): String {
        return cleanClause(phrase)
            .replace(Regex("""^(the|a|an|about|regarding|for|with)\s+""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun findArabicActionPrefix(text: String): String? {
        for ((trigger, action) in ARABIC_ACTION_MAP) {
            if (text.contains(trigger)) {
                return action
            }
        }
        return null
    }

    private fun formatEnglishTitle(text: String): String {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size > 7) {
            return words.take(6).joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }
        return words.joinToString(" ") { word ->
            if (word.equals("and", ignoreCase = true) || word.equals("of", ignoreCase = true) || word.equals("in", ignoreCase = true)) {
                word.lowercase(Locale.ROOT)
            } else {
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }
    }

    private fun sanitizeFinalTitle(raw: String): String {
        var clean = cleanClause(raw)
            .replace(Regex("""[.,;،\-]+$"""), "")
            .replace(Regex("""^[.,;،\-]+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (clean.length > 55) {
            val words = clean.split(" ")
            val sb = StringBuilder()
            for (w in words) {
                if (sb.length + w.length + 1 <= 52) {
                    if (sb.isNotEmpty()) sb.append(" ")
                    sb.append(w)
                } else {
                    break
                }
            }
            clean = sb.toString()
            if (clean.isBlank()) clean = raw.take(50)
        }

        return clean.trim().ifBlank { "New Note" }
    }

    private fun extractKeywords(text: String, matchedVocab: List<String>): List<String> {
        val result = mutableSetOf<String>()
        result.addAll(matchedVocab)

        val enWords = Regex("""\b[A-Z][a-z0-9]{2,}\b""").findAll(text).map { it.value }.take(4)
        result.addAll(enWords)

        val arWords = text.split("\\s+".toRegex())
            .map { it.replace(Regex("[^أ-ي]"), "") }
            .filter { it.length >= 4 && !ARABIC_FILLERS.contains(it) }
            .take(3)
        result.addAll(arWords)

        return result.take(6).toList()
    }
}
