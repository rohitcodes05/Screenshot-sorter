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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screensort.app.data.local.CategoryCount
import com.screensort.app.data.local.UserCategoryEntity
import com.screensort.app.ui.theme.GoldAccent
import com.screensort.app.ui.theme.getCategoryColor

/**
 * Tactile category filter pills with subtle active borders and haptic feedback.
 */
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
    val pillShape = RoundedCornerShape(20.dp)
    val haptic = LocalHapticFeedback.current

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All" filter pill
        item {
            val isAllSelected = selectedCategory == null
            Surface(
                shape = pillShape,
                color = if (isAllSelected) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = if (isAllSelected) 1.5.dp else 1.dp,
                    color = if (isAllSelected) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }
                ),
                shadowElevation = if (isAllSelected) 1.dp else 0.dp,
                modifier = Modifier
                    .height(34.dp)
                    .clip(pillShape)
                    .combinedClickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCategorySelected(null)
                        }
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAllSelected) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ) {
                        Text(
                            text = totalCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // Category pills
        items(categories, key = { it.category }) { item ->
            val isSelected = selectedCategory == item.category
            val userCat = userCatMap[item.category]
            val catColor = getCategoryColor(item.category)
            val onLongClickAction: (() -> Unit)? = userCat?.let { cat ->
                {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteCategory(cat)
                }
            }

            Surface(
                shape = pillShape,
                color = if (isSelected) {
                    catColor.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) {
                        catColor
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }
                ),
                shadowElevation = if (isSelected) 1.dp else 0.dp,
                modifier = Modifier
                    .height(34.dp)
                    .clip(pillShape)
                    .combinedClickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCategorySelected(item.category)
                        },
                        onLongClick = onLongClickAction
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 10.dp, end = 8.dp)
                ) {
                    if (userCat != null) {
                        Text(
                            text = "★",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (userCat != null) {
                        Spacer(modifier = Modifier.width(3.dp))
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEditCategory(userCat)
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Category",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) catColor else catColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else catColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
