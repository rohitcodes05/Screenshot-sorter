package com.screensort.app.domain

import java.util.Locale

/**
 * Normalizes raw OCR text into meaningful tokens and N-grams
 * for on-device clustering and topic discovery.
 */
object TextPreprocessor {

    // Regex to remove URLs, email addresses, and phone numbers/timestamps
    private val URL_REGEX = Regex("https?://\\S+|www\\.\\S+")
    private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val NON_WORD_REGEX = Regex("[^a-zA-Z0-9\\s+#._-]")
    private val MULTI_SPACE_REGEX = Regex("\\s+")

    /**
     * Cleans the raw text and returns a list of informative lowercase tokens.
     */
    fun cleanAndTokenize(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()

        val sanitized = rawText
            .replace(URL_REGEX, " ")
            .replace(EMAIL_REGEX, " ")
            .replace(NON_WORD_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
            .lowercase(Locale.ROOT)

        val rawTokens = sanitized.split(" ")
        val validTokens = mutableListOf<String>()

        for (token in rawTokens) {
            val trimmed = token.trim { it <= ' ' || it == '.' || it == ',' || it == '-' || it == '_' }
            // Filter short tokens, pure numbers, and stopwords
            if (trimmed.length >= 3 && !trimmed.all { it.isDigit() } && trimmed !in StopWords.SET) {
                validTokens.add(stem(trimmed))
            }
        }

        return validTokens
    }

    private fun stem(word: String): String {
        return when {
            word.endsWith("ous") || word.endsWith("us") || word.endsWith("is") || word.endsWith("ss") -> word
            word.endsWith("ies") && word.length > 4 -> word.dropLast(3) + "y"
            word.endsWith("es") && word.length > 4 -> word.dropLast(2)
            word.endsWith("s") && word.length > 3 -> word.dropLast(1)
            else -> word
        }
    }

    /**
     * Fuses OCR extracted text with visual concept labels detected by ML Kit Image Labeling.
     * Visual labels are weighted/repeated to act as high-level semantic anchors.
     */
    fun extractMultimodalFeatures(rawText: String, visualLabels: List<String>): List<String> {
        val features = mutableListOf<String>()

        // 1. Process OCR text tokens and n-grams
        val textTokens = cleanAndTokenize(rawText)
        features.addAll(textTokens)

        for (i in 0 until textTokens.size - 1) {
            val t1 = textTokens[i]
            val t2 = textTokens[i + 1]
            if (t1 != t2) {
                features.add("$t1 $t2")
            }
        }

        // 2. Process and weight visual labels (repeated 2x to reinforce visual anchors)
        for (label in visualLabels) {
            val cleanLabel = stem(label.trim().lowercase(Locale.ROOT))
            if (cleanLabel.length >= 3 && cleanLabel !in StopWords.SET) {
                features.add(cleanLabel)
                features.add(cleanLabel) // weight boost

                // Also pair visual label with top text tokens if available
                if (textTokens.isNotEmpty()) {
                    features.add("$cleanLabel ${textTokens[0]}")
                }
            }
        }

        return features
    }
}
