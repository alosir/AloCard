package com.alosir.card.ui.screen.billhistory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.KeyValueRow
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.common.Section
import com.alosir.card.ui.common.SectionDivider
import com.alosir.card.ui.common.SectionTitle
import com.alosir.card.ui.theme.AloCardTheme

/**
 * 账单历史页（P6 / F3.2）。
 * 每期独立存档、账单日倒序；未录入的期保留为「未录入」，不作废（ADR-0004）。
 */
@Composable
fun BillHistoryScreen(
    groupId: Long,
    onBack: () -> Unit,
    viewModel: BillHistoryViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState(groupId).collectAsStateWithLifecycle()
    val colors = AloCardTheme.colors

    PageScaffold(title = if (ui.groupName.isEmpty()) "账单历史" else "${ui.groupName} · 账单历史", onBack = onBack) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Section {
                    HintText("每期账单独立存档。未录入的期保留为「未录入」，可随时补录且仍归入该期。")
                }
            }
            items(ui.rows, key = { it.billId }) { row ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${row.billingDate.year} 年 ${row.billingDate.monthValue} 月账单",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = row.statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = when (row.statusLabel) {
                                "已结清" -> colors.done
                                "未录入" -> colors.pending
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        if (row.canUndoLastPayment) {
                            IconButton(onClick = { viewModel.undoLastPayment(groupId, row) }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "撤销上次标记",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    KeyValueRow("账单金额", row.amountText)
                    KeyValueRow("已还款", row.paidText)
                    KeyValueRow("待还金额", row.outstandingText)
                    row.overdueText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.overdue,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                SectionDivider()
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
