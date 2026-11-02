package com.alosir.card.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 银行名 → washi 索引标签色。
 *
 * 分配规则（DESIGN.md 2.3）：按银行固定，同一银行同一色板位。
 * 未登记的银行走轮转，保证视觉上仍可区分。
 *
 * 色板是**身份标识**，不是状态——状态色只用于状态。
 */
private val bankColors = listOf(
    "招商银行" to "indigo",
    "工商银行" to "teal",
    "建设银行" to "mustard",
    "中信银行" to "persimmon",
    "交通银行" to "plum",
    "农业银行" to "teal",
    "中国银行" to "indigo",
    "浦发银行" to "plum",
    "民生银行" to "mustard",
    "光大银行" to "teal",
    "兴业银行" to "indigo",
    "广发银行" to "persimmon",
    "平安银行" to "mustard",
    "华夏银行" to "plum",
    "邮政储蓄" to "teal",
    "浙商银行" to "indigo",
)

/** washi 色相（账单组身份标识的键，与主题无关，可在非 Composable 环境使用） */
enum class TabHue { INDIGO, TEAL, MUSTARD, PERSIMMON, PLUM }

/** 银行名 → 色板键（纯函数，ViewModel 可用） */
fun tabHueFor(bankName: String?): TabHue {
    val key = bankColors.firstOrNull { it.first == bankName }?.second
        ?: run {
            // 未登记的银行按名字哈希轮转，保证同银行同色
            val palette = listOf("indigo", "teal", "mustard", "persimmon", "plum")
            palette[((bankName?.hashCode() ?: 0).let { if (it < 0) -it else it }) % palette.size]
        }
    return when (key) {
        "teal" -> TabHue.TEAL
        "mustard" -> TabHue.MUSTARD
        "persimmon" -> TabHue.PERSIMMON
        "plum" -> TabHue.PLUM
        else -> TabHue.INDIGO
    }
}

@Composable
fun tabColorFor(bankName: String?): Color = tabColorFor(hue = tabHueFor(bankName))

@Composable
fun tabColorFor(hue: TabHue): Color {
    val colors = LocalAloCardColors.current
    return when (hue) {
        TabHue.TEAL -> colors.tabTeal
        TabHue.MUSTARD -> colors.tabMustard
        TabHue.PERSIMMON -> colors.tabPersimmon
        TabHue.PLUM -> colors.tabPlum
        TabHue.INDIGO -> colors.tabIndigo
    }
}
