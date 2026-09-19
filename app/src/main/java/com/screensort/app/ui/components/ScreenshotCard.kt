package com.screensort.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.screensort.app.data.local.ScreenshotGridItem
import com.screensort.app.ui.theme.getCategoryColor
import com.screensort.app.util.DateTimeUtils

/**
 * Modern tactile "Receipt / Ticket" style card for screenshots.
 * - Subtle 1dp border, minimal corner radius (6dp).
 * - Asymmetric natural image height scaling for staggered grid layout.
 * - Micro category badge stamp at top-right corner.
 * - Hairline divider separating image and bottom monospace OCR preview snippet.
 * - Multi-select checkbox indicator and active border highlighting.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenshotCard(
    screenshot: ScreenshotGridItem,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryColor = getCategoryColor(screenshot.category)
    val context = LocalContext.current
    val imageRequest = remember(screenshot.uriString) {
        ImageRequest.Builder(context)
            .data(screenshot.uriString)
            .size(400, 520)
            .precision(Precision.INEXACT)
            .crossfade(true)
            .build()
    }

    val formattedDate = remember(screenshot.dateAdded) {
        DateTimeUtils.formatDate(screenshot.dateAdded)
    }

    // Deterministic organic aspect ratio variation for natural staggered ticket flow
    val cardAspectRatio = remember(screenshot.id) {
        when ((screenshot.id % 4L).toInt()) {
            0 -> 0.92f
            1 -> 1.18f
            2 -> 1.05f
            else -> 1.28f
        }
    }

    val cardShape = RoundedCornerShape(6.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Ticket image preview container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(cardAspectRatio)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = screenshot.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )

                // Selection overlay scrim
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    )
                }

                // Micro category badge stamp (Top-Right)
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.75f)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                ) {
                    Text(
                        text = screenshot.category.uppercase(),
                        color = categoryColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Checkbox indicator (Top-Left)
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(7.dp)
                    ) {
                        if (isSelected) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.45f),
                                border = BorderStroke(1.5.dp, Color.White),
                                modifier = Modifier.size(24.dp)
                            ) {}
                        }
                    }
                }
            }

            // Hairline divider
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )

            // Monospace OCR snippet preview section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                val cleanTitle = remember(screenshot.displayName) {
                    screenshot.displayName.substringBeforeLast(".")
                }
                Text(
                    text = cleanTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                val docSnippet = remember(screenshot.id, formattedDate) {
                    "> REF#${screenshot.id.toString().padStart(4, '0')} // $formattedDate"
                }
                Text(
                    text = docSnippet,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    letterSpacing = 0.2.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
