package com.example.services.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class OcrResult(
    val rawText: String,
    val textBlocks: List<String>,
    val lineCount: Int,
    val isConfidenceGood: Boolean
)

object MenuOcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun processFile(context: Context, file: File): OcrResult = processUri(context, Uri.fromFile(file))

    suspend fun processUri(context: Context, uri: Uri): OcrResult = suspendCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val rawText = visionText.text
                    val blocks = visionText.textBlocks.map { it.text }
                    var totalLines = 0
                    for (b in visionText.textBlocks) {
                        totalLines += b.lines.size
                    }

                    val isGood = rawText.isNotBlank() && totalLines >= 1
                    cont.resume(OcrResult(rawText, blocks, totalLines, isGood))
                }
                .addOnFailureListener { e ->
                    cont.resume(OcrResult("", emptyList(), 0, false))
                }
        } catch (e: Exception) {
            cont.resume(OcrResult("", emptyList(), 0, false))
        }
    }
}
