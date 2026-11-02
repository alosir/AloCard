package com.alocard.app.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * 金额工具。
 *
 * 存储单位是「分」（Long，PRD 4.4），展示单位是「元」。
 * 所有格式化集中在这里，避免界面各处手写 DecimalFormat 产生口径漂移。
 */
object Money {

    private val displayFormat = DecimalFormat("#,##0.##")

    /**
     * 把用户输入的「元」字符串解析为「分」。
     * 接受 "8600"、"8,600.5"、"¥86" 等形态；负数与非法输入返回 null。
     */
    fun parseToCents(input: String): Long? {
        val text = input.trim()
            .removePrefix("¥").removePrefix("￥")
            .replace(",", "")
            .trim()
        if (text.isEmpty()) return null
        return try {
            val yuan = BigDecimal(text)
            if (yuan < BigDecimal.ZERO) return null
            yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (e: ArithmeticException) {
            null
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** 分 → 展示字符串。null 显示为 "—"（待录入语义，PRD 5.7.1） */
    fun format(cents: Long?): String =
        if (cents == null) "—" else format(cents)

    /** 分 → 展示字符串 */
    fun format(cents: Long): String {
        val yuan = BigDecimal(cents).movePointLeft(2)
        return "¥" + displayFormat.format(yuan)
    }
}
