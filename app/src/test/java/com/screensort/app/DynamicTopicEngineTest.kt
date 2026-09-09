package com.screensort.app

import com.screensort.app.domain.DynamicTopicEngine
import com.screensort.app.domain.TextPreprocessor
import com.screensort.app.domain.TfidfVectorizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicTopicEngineTest {

    @Test
    fun testTextPreprocessorFiltersStopwordsAndNoise() {
        val sampleText = "100% battery 10:45 PM wifi connected. Docker container build failed on Ubuntu."
        val tokens = TextPreprocessor.cleanAndTokenize(sampleText)

        // UI noise words should be removed
        assertFalse(tokens.contains("battery"))
        assertFalse(tokens.contains("wifi"))
        assertFalse(tokens.contains("pm"))

        // Salient words should be kept
        assertTrue(tokens.contains("docker"))
        assertTrue(tokens.contains("container"))
        assertTrue(tokens.contains("build"))
        assertTrue(tokens.contains("failed"))
        assertTrue(tokens.contains("ubuntu"))
    }

    @Test
    fun testFeatureExtractionIncludesBigramsAndVisualLabels() {
        val sample = "delicious chocolate cake recipe"
        val visualLabels = listOf("Food", "Dish")
        val features = TextPreprocessor.extractMultimodalFeatures(sample, visualLabels)

        // OCR text tokens and bigrams
        assertTrue(features.contains("delicious"))
        assertTrue(features.contains("chocolate"))
        assertTrue(features.contains("cake"))
        assertTrue(features.contains("recipe"))
        assertTrue(features.contains("chocolate cake"))

        // Visual labels should be included and boosted
        assertTrue(features.contains("food"))
        assertTrue(features.contains("dish"))
    }

    @Test
    fun testTfidfCosineSimilarity() {
        val vectorizer = TfidfVectorizer()
        val doc1 = listOf("docker", "container", "software", "docker container")
        val doc2 = listOf("docker", "container", "software", "docker container")
        val doc3 = listOf("recipe", "flour", "sugar", "bake", "food")

        val docs = mapOf("d1" to doc1, "d2" to doc2, "d3" to doc3)
        val vectors = vectorizer.vectorize(docs)

        val sim12 = vectorizer.cosineSimilarity(vectors["d1"]!!.termWeights, vectors["d2"]!!.termWeights)
        val sim13 = vectorizer.cosineSimilarity(vectors["d1"]!!.termWeights, vectors["d3"]!!.termWeights)

        assertTrue("Related docs should have high similarity", sim12 > 0.4)
        assertTrue("Unrelated docs should have very low similarity", sim13 < 0.1)
    }

    @Test
    fun testMultimodalClusteringMinSizeAndOthersFallback() {
        val engine = DynamicTopicEngine(similarityThreshold = 0.08f, minClusterSize = 3)

        val docs = listOf(
            // Software cluster (3 items - should form broad category)
            DynamicTopicEngine.MultimodalDoc("s1", "Docker container build failed", listOf("Software", "Screen")),
            DynamicTopicEngine.MultimodalDoc("s2", "Docker compose running containers on port 8080", listOf("Software", "Screen")),
            DynamicTopicEngine.MultimodalDoc("s3", "Kubernetes pod deployment status error log", listOf("Software", "Screenshot")),

            // Food / Recipe cluster (3 items - should form broad category)
            DynamicTopicEngine.MultimodalDoc("f1", "Chocolate cake recipe with flour and sugar", listOf("Food", "Dish")),
            DynamicTopicEngine.MultimodalDoc("f2", "Italian pasta dish with tomato sauce", listOf("Food", "Cuisine")),
            DynamicTopicEngine.MultimodalDoc("f3", "Baking fresh sourdough bread at 450 degrees", listOf("Food", "Baked Goods")),

            // Isolated single item (< 3 items - must fall back into 'Others')
            DynamicTopicEngine.MultimodalDoc("x1", "Crypto wallet seed phrase 12 secret words", listOf("Font"))
        )

        val results = engine.clusterAndNameMultimodal(docs)
        assertEquals(7, results.size)

        val s1 = results.first { it.docId == "s1" }
        val s2 = results.first { it.docId == "s2" }
        val s3 = results.first { it.docId == "s3" }

        val f1 = results.first { it.docId == "f1" }
        val f2 = results.first { it.docId == "f2" }
        val f3 = results.first { it.docId == "f3" }

        val x1 = results.first { it.docId == "x1" }

        // Verify Software items merged into one broad category
        assertEquals("Software items should share category", s1.categoryName, s2.categoryName)
        assertEquals("Software items should share category", s1.categoryName, s3.categoryName)
        assertTrue(
            "Software category should mention software or docker",
            s1.categoryName.lowercase().contains("software") || s1.categoryName.lowercase().contains("docker")
        )

        // Verify Food items merged into one broad category
        assertEquals("Food items should share category", f1.categoryName, f2.categoryName)
        assertEquals("Food items should share category", f1.categoryName, f3.categoryName)
        assertTrue(
            "Food category should mention food, dish, or recipe",
            f1.categoryName.lowercase().contains("food") || f1.categoryName.lowercase().contains("dish") || f1.categoryName.lowercase().contains("recipe")
        )

        // Verify isolated item was assigned to 'Others' because cluster size < 3
        assertEquals("Isolated item must be placed in 'Others'", "Others", x1.categoryName)
        assertEquals(-1, x1.clusterId)
    }

    @Test(timeout = 5000)
    fun testLargeDatasetPerformance2500Screenshots() {
        val engine = DynamicTopicEngine(similarityThreshold = 0.08f, minClusterSize = 3)

        // Generate 2,500 diverse screenshot documents
        val templates = listOf(
            Pair("Docker container failed build log error status healthy port 8080", listOf("Software", "Screen")),
            Pair("Flight ticket reservation confirmed gate terminal boarding pass", listOf("Document", "Text")),
            Pair("Delicious chocolate cake recipe sugar flour butter bake oven", listOf("Food", "Dish")),
            Pair("Invoice payment receipt total amount transaction bank account", listOf("Document", "Receipt")),
            Pair("Two-factor authentication code 2FA password verification secure", listOf("Software", "Lock"))
        )

        val largeDocs = (0 until 2500).map { i ->
            val tmpl = templates[i % templates.size]
            DynamicTopicEngine.MultimodalDoc(
                docId = "doc_$i",
                text = "${tmpl.first} unique_token_$i",
                visualLabels = tmpl.second
            )
        }

        val startTime = System.currentTimeMillis()
        var lastStage = ""
        val results = engine.clusterAndNameMultimodal(largeDocs) { _, _, stage ->
            lastStage = stage
        }
        val durationMs = System.currentTimeMillis() - startTime

        assertEquals(2500, results.size)
        // Verify it finished in under 3 seconds!
        assertTrue("Clustering 2500 docs should take < 3000ms, took ${durationMs}ms", durationMs < 3000)
        assertTrue(lastStage.isNotBlank())
    }
}
