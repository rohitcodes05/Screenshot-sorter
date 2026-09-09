package com.screensort.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.screensort.app.data.local.CategoryCount
import com.screensort.app.data.local.UserCategoryEntity
import com.screensort.app.ui.theme.getCategoryColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicCategoryChips(
    categories: List<CategoryCount>,
    totalCount: Int,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    userCategories: List<UserCategoryEntity> = emptyList(),
    onEditCategory: (UserCategoryEntity) -> Unit = {},
    onDeleteCategory: (UserCategoryEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userCatMap = remember(userCategories) { userCategories.associateBy { it.name } }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" filter chip
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All ($totalCount)") },
                colors = FilterChipDefaults.filterChipColors()
            )
        }

        // Category chips
        items(categories, key = { it.category }) { item ->
            val isSelected = selectedCategory == item.category
            val userCat = userCatMap[item.category]
            val catColor = getCategoryColor(item.category)

            if (userCat != null) {
                // User-defined custom category: supports tap to select, long-press to delete, and edit icon
                val shape = RoundedCornerShape(8.dp)
                Surface(
                    shape = shape,
                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .clip(shape)
                        .combinedClickable(
                            onClick = { onCategorySelected(item.category) },
                            onLongClick = { onDeleteCategory(userCat) }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "★ ${item.category}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { onEditCategory(userCat) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Category",
                                modifier = Modifier.size(13.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Badge(
                            containerColor = if (isSelected) catColor else catColor.copy(alpha = 0.2f),
                            contentColor = if (isSelected) Color.White else catColor
                        ) {
                            Text(item.count.toString(), modifier = Modifier.padding(horizontal = 2.dp))
                        }
                    }
                }
            } else {
                // Auto-discovered category chip
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(item.category) },
                    label = { Text(item.category) },
                    trailingIcon = {
                        Badge(
                            containerColor = if (isSelected) catColor else catColor.copy(alpha = 0.2f),
                            contentColor = if (isSelected) Color.White else catColor
                        ) {
                            Text(item.count.toString(), modifier = Modifier.padding(horizontal = 2.dp))
                        }
                    }
                )
            }
        }
    }
}

