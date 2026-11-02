package com.alosir.card.ui.screen.groupform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.ui.common.ActionRow
import com.alosir.card.ui.common.FilledField
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.common.Section
import com.alosir.card.ui.common.SectionTitle
import com.alosir.card.ui.common.Stepper

/**
 * 账单组表单页（P4）：新建独立账单组，或编辑已有组的账期/额度。
 * 「固定几号」与「推后几天」两种输入实时双向换算（ADR-0003）。
 */
@Composable
fun GroupFormScreen(
    groupId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: GroupFormViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(groupId) { if (groupId > 0) viewModel.load(groupId) }
    LaunchedEffect(ui.saved) { if (ui.saved) onSaved() }
    LaunchedEffect(ui.deleted) { if (ui.deleted) onDeleted() }

    PageScaffold(
        title = if (ui.isEditing) "编辑账单组" else "新建账单组",
        onBack = onBack,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
        ) {
            Section {
                SectionTitle("基本信息")
                FilledField(
                    value = ui.name,
                    onValueChange = viewModel::onNameChange,
                    label = "账单组名称",
                    placeholder = "如「招商银行」",
                    isError = ui.nameError != null,
                    supportingText = ui.nameError,
                )
            }
            Section {
                SectionTitle("账期")
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
                Text_stepper_label("最后还款日（推后 N 天）")
                Spacer(Modifier.height(6.dp))
                Stepper(value = ui.dueOffsetDays, onChange = viewModel::onDueOffsetChange, min = 1, max = 28)
                ui.duePreview?.let {
                    HintText(it, Modifier.padding(top = 6.dp))
                    HintText("换算结果可能随月份变化（N 是唯一事实来源）", Modifier.padding(top = 2.dp))
                }
                Spacer(Modifier.height(12.dp))
                FixedDayShortcut(onCommit = viewModel::onFixedDayCommit)
            }
            Section {
                SectionTitle("额度")
                FilledField(
                    value = ui.creditLimitText,
                    onValueChange = viewModel::onCreditLimitChange,
                    label = "信用额度",
                    placeholder = "0.00",
                    numeric = true,
                    supportingText = "组内多张卡共享这一份额度（ADR-0006）。",
                )
            }

            ActionRow(
                cancelText = "取消",
                onCancel = onBack,
                confirmText = "保存",
                onConfirm = viewModel::save,
                confirmEnabled = !ui.saving && ui.nameError == null && ui.billingDayError == null,
            )
        }
    }
}

@Composable
private fun Text_stepper_label(text: String) {
    androidx.compose.material3.Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 「固定几号」快捷输入：内容构成合法日号时立即换算为 N 并清空 */
@Composable
private fun FixedDayShortcut(onCommit: (String) -> Unit) {
    val value = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    com.alosir.card.ui.common.FilledField(
        value = value.value,
        onValueChange = { input ->
            value.value = input
            val day = input.trim().toIntOrNull()
            if (day != null && day in 1..28 && input.trim() == day.toString()) {
                onCommit(input)
                value.value = ""
            }
        },
        label = "或按「固定几号」输入",
        placeholder = "如 25（每月 25 号还款）",
        numeric = true,
        supportingText = "输入日号后自动换算为推后天数",
    )
}
