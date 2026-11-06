package com.alosir.card.ui.screen.grouplist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.ui.common.goldBrush
import com.alosir.card.ui.components.BankLedgerRow
import com.alosir.card.ui.components.EmptyState
import com.alosir.card.ui.theme.AloCardTheme
import com.alosir.card.ui.theme.tabColorFor

/**
 * 账单页 · v2。
 * 按银行名称呈现账单（同银行多卡合并出账），不再区分「账单组 / 逐卡账单」。
 * 按还款日升序；点击行进银行账单详情。
 */
@Composable
fun GroupListScreen(
    onGroupClick: (Long) -> Unit,
    onAddCard: () -> Unit,
    viewModel: GroupListViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AloCardTheme.colors
    var showAddBill by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.systemBars.only(
            androidx.compose.foundation.layout.WindowInsetsSides.Top + androidx.compose.foundation.layout.WindowInsetsSides.Horizontal,
        ),
        topBar = {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "账单",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "账单 · ${ui.rows.size} 家银行",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        floatingActionButton = {
            // 「+」= 添加账单（补录最近一期未录入的账单），不是添加卡片
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(goldBrush())
                    .clickable { showAddBill = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加账单", tint = colors.onGold)
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            if (ui.rows.isEmpty() && !ui.loading) {
                EmptyState(
                    title = "还没有账单",
                    description = "添加第一张信用卡时会自动创建银行账单。",
                    actionLabel = "添加卡片",
                    onAction = onAddCard,
                )
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp)) {
                    items(ui.rows, key = { it.groupId }) { row ->
                        BankLedgerRow(
                            bankName = row.bankName,
                            subLabel = if (row.cardCount > 1) "${row.cardCount} 张卡合并出账" else "尾号 ${row.firstTail}",
                            gracePeriodDays = row.gracePeriodDays,
                            dueDays = row.dueDays,
                            dueText = row.dueText,
                            dueSubText = row.dueSubText,
                            dueState = row.dueState,
                            billState = row.billState,
                            billDate = row.billDate,
                            dueDate = row.dueDate,
                            today = com.alosir.card.domain.util.DateProvider.today(),
                            tabColor = tabColorFor(hue = row.hue),
                            tabGlyph = row.bankName.firstOrNull()?.toString() ?: "卡",
                            onClick = { onGroupClick(row.groupId) },
                        )
                    }
                    item { Spacer(Modifier.height(84.dp)) }
                }
            }
        }
    }

    // 添加账单弹窗：独立 Dialog 窗口，遮住底部导航栏；上滑入、下滑出
    if (showAddBill) {
        com.alosir.card.ui.common.SlideUpSheet(onDismiss = { showAddBill = false }) {
            AddBillSheet(onDismiss = { showAddBill = false }, onAddCard = onAddCard)
        }
    }
}
