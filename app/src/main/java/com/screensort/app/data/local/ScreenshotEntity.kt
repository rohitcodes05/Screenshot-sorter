package com.screensort.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity representing a scanned screenshot with its
 * OCR extracted text and dynamically discovered category.
 */
@Entity(
    tableName = "screenshots",
    indices = [
        Index(value = ["category", "dateAdded"]),
        Index(value = ["dateAdded"])
    ]
)
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uriString: String,
    val displayName: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val extractedText: String = "",
    val visualLabels: String = "",
    val category: String = "Unclassified",
    val dominantKeywords: String = "",
    val confidence: Float = 0.0f,
    val clusterId: Int = -1,
    val isManuallyCategorized: Boolean = false
) {
    /**
     * Returns keywords parsed from comma-separated string.
     */
    fun getKeywordList(): List<String> {
        return dominantKeywords.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Returns visual labels parsed from comma-separated string.
     */
    fun getVisualLabelList(): List<String> {
        return visualLabels.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}

/**
 * Data class representing a dynamic category and its screenshot count.
 */
data class CategoryCount(
    val category: String,
    val count: Int
)
