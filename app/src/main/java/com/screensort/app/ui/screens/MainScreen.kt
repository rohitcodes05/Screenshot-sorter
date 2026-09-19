package com.screensort.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screensort.app.data.local.UserCategoryEntity
import com.screensort.app.ui.components.CreateCategoryDialog
import com.screensort.app.ui.components.DeleteCategoryConfirmationDialog
import com.screensort.app.ui.components.DynamicCategoryChips
import com.screensort.app.ui.components.EditCategoryDialog
import com.screensort.app.ui.components.ScreenshotCard
import com.screensort.app.ui.components.ScreenshotGalleryViewer
import com.screensort.app.ui.icons.AppIcons
import com.screensort.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onTriggerScan: () -> Unit,
    onTriggerImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val selectedScreenshot by viewModel.selectedScreenshot.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val categoryCounts by viewModel.categoryCounts.collectAsStateWithLifecycle()
    val userCategories by viewModel.userCategories.collectAsStateWithLifecycle()
    val screenshots by viewModel.screenshots.collectAsStateWithLifecycle()
    val selectedScreenshotIds by viewModel.selectedScreenshotIds.collectAsStateWithLifecycle()
    val isSelectionMode = selectedScreenshotIds.isNotEmpty()

    val totalCount = categoryCounts.sumOf { it.count }

    var activeViewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<UserCategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<UserCategoryEntity?>(null) }
    var showMoveCategoryMenu by remember { mutableStateOf(false) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val staggeredGridState = rememberLazyStaggeredGridState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showOptionsMenu by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(context, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(context, uri)
        }
    }

    // Dismiss selection mode on system back
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    LaunchedEffect(selectedCategory) {
        staggeredGridState.scrollToItem(0)
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    // Contextual Multi-Select Top Bar
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Selection"
                                )
                            }
                        },
                        title = {
                            Text(
                                text = "${selectedScreenshotIds.size} selected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        actions = {
                            // Select All / Deselect All Toggle
                            TextButton(
                                onClick = {
                                    if (selectedScreenshotIds.size == screenshots.size) {
                                        viewModel.clearSelection()
                                    } else {
                                        viewModel.selectAll(screenshots.map { it.id })
                                    }
                                }
                            ) {
                                Text(
                                    text = if (selectedScreenshotIds.size == screenshots.size) "Deselect" else "Select All",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Move to Category Action
                            Box {
                                TextButton(onClick = { showMoveCategoryMenu = true }) {
                                    Text("Move", fontWeight = FontWeight.SemiBold)
                                }

                                DropdownMenu(
                                    expanded = showMoveCategoryMenu,
                                    onDismissRequest = { showMoveCategoryMenu = false }
                                ) {
                                    Text(
                                        text = "Move ${selectedScreenshotIds.size} to...",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                    HorizontalDivider()
                                    val allCategoryNames = (userCategories.map { it.name } + categoryCounts.map { it.category }).distinct()
                                    allCategoryNames.forEach { catName ->
                                        DropdownMenuItem(
                                            text = { Text(catName) },
                                            onClick = {
                                                showMoveCategoryMenu = false
                                                viewModel.moveSelectedToCategory(catName)
                                            }
                                        )
                                    }
                                }
                            }

                            // Bulk Delete Action
                            IconButton(onClick = { showBulkDeleteConfirmDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                } else {
                    // Standard Top Bar
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ScreenSort",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                // 100% Offline Badge
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1B5E20).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            AppIcons.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "100% Offline",
                                            color = Color(0xFF2E7D32),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showCreateCategoryDialog = true },
                                enabled = !isScanning
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Custom Category",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = { viewModel.reclusterAll() },
                                enabled = !isScanning && totalCount > 0
                            ) {
                                Icon(
                                    AppIcons.AutoAwesome,
                                    contentDescription = "Re-cluster Categories",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { showOptionsMenu = true },
                                    enabled = !isScanning
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                DropdownMenu(
                                    expanded = showOptionsMenu,
                                    onDismissRequest = { showOptionsMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Export Backup") },
                                        leadingIcon = {
                                            Icon(AppIcons.FileUpload, contentDescription = null)
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                            exportLauncher.launch("ScreenSort_Backup_$timeStamp.json")
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Import Backup") },
                                        leadingIcon = {
                                            Icon(AppIcons.FileDownload, contentDescription = null)
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                        }
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                    DropdownMenuItem(
                                        text = { Text("Re-cluster All") },
                                        leadingIcon = {
                                            Icon(AppIcons.AutoAwesome, contentDescription = null)
                                        },
                                        enabled = totalCount > 0,
                                        onClick = {
                                            showOptionsMenu = false
                                            viewModel.reclusterAll()
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search text across screenshots...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Scanning Progress Section (Isolated to prevent recomposition of MainScreen)
                ScanningProgressSection(viewModel = viewModel)

                // Action Buttons: Scan Device & Import
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTriggerScan,
                        enabled = !isScanning,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(AppIcons.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Device")
                    }

                    OutlinedButton(
                        onClick = onTriggerImport,
                        enabled = !isScanning,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(AppIcons.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import")
                    }
                }

                // Category Filter Pills
                DynamicCategoryChips(
                    categories = categoryCounts,
                    totalCount = totalCount,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) },
                    userCategories = userCategories,
                    onEditCategory = { categoryToEdit = it },
                    onDeleteCategory = { categoryToDelete = it },
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Asymmetric Staggered Screenshots Grid or Empty State
                AnimatedContent(
                    targetState = screenshots.isEmpty(),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "GridContentAnimation"
                ) { isEmpty ->
                    if (isEmpty) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    modifier = Modifier.size(88.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            AppIcons.PhotoLibrary,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                val emptyTitle = when {
                                    searchQuery.isNotEmpty() -> "No matches found"
                                    selectedCategory != null -> "No Screenshots in \"$selectedCategory\""
                                    else -> "No Screenshots Organized Yet"
                                }
                                val emptyDescription = when {
                                    searchQuery.isNotEmpty() -> "No screenshot matched \"$searchQuery\". Check your spelling or try another keyword."
                                    selectedCategory != null -> "Screenshots matching \"$selectedCategory\" will automatically route here once scanned or imported."
                                    else -> "Tap \"Scan Device\" to discover screenshots, or \"Import\" to select photos. The app will organize them into topics automatically!"
                                }

                                Text(
                                    text = emptyTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = emptyDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )

                                if (searchQuery.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.setSearchQuery("") },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Clear Search")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            state = staggeredGridState,
                            contentPadding = PaddingValues(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalItemSpacing = 10.dp,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(screenshots, key = { _, item -> item.id }) { index, item ->
                                val isSelected = item.id in selectedScreenshotIds
                                ScreenshotCard(
                                    screenshot = item,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleSelection(item.id)
                                        } else {
                                            activeViewerIndex = index
                                            viewModel.selectScreenshot(item.id)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggleSelection(item.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bulk Delete Confirmation Dialog
        if (showBulkDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteConfirmDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        text = "Remove ${selectedScreenshotIds.size} Screenshots?",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to remove ${selectedScreenshotIds.size} selected screenshots from ScreenSort?\n\nNote: This only removes the screenshots and their OCR data from ScreenSort. The original photo files in your phone's storage will NOT be deleted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBulkDeleteConfirmDialog = false
                            viewModel.deleteSelected()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Remove All")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showBulkDeleteConfirmDialog = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Create User Category Dialog
        if (showCreateCategoryDialog) {
            CreateCategoryDialog(
                onDismiss = { showCreateCategoryDialog = false },
                onConfirm = { name, keywords ->
                    viewModel.createUserCategory(name, keywords)
                    showCreateCategoryDialog = false
                }
            )
        }

        // Edit User Category Dialog
        categoryToEdit?.let { category ->
            EditCategoryDialog(
                category = category,
                onDismiss = { categoryToEdit = null },
                onConfirm = { newName, newKeywords ->
                    viewModel.updateUserCategory(category, newName, newKeywords)
                    categoryToEdit = null
                }
            )
        }

        // Delete User Category Dialog
        categoryToDelete?.let { category ->
            DeleteCategoryConfirmationDialog(
                category = category,
                onDismiss = { categoryToDelete = null },
                onConfirm = {
                    viewModel.deleteUserCategory(category)
                    categoryToDelete = null
                }
            )
        }

        // Full-screen Gallery Viewer Overlay
        activeViewerIndex?.let { index ->
            if (screenshots.isNotEmpty()) {
                val safeIndex = index.coerceIn(0, screenshots.size - 1)
                ScreenshotGalleryViewer(
                    screenshots = screenshots,
                    initialIndex = safeIndex,
                    selectedScreenshotEntity = selectedScreenshot,
                    onPageChanged = { newPage ->
                        activeViewerIndex = newPage
                        if (newPage in screenshots.indices) {
                            viewModel.selectScreenshot(screenshots[newPage].id)
                        }
                    },
                    onDismiss = {
                        activeViewerIndex = null
                        viewModel.selectScreenshot(null)
                    },
                    onRenameCategory = { id, newName -> viewModel.updateCategory(id, newName) },
                    onDelete = { entity ->
                        viewModel.deleteScreenshot(entity)
                        if (screenshots.size <= 1) {
                            activeViewerIndex = null
                            viewModel.selectScreenshot(null)
                        } else if (safeIndex >= screenshots.size - 1) {
                            activeViewerIndex = screenshots.size - 2
                        }
                    }
                )
            } else {
                activeViewerIndex = null
            }
        }
    }
}

/**
 * Isolated progress composable so that high-frequency progress updates
 * (1/2000, 2/2000...) during scanning and clustering only recompose this component,
 * without triggering full recompositions of the MainScreen, TopAppBar, category chips, or screenshot grid.
 */
@Composable
private fun ScanningProgressSection(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val clusteringStage by viewModel.clusteringStage.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = isScanning,
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            AppIcons.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = clusteringStage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    scanProgress?.let { (cur, total) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$cur / $total",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (scanProgress != null) {
                    val (cur, total) = scanProgress!!
                    val rawProgress = if (total > 0) cur.toFloat() / total.toFloat() else 0f
                    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = rawProgress,
                        animationSpec = androidx.compose.animation.core.tween(150),
                        label = "ProgressBarAnimation"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}
