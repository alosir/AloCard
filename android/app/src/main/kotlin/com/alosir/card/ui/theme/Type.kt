package com.alosir.card.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * AloCard 字阶。
 *
 * 字号严格来自 `DESIGN.md` 第 3 章，与 `prototype/tokens.css` 对应。
 * 字阶步进：相邻层级字号比 ≥1.25:1。
 *
 * 字体：Archivo（拉丁与数字），中文回落到系统 CJK 字体。
 * 原型内置 `prototype/fonts/archivo-var-latin.woff2`；Android 侧需把字体文件
 * 放到 `res/font/` 后取消注释下方代码。未放入字体前，系统默认字体也能工作——
 * 但等宽数字特性（tnum）必须保留，那是本产品的关键视觉特征。
 */

/** 等宽表格数字：所有天数与金额必须启用，保证数字上下对齐可比。 */
const val TABULAR_FIGURES = "tnum"

private val Archivo = FontFamily.Default // TODO: 放入 res/font/archivo_var.ttf 后替换

val AloCardTypography = Typography(
    // 34sp / 600 —— 列表行的免息期大数字
    displayLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 39.sp, // 1.15
        letterSpacing = (-0.03).emUnit(),
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    // 24sp / 600 —— 空状态标题
    headlineMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.02).emUnit(),
    ),
    // 19sp / 600 —— 区块标题、额度数字、品牌名
    titleLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.01).emUnit(),
    ),
    titleMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.01).emUnit(),
    ),
    // 14sp —— 正文、卡名称
    bodyLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp, // 1.45
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    bodyMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    // 13sp —— 标签、键值、按钮
    labelLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    labelMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    // 11sp —— 说明文字、轴号
    labelSmall = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
)

/**
 * em 字距辅助函数。
 * Compose 的 TextUnit 用 `.em` 扩展属性更直观，但为兼容不同版本写法，
 * 这里显式换算成 sp。
 */
private fun Double.emUnit() = androidx.compose.ui.unit.TextUnit(
    this.toFloat(),
    androidx.compose.ui.unit.TextUnitType.Em
)
