package com.alosir.card.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CountdownCellsTest {

    /** 用户实测场景：今天 2 号、还款日 7 号，中间应有 4 个空心格（3/4/5/6 号） */
    @Test
    fun `due day is the last cell and hollow cells between are exact`() {
        val cells = countdownCells(
            billDate = LocalDate.of(2026, 10, 20),
            dueDate = LocalDate.of(2026, 11, 7),
            today = LocalDate.of(2026, 11, 2),
        )
        assertEquals(19, cells.size) // 10/20 → 11/7 共 18 天，含还款日 19 格
        assertEquals(CountdownCell.TODAY, cells[13])
        assertEquals(CountdownCell.DUE, cells[18])
        val between = cells.subList(14, 18)
        assertEquals(4, between.size)
        between.forEach { assertEquals(CountdownCell.FUTURE, it) }
    }

    @Test
    fun `today equals due day - last cell is DUE`() {
        val cells = countdownCells(
            LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 7),
        )
        assertEquals(CountdownCell.DUE, cells.last())
        assertEquals(0, cells.count { it == CountdownCell.FUTURE })
    }

    @Test
    fun `overdue beyond due day - all past except last DUE`() {
        val cells = countdownCells(
            LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 10),
        )
        assertEquals(CountdownCell.DUE, cells.last())
        assertEquals(0, cells.count { it == CountdownCell.FUTURE })
        assertEquals(cells.size - 1, cells.count { it == CountdownCell.PAST })
    }

    @Test
    fun `same day bill and due - single DUE cell`() {
        val cells = countdownCells(
            LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 1),
        )
        assertEquals(1, cells.size)
        assertEquals(CountdownCell.DUE, cells[0])
    }

    @Test
    fun `due before bill - empty`() {
        assertEquals(0, countdownCells(
            LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1),
        ).size)
    }
}
