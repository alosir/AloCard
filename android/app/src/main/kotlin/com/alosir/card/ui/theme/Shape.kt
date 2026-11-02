package com.alosir.card.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * AloCard 间距阶梯（dp）。
 *
 * 严格来自 `DESIGN.md` 第 4 章：4 / 8 / 12 / 16 / 20 / 24 / 32 / 40。
 * 使用 `LocalAloCardSpacing` 在组件中获取，而不是硬编码。
 */
data class AloCardSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 40.dp,
)

/**
 * AloCard 形状 · v2「鎏金卡夹」。
 *
 * 硬边手账语言废弃，改为轻盈的圆角体系：
 * - 徽标、键帽：6–10dp
 * - 卡片、账簿行、按钮、输入框：14–18dp
 * - 卡面：20dp；FAB：18dp；弹层顶部：24dp
 */
val AloCardShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** 触控目标：≥48dp，相邻间隔 ≥8dp（DESIGN.md 第 4 章） */
val MinTouchTarget = 48.dp
val TouchTargetGap = 8.dp
