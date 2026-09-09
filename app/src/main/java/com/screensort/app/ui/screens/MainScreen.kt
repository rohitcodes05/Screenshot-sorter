package com.screensort.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import com.screensort.app.data.local.UserCategoryEntity
import com.screensort.app.ui.components.CreateCategoryDialog
import com.screensort.app.ui.components.DeleteCategoryConfirmationDialog
import com.screensort.app.ui.components.EditCategoryDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screensort.app.ui.components.DynamicCategoryChips
import com.screensort.app.ui.components.ScreenshotCard
import com.screensort.app.ui.components.ScreenshotDetailDialog
import com.screensort.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    val totalCount = categoryCounts.sumOf { it.count }

    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<UserCategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<UserCategoryEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()

    LaunchedEffect(selectedCategory) {
        gridState.scrollToItem(0)
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
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
                                    Icons.Default.Lock,
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
                            Icons.Default.AutoAwesome,
                            contentDescription = "Re-cluster Categories",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Device")
                }

                OutlinedButton(
                    onClick = onTriggerImport,
                    enabled = !isScanning,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import")
                }
            }

            // Category Filter Chips (User categories marked with ★)
            DynamicCategoryChips(
                categories = categoryCounts,
                totalCount = totalCount,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) },
                userCategories = userCategories,
                onEditCategory = { categoryToEdit = it },
                onDeleteCategory = { categoryToDelete = it },
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // Screenshots Grid or Empty State
            if (screenshots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val emptyTitle = when {
                            searchQuery.isNotEmpty() -> "No matches found"
                            selectedCategory != null -> "No Screenshots in \"$selectedCategory\" Yet"
                            else -> "No Screenshots Organized Yet"
                        }
                        val emptyDescription = when {
                            searchQuery.isNotEmpty() -> "Try searching for a different keyword or clear the search bar."
                            selectedCategory != null -> "Screenshots matching \"$selectedCategory\" (or its keywords) will appear here once scanned or imported."
                            else -> "Tap \"Scan Device\" to automatically discover screenshots, or \"Import\" to choose images. The app will extract text and invent categories dynamically!"
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
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(screenshots, key = { it.id }) { item ->
                        ScreenshotCard(
                            screenshot = item,
                            onClick = { viewModel.selectScreenshot(item.id) }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedScreenshot?.let { item ->
        ScreenshotDetailDialog(
            screenshot = item,
            onDismiss = { viewModel.selectScreenshot(null) },
            onRenameCategory = { id, newName -> viewModel.updateCategory(id, newName) },
            onDelete = { viewModel.deleteScreenshot(it) }
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = clusteringStage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    scanProgress?.let { (cur, total) ->
                        Text(
                            text = "$cur / $total",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (scanProgress != null) {
                    val (cur, total) = scanProgress!!
                    val progress = if (total > 0) cur.toFloat() / total.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
