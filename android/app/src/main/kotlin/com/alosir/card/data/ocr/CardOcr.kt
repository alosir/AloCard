package com.alosir.card.data.ocr

import android.content.Context
import android.net.Uri
import com.alosir.card.domain.CardNumberIdentifier
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * 卡面 OCR（本机 ML Kit 文字识别）。
 * 拍照 → 识别卡号（13–19 位，优先 Luhn 通过）/ 有效期（MM/YY → YYYY-MM）/ 银行名。
 * 照片与识别结果均不出设备；银行名仅在 BIN 未命中时作为兜底回填。
 */
object CardOcr {

    data class Result(
        val cardNumber: String?,
        val expiryYm: String?,
        val bankName: String?,
    )

    /** 同步识别（调用方须切到 IO 线程） */
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

    internal fun parse(text: String): Result {
        // 银行名：关键词包含匹配（OCR 常把全称切碎，关键词取标准短名）
        val bank = BANK_KEYWORDS.firstOrNull { text.contains(it) }

        // 有效期：MM/YY 或 MM/YYYY
        val expiry = Regex("""(0[1-9]|1[0-2])\s*/\s*(\d{2}(?:\d{2})?)""").find(text)?.let { m ->
            val mm = m.groupValues[1]
            val yy = m.groupValues[2]
            "${if (yy.length == 4) yy else "20$yy"}-$mm"
        }

        // 卡号：允许空格/连字符分组的 13–19 位数字串；优先 Luhn 通过者
        val candidates = Regex("""(?:\d[\s-]?){13,19}""").findAll(text)
            .map { it.value.replace(Regex("""[\s-]"""), "") }
            .filter { it.length in 13..19 }
            .distinct()
            .toList()
        val number = candidates.firstOrNull { CardNumberIdentifier.luhnCheck(it) }
            ?: candidates.firstOrNull()

        return Result(number, expiry, bank)
    }

    private val BANK_KEYWORDS = listOf(
        "招商银行", "工商银行", "建设银行", "农业银行", "中国银行", "交通银行",
        "中信银行", "浦发银行", "光大银行", "民生银行", "兴业银行", "广发银行",
        "平安银行", "华夏银行", "浙商银行", "北京银行", "上海银行", "邮储银行", "邮政储蓄",
    )
}
