package com.alosir.card.ui.screen.addcard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.domain.CardNumberIdentifier
import com.alosir.card.data.ocr.CardOcr
import com.alosir.card.domain.util.Money
import com.alosir.card.ui.common.FilledField
import com.alosir.card.ui.common.ExpiryField
import com.alosir.card.ui.common.BillingDayPicker
import com.alosir.card.ui.common.FilledButton
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.common.Section
import com.alosir.card.ui.common.SectionTitle
import com.alosir.card.ui.common.Stepper
import com.alosir.card.ui.common.TonalButton
import com.alosir.card.ui.components.CreditCardFace
import com.alosir.card.ui.theme.AloCardTheme

/**
 * 新增/编辑卡片 · v2。
 * 实时卡面预览 + 卡号边输边识别（银行/卡组织）+ 同银行合并出账推荐。
 * 所有字段均可空，账期三项「必填但可稍后补」（ADR-0009）。
 */
@Composable
fun AddCardScreen(
    cardId: Long?,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onScanCard: () -> Unit = {},
    viewModel: AddCardViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val colors = AloCardTheme.colors

    // ---- 实时卡面识别（CameraX + ML Kit，识别到即自动回填） ----
    val context = LocalContext.current
    LaunchedEffect(com.alosir.card.ui.screen.scan.ScanResultBus.lastResult) {
        val result = com.alosir.card.ui.screen.scan.ScanResultBus.lastResult
        if (result != null) {
            com.alosir.card.ui.screen.scan.ScanResultBus.lastResult = null
            result.cardNumber?.let(viewModel::onCardNumberChange)
            result.expiryYm?.let(viewModel::onExpiryChange)
            val now = viewModel.ui.value
            if (now.bankName.isEmpty() || !now.detectedFromNumber) {
                result.bankName?.let(viewModel::onBankNameChange)
            }
        }
    }


    LaunchedEffect(cardId) {
        if (cardId != null && cardId > 0) viewModel.loadCard(cardId)
        else viewModel.restoreDraftIfFresh() // 新建：恢复 1 小时内未保存的草稿
    }
    LaunchedEffect(ui.savedGroupId) {
        val saved = ui.savedGroupId
        if (saved != null && saved > 0) onSaved(saved)
        else if (saved == -2L) onBack() // 编辑完成
    }

    PageScaffold(
        title = if (ui.isEditing) "编辑卡片" else "添加卡片",
        onBack = onBack,
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                TonalButton("取消", onBack, Modifier.weight(1f))
                FilledButton(
                    text = if (ui.saving) "保存中…" else "保存卡片",
                    onClick = viewModel::save,
                    modifier = Modifier.weight(2f),
                    enabled = ui.canSave,
                )
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp),
        ) {
            // ---- 实时卡面预览 ----
            val digits = CardNumberIdentifier.normalize(ui.cardNumber)
            val previewTail = if (digits.length >= 4) digits.takeLast(4) else "••••"
            CreditCardFace(
                bankName = ui.bankName.ifEmpty { "待识别银行" },
                orgText = schemeLabel(ui.cardScheme),
                tail = previewTail,
                graceDays = null,
                dueText = ui.billingDayText.trim().toIntOrNull()?.let { day ->
                    val anchor = com.alosir.card.domain.DateEngine.currentBillingDate(
                        day, com.alosir.card.domain.util.DateProvider.today()
                    )
                    val due = anchor.plusDays(ui.dueOffsetDays.toLong())
                    "${due.monthValue}/${due.dayOfMonth}"
                } ?: "—",
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 14.dp),
            )

            // ---- 出账方式（新建时）：独立出账 / 并入同银行合并出账 ----
            if (!ui.isEditing) {
                Section {
                    SectionTitle("出账方式")
                    Row(Modifier.fillMaxWidth()) {
                        ChoiceChip(
                            selected = ui.choice == GroupChoice.NEW,
                            text = "独立出账",
                            onClick = { viewModel.onChoiceChange(GroupChoice.NEW) },
                            modifier = Modifier.weight(1f),
                        )
                        if (ui.matchedGroup != null) {
                            Spacer(Modifier.width(8.dp))
                            ChoiceChip(
                                selected = ui.choice == GroupChoice.JOIN,
                                text = "并入${ui.matchedGroup!!.bankName ?: ui.matchedGroup!!.name}（推荐）",
                                onClick = { viewModel.onChoiceChange(GroupChoice.JOIN) },
                                modifier = Modifier.weight(2f),
                            )
                        }
                    }
                    HintText(
                        "同银行且账单日相同的多张卡可合并出账，共享账期与额度。",
                        Modifier.padding(top = 8.dp),
                    )
                    if (ui.choice == GroupChoice.JOIN && ui.matchedGroup != null) {
                        val g = ui.matchedGroup!!
                        HintText(
                            "继承：账单日 ${g.billingDay ?: "—"} 号 · 推后 ${g.dueOffsetDays ?: "—"} 天还款 · 额度 ${Money.format(g.creditLimitCents)}（不可单独修改）",
                            Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            // ---- 卡片信息 ----
            Section {
                SectionTitle("卡片信息")
                if (ui.isEditing) {
                    // 卡号与所属银行只读，不允许变更（项 7）
                    com.alosir.card.ui.common.KeyValueRow("卡号（不可变更）", "尾号 ${ui.cardTail}")
                    com.alosir.card.ui.common.KeyValueRow("银行（不可变更）", ui.bankName.ifEmpty { "未识别" })
                    Spacer(Modifier.height(12.dp))
                }
                if (!ui.isEditing) {
                    FilledField(
                        value = ui.cardNumber,
                        onValueChange = viewModel::onCardNumberChange,
                        label = "卡号",
                        placeholder = "输入即识别银行与卡组织",
                        numeric = true,
                        isError = ui.cardNumberError != null,
                        supportingText = ui.cardNumberError ?: ui.cardNumberNote ?: "支持银联 / Visa / Mastercard / JCB · 本地离线识别",
                        visualTransformation = com.alosir.card.ui.common.CardNumberVisualTransformation(),
                        trailingIcon = {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Default.PhotoCamera,
                                contentDescription = "扫一扫识别卡面",
                                tint = colors.goldText,
                                modifier = Modifier.clickable { onScanCard() }.padding(8.dp),
                            )
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                }
                FilledField(
                    value = ui.cardName,
                    onValueChange = viewModel::onCardNameChange,
                    label = "卡名称",
                    placeholder = "选填，如「经典白」",
                )
                Spacer(Modifier.height(12.dp))
                ExpiryField(
                    value = ui.expiryYm,
                    onValueChange = viewModel::onExpiryChange,
                    label = "有效期",
                )
                if (!ui.isEditing) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "常用银行（点选，或下方手输）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("招商银行", "工商银行", "建设银行", "农业银行", "中国银行", "交通银行", "中信银行", "兴业银行").forEach { bank ->
                            ChoiceChip(
                                selected = ui.bankName == bank,
                                text = bank,
                                onClick = { viewModel.onBankNameChange(bank) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FilledField(
                        value = ui.bankName,
                        onValueChange = viewModel::onBankNameChange,
                        label = "银行",
                        placeholder = "选填，识别不到时手动填写",
                        supportingText = if (ui.detectedFromNumber) "根据卡号识别，可修改" else null,
                    )
                }
                Spacer(Modifier.height(4.dp))
                HintText("卡组织：${schemeLabel(ui.cardScheme)} · 卡号在本地加密存储，界面默认只显示末 4 位。")
            }

            // ---- 账期与额度（新建组时编辑；加入组时只读继承说明；编辑卡片时可修改） ----
            if (ui.isEditing || ui.choice != GroupChoice.JOIN) {
                Section {
                    SectionTitle("账期与额度")
                    Text(
                        text = "账单日（点选，每月固定）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    BillingDayPicker(
                        selected = ui.billingDayText.trim().toIntOrNull(),
                        onSelect = { viewModel.onBillingDayChange(it.toString()) },
                    )
                    Spacer(Modifier.height(12.dp))

                    // 最后还款日：步进器实时换算（ADR-0003）
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
                        val anchor = com.alosir.card.domain.DateEngine.currentBillingDate(
                            day, com.alosir.card.domain.util.DateProvider.today()
                        )
                        val due = anchor.plusDays(ui.dueOffsetDays.toLong())
                        HintText(
                            "账单日 $day 号 → 本期还款日 ${due.monthValue} 月 ${due.dayOfMonth} 日（推后 ${ui.dueOffsetDays} 天）",
                            Modifier.padding(top = 6.dp),
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

            ui.error?.let {
                HintText(it, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(4.dp))
            }

            HintText(
                "账单日、还款日与额度可稍后补充，补全前该组显示为「待完善」。",
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}


/** 出账方式选择 chip · 金边选中态 */
@Composable
private fun ChoiceChip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AloCardTheme.colors
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.goldSoft else colors.paperSunken)
            .border(
                1.dp,
                if (selected) colors.gold.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.goldText else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
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

