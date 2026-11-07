package com.alosir.card.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizingTest {

    @Test
    fun `免息期列数与桌面横向格数一致`() {
        assertEquals(2, WidgetSizing.graceColumnCount(widthCells = 2))
        assertEquals(3, WidgetSizing.graceColumnCount(widthCells = 3))
        assertEquals(4, WidgetSizing.graceColumnCount(widthCells = 4))
    }

    @Test
    fun `免息期展示数量等于列数乘三行`() {
        assertEquals(6, WidgetSizing.graceItemLimit(widthCells = 2, heightCells = 2))
        assertEquals(9, WidgetSizing.graceItemLimit(widthCells = 3, heightCells = 2))
        assertEquals(12, WidgetSizing.graceItemLimit(widthCells = 4, heightCells = 2))
    }

    @Test
    fun `免息期纵向扩大后展示更多`() {
        assertEquals(12, WidgetSizing.graceItemLimit(widthCells = 2, heightCells = 3))
        assertEquals(18, WidgetSizing.graceItemLimit(widthCells = 3, heightCells = 4))
    }

    @Test
    fun `待还初始五条并随纵向格数增加`() {
        assertEquals(5, WidgetSizing.dueItemLimit(heightCells = 2))
        assertEquals(8, WidgetSizing.dueItemLimit(heightCells = 3))
        assertEquals(11, WidgetSizing.dueItemLimit(heightCells = 4))
    }

    @Test
    fun `异常尺寸使用安全下限`() {
        assertEquals(3, WidgetSizing.graceItemLimit(widthCells = 1, heightCells = 1))
        assertEquals(5, WidgetSizing.dueItemLimit(heightCells = 1))
    }
}
