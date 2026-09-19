package com.screensort.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6750A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

val SurfaceLight = Color(0xFFFBF8FD)
val SurfaceDark = Color(0xFF141218)

val GoldAccent = Color(0xFFF59E0B)

// Palette for dynamic category badge colors - refined for high contrast and elegance
private val CategoryPalette = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF0D9488), // Teal
    Color(0xFFE11D48), // Rose
    Color(0xFF2563EB), // Blue
    Color(0xFFD97706), // Amber
    Color(0xFF16A34A), // Emerald Green
    Color(0xFF9333EA), // Purple
    Color(0xFF0284C7), // Sky Blue
    Color(0xFFEA580C), // Orange
    Color(0xFF475569)  // Slate
)

/**
 * Deterministically generates a consistent and pleasant accent color
 * for any dynamically discovered category name.
 */
fun getCategoryColor(categoryName: String): Color {
    if (categoryName.isBlank() || categoryName.equals("Unclassified", ignoreCase = true)) {
        return Color(0xFF64748B)
    }
    val hash = abs(categoryName.hashCode())
    return CategoryPalette[hash % CategoryPalette.size]
}
