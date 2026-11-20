package com.alosir.card.ui.screen.groupform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.domain.DateEngine
import com.alosir.card.ui.common.ActionRow
import com.alosir.card.ui.common.FilledField
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.common.Section
import com.alosir.card.ui.common.SectionTitle
import com.alosir.card.ui.common.Stepper
import com.alosir.card.ui.theme.AloCardTheme

@Composable
fun GroupFormScreen(
    groupId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onPickIcon: () -> Unit = {},
    viewModel: GroupFormViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(com.alosir.card.ui.screen.icons.IconSelectionBus.selectedLogoKey) {
        val key = com.alosir.card.ui.screen.icons.IconSelectionBus.selectedLogoKey
        if (key != null) {
            viewModel.onLogoSelected(key)
            com.alosir.card.ui.screen.icons.IconSelectionBus.selectedLogoKey = null
        }
    }

    LaunchedEffect(groupId) { if (groupId > 0) viewModel.load(groupId) }
    LaunchedEffect(ui.saved) { if (ui.saved) onSaved() }
    LaunchedEffect(ui.deleted) { if (ui.deleted) onDeleted() }

    PageScaffold(
        title = if (ui.isEditing) "编辑账单组" else "新建账单组",
        onBack = onBack,
        bottomBar = {
            ActionRow(
                cancelText = "取消",
                onCancel = onBack,
                confirmText = "保存",
                onConfirm = viewModel::save,
                confirmEnabled = !ui.saving && ui.nameError == null &&
                    ui.billingDayError == null && ui.dueError == null,
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
        ) {
            Section {
                SectionTitle("银行")
                FilledField(
                    value = ui.name,
                    onValueChange = viewModel::onNameChange,
                    label = "银行（标准维度）",
                    placeholder = "如「招商银行」",
                    isError = ui.nameError != null,
                    supportingText = ui.nameError ?: "同银行多卡共享额度（取最高，不叠加）；合并出账的卡共用账期。",
                    trailingIcon = {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(28.dp)
                                .clickable(onClick = onPickIcon),
                        ) {
                            com.alosir.card.ui.components.GroupLogo(
                                bankName = ui.name,
                                logoKey = ui.logoKey,
                                glyph = "银",
                                color = androidx.compose.ui.graphics.Color.Transparent,
                                size = 28.dp,
                            )
                        }
                    },
                )
            }
            Section {
                SectionTitle("账期")
                Text_stepper_label("账单日（点选，每月固定）")
                Spacer(Modifier.height(8.dp))
                com.alosir.card.ui.common.BillingDayPicker(
                    selected = ui.billingDayText.trim().toIntOrNull(),
                    onSelect = { viewModel.onBillingDayChange(it.toString()) },
                )
                if ((ui.billingDayText.trim().toIntOrNull() ?: 0) >= 29) {
                    HintText("无此日期的取月末最后一天", Modifier.padding(top = 6.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text_stepper_label("最后还款日")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(
                        selected = ui.dueMode == DateEngine.DUE_MODE_OFFSET,
                        text = "按账单日推后",
                        onClick = { viewModel.onDueModeChange(DateEngine.DUE_MODE_OFFSET) },
                    )
                    ChoiceChip(
                        selected = ui.dueMode == DateEngine.DUE_MODE_FIXED_DAY,
                        text = "每月固定",
                        onClick = { viewModel.onDueModeChange(DateEngine.DUE_MODE_FIXED_DAY) },
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (ui.dueMode == DateEngine.DUE_MODE_OFFSET) {
                    Stepper(value = ui.dueOffsetDays, onChange = viewModel::onDueOffsetChange, min = 1, max = 28)
                } else {
                    FilledField(
                        value = ui.fixedDayText,
                        onValueChange = viewModel::onFixedDayChange,
                        label = "每月几号还款",
                        placeholder = "如 25",
                        numeric = true,
                        isError = ui.dueError != null,
                        supportingText = ui.dueError ?: "还款日固定，不再换算为推后天数；无此日期取月末。",
                    )
                }
                ui.duePreview?.let { HintText(it, Modifier.padding(top = 6.dp)) }
            }
            Section {
                SectionTitle("额度")
                FilledField(
                    value = ui.creditLimitText,
                    onValueChange = viewModel::onCreditLimitChange,
                    label = "信用额度",
                    placeholder = "0.00",
                    numeric = true,
                    supportingText = "组内多张卡共享这一份额度。",
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Text_stepper_label(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ChoiceChip(selected: Boolean, text: String, onClick: () -> Unit) {
    val colors = AloCardTheme.colors
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
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
        )
    }
}
