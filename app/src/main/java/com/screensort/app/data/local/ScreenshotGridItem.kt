package com.screensort.app.data.local

/**
 * Lightweight projection of a screenshot for grid display and navigation.
 * Contains only the minimal metadata required by the grid card.
 * Excludes heavy OCR text, visual labels, and embeddings to prevent
 * CursorWindow overflow and eliminate memory overhead for 2000+ screenshots.
 */
data class ScreenshotGridItem(
    val id: Long,
    val uriString: String,
    val displayName: String,
    val dateAdded: Long,
    val category: String
)
