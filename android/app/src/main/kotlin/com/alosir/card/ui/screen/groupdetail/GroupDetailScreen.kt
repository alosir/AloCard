package com.alosir.card.ui.screen.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.domain.DateEngine
import com.alosir.card.domain.util.DateProvider
import com.alosir.card.domain.util.Money
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.KeyValueRow
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.common.FilledButton
import com.alosir.card.ui.common.TonalButton
import com.alosir.card.ui.components.BankChip
import com.alosir.card.ui.components.BankLogo
import com.alosir.card.ui.components.BillState
import com.alosir.card.ui.components.DueState
import com.alosir.card.ui.components.StatusChip
import com.alosir.card.ui.theme.AloCardTheme
import com.alosir.card.ui.theme.tabColorFor

/**
 * 银行账单详情 · v2。
 * 本期应还（剩余）→ 账期/共享额度 → 组内卡片 → 录入/修改账单 + 标记还款。
 * 同银行多卡合并出账；账期、额度、账单金额挂在组上，卡片只挂基础信息。
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
    val colors = AloCardTheme.colors
    // 录入/还款改为本页内浮层，详情页留在原地不划出（项 3）
    var showAmountEntry by remember { mutableStateOf(false) }
    var showPaymentMark by remember { mutableStateOf(false) }

    // 金额浮窗允许截屏（不含完整卡号）；关闭后恢复详情页卡号保护
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    LaunchedEffect(showAmountEntry, showPaymentMark) {
        val window = activity?.window ?: return@LaunchedEffect
        if (showAmountEntry || showPaymentMark) {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
    }

    if (ui.deleted) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onDeleted() }
        return
    }

    PageScaffold(
        title = group?.let { it.bankName ?: it.name } ?: "银行账单",
        onBack = onBack,
        actions = {
            if (group != null) {
                IconButton(onClick = { onEdit(group.id) }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑账期", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { viewModel.showDeleteConfirm() }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除账单组", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp),
        ) {
            if (group != null) {
                val today = DateProvider.today()
                val bill = group.currentBill
                val billState = when (DateEngine.billStatus(
                    bill?.billAmountCents?.toDouble(), bill?.paidAmountCents?.toDouble(),
                )) {
                    DateEngine.BILL_PENDING_ENTRY -> BillState.PENDING_ENTRY
                    DateEngine.BILL_PARTIALLY_PAID -> BillState.PARTIALLY_PAID
                    DateEngine.BILL_SETTLED -> BillState.SETTLED
                    else -> BillState.DUE_UNPAID
                }
                val dueDays = DateEngine.daysUntilDue(group.billingDay, group.dueOffsetDays, today)
                // 已结清（剩余待还 ≤ 0）永不逾期——即使已过最后还款日
                val dueState = if (billState == BillState.SETTLED || dueDays == null) DueState.NORMAL else when (DateEngine.dueStatus(dueDays)) {
                    DateEngine.STATUS_DUE -> DueState.DUE
                    DateEngine.STATUS_OVERDUE -> DueState.OVERDUE
                    else -> DueState.NORMAL
                }
                val dueDate = DateEngine.dueDateOf(group.billingDay ?: 1, group.dueOffsetDays ?: 20, today)

                // 本期应还卡
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.paperRaised)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("本期应还（剩余）", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            bill?.outstandingCents?.let { Money.format(it) } ?: "待录入",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.goldText,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            "账单 ${bill?.billAmountCents?.let { Money.format(it) } ?: "—"} · 已还 ${Money.format(bill?.paidAmountCents ?: 0L)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${dueDate.monthValue}/${dueDate.dayOfMonth} 还款",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        StatusChip(billState = billState, dueState = dueState, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                // 账期与额度
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.paperRaised)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    KeyValueRow("账期", "${group.billingDay?.let { "每月 $it 号出账" } ?: "待完善"} · ${group.dueOffsetDays?.let { "推后 $it 天还款" } ?: "待完善"}")
                    KeyValueRow("共享额度", Money.format(group.creditLimitCents))
                    KeyValueRow("免息期（今日）", DateEngine.gracePeriod(group.billingDay, group.dueOffsetDays, today)?.let { "$it 天" } ?: "待完善")
                    Row(
                        Modifier.fillMaxWidth().clickable { onHistory(group.id) }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("历史账单", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("逐期存档 ›", style = MaterialTheme.typography.labelMedium, color = colors.goldText)
                    }
                }

                // 组内卡片
                Text(
                    "组内卡片 · ${group.cards.size} 张${if (group.cards.size > 1) "（合并出账）" else ""}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 8.dp),
                )
                if (group.cards.isEmpty()) {
                    HintText(
                        "组内没有卡片。账单组保留，额度仍计入总授信（ADR-0006）。",
                        Modifier.padding(horizontal = 20.dp),
                    )
                }
                group.cards.forEach { card ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.paperRaised)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .clickable { onEditCard(card.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BankLogo(
                            bankName = group.bankName ?: group.name,
                            glyph = (group.bankName ?: group.name).first().toString(),
                            color = tabColorFor(group.bankName),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                card.cardLabel?.takeIf { it.isNotBlank() } ?: "尾号 ${card.cardNumberLast4 ?: "????"}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            HintText("尾号 ${card.cardNumberLast4 ?: "—"} · ${card.cardScheme.uppercase()} · 有效期 ${card.expiryYm ?: "未填"}")
                        }
                        TextButton(onClick = { viewModel.removeCard(card.id) }) {
                            Text("删除", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TonalButton("录入 / 修改账单", { showAmountEntry = true }, Modifier.weight(1f))
                    FilledButton("标记还款", { showPaymentMark = true }, Modifier.weight(1.4f))
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

    if (showAmountEntry) {
        com.alosir.card.ui.common.SlideUpSheet(onDismiss = { showAmountEntry = false }) {
            com.alosir.card.ui.screen.amountentry.AmountEntryScreen(
                groupId = groupId,
                onBack = { showAmountEntry = false },
                onSaved = { showAmountEntry = false },
            )
        }
    }
    if (showPaymentMark) {
        com.alosir.card.ui.common.SlideUpSheet(onDismiss = { showPaymentMark = false }) {
            com.alosir.card.ui.screen.paymentmark.PaymentMarkScreen(
                groupId = groupId,
                onBack = { showPaymentMark = false },
                onSaved = { showPaymentMark = false },
            )
        }
    }
}
