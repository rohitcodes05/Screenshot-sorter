package com.screensort.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 100% Offline Multimodal Image Analyzer combining:
 * 1. Google ML Kit OCR (Latin Text Recognition)
 * 2. Google ML Kit Image Labeling (400+ visual concepts: Food, Software, Documents, Nature, etc.)
 *
 * Runs completely on-device without any internet connection.
 */
class ImageAnalysisManager {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.50f)
            .build()
    )

    data class AnalysisResult(
        val extractedText: String,
        val visualLabels: List<String>
    )

    /**
     * Extracts both OCR text and visual labels from an image Uri.
     */
    suspend fun analyzeImage(context: Context, imageUri: Uri): AnalysisResult = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)

            coroutineScope {
                val textDeferred = async {
                    try {
                        val visionText = textRecognizer.process(inputImage).await()
                        visionText.text.trim()
                    } catch (e: Exception) {
                        ""
                    }
                }

                val labelsDeferred = async {
                    try {
                        val labels = imageLabeler.process(inputImage).await()
                        labels
                            .filter { it.confidence >= 0.50f }
                            .map { it.text.trim() }
                            .filter { it.isNotBlank() }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val text = textDeferred.await()
                val visualLabels = labelsDeferred.await()

                AnalysisResult(
                    extractedText = text,
                    visualLabels = visualLabels
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AnalysisResult(extractedText = "", visualLabels = emptyList())
        }
    }

    /**
     * Releases ML Kit resources when no longer needed.
     */
    fun close() {
        try {
            textRecognizer.close()
            imageLabeler.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
