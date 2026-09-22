package com.example

import com.example.notes.naming.CaptureTitleSuggester
import com.example.notes.naming.TitleStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureTitleSuggesterTest {

    private val customVocab = listOf("Galala", "Piatra", "BOQ", "Variation Order", "Shop Drawing", "New Cairo", "Palmariva")

    @Test
    fun testEgyptianArabicMarblePrices() {
        val input = "بكرة لازم أكلم أحمد بتاع الرخام بخصوص أسعار الجلالة للمشروع"
        val result = CaptureTitleSuggester.suggest(input, customVocab)

        assertNotNull(result.title)
        assertTrue(result.title.isNotBlank())
        assertTrue("Title should be <= 55 chars: ${result.title}", result.title.length <= 55)
        assertTrue(
            "Title should contain key person or topic: ${result.title}",
            result.title.contains("أحمد") || result.title.contains("الجلالة") || result.title.contains("أسعار")
        )
    }

    @Test
    fun testEnglishShopDrawingReview() {
        val input = "I need to review the bathroom shop drawings before Thursday."
        val result = CaptureTitleSuggester.suggest(input, customVocab)

        assertNotNull(result.title)
        assertTrue(result.title.isNotBlank())
        assertTrue("Title length should be <= 55 chars: ${result.title}", result.title.length <= 55)
        assertTrue(
            "Title should capture shop drawings or review: ${result.title}",
            result.title.contains("Bathroom", ignoreCase = true) || result.title.contains("Shop Drawing", ignoreCase = true) || result.title.contains("Review", ignoreCase = true)
        )
    }

    @Test
    fun testArabicEnglishCodeSwitchingWithBoq() {
        val input = "المهندس بعت الـ BOQ والـ Variation Order بخصوص مشروع New Cairo عشان نراجعهم"
        val result = CaptureTitleSuggester.suggest(input, customVocab)

        assertNotNull(result.title)
        assertTrue(result.title.isNotBlank())
        assertTrue("Title should contain key vocabulary: ${result.title}", result.title.contains("BOQ") || result.title.contains("Variation Order") || result.title.contains("New Cairo"))
    }

    @Test
    fun testArabicCarInsuranceReminder() {
        val input = "فكرني أشوف موضوع التأمين بتاع العربية الأسبوع الجاي ضروري"
        val result = CaptureTitleSuggester.suggest(input, customVocab)

        assertNotNull(result.title)
        assertTrue(result.title.isNotBlank())
        assertTrue(
            "Title should contain car or insurance: ${result.title}",
            result.title.contains("تأمين") || result.title.contains("العربية") || result.title.contains("تذكير")
        )
    }

    @Test
    fun testIdeaNoteWithIndirectLighting() {
        val input = "كنت بفكر إننا ممكن نغير تصميم الريسبشن ونستخدم إضاءة indirect بدل السبوتات الكتير"
        val result = CaptureTitleSuggester.suggest(input, customVocab)

        assertNotNull(result.title)
        assertTrue(result.title.isNotBlank())
        assertTrue(
            "Title should reflect idea / reception / lighting: ${result.title}",
            result.title.contains("فكرة") || result.title.contains("الريسبشن") || result.title.contains("indirect", ignoreCase = true)
        )
    }

    @Test
    fun testShortNote() {
        val input = "buy milk tomorrow"
        val result = CaptureTitleSuggester.suggest(input, customVocab)

        assertNotNull(result.title)
        assertTrue(result.title.contains("Milk", ignoreCase = true) || result.title.contains("Buy", ignoreCase = true))
    }

    @Test
    fun testEmptyNoteFallback() {
        val result = CaptureTitleSuggester.suggest("", customVocab)
        assertEquals("New Note", result.title)
        assertEquals(TitleStrategy.FALLBACK, result.strategy)
    }

    @Test
    fun testFillerRemovalDoesNotLeaveEmpty() {
        val input = "يعني بص يا باشا um like basically والله أنا كنت عايز بس أشوف أسعار الرخام"
        val result = CaptureTitleSuggester.suggest(input, customVocab)

        assertNotNull(result.title)
        assertTrue(result.title.isNotBlank())
        assertFalse(result.title.startsWith("يعني"))
        assertFalse(result.title.startsWith("um"))
    }

    @Test
    fun testWordCountConstraint() {
        val longRamblingText = "We had a really long conversation yesterday about the upcoming master schedule and whether we need to adjust the milestones for phase two before presenting the budget to the board of directors in London next quarter."
        val result = CaptureTitleSuggester.suggest(longRamblingText, customVocab)

        val words = result.title.split("\\s+".toRegex())
        assertTrue("Title should have between 1 and 8 words: ${words.size} (${result.title})", words.size in 1..8)
        assertTrue("Title should be <= 55 chars: ${result.title.length}", result.title.length <= 55)
    }
}
