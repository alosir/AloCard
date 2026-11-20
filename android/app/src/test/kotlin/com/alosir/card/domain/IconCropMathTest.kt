package com.alosir.card.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class IconCropMathTest {
    @Test
    fun `默认裁剪为居中的最大正方形`() {
        val crop = IconCropMath.crop(1000, 500, 0.5f, 0.5f, 1f)
        assertEquals(250, crop.left)
        assertEquals(0, crop.top)
        assertEquals(500, crop.side)
    }

    @Test
    fun `缩放和边界夹紧不会超出原图`() {
        val crop = IconCropMath.crop(1000, 500, 0.9f, 0.9f, 2f)
        assertEquals(625, crop.left)
        assertEquals(250, crop.top)
        assertEquals(250, crop.side)
        assertEquals(0.75f, crop.centerX)
        assertEquals(0.75f, crop.centerY)
    }

    @Test
    fun `过小缩放使用一防止除零`() {
        val crop = IconCropMath.crop(100, 100, 0.5f, 0.5f, 0f)
        assertEquals(100, crop.side)
    }
}
