package com.screensort.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralized date and time utilities for ScreenSort.
 * Handles timestamp normalization between seconds and milliseconds,
 * and formats timestamps consistently across the UI and data layers.
 */
object DateTimeUtils {

    /**
     * Threshold to differentiate between epoch seconds (~1.7e9) and epoch milliseconds (~1.7e12).
     * 100_000_000_000L corresponds to Saturday, March 3, 1973 9:46:40 AM GMT in milliseconds,
     * or distant future in seconds. Any timestamp below this is in seconds.
     */
    private const val SECOND_MS_THRESHOLD = 100_000_000_000L

    /**
     * Normalizes a timestamp into epoch milliseconds.
     * Handles timestamps provided in seconds (from Android MediaStore) or missing/zero timestamps.
     */
    fun normalizeTimestamp(timestamp: Long): Long {
        return when {
            timestamp <= 0L -> System.currentTimeMillis()
            timestamp < SECOND_MS_THRESHOLD -> timestamp * 1000L
            else -> timestamp
        }
    }

    /**
     * Formats an epoch timestamp (in seconds or milliseconds) into a user-friendly date string.
     * e.g., "Sep 13, 2026"
     */
    fun formatDate(
        timestamp: Long,
        pattern: String = "MMM d, yyyy",
        locale: Locale = Locale.getDefault()
    ): String {
        return try {
            val normalized = normalizeTimestamp(timestamp)
            SimpleDateFormat(pattern, locale).format(Date(normalized))
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Formats an epoch timestamp into a user-friendly date & time string.
     * e.g., "Sep 13, 2026, 5:30 PM"
     */
    fun formatDateTime(
        timestamp: Long,
        pattern: String = "MMM d, yyyy, h:mm a",
        locale: Locale = Locale.getDefault()
    ): String {
        return try {
            val normalized = normalizeTimestamp(timestamp)
            SimpleDateFormat(pattern, locale).format(Date(normalized))
        } catch (_: Exception) {
            ""
        }
    }
}
