package com.alosir.card.domain

import com.alosir.card.domain.model.GroupQuota
import org.junit.Assert.assertEquals
import org.junit.Test

class QuotaAggregationTest {
    @Test
    fun `同银行额度取最高且已用合计`() {
        val rows = QuotaAggregation.byBank(
            listOf(
                GroupQuota(1, "招商银行", "招商银行", 50_000_00L, 10_000_00L, true),
                GroupQuota(2, "招商银行", "招商银行", 80_000_00L, 5_000_00L, false),
            )
        )
        assertEquals(1, rows.size)
        assertEquals(80_000_00L, rows[0].creditLimitCents)
        assertEquals(15_000_00L, rows[0].usedCents)
        assertEquals(true, rows[0].pendingEntry)
    }

    @Test
    fun `不同银行分别展示且无银行名按组兜底`() {
        val rows = QuotaAggregation.byBank(
            listOf(
                GroupQuota(1, "招商银行", "招商银行", 50_000_00L, 10_000_00L, false),
                GroupQuota(2, "工商银行", "工商银行", 40_000_00L, 1_000_00L, false),
                GroupQuota(3, "自定义组", null, 20_000_00L, 2_000_00L, true),
            )
        )
        assertEquals(3, rows.size)
        assertEquals(listOf(50_000_00L, 40_000_00L, 20_000_00L), rows.map { it.creditLimitCents })
    }
}
