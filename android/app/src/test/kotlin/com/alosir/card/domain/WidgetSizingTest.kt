package com.alosir.card.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizingTest {

    @Test
    fun `免息期按横向格数每行展示三列内容`() {
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
