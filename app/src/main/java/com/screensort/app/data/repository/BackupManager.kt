package com.screensort.app.data.repository

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents the complete backup container exported from or imported into ScreenSort.
 */
data class BackupPayload(
    val version: Int = 1,
    val appVersion: String = "1.0.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val userCategories: List<UserCategoryBackupItem> = emptyList(),
    val screenshots: List<ScreenshotBackupItem> = emptyList()
)

/**
 * Backup representation of a user-defined custom category.
 */
data class UserCategoryBackupItem(
    val name: String,
    val keywords: String = "",
    val dateCreated: Long = 0L
)

/**
 * Backup representation of a screenshot's categorization and OCR metadata.
 * Note: Actual image bitmaps are excluded to ensure backup files remain ultra-lightweight.
 */
data class ScreenshotBackupItem(
    val uriString: String = "",
    val displayName: String,
    val dateAdded: Long,
    val category: String,
    val dominantKeywords: String = "",
    val extractedText: String = "",
    val visualLabels: String = "",
    val confidence: Float = 0.0f,
    val isManuallyCategorized: Boolean = false
)

/**
 * Summary of a backup import operation.
 */
data class ImportResult(
    val newScreenshotsCount: Int,
    val updatedScreenshotsCount: Int,
    val newCategoriesCount: Int
)

/**
 * Handles JSON serialization and deserialization of ScreenSort metadata backups.
 */
object BackupManager {

    private const val KEY_VERSION = "version"
    private const val KEY_APP_VERSION = "appVersion"
    private const val KEY_EXPORTED_AT = "exportedAt"
    private const val KEY_USER_CATEGORIES = "userCategories"
    private const val KEY_SCREENSHOTS = "screenshots"

    // User category fields
    private const val KEY_CAT_NAME = "name"
    private const val KEY_CAT_KEYWORDS = "keywords"
    private const val KEY_CAT_DATE = "dateCreated"

    // Screenshot fields
    private const val KEY_SHOT_URI = "uriString"
    private const val KEY_SHOT_NAME = "displayName"
    private const val KEY_SHOT_DATE = "dateAdded"
    private const val KEY_SHOT_CATEGORY = "category"
    private const val KEY_SHOT_KEYWORDS = "dominantKeywords"
    private const val KEY_SHOT_TEXT = "extractedText"
    private const val KEY_SHOT_LABELS = "visualLabels"
    private const val KEY_SHOT_CONFIDENCE = "confidence"
    private const val KEY_SHOT_MANUAL = "isManuallyCategorized"

    /**
     * Serializes a [BackupPayload] into a formatted JSON string.
     */
    fun serializeToJson(payload: BackupPayload): String {
        val root = JSONObject()
        root.put(KEY_VERSION, payload.version)
        root.put(KEY_APP_VERSION, payload.appVersion)
        root.put(KEY_EXPORTED_AT, payload.exportedAt)

        // Serialize user categories
        val catArray = JSONArray()
        for (cat in payload.userCategories) {
            val catObj = JSONObject()
            catObj.put(KEY_CAT_NAME, cat.name)
            catObj.put(KEY_CAT_KEYWORDS, cat.keywords)
            catObj.put(KEY_CAT_DATE, cat.dateCreated)
            catArray.put(catObj)
        }
        root.put(KEY_USER_CATEGORIES, catArray)

        // Serialize screenshots
        val shotArray = JSONArray()
        for (shot in payload.screenshots) {
            val shotObj = JSONObject()
            shotObj.put(KEY_SHOT_NAME, shot.displayName)
            shotObj.put(KEY_SHOT_DATE, shot.dateAdded)
            shotObj.put(KEY_SHOT_CATEGORY, shot.category)
            if (shot.uriString.isNotBlank()) shotObj.put(KEY_SHOT_URI, shot.uriString)
            if (shot.dominantKeywords.isNotBlank()) shotObj.put(KEY_SHOT_KEYWORDS, shot.dominantKeywords)
            if (shot.extractedText.isNotBlank()) shotObj.put(KEY_SHOT_TEXT, shot.extractedText)
            if (shot.visualLabels.isNotBlank()) shotObj.put(KEY_SHOT_LABELS, shot.visualLabels)
            if (shot.confidence > 0f) shotObj.put(KEY_SHOT_CONFIDENCE, shot.confidence.toDouble())
            if (shot.isManuallyCategorized) shotObj.put(KEY_SHOT_MANUAL, shot.isManuallyCategorized)
            shotArray.put(shotObj)
        }
        root.put(KEY_SCREENSHOTS, shotArray)

        return root.toString(2)
    }

    /**
     * Parses a JSON string into a validated [BackupPayload].
     */
    fun deserializeFromJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val version = root.optInt(KEY_VERSION, 1)
        val appVersion = root.optString(KEY_APP_VERSION, "1.0.0")
        val exportedAt = root.optLong(KEY_EXPORTED_AT, System.currentTimeMillis())

        val categoriesList = mutableListOf<UserCategoryBackupItem>()
        val catArray = root.optJSONArray(KEY_USER_CATEGORIES)
        if (catArray != null) {
            for (i in 0 until catArray.length()) {
                val catObj = catArray.optJSONObject(i) ?: continue
                val name = catObj.optString(KEY_CAT_NAME, "").trim()
                if (name.isNotEmpty()) {
                    categoriesList.add(
                        UserCategoryBackupItem(
                            name = name,
                            keywords = catObj.optString(KEY_CAT_KEYWORDS, ""),
                            dateCreated = catObj.optLong(KEY_CAT_DATE, 0L)
                        )
                    )
                }
            }
        }

        val screenshotsList = mutableListOf<ScreenshotBackupItem>()
        val shotArray = root.optJSONArray(KEY_SCREENSHOTS)
        if (shotArray != null) {
            for (i in 0 until shotArray.length()) {
                val shotObj = shotArray.optJSONObject(i) ?: continue
                val displayName = shotObj.optString(KEY_SHOT_NAME, "").trim()
                if (displayName.isNotEmpty()) {
                    screenshotsList.add(
                        ScreenshotBackupItem(
                            displayName = displayName,
                            dateAdded = shotObj.optLong(KEY_SHOT_DATE, 0L),
                            category = shotObj.optString(KEY_SHOT_CATEGORY, "Others"),
                            uriString = shotObj.optString(KEY_SHOT_URI, ""),
                            dominantKeywords = shotObj.optString(KEY_SHOT_KEYWORDS, ""),
                            extractedText = shotObj.optString(KEY_SHOT_TEXT, ""),
                            visualLabels = shotObj.optString(KEY_SHOT_LABELS, ""),
                            confidence = shotObj.optDouble(KEY_SHOT_CONFIDENCE, 0.0).toFloat(),
                            isManuallyCategorized = shotObj.optBoolean(KEY_SHOT_MANUAL, false)
                        )
                    )
                }
            }
        }

        return BackupPayload(
            version = version,
            appVersion = appVersion,
            exportedAt = exportedAt,
            userCategories = categoriesList,
            screenshots = screenshotsList
        )
    }
}
