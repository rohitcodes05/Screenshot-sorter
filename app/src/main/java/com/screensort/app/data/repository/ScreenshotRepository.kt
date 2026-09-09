package com.screensort.app.data.repository

import android.content.Context
import android.net.Uri
import com.screensort.app.data.local.CategoryCount
import com.screensort.app.data.local.ScreenshotDao
import com.screensort.app.data.local.ScreenshotEntity
import com.screensort.app.data.local.ScreenshotGridItem
import com.screensort.app.data.local.UserCategoryDao
import com.screensort.app.data.local.UserCategoryEntity
import com.screensort.app.domain.DynamicTopicEngine
import com.screensort.app.domain.UserCategoryMatcher
import com.screensort.app.ocr.ImageAnalysisManager
import com.screensort.app.ocr.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository orchestrating multimodal analysis (OCR + Image Labeling),
 * user-defined category matching, dynamic broad clustering, and database persistence.
 */
class ScreenshotRepository(
    private val context: Context,
    private val dao: ScreenshotDao,
    private val userCategoryDao: UserCategoryDao,
    private val analysisManager: ImageAnalysisManager = ImageAnalysisManager(),
    private val topicEngine: DynamicTopicEngine = DynamicTopicEngine(minClusterSize = 3)
) {

    private val mediaStoreScanner = MediaStoreScanner(context)

    fun getAllScreenshots(): Flow<List<ScreenshotGridItem>> = dao.getAllScreenshots()

    fun getScreenshotsByCategory(category: String): Flow<List<ScreenshotGridItem>> =
        dao.getScreenshotsByCategory(category)

    fun searchScreenshots(query: String): Flow<List<ScreenshotGridItem>> =
        dao.searchScreenshots(query)

    suspend fun getScreenshotById(id: Long): ScreenshotEntity? = withContext(Dispatchers.IO) {
        dao.getScreenshotById(id)
    }

    fun getCategoryCounts(): Flow<List<CategoryCount>> = dao.getCategoryCounts()

    fun getAllUserCategories(): Flow<List<UserCategoryEntity>> =
        userCategoryDao.getAllUserCategories()

    suspend fun createUserCategory(name: String, keywords: String) = withContext(Dispatchers.IO) {
        val category = UserCategoryEntity(
            name = name.trim(),
            keywords = keywords.trim()
        )
        userCategoryDao.insert(category)
        // Automatically re-sort screenshots to populate the new category
        reclusterAll()
    }

    suspend fun updateUserCategory(
        category: UserCategoryEntity,
        newName: String,
        newKeywords: String
    ) = withContext(Dispatchers.IO) {
        val oldName = category.name
        val cleanNewName = newName.trim()
        val cleanNewKeywords = newKeywords.trim()
        val updated = category.copy(name = cleanNewName, keywords = cleanNewKeywords)
        userCategoryDao.update(updated)

        if (oldName != cleanNewName) {
            dao.renameCategory(oldName, cleanNewName)
        }
        reclusterAll()
    }

    suspend fun deleteUserCategory(category: UserCategoryEntity) = withContext(Dispatchers.IO) {
        userCategoryDao.delete(category)
        dao.resetCategoryToPending(category.name)
        reclusterAll()
    }

    private data class ImageIngestItem(
        val uri: Uri,
        val displayName: String,
        val dateAdded: Long
    )

    private suspend fun ingestAndProcessImages(
        items: List<ImageIngestItem>,
        onProgress: (current: Int, total: Int) -> Unit
    ): Int {
        if (items.isEmpty()) return 0

        val extractedList = mutableListOf<ScreenshotEntity>()

        for (i in items.indices) {
            val item = items[i]
            onProgress(i + 1, items.size)

            val analysis = analysisManager.analyzeImage(context, item.uri)

            extractedList.add(
                ScreenshotEntity(
                    uriString = item.uri.toString(),
                    displayName = item.displayName,
                    dateAdded = item.dateAdded,
                    extractedText = analysis.extractedText,
                    visualLabels = analysis.visualLabels.joinToString(", "),
                    category = "Pending",
                    dominantKeywords = "",
                    confidence = 0f
                )
            )
        }

        dao.insertAll(extractedList)
        reclusterAll()

        return items.size
    }

    /**
     * Scans the device for new screenshots, runs offline multimodal analysis (OCR + Labels),
     * and routes into user categories or dynamic clusters.
     */
    suspend fun scanAndProcessDeviceScreenshots(
        onProgress: (current: Int, total: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val discovered = mediaStoreScanner.findScreenshots()
        if (discovered.isEmpty()) return@withContext 0

        val alreadyScannedUris = dao.getAllScannedUris().toSet()
        val newScreenshots = discovered
            .filter { it.uri.toString() !in alreadyScannedUris }
            .map { ImageIngestItem(it.uri, it.displayName, it.dateAdded) }

        ingestAndProcessImages(newScreenshots, onProgress)
    }

    /**
     * Processes manually picked images with multimodal analysis.
     */
    suspend fun processImportedImages(
        uris: List<Uri>,
        onProgress: (current: Int, total: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext 0

        val now = System.currentTimeMillis()
        val items = uris.mapIndexed { index, uri ->
            ImageIngestItem(
                uri = uri,
                displayName = "Imported_${now}_$index",
                dateAdded = now
            )
        }

        ingestAndProcessImages(items, onProgress)
    }

    /**
     * Re-analyzes all screenshots:
     * 1. Preserves user-assigned categories (where isManuallyCategorized == true).
     * 2. Prioritizes matching against user-defined categories.
     * 3. Remaining unmatched screenshots are dynamically clustered into broad categories or "Others".
     */
    suspend fun reclusterAll(
        onProgress: ((current: Int, total: Int, stage: String) -> Unit)? = null
    ) = withContext(Dispatchers.Default) {
        val allScreenshots = dao.getAllScreenshotsSync()
        if (allScreenshots.isEmpty()) return@withContext

        // Preserve manually categorized screenshots so user edits are never lost
        val automatableScreenshots = allScreenshots.filter { !it.isManuallyCategorized }
        if (automatableScreenshots.isEmpty()) return@withContext

        val userCategories = userCategoryDao.getAllUserCategoriesSync()
        val preparedCategories = UserCategoryMatcher.prepareCategories(userCategories)
        val docMap = automatableScreenshots.associateBy { it.id.toString() }

        val matchedByUserCategories = mutableListOf<ScreenshotEntity>()
        val unmatchedForAutoClustering = mutableListOf<DynamicTopicEngine.MultimodalDoc>()

        // Step 1: Match against user-defined categories using pre-computed keyword sets
        onProgress?.invoke(0, automatableScreenshots.size, "Matching user categories...")
        for (item in automatableScreenshots) {
            val visualList = item.getVisualLabelList()
            val userMatch = UserCategoryMatcher.findBestMatchPrepared(item.extractedText, visualList, preparedCategories)

            if (userMatch != null) {
                matchedByUserCategories.add(
                    item.copy(
                        category = userMatch.categoryName,
                        dominantKeywords = userMatch.matchedKeywords.joinToString(", "),
                        confidence = 95.0f,
                        clusterId = -2
                    )
                )
            } else {
                unmatchedForAutoClustering.add(
                    DynamicTopicEngine.MultimodalDoc(
                        docId = item.id.toString(),
                        text = item.extractedText,
                        visualLabels = visualList
                    )
                )
            }
        }

        // Step 2: Auto-cluster only the remaining unmatched screenshots
        val autoResults = if (unmatchedForAutoClustering.isNotEmpty()) {
            topicEngine.clusterAndNameMultimodal(unmatchedForAutoClustering, onProgress)
        } else {
            emptyList()
        }
        val autoResultMap = autoResults.associateBy { it.docId }

        val allUpdated = mutableListOf<ScreenshotEntity>()
        allUpdated.addAll(matchedByUserCategories)

        for (doc in unmatchedForAutoClustering) {
            val original = docMap[doc.docId] ?: continue
            val auto = autoResultMap[doc.docId]
            if (auto != null) {
                allUpdated.add(
                    original.copy(
                        category = auto.categoryName,
                        dominantKeywords = auto.dominantKeywords.joinToString(", "),
                        confidence = auto.confidence,
                        clusterId = auto.clusterId
                    )
                )
            }
        }

        if (allUpdated.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                dao.updateAll(allUpdated)
            }
        }
    }

    suspend fun updateCategory(id: Long, newCategory: String) = withContext(Dispatchers.IO) {
        dao.updateCategory(id, newCategory, isManual = true)
    }

    suspend fun deleteScreenshot(screenshot: ScreenshotEntity) = withContext(Dispatchers.IO) {
        dao.delete(screenshot)
    }
}
