package com.screensort.app.ocr

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Discovers screenshots stored on the Android device using MediaStore.
 */
class MediaStoreScanner(private val context: Context) {

    data class DiscoveredScreenshot(
        val uri: Uri,
        val displayName: String,
        val dateAdded: Long
    )

    /**
     * Queries MediaStore for screenshots on the device.
     */
    suspend fun findScreenshots(): List<DiscoveredScreenshot> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DiscoveredScreenshot>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Images.Media.RELATIVE_PATH)
        }

        // Search in common screenshot paths and filenames
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        } else {
            "${MediaStore.Images.Media.DATA} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        }

        val selectionArgs = arrayOf("%Screenshots%", "%Screenshot%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection.toTypedArray(),
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Screenshot_$id"
                    val dateAdded = cursor.getLong(dateColumn) * 1000 // Convert to milliseconds
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    results.add(
                        DiscoveredScreenshot(
                            uri = contentUri,
                            displayName = name,
                            dateAdded = if (dateAdded > 0) dateAdded else System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results
    }
}
