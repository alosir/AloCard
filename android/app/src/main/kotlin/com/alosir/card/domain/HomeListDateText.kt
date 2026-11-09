package com.alosir.card.domain

import java.time.LocalDate

object HomeListDateText {
    fun graceBillAndDue(billDate: LocalDate, dueDate: LocalDate): String =
        "${billDate.shortLabel()} 出账 / ${dueDate.shortLabel()} 还款"

    fun currentDue(dueDate: LocalDate): String = "${dueDate.shortLabel()} 还款"

    fun nextDue(dueDate: LocalDate): String = "${dueDate.shortLabel()} 下期还款"

    private fun LocalDate.shortLabel(): String = "$monthValue/$dayOfMonth"
}
