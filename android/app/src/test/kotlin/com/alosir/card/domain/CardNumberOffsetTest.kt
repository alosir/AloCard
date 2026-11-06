package com.alosir.card.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CardNumberOffsetTest {

    /** 组边界（每 4 位）必须落在该组末尾，不得越界——回归「第 4 位闪退」 */
    @Test
    fun `group boundaries stay inside grouped text`() {
        // 原始 4 位 → 分组 "1234"（长度 4），偏移 4 必须 = 4（不能是 5）
        assertEquals(4, cardNumberOriginalToTransformed(4))
        // 原始 8 位 → 分组 "1234 5678"（长度 9），偏移 8 必须 = 9（不能是 10）
        assertEquals(9, cardNumberOriginalToTransformed(8))
        // 原始 16 位 → 分组长度 19，偏移 16 必须 = 19（不能是 20）
        assertEquals(19, cardNumberOriginalToTransformed(16))
    }

    @Test
    fun `non boundary offsets map correctly`() {
        assertEquals(0, cardNumberOriginalToTransformed(0))
        assertEquals(1, cardNumberOriginalToTransformed(1))
        assertEquals(6, cardNumberOriginalToTransformed(5))
        assertEquals(12, cardNumberOriginalToTransformed(10))
    }

    @Test
    fun `reverse mapping round trips`() {
        for (original in 0..16) {
            val transformed = cardNumberOriginalToTransformed(original)
            assertEquals(original, cardNumberTransformedToOriginal(transformed))
        }
    }
}
