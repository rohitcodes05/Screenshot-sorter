package com.screensort.app

import com.screensort.app.data.repository.BackupManager
import com.screensort.app.data.repository.BackupPayload
import com.screensort.app.data.repository.ScreenshotBackupItem
import com.screensort.app.data.repository.UserCategoryBackupItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    @Test
    fun testSerializeAndDeserializeRoundTrip() {
        val categories = listOf(
            UserCategoryBackupItem(name = "Movies", keywords = "film, cinema, netflix", dateCreated = 1720000000L),
            UserCategoryBackupItem(name = "Finance", keywords = "receipt, invoice, tax", dateCreated = 1720001000L)
        )

        val screenshots = listOf(
            ScreenshotBackupItem(
                displayName = "Screenshot_1.png",
                dateAdded = 1725000000L,
                category = "Movies",
                dominantKeywords = "netflix, film",
                extractedText = "Watch Stranger Things on Netflix",
                visualLabels = "Television, Display",
                confidence = 0.88f,
                isManuallyCategorized = true,
                uriString = "content://media/external/images/media/101"
            ),
            ScreenshotBackupItem(
                displayName = "Screenshot_2.png",
                dateAdded = 1725002000L,
                category = "Receipts & Invoice",
                dominantKeywords = "total, balance",
                extractedText = "Total Amount Due: $45.00",
                visualLabels = "Receipt, Document",
                confidence = 0.92f,
                isManuallyCategorized = false,
                uriString = "content://media/external/images/media/102"
            )
        )

        val originalPayload = BackupPayload(
            version = 1,
            appVersion = "1.0.0",
            exportedAt = 1725005000L,
            userCategories = categories,
            screenshots = screenshots
        )

        val jsonString = BackupManager.serializeToJson(originalPayload)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("\"version\": 1"))
        assertTrue(jsonString.contains("Movies"))
        assertTrue(jsonString.contains("Screenshot_1.png"))

        val restoredPayload = BackupManager.deserializeFromJson(jsonString)
        assertEquals(originalPayload.version, restoredPayload.version)
        assertEquals(originalPayload.appVersion, restoredPayload.appVersion)
        assertEquals(originalPayload.exportedAt, restoredPayload.exportedAt)

        // Verify categories
        assertEquals(2, restoredPayload.userCategories.size)
        assertEquals("Movies", restoredPayload.userCategories[0].name)
        assertEquals("film, cinema, netflix", restoredPayload.userCategories[0].keywords)
        assertEquals(1720000000L, restoredPayload.userCategories[0].dateCreated)
        assertEquals("Finance", restoredPayload.userCategories[1].name)

        // Verify screenshots
        assertEquals(2, restoredPayload.screenshots.size)
        val shot1 = restoredPayload.screenshots[0]
        assertEquals("Screenshot_1.png", shot1.displayName)
        assertEquals("Movies", shot1.category)
        assertEquals("netflix, film", shot1.dominantKeywords)
        assertEquals("Watch Stranger Things on Netflix", shot1.extractedText)
        assertEquals("Television, Display", shot1.visualLabels)
        assertEquals(0.88f, shot1.confidence, 0.01f)
        assertTrue(shot1.isManuallyCategorized)
        assertEquals("content://media/external/images/media/101", shot1.uriString)

        val shot2 = restoredPayload.screenshots[1]
        assertEquals("Screenshot_2.png", shot2.displayName)
        assertEquals("Receipts & Invoice", shot2.category)
        assertFalse(shot2.isManuallyCategorized)
    }

    @Test
    fun testDeserializeHandlesEmptyOrMissingFieldsGracefully() {
        val minimalJson = "{}"
        val payload = BackupManager.deserializeFromJson(minimalJson)

        assertEquals(1, payload.version)
        assertEquals("1.0.0", payload.appVersion)
        assertTrue(payload.userCategories.isEmpty())
        assertTrue(payload.screenshots.isEmpty())
    }

    @Test
    fun testPartialScreenshotDataHandlesDefaults() {
        val json = """
            {
                "version": 1,
                "screenshots": [
                    {
                        "displayName": "Test_Shot.png"
                    }
                ]
            }
        """.trimIndent()

        val payload = BackupManager.deserializeFromJson(json)
        assertEquals(1, payload.screenshots.size)
        assertEquals("Test_Shot.png", payload.screenshots[0].displayName)
        assertEquals("Others", payload.screenshots[0].category)
        assertEquals("", payload.screenshots[0].extractedText)
        assertFalse(payload.screenshots[0].isManuallyCategorized)
    }

    @Test
    fun testMalformedJsonArrayElementsSkippedGracefully() {
        val malformedJson = """
            {
                "version": 1,
                "userCategories": [
                    null,
                    "invalid_string_entry",
                    { "name": "Valid Category", "keywords": "test" },
                    { "name": "   " }
                ],
                "screenshots": [
                    null,
                    12345,
                    { "displayName": "Valid_Shot.png", "category": "Valid Category" },
                    { "displayName": "" }
                ]
            }
        """.trimIndent()

        val payload = BackupManager.deserializeFromJson(malformedJson)

        // Valid category parsed, null/string/blank skipped
        assertEquals(1, payload.userCategories.size)
        assertEquals("Valid Category", payload.userCategories[0].name)

        // Valid screenshot parsed, null/int/empty name skipped
        assertEquals(1, payload.screenshots.size)
        assertEquals("Valid_Shot.png", payload.screenshots[0].displayName)
    }
}
