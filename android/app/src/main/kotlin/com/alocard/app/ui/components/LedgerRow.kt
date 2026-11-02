package com.alocard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alocard.app.domain.DateEngine
import com.alocard.app.ui.theme.AloCardTheme
import java.time.LocalDate

/**
 * 账单业务状态。与 DateEngine.billStatus 的返回值对应。
 */
enum class BillState(val label: String) {
    PENDING_ENTRY("待录入"),
    DUE_UNPAID("待还款"),
    PARTIALLY_PAID("部分还款"),
    SETTLED("已结清"),
}

/**
 * 时效状态。与 DateEngine.dueStatus 的返回值对应。
 */
enum class DueState { NORMAL, DUE, OVERDUE }

/**
 * 账簿行 —— 列表的原子单元。
 *
 * 结构（DESIGN.md 5.1）：
 * ```
 * [索引标签 30dp] │ 卡名称  尾号 6688  [共享账单]        [状态徽标]
 *                 │ 52 天免息                    今天还款
 *                 │ 10/25 还款
 *                 │ ▪▪▪▪▪■□□□□□□□□□□□□□□□□□□□□
 *                 │ 10/20 出账 → 11/14 还款
 * ```
 *
 * 行底部分隔线只覆盖右侧内容区，不延伸到索引标签下方——标签是插进纸脊的书签，
 * 压在纸脊上。这是 HTML 原型里已确认的处理方式。
 */
@Composable
fun LedgerRow(
    cardName: String,
    cardTail: String,
    sharedCount: Int?,
    gracePeriodDays: Int?,
    dueText: String,
    dueSubText: String,
    dueState: DueState,
    billState: BillState,
    billDate: LocalDate,
    dueDate: LocalDate,
    today: LocalDate,
    tabColor: androidx.compose.ui.graphics.Color,
    tabGlyph: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AloCardTheme.colors

    Row(modifier = modifier.fillMaxWidth()) {
        // 索引标签
        WashiTab(color = tabColor, glyph = tabGlyph, modifier = Modifier.width(30.dp))

        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            // 第一行：卡名称 / 尾号 / 共享徽标 / 状态徽标
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cardName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = "尾号 $cardTail",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
                if (sharedCount != null && sharedCount > 1) {
                    SharedBadge(
                        count = sharedCount,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                StatusChip(
                    billState = billState,
                    dueState = dueState,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            // 第二行：免息期大数字 + 距还款日
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (gracePeriodDays != null) {
                    Text(
                        text = "$gracePeriodDays",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "天免息",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 3.dp),
                    )
                } else {
                    Text(
                        text = "待完善",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 右侧：距还款日
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    val dueColor = when (dueState) {
                        DueState.OVERDUE -> colors.overdue
                        DueState.DUE -> colors.due
                        DueState.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = dueText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = when (dueState) {
                            DueState.OVERDUE, DueState.DUE ->
                                androidx.compose.ui.text.font.FontWeight.Medium
                            else -> androidx.compose.ui.text.font.FontWeight.Normal
                        },
                        color = dueColor,
                    )
                    Text(
                        text = dueSubText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 第三行：倒计时格带
            if (gracePeriodDays != null) {
                CountdownStrip(
                    billDate = billDate,
                    dueDate = dueDate,
                    today = today,
                    isDueToday = dueState == DueState.DUE,
                    overdue = dueState == DueState.OVERDUE,
                    modifier = Modifier.padding(top = 10.dp),
                )
                CountdownCaption(
                    billDate = billDate,
                    dueDate = dueDate,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
    }
}

/**
 * washi 索引标签。
 *
 * 硬边实色块，从纸面左缘伸出，底部折角。真实手账侧标签是从页面边缘伸出的，
 * 不是贴在纸上的色条——所以左侧是书脊（无点阵的纸面槽），
 * 纸面（点阵）在标签右侧断开。
 *
 * 折角用 Canvas 画，不引入图片资源。
 */
@Composable
fun WashiTab(
    color: androidx.compose.ui.graphics.Color,
    glyph: String,
    modifier: Modifier = Modifier,
) {
    val colors = AloCardTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // 书脊：标签左侧 16dp 的同色纸面（CSS 里是 box-shadow: -16dp 0 0 0 var(--paper)）
                drawRect(
                    color = colors.paper,
                    topLeft = Offset(-16.dp.toPx(), 0f),
                    size = Size(16.dp.toPx(), size.height),
                )
                // 折角：底部 11dp 的暗色三角
                val corner = Path().apply {
                    moveTo(0f, size.height - 11.dp.toPx())
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = corner,
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.26f),
                )
            }
            .background(color),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = colors.onTab,
            modifier = Modifier.padding(top = 15.dp),
        )
    }
}

/** 共享账单徽标 */
@Composable
private fun SharedBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = "共享账单",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 状态徽标。
 *
 * **必须带文字，不能只靠颜色**——照顾色觉障碍用户（DESIGN.md 第 8 章）。
 * 状态由业务状态与时效状态共同决定：逾期优先于一切，其次到期，最后看业务状态。
 */
@Composable
fun StatusChip(
    billState: BillState,
    dueState: DueState,
    modifier: Modifier = Modifier,
) {
    val colors = AloCardTheme.colors
    val (bg, fg, text) = when {
        dueState == DueState.OVERDUE ->
            Triple(colors.overdueBg, colors.overdue, "逾期")
        dueState == DueState.DUE ->
            Triple(colors.dueBg, colors.due, "到期")
        billState == BillState.SETTLED ->
            Triple(colors.doneBg, colors.done, "已结清")
        billState == BillState.PENDING_ENTRY ->
            Triple(colors.pendingBg, colors.pending, "待录入")
        billState == BillState.PARTIALLY_PAID ->
            Triple(colors.dueBg, colors.due, "部分还款")
        else ->
            Triple(colors.pendingBg, colors.pending, "待还款")
    }

    Box(
        modifier = modifier
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = fg,
        )
    }
}

/** 账单组分隔线：只覆盖右侧内容区，不延伸到索引标签下方 */
@Composable
fun LedgerGroupDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = 42.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

// ---------- 预览 ----------

@Preview(name = "账簿行 · 浅色", showBackground = true, widthDp = 392)
@Composable
private fun LedgerRowPreview() {
    AloCardTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            LedgerRow(
                cardName = "日常消费卡",
                cardTail = "6688",
                sharedCount = 2,
                gracePeriodDays = 49,
                dueText = "今天还款",
                dueSubText = "10/25 还款",
                dueState = DueState.DUE,
                billState = BillState.PARTIALLY_PAID,
                billDate = LocalDate.of(2026, 10, 5),
                dueDate = LocalDate.of(2026, 10, 25),
                today = LocalDate.of(2026, 10, 24),
                tabColor = AloCardTheme.colors.tabIndigo,
                tabGlyph = "招",
            )
        }
    }
}

