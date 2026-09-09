package com.screensort.app

import com.screensort.app.data.local.UserCategoryEntity
import com.screensort.app.domain.UserCategoryMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UserCategoryMatcherTest {

    @Test
    fun testMatchesOcrTextAgainstUserCategory() {
        val userCategories = listOf(
            UserCategoryEntity(id = 1, name = "Movies", keywords = "film, cinema, netflix, director, actor"),
            UserCategoryEntity(id = 2, name = "Software", keywords = "github, code, python, docker, git"),
            UserCategoryEntity(id = 3, name = "Finance", keywords = "receipt, invoice, bank, payment, tax")
        )

        val ocrSample = "Inception film directed by Christopher Nolan. Stream on Netflix in 4K."
        val match = UserCategoryMatcher.findBestMatch(ocrSample, emptyList(), userCategories)

        assertNotNull(match)
        assertEquals("Movies", match!!.categoryName)
    }

    @Test
    fun testMatchesVisualLabelsAgainstUserCategory() {
        val userCategories = listOf(
            UserCategoryEntity(id = 1, name = "Food & Dining", keywords = "food, dish, recipe, restaurant"),
            UserCategoryEntity(id = 2, name = "Documents", keywords = "document, text, paper, contract")
        )

        // Sparse OCR text, but ML Kit detected visual label "Food"
        val ocrSample = "Table 4 special"
        val visualLabels = listOf("Food", "Dish", "Cuisine")

        val match = UserCategoryMatcher.findBestMatch(ocrSample, visualLabels, userCategories)

        assertNotNull(match)
        assertEquals("Food & Dining", match!!.categoryName)
    }

    @Test
    fun testUnmatchedReturnsNull() {
        val userCategories = listOf(
            UserCategoryEntity(id = 1, name = "Movies", keywords = "film, cinema, netflix"),
            UserCategoryEntity(id = 2, name = "Finance", keywords = "receipt, invoice, bank")
        )

        val ocrSample = "Git commit -m 'Fixed bug in compiler' and pushed to main"
        val visualLabels = listOf("Software", "Screen")

        val match = UserCategoryMatcher.findBestMatch(ocrSample, visualLabels, userCategories)

        // Since user did NOT define a Software category, it should return null
        // so it can fall back to auto-clustering rather than forcing into Movies or Finance!
        assertNull(match)
    }

    @Test
    fun testPreparedCategoryMatchingProducesIdenticalResults() {
        val userCategories = listOf(
            UserCategoryEntity(id = 1, name = "Movies", keywords = "film, cinema, netflix"),
            UserCategoryEntity(id = 2, name = "Finance", keywords = "receipt, invoice, bank")
        )

        val prepared = UserCategoryMatcher.prepareCategories(userCategories)
        assertEquals(2, prepared.size)

        val ocrSample = "Order confirmation invoice #12345 bank transaction complete"
        val matchDirect = UserCategoryMatcher.findBestMatch(ocrSample, emptyList(), userCategories)
        val matchPrepared = UserCategoryMatcher.findBestMatchPrepared(ocrSample, emptyList(), prepared)

        assertNotNull(matchDirect)
        assertNotNull(matchPrepared)
        assertEquals("Finance", matchPrepared!!.categoryName)
        assertEquals(matchDirect!!.categoryName, matchPrepared.categoryName)
        assertEquals(matchDirect.matchScore, matchPrepared.matchScore, 0.001f)
    }

    @Test
    fun testUpdatedCategoryMatchesNewKeywords() {
        val original = UserCategoryEntity(id = 1, name = "Movies", keywords = "film, cinema")
        val sample = "Playing Elden Ring and defeating the final boss in coop gaming"

        // Initially does not match "Movies"
        val initialMatch = UserCategoryMatcher.findBestMatch(sample, emptyList(), listOf(original))
        assertNull(initialMatch)

        // After user edits/renames category to "Gaming" with gaming keywords:
        val updated = original.copy(name = "Gaming", keywords = "game, gaming, boss, steam, playstation")
        val afterUpdateMatch = UserCategoryMatcher.findBestMatch(sample, emptyList(), listOf(updated))

        assertNotNull(afterUpdateMatch)
        assertEquals("Gaming", afterUpdateMatch!!.categoryName)
    }

    @Test
    fun testDeletedCategoryNoLongerMatches() {
        val movies = UserCategoryEntity(id = 1, name = "Movies", keywords = "film, cinema, netflix")
        val finance = UserCategoryEntity(id = 2, name = "Finance", keywords = "receipt, invoice, bank")
        val activeCategories = mutableListOf(movies, finance)

        val movieSample = "Netflix original film streaming now in 4K HDR"

        // With Movies category active: matches "Movies"
        val matchBefore = UserCategoryMatcher.findBestMatch(movieSample, emptyList(), activeCategories)
        assertNotNull(matchBefore)
        assertEquals("Movies", matchBefore!!.categoryName)

        // After user deletes "Movies" category:
        activeCategories.remove(movies)
        val matchAfter = UserCategoryMatcher.findBestMatch(movieSample, emptyList(), activeCategories)

        // Must return null so it drops into dynamic topic engine / Others!
        assertNull(matchAfter)
    }
}
