package com.screensort.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for reactive queries and updates on screenshots.
 */
@Dao
interface ScreenshotDao {

    @Query("SELECT id, uriString, displayName, dateAdded, category FROM screenshots ORDER BY dateAdded DESC")
    fun getAllScreenshots(): Flow<List<ScreenshotGridItem>>

    @Query("SELECT id, uriString, displayName, dateAdded, category FROM screenshots WHERE category = :category ORDER BY dateAdded DESC")
    fun getScreenshotsByCategory(category: String): Flow<List<ScreenshotGridItem>>

    @Query("""
        SELECT id, uriString, displayName, dateAdded, category FROM screenshots 
        WHERE extractedText LIKE '%' || :query || '%' 
           OR displayName LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%'
        ORDER BY dateAdded DESC
    """)
    fun searchScreenshots(query: String): Flow<List<ScreenshotGridItem>>

    @Query("SELECT * FROM screenshots WHERE id = :id LIMIT 1")
    suspend fun getScreenshotById(id: Long): ScreenshotEntity?

    @Query("SELECT category, COUNT(*) as count FROM screenshots GROUP BY category ORDER BY count DESC")
    fun getCategoryCounts(): Flow<List<CategoryCount>>

    @Query("SELECT * FROM screenshots")
    suspend fun getAllScreenshotsSync(): List<ScreenshotEntity>

    @Query("SELECT uriString FROM screenshots")
    suspend fun getAllScannedUris(): List<String>

    @Query("SELECT id, uriString, displayName, dateAdded, category FROM screenshots")
    suspend fun getAllGridItemsSync(): List<ScreenshotGridItem>

    @Query("UPDATE screenshots SET uriString = :newUri WHERE id = :id")
    suspend fun updateUri(id: Long, newUri: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(screenshots: List<ScreenshotEntity>)

    @Update
    suspend fun updateAll(screenshots: List<ScreenshotEntity>)

    @Query("UPDATE screenshots SET category = :newCategory, isManuallyCategorized = :isManual WHERE id = :id")
    suspend fun updateCategory(id: Long, newCategory: String, isManual: Boolean = true)

    @Query("UPDATE screenshots SET category = 'Pending', clusterId = -1, isManuallyCategorized = 0 WHERE category = :categoryName")
    suspend fun resetCategoryToPending(categoryName: String)

    @Query("UPDATE screenshots SET category = :newCategory WHERE category = :oldCategory")
    suspend fun renameCategory(oldCategory: String, newCategory: String)

    @Delete
    suspend fun delete(screenshot: ScreenshotEntity)

    @Query("UPDATE screenshots SET category = :newCategory, isManuallyCategorized = 1 WHERE id IN (:ids)")
    suspend fun updateCategoryBulk(ids: List<Long>, newCategory: String)

    @Query("DELETE FROM screenshots WHERE id IN (:ids)")
    suspend fun deleteBulk(ids: List<Long>)
}
