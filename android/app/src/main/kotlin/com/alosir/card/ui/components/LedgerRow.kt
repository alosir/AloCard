package com.alosir.card.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alosir.card.ui.theme.AloCardTheme

/** 账单业务状态。与 DateEngine.billStatus 的返回值对应。 */
enum class BillState(val label: String) {
    PENDING_ENTRY("待录入"),
    DUE_UNPAID("待还款"),
    PARTIALLY_PAID("部分还款"),
    SETTLED("已结清"),
}

/** 时效状态。与 DateEngine.dueStatus 的返回值对应。 */
enum class DueState { NORMAL, DUE, OVERDUE }

/**
 * 状态徽标 · v2：圆角 6dp 胶囊。
 * **必须带文字，不能只靠颜色**——照顾色觉障碍用户。
 * 逾期优先于一切，其次到期，最后看业务状态。
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
        billState == BillState.DUE_UNPAID ->
            Triple(colors.dueBg, colors.due, "待还款")
        else ->
            Triple(colors.pendingBg, colors.pending, "待录入")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = fg,
        )
    }
}
