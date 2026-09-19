package com.screensort.app

import com.screensort.app.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DateTimeUtilsTest {

    @Test
    fun testNormalizeSecondsToMilliseconds() {
        // 1710243200 seconds -> ~March 12, 2024
        val seconds = 1710243200L
        val normalized = DateTimeUtils.normalizeTimestamp(seconds)
        assertEquals(1710243200000L, normalized)
    }

    @Test
    fun testNormalizeMillisecondsPreserved() {
        // 1710243200000 milliseconds
        val millis = 1710243200000L
        val normalized = DateTimeUtils.normalizeTimestamp(millis)
        assertEquals(1710243200000L, normalized)
    }

    @Test
    fun testNormalizeZeroOrNegativeUsesCurrentTime() {
        val before = System.currentTimeMillis()
        val normalizedZero = DateTimeUtils.normalizeTimestamp(0L)
        val normalizedNegative = DateTimeUtils.normalizeTimestamp(-100L)
        val after = System.currentTimeMillis()

        assertTrue(normalizedZero in before..after)
        assertTrue(normalizedNegative in before..after)
    }

    @Test
    fun testFormatDateHandlesSecondsAndMillisEqually() {
        val seconds = 1710243200L // 2024-03-12
        val millis = 1710243200000L

        val dateFromSeconds = DateTimeUtils.formatDate(seconds, "yyyy-MM-dd", Locale.US)
        val dateFromMillis = DateTimeUtils.formatDate(millis, "yyyy-MM-dd", Locale.US)

        assertEquals("2024-03-12", dateFromSeconds)
        assertEquals("2024-03-12", dateFromMillis)
    }

    @Test
    fun testFormatDateTime() {
        val seconds = 1710243200L
        val formatted = DateTimeUtils.formatDateTime(seconds, "yyyy-MM-dd HH:mm", Locale.US)
        assertTrue(formatted.startsWith("2024-03-12"))
    }
}
