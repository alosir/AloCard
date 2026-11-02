package com.alocard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alocard.app.domain.util.Money
import com.alocard.app.ui.screen.SortMode
import com.alocard.app.ui.theme.AloCardTheme

/**
 * 额度总览条。
 *
 * 无边框无底色，靠留白与底部分隔线表达层级（DESIGN.md 5.4）——
 * 不用带边框的卡片，避免与屏幕外框构成卡片套卡片。
 *
 * 「（估）」二字必须出现在标签上，且说明行写明「仅统计已出账账单」——
 * 这是 ADR-0007 的口径：已用额度不含未出账消费，故为偏乐观的估算。
 */
@Composable
fun CreditOverview(
    usedCents: Long,
    totalCents: Long,
    remainCents: Long,
    modifier: Modifier = Modifier,
) {
    val colors = AloCardTheme.colors
    val ratio = if (totalCents > 0) (usedCents.toDouble() / totalCents).coerceIn(0.0, 1.0) else 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Metric(label = "已用额度", value = Money.format(usedCents))
            Metric(
                label = "剩余可用（估）",
                value = Money.format(remainCents),
                alignEnd = true,
            )
        }

        // 比例条
        Box(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.meterTrack),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(ratio.toFloat())
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.meterFill),
            )
        }

        Text(
            text = "${Money.format(totalCents)} · 仅统计已出账账单，未出账消费未计入",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Metric(label: String, value: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * 待办聚合条。
 *
 * 让「待录入引导录入」这个需求在首页就有明确落点——
 * 用户不必自己逐张卡去找。
 */
@Composable
fun TodoBanner(
    summary: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val colors = AloCardTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.dueBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(3.5.dp))
                .background(colors.due),
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.labelMedium,
            color = colors.due,
            modifier = Modifier.padding(start = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.due,
            modifier = Modifier.size(15.dp),
        )
    }
}

/**
 * 排序分段控件。
 *
 * 两个排序维度对等、没有主次，不该藏进下拉——
 * 单次点击即切换（DESIGN.md 5.6）。
 */
@Composable
fun SortSegment(
    selected: SortMode,
    onSelect: (SortMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(3.dp),
    ) {
        SortMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest
                        else Color.Transparent
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 空状态。
 *
 * 图标与标题**同行**，不做「方块图标 + 标题」的竖向卡片模板——
 * 那是被禁的 AI 生成界面俗套。
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(20.dp))
            androidx.compose.material3.Button(
                onClick = onAction,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}
