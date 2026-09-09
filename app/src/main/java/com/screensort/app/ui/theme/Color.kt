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

// Palette for dynamic category badge colors
private val CategoryPalette = listOf(
    Color(0xFF6750A4), // Purple
    Color(0xFF006C50), // Teal
    Color(0xFF984061), // Magenta
    Color(0xFF006494), // Blue
    Color(0xFF904D00), // Amber / Orange
    Color(0xFF386A20), // Forest Green
    Color(0xFF705574), // Mauve
    Color(0xFF5B5D72), // Slate
    Color(0xFF8B4A60), // Rose
    Color(0xFF006972)  // Cyan
)

/**
 * Deterministically generates a consistent and pleasant accent color
 * for any dynamically discovered category name.
 */
fun getCategoryColor(categoryName: String): Color {
    if (categoryName.isBlank() || categoryName.equals("Unclassified", ignoreCase = true)) {
        return Color(0xFF757575)
    }
    val hash = abs(categoryName.hashCode())
    return CategoryPalette[hash % CategoryPalette.size]
}
