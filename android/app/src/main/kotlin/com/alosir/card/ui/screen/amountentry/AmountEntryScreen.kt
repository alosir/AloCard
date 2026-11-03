package com.alosir.card.ui.screen.amountentry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.ui.common.AmountSheetScaffold

/**
 * 录入账单金额 · v0.0.3：底部浮层 + APP 内数字键盘。
 * 覆盖式：重复录入为最后一次的值，不累加（ADR-0004）。
 * 已还/待还提示只显示一次（修复旧版重复两行）。
 */
@Composable
fun AmountEntryScreen(
    groupId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AmountEntryViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(groupId) { viewModel.load(groupId) }
    LaunchedEffect(ui.saved) {
        if (ui.saved) {
            viewModel.markSavedHandled()
            onSaved()
        }
    }

    AmountSheetScaffold(
        title = "录入账单金额",
        onDismiss = onBack,
        amountText = ui.amountText,
        onAmountChange = viewModel::onAmountChange,
        amountLabel = "本期账单金额（元）",
        error = ui.amountError,
        infoLine = "${ui.groupName} · 本期账单日 ${ui.billDateText}",
        fieldHint = ui.hint ?: "重复录入为覆盖，不累加；之后可随时修改。",
        confirmText = "保存",
        onConfirm = viewModel::save,
        confirmEnabled = !ui.saving && ui.amountError == null && ui.amountText.isNotBlank(),
        blockedReason = if (ui.entryOpen) null else ui.closedReason ?: "录入未开放",
    )
}
