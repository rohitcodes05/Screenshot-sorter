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
import com.screensort.app.util.DateTimeUtils
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

    private fun normalizeTimestamp(timestamp: Long): Long =
        DateTimeUtils.normalizeTimestamp(timestamp)

    /**
     * Scans the device for new screenshots, runs offline multimodal analysis (OCR + Labels),
     * and routes into user categories or dynamic clusters.
     * Reconciles existing screenshots from restored backups when device MediaStore URIs change.
     */
    suspend fun scanAndProcessDeviceScreenshots(
        onProgress: (current: Int, total: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val discovered = mediaStoreScanner.findScreenshots()
        if (discovered.isEmpty()) return@withContext 0

        val alreadyScannedUris = dao.getAllScannedUris().toSet()
        val candidateNew = discovered.filter { it.uri.toString() !in alreadyScannedUris }
        if (candidateNew.isEmpty()) return@withContext 0

        val existingScreenshots = dao.getAllGridItemsSync()
        val existingByName = existingScreenshots.groupBy { it.displayName.trim().lowercase() }

        val newScreenshots = mutableListOf<ImageIngestItem>()

        for (item in candidateNew) {
            val itemUriStr = item.uri.toString()
            val itemNormDate = normalizeTimestamp(item.dateAdded)
            val nameClean = item.displayName.trim().lowercase()
            val candidates = existingByName[nameClean]

            // Check if this image already exists in DB (e.g. from restored backup) with dead/empty/old URI
            val matchedExisting = candidates?.firstOrNull { candidate ->
                candidate.uriString != itemUriStr &&
                    kotlin.math.abs(normalizeTimestamp(candidate.dateAdded) - itemNormDate) <= 5000L
            }

            if (matchedExisting != null) {
                // Reconcile: update the URI on the existing DB record without loading full OCR text
                dao.updateUri(matchedExisting.id, itemUriStr)
            } else {
                // Truly new screenshot requiring fresh multimodal analysis
                newScreenshots.add(ImageIngestItem(item.uri, item.displayName, item.dateAdded))
            }
        }

        if (newScreenshots.isNotEmpty()) {
            ingestAndProcessImages(newScreenshots, onProgress)
        } else {
            0
        }
    }

    /**
     * Processes manually picked images with multimodal analysis.
     */
    suspend fun processImportedImages(
        uris: List<Uri>,
        onProgress: (current: Int, total: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext 0

        val alreadyScannedUris = dao.getAllScannedUris().toSet()
        val candidateUris = uris.filter { it.toString() !in alreadyScannedUris }
        if (candidateUris.isEmpty()) return@withContext 0

        val now = System.currentTimeMillis()
        val items = candidateUris.mapIndexed { index, uri ->
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

    suspend fun updateCategoryBulk(ids: List<Long>, newCategory: String) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        dao.updateCategoryBulk(ids, newCategory)
    }

    suspend fun deleteScreenshotsBulk(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        dao.deleteBulk(ids)
    }

    /**
     * Exports all user categories and screenshot metadata as a portable [BackupPayload].
     */
    suspend fun exportBackupData(): BackupPayload = withContext(Dispatchers.IO) {
        val userCategories = userCategoryDao.getAllUserCategoriesSync().map {
            UserCategoryBackupItem(
                name = it.name,
                keywords = it.keywords,
                dateCreated = it.dateCreated
            )
        }
        val screenshots = dao.getAllScreenshotsSync().map {
            ScreenshotBackupItem(
                uriString = it.uriString,
                displayName = it.displayName,
                dateAdded = it.dateAdded,
                category = it.category,
                dominantKeywords = it.dominantKeywords,
                extractedText = it.extractedText,
                visualLabels = it.visualLabels,
                confidence = it.confidence,
                isManuallyCategorized = it.isManuallyCategorized
            )
        }
        BackupPayload(
            version = 1,
            appVersion = "1.0.0",
            exportedAt = System.currentTimeMillis(),
            userCategories = userCategories,
            screenshots = screenshots
        )
    }

    /**
     * Merges imported backup payload non-destructively:
     * - Adds new user categories without duplicating existing ones.
     * - Enriches existing screenshots with missing OCR/category data, preserving manual categorization.
     * - Inserts new screenshots that do not currently exist in the database.
     */
    suspend fun importBackupData(backup: BackupPayload): ImportResult = withContext(Dispatchers.IO) {
        var newCatsCount = 0
        var newScreenshotsCount = 0
        var updatedScreenshotsCount = 0

        // 1. Merge User Categories (Case-insensitive check with seen set)
        val existingCats = userCategoryDao.getAllUserCategoriesSync()
        val seenCatNames = existingCats.map { it.name.trim().lowercase() }.toMutableSet()

        for (catBackup in backup.userCategories) {
            val cleanName = catBackup.name.trim()
            if (cleanName.isNotEmpty() && cleanName.lowercase() !in seenCatNames) {
                userCategoryDao.insert(
                    UserCategoryEntity(
                        name = cleanName,
                        keywords = catBackup.keywords,
                        dateCreated = if (catBackup.dateCreated > 0) normalizeTimestamp(catBackup.dateCreated) else System.currentTimeMillis()
                    )
                )
                seenCatNames.add(cleanName.lowercase())
                newCatsCount++
            }
        }

        // 2. Merge Screenshots
        val existingScreenshots = dao.getAllScreenshotsSync()
        val existingByUri = existingScreenshots.filter { it.uriString.isNotBlank() }.associateBy { it.uriString }
        val existingByNameList = existingScreenshots.groupBy { it.displayName.trim().lowercase() }

        val toInsert = mutableListOf<ScreenshotEntity>()
        val toUpdate = mutableListOf<ScreenshotEntity>()
        val seenBackupKeys = mutableSetOf<String>()

        for (item in backup.screenshots) {
            val nameClean = item.displayName.trim()
            if (nameClean.isEmpty()) continue

            val itemNormDate = normalizeTimestamp(item.dateAdded)
            val dedupeKey = "${nameClean.lowercase()}_$itemNormDate"
            if (dedupeKey in seenBackupKeys) continue
            seenBackupKeys.add(dedupeKey)

            // Safe Matching strategy:
            // 1. Exact URI match
            // 2. Name + Date match within ±5 seconds tolerance (prevents generic filename collisions)
            val existing = (if (item.uriString.isNotBlank()) existingByUri[item.uriString] else null)
                ?: existingByNameList[nameClean.lowercase()]?.firstOrNull { candidate ->
                    kotlin.math.abs(normalizeTimestamp(candidate.dateAdded) - itemNormDate) <= 5000L
                }

            if (existing != null) {
                var changed = false
                var newCategory = existing.category
                var newKeywords = existing.dominantKeywords
                var newText = existing.extractedText
                var newLabels = existing.visualLabels
                var newManual = existing.isManuallyCategorized

                if (item.isManuallyCategorized && !existing.isManuallyCategorized) {
                    newCategory = item.category
                    newManual = true
                    changed = true
                } else if ((existing.category == "Pending" || existing.category == "Others" || existing.category == "Unclassified") &&
                    item.category != "Pending" && item.category != "Unclassified") {
                    newCategory = item.category
                    if (item.isManuallyCategorized) newManual = true
                    changed = true
                }

                if (existing.extractedText.isBlank() && item.extractedText.isNotBlank()) {
                    newText = item.extractedText
                    changed = true
                }

                if (existing.visualLabels.isBlank() && item.visualLabels.isNotBlank()) {
                    newLabels = item.visualLabels
                    changed = true
                }

                if (existing.dominantKeywords.isBlank() && item.dominantKeywords.isNotBlank()) {
                    newKeywords = item.dominantKeywords
                    changed = true
                }

                if (changed) {
                    toUpdate.add(
                        existing.copy(
                            category = newCategory,
                            dominantKeywords = newKeywords,
                            extractedText = newText,
                            visualLabels = newLabels,
                            isManuallyCategorized = newManual
                        )
                    )
                    updatedScreenshotsCount++
                }
            } else {
                toInsert.add(
                    ScreenshotEntity(
                        id = 0,
                        uriString = item.uriString,
                        displayName = item.displayName,
                        dateAdded = itemNormDate,
                        extractedText = item.extractedText,
                        visualLabels = item.visualLabels,
                        category = item.category.ifBlank { "Others" },
                        dominantKeywords = item.dominantKeywords,
                        confidence = item.confidence,
                        isManuallyCategorized = item.isManuallyCategorized
                    )
                )
                newScreenshotsCount++
            }
        }

        if (toInsert.isNotEmpty()) {
            dao.insertAll(toInsert)
        }
        if (toUpdate.isNotEmpty()) {
            dao.updateAll(toUpdate)
        }

        ImportResult(
            newScreenshotsCount = newScreenshotsCount,
            updatedScreenshotsCount = updatedScreenshotsCount,
            newCategoriesCount = newCatsCount
        )
    }

    /**
     * Closes underlying resources when repository is destroyed.
     */
    fun close() {
        analysisManager.close()
    }
}
