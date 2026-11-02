package com.alocard.app.ui.screen.groupdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alocard.app.domain.util.Money
import com.alocard.app.ui.common.HintText
import com.alocard.app.ui.common.KeyValueRow
import com.alocard.app.ui.common.PageScaffold
import com.alocard.app.ui.common.Section
import com.alocard.app.ui.common.SectionTitle
import com.alocard.app.ui.common.TonalButton
import com.alocard.app.ui.common.FilledButton

/**
 * 账单组详情页（P5）。
 * 列出组内卡片、账期继承值；提供编辑、删除组（二次确认）、账单历史入口。
 */
@Composable
fun GroupDetailScreen(
    groupId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onHistory: (Long) -> Unit,
    onDeleted: () -> Unit,
    onEditCard: (Long) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState(groupId).collectAsStateWithLifecycle()
    val group = ui.group

    if (ui.deleted) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onDeleted() }
        return
    }

    PageScaffold(
        title = group?.name ?: "账单组",
        onBack = onBack,
        actions = {
            IconButton(onClick = { viewModel.showDeleteConfirm() }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除账单组",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
        ) {
            if (group != null) {
                Section {
                    SectionTitle("账期与额度")
                    KeyValueRow("账单日", group.billingDay?.let { "每月 $it 号" } ?: "待完善")
                    KeyValueRow("最后还款日", group.dueOffsetDays?.let { "推后 $it 天" } ?: "待完善")
                    KeyValueRow("信用额度", Money.format(group.creditLimitCents))
                    KeyValueRow("共享账单", if (group.cards.size > 1) "${group.cards.size} 张卡" else "单卡")
                }
                Section {
                    SectionTitle("组内卡片")
                    if (group.cards.isEmpty()) {
                        HintText("组内没有卡片。账单组保留，额度仍计入总授信（ADR-0006）。")
                    }
                    group.cards.forEach { card ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditCard(card.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    card.cardLabel ?: "尾号 ${card.cardNumberLast4 ?: "????"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                HintText("有效期 ${card.expiryYm ?: "未填"} · 尾号 ${card.cardNumberLast4 ?: "—"}")
                            }
                            TextButton(onClick = { viewModel.removeCard(card.id) }) {
                                Text("删除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                Section {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    ) {
                        TonalButton("编辑账期", { onEdit(group.id) }, Modifier.weight(1f))
                        FilledButton("账单历史", { onHistory(group.id) }, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (ui.confirmDelete && group != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text("删除「${group.name}」账单组？") },
            text = { Text("${group.cards.size} 张卡及其全部账单记录将被删除，总授信额度相应减少。此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(group.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) { Text("取消") }
            },
        )
    }
}
