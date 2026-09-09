package com.screensort.app

import com.screensort.app.data.local.ScreenshotGridItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotGridItemTest {

    @Test
    fun testLightweightModelExcludesHeavyFields() {
        val nonSyntheticFields = ScreenshotGridItem::class.java.declaredFields
            .filter { !it.isSynthetic && !it.name.startsWith("$") }
            .map { it.name }
            .toSet()

        // Confirmed lightweight fields
        assertEquals(5, nonSyntheticFields.size)
        assertTrue(nonSyntheticFields.contains("id"))
        assertTrue(nonSyntheticFields.contains("uriString"))
        assertTrue(nonSyntheticFields.contains("displayName"))
        assertTrue(nonSyntheticFields.contains("dateAdded"))
        assertTrue(nonSyntheticFields.contains("category"))

        // Confirmed heavy fields are excluded
        assertFalse("extractedText must not be present in grid projection", nonSyntheticFields.contains("extractedText"))
        assertFalse("visualLabels must not be present in grid projection", nonSyntheticFields.contains("visualLabels"))
        assertFalse("dominantKeywords must not be present in grid projection", nonSyntheticFields.contains("dominantKeywords"))

        // Verify instantiation
        val item = ScreenshotGridItem(
            id = 1L,
            uriString = "content://media/123",
            displayName = "test.png",
            dateAdded = 1000L,
            category = "Receipts"
        )
        assertEquals(1L, item.id)
        assertEquals("content://media/123", item.uriString)
        assertEquals("test.png", item.displayName)
        assertEquals(1000L, item.dateAdded)
        assertEquals("Receipts", item.category)
    }
}
