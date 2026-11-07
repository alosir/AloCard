package com.alosir.card.domain

object WidgetSizing {
    fun graceColumnCount(widthCells: Int): Int = widthCells.coerceAtLeast(1)

    fun graceItemLimit(widthCells: Int, heightCells: Int): Int {
        val verticalSteps = (heightCells.coerceAtLeast(1) + 1) / 2
        return graceColumnCount(widthCells) * 3 * verticalSteps
    }

    fun dueItemLimit(heightCells: Int): Int =
        5 + (heightCells.coerceAtLeast(2) - 2) * 3

    fun cellsForDp(dp: Int): Int = ((dp + 30) / 70).coerceAtLeast(1)
}
