package com.alosir.card.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeListDateTextTest {

    @Test
    fun `免息期同时显示消费所属账单的出账日和还款日`() {
        assertEquals(
            "11/20 出账 / 12/10 还款",
            HomeListDateText.graceBillAndDue(
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 12, 10),
            ),
        )
    }

    @Test
    fun `距还款日待还状态只显示本期还款日`() {
        assertEquals(
            "12/10 还款",
            HomeListDateText.currentDue(LocalDate.of(2026, 12, 10)),
        )
    }

    @Test
    fun `距还款日已结清状态显示下期还款日`() {
        assertEquals(
            "12/10 下期还款",
            HomeListDateText.nextDue(LocalDate.of(2026, 12, 10)),
        )
    }
}
