package com.screensort.app.domain

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * High-performance on-device TF-IDF vectorizer.
 * Pruned to top salient terms per document for lightning-fast similarity computation
 * across thousands of screenshots.
 */
class TfidfVectorizer(
    private val maxFeaturesPerDoc: Int = 20
) {

    data class DocumentVector(
        val docId: String,
        val termWeights: Map<String, Float>
    )

    /**
     * Builds TF-IDF vectors for a collection of documents.
     * @param documents Map of documentId to list of feature tokens (unigrams + bigrams)
     */
    fun vectorize(documents: Map<String, List<String>>): Map<String, DocumentVector> {
        if (documents.isEmpty()) return emptyMap()

        val numDocs = documents.size.toFloat()

        // 1. Calculate Document Frequency (DF) for each term
        val documentFrequency = mutableMapOf<String, Int>()
        for ((_, tokens) in documents) {
            val uniqueTokens = tokens.toSet()
            for (token in uniqueTokens) {
                documentFrequency[token] = (documentFrequency[token] ?: 0) + 1
            }
        }

        // 2. Compute IDF for each term: ln((1 + N) / (1 + DF)) + 1
        val idf = mutableMapOf<String, Float>()
        for ((term, df) in documentFrequency) {
            idf[term] = (ln((1.0f + numDocs) / (1.0f + df.toFloat())) + 1.0f)
        }

        // 3. Compute TF-IDF for each document, prune to top features, and L2-normalize
        val result = mutableMapOf<String, DocumentVector>()

        for ((docId, tokens) in documents) {
            if (tokens.isEmpty()) {
                result[docId] = DocumentVector(docId, emptyMap())
                continue
            }

            // Term frequency in this document
            val tf = mutableMapOf<String, Float>()
            for (token in tokens) {
                tf[token] = (tf[token] ?: 0f) + 1f
            }

            val docSize = tokens.size.toFloat()
            val rawWeights = mutableMapOf<String, Float>()

            for ((term, count) in tf) {
                val termTf = count / docSize
                val termIdf = idf[term] ?: 1.0f
                rawWeights[term] = termTf * termIdf
            }

            // Prune to top features to keep vectors sparse and fast
            val pruned = if (rawWeights.size > maxFeaturesPerDoc) {
                rawWeights.entries
                    .sortedByDescending { it.value }
                    .take(maxFeaturesPerDoc)
                    .associate { it.key to it.value }
            } else {
                rawWeights
            }

            // L2 normalization (unit vector)
            var sumSquares = 0f
            for (weight in pruned.values) {
                sumSquares += weight * weight
            }

            val norm = sqrt(sumSquares)
            val normalizedWeights = if (norm > 0f) {
                pruned.mapValues { (_, w) -> w / norm }
            } else {
                emptyMap()
            }

            result[docId] = DocumentVector(docId, normalizedWeights)
        }

        return result
    }

    /**
     * Calculates the cosine similarity between two unit-normalized TF-IDF vectors.
     * Value ranges from 0.0f to 1.0f.
     */
    fun cosineSimilarity(v1: Map<String, Float>, v2: Map<String, Float>): Float {
        if (v1.isEmpty() || v2.isEmpty()) return 0.0f

        val (smaller, larger) = if (v1.size < v2.size) Pair(v1, v2) else Pair(v2, v1)

        var dotProduct = 0.0f
        for ((term, weight1) in smaller) {
            val weight2 = larger[term]
            if (weight2 != null) {
                dotProduct += weight1 * weight2
            }
        }

        return dotProduct.coerceIn(0.0f, 1.0f)
    }
}
