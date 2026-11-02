package com.alocard.app.ui.screen.addcard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alocard.app.domain.util.Money
import com.alocard.app.ui.common.ActionRow
import com.alocard.app.ui.common.FilledField
import com.alocard.app.ui.common.FilledButton
import com.alocard.app.ui.common.HintText
import com.alocard.app.ui.common.PageScaffold
import com.alocard.app.ui.common.Section
import com.alocard.app.ui.common.SectionTitle
import com.alocard.app.ui.common.Stepper
import com.alocard.app.ui.common.TonalButton

/**
 * 新增卡片页（P3）/ 编辑卡片（F1.2）。
 *
 * 流程：共享账单选择 → 卡片信息 → 账期与额度 → 保存。
 * 所有字段均可空，账期三项「必填但可稍后补」（ADR-0009）。
 */
@Composable
fun AddCardScreen(
    cardId: Long?,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: AddCardViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(cardId) {
        if (cardId != null && cardId > 0) viewModel.loadCard(cardId)
    }
    LaunchedEffect(ui.savedGroupId) {
        val saved = ui.savedGroupId
        if (saved != null && saved > 0) onSaved(saved)
        else if (saved == -2L) onBack() // 编辑完成
    }

    PageScaffold(
        title = if (ui.isEditing) "编辑卡片" else "新增卡片",
        onBack = onBack,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
        ) {
            if (!ui.isEditing) {
                // ---- 共享账单选择 ----
                Section {
                    SectionTitle("这张卡共享账单吗")
                    ChoiceRow(
                        selected = ui.choice == GroupChoice.NEW,
                        title = "新建账单组",
                        meta = "这张卡独立出账，单独设置账期与额度",
                        onClick = { viewModel.onChoiceChange(GroupChoice.NEW) },
                    )
                    if (ui.matchedGroup != null) {
                        Spacer(Modifier.height(8.dp))
                        ChoiceRow(
                            selected = ui.choice == GroupChoice.JOIN,
                            title = "加入「${ui.matchedGroup!!.name}」账单组",
                            meta = "同银行同账单日，加入后完全继承账期与额度（只读）",
                            onClick = { viewModel.onChoiceChange(GroupChoice.JOIN) },
                        )
                    }
                }
            }

            // ---- 卡片信息 ----
            Section {
                SectionTitle("卡片信息")
                FilledField(
                    value = ui.cardNumber,
                    onValueChange = viewModel::onCardNumberChange,
                    label = "卡号",
                    placeholder = "选填，填完整卡号可自动识别银行与卡组织",
                    numeric = true,
                    isError = ui.cardNumberError != null,
                    supportingText = ui.cardNumberError ?: ui.cardNumberNote,
                )
                Spacer(Modifier.height(12.dp))
                FilledField(
                    value = ui.cardName,
                    onValueChange = viewModel::onCardNameChange,
                    label = "卡名称",
                    placeholder = "选填，如「日常消费卡」",
                )
                Spacer(Modifier.height(12.dp))
                FilledField(
                    value = ui.expiryYm,
                    onValueChange = viewModel::onExpiryChange,
                    label = "有效期",
                    placeholder = "YYYY-MM",
                    numeric = true,
                    isError = ui.expiryError != null,
                    supportingText = ui.expiryError ?: "到期前 30 天会提醒换卡。",
                )
                Spacer(Modifier.height(12.dp))
                FilledField(
                    value = ui.bankName,
                    onValueChange = viewModel::onBankNameChange,
                    label = "银行",
                    placeholder = "选填，识别不到时手动填写",
                    supportingText = if (ui.detectedFromNumber) "根据卡号识别，可修改" else null,
                )
                Spacer(Modifier.height(4.dp))
                HintText("卡组织：${schemeLabel(ui.cardScheme)} · 卡号在本地加密存储，界面默认只显示末 4 位。")
            }

            // ---- 账期与额度（新建组时编辑；加入组时只读继承说明） ----
            if (!ui.isEditing) {
                Section {
                    SectionTitle("账期与额度")
                    if (ui.choice == GroupChoice.JOIN && ui.matchedGroup != null) {
                        val g = ui.matchedGroup!!
                        HintText(
                            "继承自「${g.name}」：" +
                                "账单日 ${g.billingDay ?: "—"} 号 · " +
                                "推后 ${g.dueOffsetDays ?: "—"} 天还款 · " +
                                "额度 ${Money.format(g.creditLimitCents)}",
                        )
                        HintText("共享账单组的账期与额度不可单独修改（ADR-0006）。", Modifier.padding(top = 4.dp))
                    } else {
                        FilledField(
                            value = ui.groupName,
                            onValueChange = viewModel::onGroupNameChange,
                            label = "账单组名称",
                            placeholder = "默认取银行名",
                        )
                        Spacer(Modifier.height(12.dp))
                        FilledField(
                            value = ui.billingDayText,
                            onValueChange = viewModel::onBillingDayChange,
                            label = "账单日",
                            placeholder = "1–28",
                            numeric = true,
                            isError = ui.billingDayError != null,
                            supportingText = ui.billingDayError ?: "银行实际账单日只在 1–28 号之间（ADR-0002）",
                        )
                        Spacer(Modifier.height(12.dp))

                        // 最后还款日：两种输入方式实时换算（ADR-0003）
                        Text(
                            text = "最后还款日",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Stepper(
                            value = ui.dueOffsetDays,
                            onChange = viewModel::onDueOffsetChange,
                            min = 1,
                            max = 28,
                        )
                        val day = ui.billingDayText.trim().toIntOrNull()
                        if (day != null) {
                            val anchor = com.alocard.app.domain.DateEngine.currentBillingDate(
                                day, com.alocard.app.domain.util.DateProvider.today()
                            )
                            val due = anchor.plusDays(ui.dueOffsetDays.toLong())
                            HintText(
                                "账单日 $day 号 → 本期还款日 ${due.monthValue} 月 ${due.dayOfMonth} 日",
                                Modifier.padding(top = 6.dp),
                            )
                            HintText(
                                "换算结果可能随月份变化（N 是唯一事实来源，PRD 5.2.3）",
                                Modifier.padding(top = 2.dp),
                            )
                        } else {
                            HintText("先填账单日，这里会实时换算还款日。", Modifier.padding(top = 6.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        FilledField(
                            value = ui.creditLimitText,
                            onValueChange = viewModel::onCreditLimitChange,
                            label = "信用额度",
                            placeholder = "0.00",
                            numeric = true,
                            supportingText = "用于汇总总授信、已用与剩余可用额度。",
                        )
                    }
                }
            }

            ui.error?.let {
                HintText(it, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(4.dp))
            }

            if (ui.isEditing) {
                ActionRow(
                    cancelText = "取消",
                    onCancel = onBack,
                    confirmText = "保存",
                    onConfirm = viewModel::save,
                    confirmEnabled = ui.canSave,
                )
            } else {
                FilledButton(
                    text = if (ui.saving) "保存中…" else "保存卡片",
                    onClick = viewModel::save,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = ui.canSave,
                )
                HintText(
                    "账单日、还款日与额度可稍后补充，补全前该组显示为「待完善」。",
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    selected: Boolean,
    title: String,
    meta: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun schemeLabel(scheme: String): String = when (scheme) {
    "union" -> "中国银联"
    "visa" -> "Visa"
    "mastercard" -> "Mastercard"
    "amex" -> "美国运通"
    "jcb" -> "JCB"
    "diners" -> "Diners Club"
    else -> "未知"
}
