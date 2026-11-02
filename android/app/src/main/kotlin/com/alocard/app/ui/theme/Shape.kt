package com.alocard.app.ui.theme

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
 * AloCard 形状。
 *
 * 手账世界的元素多为硬边——圆角只在 Material 标准控件上出现。
 * - 索引标签、账簿行：0dp 硬边
 * - 输入框、按钮：4dp
 * - 弹层顶部：12dp
 * - 分段控件、徽标：胶囊
 */
val AloCardShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),  // 硬边：标签、账簿行
    small = RoundedCornerShape(4.dp),       // 输入框、按钮、PIN 位
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),      // 弹层顶部
    extraLarge = RoundedCornerShape(18.dp), // FAB、小组件
)

/** 触控目标：≥48dp，相邻间隔 ≥8dp（DESIGN.md 第 4 章） */
val MinTouchTarget = 48.dp
val TouchTargetGap = 8.dp
