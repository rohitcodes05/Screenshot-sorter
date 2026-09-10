package com.screensort.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.screensort.app.data.local.CategoryCount
import com.screensort.app.data.local.ScreenshotEntity
import com.screensort.app.data.local.ScreenshotGridItem
import com.screensort.app.data.local.UserCategoryEntity
import com.screensort.app.data.repository.BackupManager
import com.screensort.app.data.repository.ScreenshotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: ScreenshotRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val scanProgress: StateFlow<Pair<Int, Int>?> = _scanProgress.asStateFlow()

    private val _clusteringStage = MutableStateFlow("Processing...")
    val clusteringStage: StateFlow<String> = _clusteringStage.asStateFlow()

    private val _selectedScreenshot = MutableStateFlow<ScreenshotEntity?>(null)
    val selectedScreenshot: StateFlow<ScreenshotEntity?> = _selectedScreenshot.asStateFlow()

    private var selectScreenshotJob: Job? = null
    private var currentSelectedId: Long? = null

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // User-defined categories flow
    val userCategories: StateFlow<List<UserCategoryEntity>> = repository.getAllUserCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Merged category counts flow: ensures every user-defined category is always visible (even with 0 screenshots)
    val categoryCounts: StateFlow<List<CategoryCount>> = combine(
        repository.getCategoryCounts(),
        repository.getAllUserCategories()
    ) { dbCounts, userCats ->
        val countsMap = dbCounts.associate { it.category to it.count }
        val userCatSet = userCats.map { it.name }.toSet()

        // 1. User categories first (with DB count or 0)
        val userCategoryCounts = userCats.map { uCat ->
            CategoryCount(
                category = uCat.name,
                count = countsMap[uCat.name] ?: 0
            )
        }

        // 2. Dynamic auto-discovered categories (not user-defined), keeping DB sort order
        val dynamicCategoryCounts = dbCounts.filter { it.category !in userCatSet }

        userCategoryCounts + dynamicCategoryCounts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery
        .debounce { query ->
            if (query.isBlank()) 0L else 300L
        }

    // Filtered screenshots flow returning lightweight projection items
    val screenshots: StateFlow<List<ScreenshotGridItem>> = combine(
        debouncedSearchQuery,
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        when {
            query.isNotBlank() -> repository.searchScreenshots(query.trim())
            category != null -> repository.getScreenshotsByCategory(category)
            else -> repository.getAllScreenshots()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        if (_searchQuery.value.isNotBlank()) {
            _searchQuery.value = ""
        }
    }

    /**
     * Race-safe selection of a screenshot by ID.
     * Cancels any pending fetch job and validates that the fetched entity matches the latest selected ID.
     */
    fun selectScreenshot(id: Long?) {
        selectScreenshotJob?.cancel()
        currentSelectedId = id
        if (id == null) {
            _selectedScreenshot.value = null
            return
        }
        selectScreenshotJob = viewModelScope.launch {
            val entity = repository.getScreenshotById(id)
            if (currentSelectedId == id) {
                _selectedScreenshot.value = entity
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    /**
     * Scans device screenshots, runs 100% offline OCR, and auto-clusters.
     */
    fun scanDeviceScreenshots() {
        if (_isScanning.value) return

        viewModelScope.launch {
            _isScanning.value = true
            _scanProgress.value = null
            try {
                val count = repository.scanAndProcessDeviceScreenshots { current, total ->
                    _scanProgress.value = Pair(current, total)
                }
                _userMessage.value = if (count > 0) {
                    "Discovered & categorized $count new screenshots!"
                } else {
                    "No new screenshots found on device."
                }
            } catch (e: Exception) {
                _userMessage.value = "Error scanning: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
                _scanProgress.value = null
            }
        }
    }

    /**
     * Imports and processes user-selected images.
     */
    fun importImages(uris: List<Uri>) {
        if (_isScanning.value || uris.isEmpty()) return

        viewModelScope.launch {
            _isScanning.value = true
            _scanProgress.value = null
            try {
                val count = repository.processImportedImages(uris) { current, total ->
                    _scanProgress.value = Pair(current, total)
                }
                _userMessage.value = "Processed & categorized $count imported images!"
            } catch (e: Exception) {
                _userMessage.value = "Error processing images: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
                _scanProgress.value = null
            }
        }
    }

    /**
     * Re-analyzes all screenshots and dynamically reorganizes them.
     */
    fun reclusterAll() {
        if (_isScanning.value) return

        viewModelScope.launch {
            _isScanning.value = true
            _scanProgress.value = null
            _clusteringStage.value = "Starting clustering..."
            try {
                repository.reclusterAll { current, total, stage ->
                    _scanProgress.value = Pair(current, total)
                    _clusteringStage.value = stage
                }
                _userMessage.value = "Re-clustered all screenshots into broad categories!"
            } catch (e: Exception) {
                _userMessage.value = "Error re-clustering: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
                _scanProgress.value = null
                _clusteringStage.value = "Ready"
            }
        }
    }

    fun updateCategory(id: Long, newCategory: String) {
        viewModelScope.launch {
            repository.updateCategory(id, newCategory)
            // Update currently selected screenshot if opened
            _selectedScreenshot.value?.let { current ->
                if (current.id == id) {
                    _selectedScreenshot.value = current.copy(
                        category = newCategory,
                        isManuallyCategorized = true
                    )
                }
            }
        }
    }

    fun deleteScreenshot(screenshot: ScreenshotEntity) {
        viewModelScope.launch {
            repository.deleteScreenshot(screenshot)
            if (_selectedScreenshot.value?.id == screenshot.id) {
                _selectedScreenshot.value = null
            }
        }
    }

    fun createUserCategory(name: String, keywords: String) {
        if (_isScanning.value) {
            _userMessage.value = "Operation in progress, please wait."
            return
        }
        viewModelScope.launch {
            _isScanning.value = true
            _clusteringStage.value = "Sorting screenshots into $name..."
            try {
                repository.createUserCategory(name, keywords)
                _userMessage.value = "Category '$name' created and screenshots sorted!"
            } catch (e: Exception) {
                _userMessage.value = "Error creating category: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
                _clusteringStage.value = "Ready"
            }
        }
    }

    fun updateUserCategory(category: UserCategoryEntity, newName: String, newKeywords: String) {
        if (_isScanning.value) {
            _userMessage.value = "Operation in progress, please wait."
            return
        }
        viewModelScope.launch {
            _isScanning.value = true
            _clusteringStage.value = "Updating category '$newName'..."
            try {
                val oldName = category.name
                val cleanNewName = newName.trim()
                repository.updateUserCategory(category, cleanNewName, newKeywords)
                if (_selectedCategory.value == oldName) {
                    _selectedCategory.value = cleanNewName
                }
                _userMessage.value = "Updated category to '$cleanNewName'!"
            } catch (e: Exception) {
                _userMessage.value = "Error updating category: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
                _clusteringStage.value = "Ready"
            }
        }
    }

    fun deleteUserCategory(category: UserCategoryEntity) {
        if (_isScanning.value) {
            _userMessage.value = "Operation in progress, please wait."
            return
        }
        viewModelScope.launch {
            _isScanning.value = true
            _clusteringStage.value = "Reorganizing screenshots after deleting '${category.name}'..."
            try {
                val deletedName = category.name
                repository.deleteUserCategory(category)
                if (_selectedCategory.value == deletedName) {
                    _selectedCategory.value = null
                }
                _userMessage.value = "Deleted category '$deletedName'. Screenshots reorganized!"
            } catch (e: Exception) {
                _userMessage.value = "Error deleting category: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
                _clusteringStage.value = "Ready"
            }
        }
    }

    /**
     * Exports all screenshot metadata and user categories as a JSON file to the destination URI.
     */
    fun exportBackup(context: Context, destinationUri: Uri) {
        if (_isScanning.value) {
            _userMessage.value = "Please wait for current operation to finish."
            return
        }
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backup = repository.exportBackupData()
                val jsonString = BackupManager.serializeToJson(backup)

                appContext.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Unable to write to destination file.")

                _userMessage.value = "Backup exported: ${backup.screenshots.size} screenshots, ${backup.userCategories.size} categories!"
            } catch (e: Exception) {
                _userMessage.value = "Failed to export backup: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    /**
     * Imports screenshot metadata and user categories from a JSON backup file,
     * merging non-destructively with existing data.
     */
    fun importBackup(context: Context, sourceUri: Uri) {
        if (_isScanning.value) {
            _userMessage.value = "Please wait for current operation to finish."
            return
        }
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _clusteringStage.value = "Restoring backup data..."
            try {
                // Defensive size check: prevent OutOfMemoryError on gigantic or non-backup files
                appContext.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                    if (pfd.statSize > MAX_BACKUP_SIZE_BYTES) {
                        throw IllegalStateException("File exceeds 25MB limit. Please select a valid ScreenSort backup file.")
                    }
                }

                val jsonString = appContext.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                } ?: throw IllegalStateException("Unable to read backup file.")

                val backup = BackupManager.deserializeFromJson(jsonString)
                val result = repository.importBackupData(backup)

                _userMessage.value = "Backup restored: ${result.newScreenshotsCount} added, ${result.updatedScreenshotsCount} updated, ${result.newCategoriesCount} categories added!"
            } catch (e: Exception) {
                _userMessage.value = "Failed to import backup: ${e.localizedMessage ?: "Invalid backup file"}"
            } finally {
                _isScanning.value = false
                _clusteringStage.value = "Ready"
            }
        }
    }

    companion object {
        private const val MAX_BACKUP_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB safety limit
    }

    class Factory(private val repository: ScreenshotRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
