package com.alocard.app.ui.screen.grouplist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alocard.app.domain.util.Money
import com.alocard.app.ui.common.HintText
import com.alocard.app.ui.common.SectionDivider
import com.alocard.app.ui.common.dotGrid
import com.alocard.app.ui.components.EmptyState
import com.alocard.app.ui.theme.AloCardTheme

/**
 * 账单组管理页（P14，技术规范 §2.5 补 PRD 缺口）。
 * 空组的删除入口、账单组维度总览都在这里。
 */
@Composable
fun GroupListScreen(
    onGroupClick: (Long) -> Unit,
    onAddCard: () -> Unit,
    viewModel: GroupListViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCard,
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增卡片")
            }
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .dotGrid(AloCardTheme.colors.dot),
        ) {
            Text(
                text = "账单组",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            if (ui.rows.isEmpty()) {
                EmptyState(
                    title = "还没有账单组",
                    description = "添加第一张信用卡时会自动创建账单组。",
                    actionLabel = "添加卡片",
                    onAction = onAddCard,
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(ui.rows, key = { it.groupId }) { row ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onGroupClick(row.groupId) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = row.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (row.pendingEntry) {
                                    Text(
                                        text = "待录入",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AloCardTheme.colors.pending,
                                    )
                                }
                            }
                            HintText(
                                "额度 ${Money.format(row.creditLimitCents)} · 已用 ${Money.format(row.usedCents)}",
                                Modifier.padding(top = 2.dp),
                            )
                        }
                        SectionDivider()
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
