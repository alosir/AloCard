package com.alosir.card.ui.screen.quotadetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.domain.util.Money
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.common.goldBrush
import com.alosir.card.ui.components.CreditOverview
import com.alosir.card.ui.theme.AloCardTheme

/**
 * 额度页 · v2：总额度总览 + 按银行账单组的占用。
 * 同银行合并出账共享额度，不重复计数；剩余可用为估算，口径必须标注。
 */
@Composable
fun QuotaDetailScreen(
    onBack: () -> Unit,
    viewModel: QuotaDetailViewModel = hiltViewModel(),
) {
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val colors = AloCardTheme.colors

    PageScaffold(title = "额度", onBack = null) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Spacer(Modifier.height(8.dp))
                CreditOverview(
                    usedCents = totals.usedCents,
                    totalCents = totals.totalLimitCents,
                    remainCents = totals.remainCents,
                )
                HintText(
                    "同银行合并出账共享额度，不重复计数；剩余可用为偏乐观估算（ADR-0007）。",
                    Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp),
                )
            }
            items(rows, key = { it.groupId }) { row ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.paperRaised)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Row {
                        Text(
                            text = row.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (row.pendingEntry) {
                            Text(
                                text = "待录入",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.pending,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(
                            "已用 ${Money.format(row.usedCents)}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "共享额度 ${row.creditLimitCents?.let { Money.format(it) } ?: "—"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    row.creditLimitCents?.let { limit ->
                        val ratio = if (limit > 0) (row.usedCents.toDouble() / limit).coerceIn(0.0, 1.0) else 0.0
                        Box(
                            Modifier
                                .padding(top = 10.dp)
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
                                    .background(goldBrush())
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
