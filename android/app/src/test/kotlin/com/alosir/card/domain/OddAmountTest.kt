package com.alosir.card.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OddAmountTest {
    @Test
    fun `按个位十位百位千位轮流提取含小数零头`() {
        val amount = 1_234_567L
        assertEquals(567L, OddAmount.forClick(amount, 0))
        assertEquals(4_567L, OddAmount.forClick(amount, 1))
        assertEquals(34_567L, OddAmount.forClick(amount, 2))
        assertEquals(234_567L, OddAmount.forClick(amount, 3))
        assertEquals(567L, OddAmount.forClick(amount, 4))
    }

    @Test
    fun `个位为零时跳过零额`() {
        val amount = 1_234_000L
        assertEquals(4_000L, OddAmount.forClick(amount, 0))
        assertEquals(34_000L, OddAmount.forClick(amount, 1))
        assertEquals(234_000L, OddAmount.forClick(amount, 2))
        assertEquals(4_000L, OddAmount.forClick(amount, 3))
    }

    @Test
    fun `整百金额没有可用零头`() {
        assertNull(OddAmount.forClick(100_000L, 0))
        assertNull(OddAmount.forClick(0L, 0))
    }
}
