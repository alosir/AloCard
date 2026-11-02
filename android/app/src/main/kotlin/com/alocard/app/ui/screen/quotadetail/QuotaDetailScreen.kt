package com.alocard.app.ui.screen.quotadetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alocard.app.domain.util.Money
import com.alocard.app.ui.common.HintText
import com.alocard.app.ui.common.KeyValueRow
import com.alocard.app.ui.common.PageScaffold
import com.alocard.app.ui.common.Section
import com.alocard.app.ui.common.SectionDivider
import com.alocard.app.ui.common.SectionTitle
import com.alocard.app.ui.theme.AloCardTheme

/**
 * 额度明细页（P7 / F6.1）：总览三项 + 逐账单组占用。
 */
@Composable
fun QuotaDetailScreen(
    onBack: () -> Unit,
    viewModel: QuotaDetailViewModel = hiltViewModel(),
) {
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val colors = AloCardTheme.colors

    PageScaffold(title = "额度", onBack = onBack) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Section {
                    SectionTitle("总览")
                    Row(Modifier.fillMaxWidth()) {
                        TotalItem("已用额度", Money.format(totals.usedCents), Modifier.weight(1f))
                        TotalItem("剩余可用（估）", Money.format(totals.remainCents), Modifier.weight(1f), muted = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    val ratio = if (totals.totalLimitCents > 0) {
                        (totals.usedCents.toDouble() / totals.totalLimitCents).coerceIn(0.0, 1.0)
                    } else 0.0
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.meterTrack)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(ratio.toFloat())
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.meterFill)
                        )
                    }
                    HintText(
                        "总授信 ${Money.format(totals.totalLimitCents)} · 仅统计已出账账单，未出账消费未计入，剩余可用为偏乐观估算（ADR-0007）。",
                        Modifier.padding(top = 8.dp),
                    )
                }
            }
            items(rows, key = { it.groupId }) { row ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row {
                        Text(
                            text = row.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (row.pendingEntry) {
                            Text(
                                text = "待录入",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.pending,
                            )
                        }
                    }
                    KeyValueRow("信用额度", Money.format(row.creditLimitCents))
                    KeyValueRow("已用", Money.format(row.usedCents))
                    row.creditLimitCents?.let { limit ->
                        val ratio = if (limit > 0) (row.usedCents.toDouble() / limit).coerceIn(0.0, 1.0) else 0.0
                        Box(
                            Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.meterTrack)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(ratio.toFloat())
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.meterFill)
                            )
                        }
                    }
                }
                SectionDivider()
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TotalItem(label: String, value: String, modifier: Modifier = Modifier, muted: Boolean = false) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
