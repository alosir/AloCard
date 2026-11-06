package com.alosir.card.ui.screen.carddetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.domain.util.Money
import com.alosir.card.ui.common.groupCardNumber
import com.alosir.card.ui.common.ActionRow
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.KeyValueRow
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.common.Section
import com.alosir.card.ui.common.SectionTitle
import com.alosir.card.ui.components.BillState
import com.alosir.card.ui.components.CountdownCaption
import com.alosir.card.ui.components.CountdownLegend
import com.alosir.card.ui.components.CountdownStrip
import com.alosir.card.ui.components.DueState
import com.alosir.card.ui.components.StatusChip
import com.alosir.card.ui.theme.AloCardTheme
import com.alosir.card.ui.theme.tabColorFor

/**
 * 卡片详情页（P2）。
 *
 * 结构（DESIGN.md 6.3，自上而下）：
 * 免息期大数字 + 状态 → 格带 + 说明 → 额度 → 账期 → 本期账单 → 卡片信息 → 双按钮。
 * 截屏保护 FLAG_SECURE 由 Activity 按路由设置（Phase 4）。
 */
@Composable
fun CardDetailScreen(
    groupId: Long,
    onBack: () -> Unit,
    onEditGroup: (Long) -> Unit,
    onEditCard: (Long) -> Unit,
    onPinRequired: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState(groupId).collectAsStateWithLifecycle()
    // 录入/还款改为本页内浮层，详情页留在原地不划出（项 3）
    var showAmountEntry by remember { mutableStateOf(false) }
    var showPaymentMark by remember { mutableStateOf(false) }

    if (ui.loading) {
        PageScaffold(title = "", onBack = onBack) {
            Box(Modifier.fillMaxSize())
        }
        return
    }

    PageScaffold(
        title = ui.groupName,
        onBack = onBack,
        actions = {
            IconButton(onClick = { onEditGroup(ui.groupId) }) {
                Icon(Icons.Default.Edit, contentDescription = "编辑账单组", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        bottomBar = {
            // 操作按钮固定悬浮在底部，不随内容滚动（项 2）
            ActionRow(
                cancelText = "录入账单金额",
                onCancel = { showAmountEntry = true },
                confirmText = "标记还款",
                onConfirm = { showPaymentMark = true },
                confirmEnabled = ui.billState != BillState.PENDING_ENTRY,
            )
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val colors = AloCardTheme.colors

            // ---- 免息期 + 时效 ----
            Section {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (ui.gracePeriodDays != null) {
                        Text(
                            text = "${ui.gracePeriodDays}",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "天免息",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 5.dp),
                        )
                    } else {
                        Text(
                            text = "待完善",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        val dueColor = when (ui.dueState) {
                            DueState.OVERDUE -> colors.overdue
                            DueState.DUE -> colors.due
                            DueState.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = ui.dueText,
                            style = MaterialTheme.typography.labelLarge,
                            color = dueColor,
                        )
                        if (ui.billAmountCents != null) {
                            Text(
                                text = "待还 ${Money.format(ui.billAmountCents!! - (ui.paidAmountCents ?: 0L))}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                StatusChip(
                    billState = ui.billState,
                    dueState = ui.dueState,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (ui.gracePeriodDays != null) {
                    CountdownStrip(
                        billDate = ui.billDate,
                        dueDate = ui.dueDate,
                        today = DateProviderToday(),
                        isDueToday = ui.dueState == DueState.DUE,
                        overdue = ui.dueState == DueState.OVERDUE,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    CountdownCaption(
                        billDate = ui.billDate,
                        dueDate = ui.dueDate,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                    CountdownLegend(modifier = Modifier.padding(top = 8.dp))
                }
            }

            // ---- 额度 ----
            Section {
                SectionTitle("额度")
                val used = (ui.billAmountCents?.let { it - (ui.paidAmountCents ?: 0L) } ?: 0L)
                    .coerceAtLeast(0L)
                val limit = ui.creditLimitCents
                val remain = limit?.let { (it - used).coerceAtLeast(0L) }
                KeyValueRow("已用", Money.format(used))
                KeyValueRow("剩余可用（估）", Money.format(remain))
                if (limit != null) {
                    val ratio = if (limit > 0) (used.toDouble() / limit).coerceIn(0.0, 1.0) else 0.0
                    Box(
                        Modifier
                            .padding(top = 8.dp)
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
                    KeyValueRow("授信额度", Money.format(limit))
                }
                HintText(
                    "剩余可用为估算值，仅统计已出账账单；未出账消费未计入。",
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            // ---- 账期 ----
            Section {
                SectionTitle("账期")
                if (ui.billingDay != null) {
                    KeyValueRow("账单日", "每月 ${ui.billingDay} 号")
                    val offset = ui.dueOffsetDays ?: 20
                    KeyValueRow("最后还款日", "推后 $offset 天（${ui.dueDate.monthValue} 月 ${ui.dueDate.dayOfMonth} 日）")
                    KeyValueRow("本期账单日", "${ui.billDate.monthValue} 月 ${ui.billDate.dayOfMonth} 日")
                    KeyValueRow("下期账单日", "${ui.nextBillDate.monthValue} 月 ${ui.nextBillDate.dayOfMonth} 日")
                } else {
                    HintText("账单日未设置——到「编辑账单组」补全后这里会显示完整账期。")
                }
            }

            // ---- 本期账单 ----
            Section {
                SectionTitle("本期账单")
                if (ui.billState == BillState.PENDING_ENTRY) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "账单金额待录入",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    KeyValueRow("账单金额", Money.format(ui.billAmountCents))
                    KeyValueRow("已还款", Money.format(ui.paidAmountCents))
                    KeyValueRow(
                        "待还金额",
                        ui.outstandingText,
                        valueColor = if (ui.billState != BillState.SETTLED) colors.due else null,
                    )
                    KeyValueRow("状态", billStateLabel(ui.billState))
                }
            }

            // ---- 卡片 ----
            Section {
                SectionTitle("卡片" + if (ui.cards.size > 1) "（共享账单 ${ui.cards.size} 张）" else "")
                ui.cards.forEach { card ->
                    CardInfoRow(
                        card = card,
                        revealed = ui.revealedNumber?.takeIf { card.hasNumber },
                        onReveal = {
                            if (viewModel.isUnlocked()) {
                                viewModel.revealCardNumber(card.cardId)
                            } else {
                                // 未解锁：先走安全密码验证（PRD F1.4），回话期内再点即显示
                                onPinRequired()
                            }
                        },
                        onHide = { viewModel.hideCardNumber() },
                        onClick = { onEditCard(card.cardId) },
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            // 操作按钮已上移到 PageScaffold 的固定底栏，不再放在滚动区
        }
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

@Composable
private fun DateProviderToday() = com.alosir.card.domain.util.DateProvider.today()

@Composable
private fun CardInfoRow(
    card: CardRow,
    revealed: String?,
    onReveal: () -> Unit,
    onHide: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = AloCardTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = card.displayName,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = revealed?.let { groupCardNumber(it) } ?: "•••• •••• •••• ${card.tail}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (revealed != null) colors.due else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            // 复制与显隐图标右对齐、与卡号同一水平线
            if (revealed != null) {
                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                val appContext = androidx.compose.ui.platform.LocalContext.current
                IconButton(onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(revealed))
                    android.widget.Toast.makeText(appContext, "卡号已复制", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制卡号",
                        tint = colors.goldText,
                        modifier = Modifier.width(16.dp),
                    )
                }
            }
            if (card.hasNumber) {
                IconButton(onClick = { if (revealed == null) onReveal() else onHide() }) {
                    Icon(
                        imageVector = if (revealed == null) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (revealed == null) "查看完整卡号" else "隐藏卡号",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(18.dp),
                    )
                }
            }
        }
        HintText(
            "有效期 ${card.expiryYm ?: "未填"} · ${card.schemeDisplay}",
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun billStateLabel(state: BillState): String = state.label
