package com.screensort.app.domain

import com.screensort.app.data.local.UserCategoryEntity
import java.util.Locale

/**
 * Matches a screenshot's OCR text and visual labels against user-defined categories.
 * Ensures that screenshots matching user-defined categories are routed directly
 * into them without generating random auto-clusters.
 */
object UserCategoryMatcher {

    data class MatchResult(
        val categoryName: String,
        val matchScore: Float,
        val matchedKeywords: List<String>
    )

    data class PreparedCategory(
        val categoryName: String,
        val catNameLower: String,
        val allTargetKeywords: List<String>
    )

    /**
     * Pre-computes and caches tokenized keyword sets for user categories.
     * Call once before matching across large batches of screenshots.
     */
    fun prepareCategories(userCategories: List<UserCategoryEntity>): List<PreparedCategory> {
        return userCategories.map { userCat ->
            val catName = userCat.name.trim()
            val catNameLower = catName.lowercase(Locale.ROOT)
            val nameKeywords = TextPreprocessor.cleanAndTokenize(catName)
            val customKeywords = userCat.getKeywordList()
            val allTargetKeywords = (nameKeywords + customKeywords + catNameLower).distinct()
            PreparedCategory(
                categoryName = catName,
                catNameLower = catNameLower,
                allTargetKeywords = allTargetKeywords
            )
        }
    }

    /**
     * Evaluates a screenshot against all user categories using pre-computed keyword sets.
     */
    fun findBestMatchPrepared(
        extractedText: String,
        visualLabels: List<String>,
        preparedCategories: List<PreparedCategory>
    ): MatchResult? {
        if (preparedCategories.isEmpty()) return null

        val textTokens = TextPreprocessor.cleanAndTokenize(extractedText).toSet()
        val textLower = extractedText.lowercase(Locale.ROOT)
        val visualLabelsLower = visualLabels.map { it.trim().lowercase(Locale.ROOT) }

        var bestMatch: MatchResult? = null
        var highestScore = 0.0f

        for (userCat in preparedCategories) {
            val catName = userCat.categoryName
            val catNameLower = userCat.catNameLower
            val allTargetKeywords = userCat.allTargetKeywords

            val matchedKeywords = mutableListOf<String>()
            var score = 0.0f

            // 1. Check visual labels (high weight: +2.5 per match)
            for (vLabel in visualLabelsLower) {
                if (allTargetKeywords.any { kw -> vLabel == kw || vLabel.contains(kw) || kw.contains(vLabel) }) {
                    score += 2.5f
                    matchedKeywords.add("visual:$vLabel")
                }
            }

            // 2. Check full category name substring match in text (+2.0)
            if (textLower.contains(catNameLower) && catNameLower.length >= 3) {
                score += 2.0f
                matchedKeywords.add(catNameLower)
            }

            // 3. Check individual keyword token hits in OCR text (+1.0 each)
            for (keyword in allTargetKeywords) {
                val cleanKw = keyword.trim()
                if (cleanKw.length >= 3) {
                    if (textTokens.contains(cleanKw) || textLower.contains(cleanKw)) {
                        score += 1.0f
                        if (cleanKw !in matchedKeywords) {
                            matchedKeywords.add(cleanKw)
                        }
                    }
                }
            }

            // Threshold: at least one strong keyword or visual match (score >= 1.0)
            if (score >= 1.0f && score > highestScore) {
                highestScore = score
                bestMatch = MatchResult(
                    categoryName = catName,
                    matchScore = score,
                    matchedKeywords = matchedKeywords.take(4)
                )
            }
        }

        return bestMatch
    }

    /**
     * Evaluates a screenshot against all user categories.
     * Returns the best matching user category, or null if no confident match.
     */
    fun findBestMatch(
        extractedText: String,
        visualLabels: List<String>,
        userCategories: List<UserCategoryEntity>
    ): MatchResult? {
        if (userCategories.isEmpty()) return null
        return findBestMatchPrepared(extractedText, visualLabels, prepareCategories(userCategories))
    }
}
