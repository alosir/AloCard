package com.alosir.card.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 卡号显示分组：每 4 位数字空一格（存储仍是原始数字串，仅展示分组）。
 */
class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val grouped = text.text.chunked(4).joinToString(" ")
        return TransformedText(AnnotatedString(grouped), CardNumberOffsetMapping)
    }

    private object CardNumberOffsetMapping : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int =
            com.alosir.card.domain.cardNumberOriginalToTransformed(offset)

        override fun transformedToOriginal(offset: Int): Int =
            com.alosir.card.domain.cardNumberTransformedToOriginal(offset)
    }
}

/** 纯文本分组（详情页展示完整卡号用） */
fun groupCardNumber(digits: String): String = digits.chunked(4).joinToString(" ")