@Preview(name = "账簿行 · 深色", showBackground = true, widthDp = 392)
@Composable
private fun LedgerRowDarkPreview() {
    AloCardTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            LedgerRow(
                cardName = "旅行卡",
                cardTail = "8820",
                sharedCount = null,
                gracePeriodDays = 22,
                dueText = "已逾期 11 天",
                dueSubText = "10/13 还款",
                dueState = DueState.OVERDUE,
                billState = BillState.DUE_UNPAID,
                billDate = LocalDate.of(2026, 9, 28),
                dueDate = LocalDate.of(2026, 10, 13),
                today = LocalDate.of(2026, 10, 24),
                tabColor = AloCardTheme.colors.tabMustard,
                tabGlyph = "建",
            )
        }
    }
}

@Preview(name = "账簿行 · 待完善", showBackground = true, widthDp = 392)
@Composable
private fun LedgerRowIncompletePreview() {
    AloCardTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            LedgerRow(
                cardName = "备用卡",
                cardTail = "2205",
                sharedCount = null,
                gracePeriodDays = null,
                dueText = "补充账期后可用",
                dueSubText = "",
                dueState = DueState.NORMAL,
                billState = BillState.PENDING_ENTRY,
                billDate = LocalDate.of(2026, 10, 1),
                dueDate = LocalDate.of(2026, 10, 21),
                today = LocalDate.of(2026, 10, 24),
                tabColor = AloCardTheme.colors.tabPlum,
                tabGlyph = "交",
            )
        }
    }
}
