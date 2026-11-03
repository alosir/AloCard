package com.alosir.card.ui.screen.paymentmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.ui.common.AmountSheetScaffold

/**
 * 标记还款 · v0.0.3：底部浮层 + APP 内数字键盘 + 「全部」一键填剩余待还。
 * 累计式：多次标记金额相加；标记后已用额度即时释放（ADR-0007）。
 */
@Composable
fun PaymentMarkScreen(
    groupId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: PaymentMarkViewModel = hiltViewModel(),
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
        title = "标记还款",
        onDismiss = onBack,
        amountText = ui.amountText,
        onAmountChange = viewModel::onAmountChange,
        amountLabel = "本次还款金额（元）",
        error = ui.amountError,
        infoLine = ui.groupName,
        fieldHint = "可部分或全部，多次标记为累计；标记后额度即时释放。",
        onFillAll = viewModel::fillAllOutstanding,
        confirmText = "确认标记",
        onConfirm = viewModel::save,
        confirmEnabled = !ui.saving && ui.amountError == null && ui.amountText.isNotBlank(),
        blockedReason = ui.blockedReason,
    )
}
