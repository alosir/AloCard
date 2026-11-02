package com.alocard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alocard.app.ui.theme.AloCardTheme
import java.time.LocalDate

/**
 * 倒计时格带 —— AloCard 的签名组件。
 *
 * 一格一天，从本期账单日到还款日。已过填浅、今日黑色实心且加高、
 * 还款日实心状态色、未来空心。不读数字就能看出「今天在这期账的第几天」。
 *
 * 关键实现约束（DESIGN.md 5.2）：
 * - **每格固定 5dp 宽 + 2dp 间距，不能用 weight 拉伸**。
 *   CSS 原型里踩过这个坑：flex:1 会把格带拉成延伸到屏幕外的长条，语义全失。
 *   格数由天数决定，宽度随之变化，两张卡的格带长度差本身就是信息。
 * - 今日格与还款日格高 12dp，其余 7dp——用高度做视觉锚点。
 *
 * 逾期时整条填实、末格标记逾期色，表示已越过还款日。
 *
 * @param billDate 本期账单日
 * @param dueDate 本期账单的最后还款日
 * @param today 今天
 * @param isDueToday 今天是否正是最后还款日（影响末格用 due 还是 overdue 色）
 * @param overdue 本期账单是否已逾期
 */
@Composable
fun CountdownStrip(
    billDate: LocalDate,
    dueDate: LocalDate,
    today: LocalDate,
    isDueToday: Boolean = false,
    overdue: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = AloCardTheme.colors
    val span = java.time.temporal.ChronoUnit.DAYS.between(billDate, dueDate).toInt()
    if (span <= 0) return

    // drawBehind 内不能调用 @Composable 取值，提前取出
    val futureCell = MaterialTheme.colorScheme.outline
    val todayIdx = java.time.temporal.ChronoUnit.DAYS.between(billDate, today).toInt()

    // 逾期时：整条填实，末格为逾期标记
    if (overdue && todayIdx >= span) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(CellGap),
            verticalAlignment = Alignment.Bottom,
        ) {
            repeat(span) { i ->
                val isLast = i == span - 1
                Box(
                    Modifier
                        .width(CellWidth)
                        .height(if (isLast) CellHeightTall else CellHeightNormal)
                        .background(if (isLast) colors.overdue else colors.countdownPast)
                )
            }
        }
        return
    }

    val idx = todayIdx.coerceIn(0, span - 1)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CellGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(span) { i ->
            val isToday = i == idx
            val isDue = i == span - 1
            val height = if (isToday || isDue) CellHeightTall else CellHeightNormal
            val color = when {
                isToday -> MaterialTheme.colorScheme.onSurface
                isDue -> if (isDueToday) colors.due else colors.overdue
                i < idx -> colors.countdownPast
                else -> Color.Transparent
            }
            Box(
                Modifier
                    .width(CellWidth)
                    .height(height)
                    .then(
                        if (color == Color.Transparent) {
                            // 未来格：1dp 内描边，不用 fill
                            Modifier.drawBehind {
                                drawRect(
                                    color = futureCell,
                                    topLeft = Offset.Zero,
                                    size = Size(size.width, size.height),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 1.dp.toPx()
                                    ),
                                )
                            }
                        } else {
                            Modifier.background(color)
                        }
                    )
            )
        }
    }
}

/** 单格宽：固定 5dp，不随容器拉伸 */
private val CellWidth: Dp = 5.dp

/** 格间距 */
private val CellGap: Dp = 2.dp

/** 普通格高 */
private val CellHeightNormal: Dp = 7.dp

/** 今日格与还款日格高（视觉锚点） */
private val CellHeightTall: Dp = 12.dp

/**
 * 格带图例。说明这一排方块是什么——没有它用户不知道格带含义。
 */
@Composable
fun CountdownLegend(modifier: Modifier = Modifier) {
    val colors = AloCardTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(color = colors.countdownPast, label = "已过")
        LegendItem(color = MaterialTheme.colorScheme.onSurface, label = "今天")
        LegendItem(color = colors.overdue, label = "还款日")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 格带说明文字。格式「10/20 出账 → 11/14 还款」。
 */
@Composable
fun CountdownCaption(
    billDate: LocalDate,
    dueDate: LocalDate,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "${billDate.monthValue}/${billDate.dayOfMonth} 出账 → " +
            "${dueDate.monthValue}/${dueDate.dayOfMonth} 还款",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Preview(name = "格带 · 浅色", showBackground = true)
@Composable
private fun CountdownStripPreview() {
    AloCardTheme(darkTheme = false) {
        Box(Modifier.padding(16.dp)) {
            Column {
                CountdownStrip(
                    billDate = LocalDate.of(2026, 10, 20),
                    dueDate = LocalDate.of(2026, 11, 14),
                    today = LocalDate.of(2026, 10, 24),
                )
                CountdownCaption(
                    billDate = LocalDate.of(2026, 10, 20),
                    dueDate = LocalDate.of(2026, 11, 14),
                    modifier = Modifier.padding(top = 5.dp),
                )
                CountdownLegend(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Preview(name = "格带 · 逾期", showBackground = true)
@Composable
private fun CountdownStripOverduePreview() {
    AloCardTheme(darkTheme = false) {
        Box(Modifier.padding(16.dp)) {
            CountdownStrip(
                billDate = LocalDate.of(2026, 9, 28),
                dueDate = LocalDate.of(2026, 10, 13),
                today = LocalDate.of(2026, 10, 24),
                overdue = true,
            )
        }
    }
}

@Preview(name = "格带 · 深色", showBackground = true)
@Composable
private fun CountdownStripDarkPreview() {
    AloCardTheme(darkTheme = true) {
        Box(Modifier.padding(16.dp)) {
            CountdownStrip(
                billDate = LocalDate.of(2026, 10, 20),
                dueDate = LocalDate.of(2026, 11, 14),
                today = LocalDate.of(2026, 10, 24),
            )
        }
    }
}
