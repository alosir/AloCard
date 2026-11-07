package com.alosir.card.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import com.alosir.card.domain.CardTextParser
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object CardOcr {

    data class Result(
        val cardNumber: String?,
        val expiryYm: String?,
        val bankName: String?,
    )

    fun recognize(context: Context, uri: Uri): Result {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val visionText = Tasks.await(recognizer.process(image))
            parse(visionText.text)
        } finally {
            recognizer.close()
        }
    }

    fun recognize(bitmap: Bitmap, rotationDegrees: Int): Result {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val original = recognizeVariant(recognizer, bitmap, rotationDegrees)
            if (original.isComplete()) return original
            val enhanced = recognizeVariant(recognizer, enhanceForOcr(bitmap), rotationDegrees)
            if (enhanced.isComplete()) return enhanced
            Result(
                cardNumber = original.cardNumber ?: enhanced.cardNumber,
                expiryYm = original.expiryYm ?: enhanced.expiryYm,
                bankName = original.bankName ?: enhanced.bankName,
            )
        } finally {
            recognizer.close()
        }
    }

    private fun recognizeVariant(
        recognizer: TextRecognizer,
        bitmap: Bitmap,
        rotationDegrees: Int,
    ): Result {
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
        return parse(Tasks.await(recognizer.process(image)).text)
    }

    internal fun parse(text: String): Result {
        val parsed = CardTextParser.parse(text)
        return Result(parsed.cardNumber, parsed.expiryYm, parsed.bankName)
    }

    private fun Result.isComplete(): Boolean = cardNumber != null && expiryYm != null

    private fun enhanceForOcr(source: Bitmap): Bitmap {
        val target = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val contrast = 1.65f
        val translate = 255f * (1f - contrast) / 2f
        val matrix = ColorMatrix().apply {
            setSaturation(0f)
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f,
                    )
                )
            )
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        Canvas(target).drawBitmap(source, 0f, 0f, paint)
        return target
    }
}
