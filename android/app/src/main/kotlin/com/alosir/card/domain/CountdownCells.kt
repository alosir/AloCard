package com.alosir.card.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 倒计时格带的单格状态 */
enum class CountdownCell { PAST, TODAY, DUE, FUTURE }

/**
 * 一格一天：本期账单日 → 还款日，**含还款日当天**，共 span+1 格。
 * 末格恒为还款日（DUE）； TODAY 与 DUE 同日时末格仍为 DUE（颜色由界面区分到期色）。
 * 逾期越过还款日时 TODAY 被钳到末格，整队已过、末格 DUE——与旧逾期视觉一致。
 */
fun countdownCells(billDate: LocalDate, dueDate: LocalDate, today: LocalDate): List<CountdownCell> {
    val span = ChronoUnit.DAYS.between(billDate, dueDate).toInt()
    if (span < 0) return emptyList()
    val todayIdx = ChronoUnit.DAYS.between(billDate, today).toInt().coerceIn(0, span)
    return (0..span).map { i ->
        when {
            i == span -> CountdownCell.DUE
            i == todayIdx -> CountdownCell.TODAY
            i < todayIdx -> CountdownCell.PAST
            else -> CountdownCell.FUTURE
        }
    }
}
