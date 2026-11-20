package com.alosir.card.domain

import kotlin.math.roundToInt

data class IconCropRegion(
    val left: Int,
    val top: Int,
    val side: Int,
    val centerX: Float,
    val centerY: Float,
    val zoom: Float,
)

object IconCropMath {
    fun crop(
        width: Int,
        height: Int,
        centerX: Float,
        centerY: Float,
        zoom: Float,
    ): IconCropRegion {
        val safeZoom = zoom.coerceAtLeast(1f)
        val half = 0.5f / safeZoom
        val normalizedCenterX = centerX.coerceIn(half, 1f - half)
        val normalizedCenterY = centerY.coerceIn(half, 1f - half)
        val side = (minOf(width, height) / safeZoom).roundToInt().coerceAtLeast(1)
        val left = (normalizedCenterX * width - side / 2f).roundToInt().coerceIn(0, width - side)
        val top = (normalizedCenterY * height - side / 2f).roundToInt().coerceIn(0, height - side)
        return IconCropRegion(left, top, side, normalizedCenterX, normalizedCenterY, safeZoom)
    }
}
