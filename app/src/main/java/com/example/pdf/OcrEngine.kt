package com.example.pdf

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object OcrEngine {

    data class OcrResult(
        val fullText: String,
        val blockCount: Int,
        val lineCount: Int,
        val isSuccessful: Boolean,
        val errorMessage: String? = null
    )

    suspend fun recognizeText(bitmap: Bitmap): OcrResult = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    val blocks = visionText.textBlocks.size
                    var lines = 0
                    for (block in visionText.textBlocks) {
                        lines += block.lines.size
                    }
                    if (continuation.isActive) {
                        continuation.resume(
                            OcrResult(
                                fullText = text,
                                blockCount = blocks,
                                lineCount = lines,
                                isSuccessful = true
                            )
                        )
                    }
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resume(
                            OcrResult(
                                fullText = "",
                                blockCount = 0,
                                lineCount = 0,
                                isSuccessful = false,
                                errorMessage = e.localizedMessage ?: "OCR recognition failed"
                            )
                        )
                    }
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(
                    OcrResult(
                        fullText = "",
                        blockCount = 0,
                        lineCount = 0,
                        isSuccessful = false,
                        errorMessage = e.localizedMessage ?: "OCR initialization error"
                    )
                )
            }
        }
    }
}
