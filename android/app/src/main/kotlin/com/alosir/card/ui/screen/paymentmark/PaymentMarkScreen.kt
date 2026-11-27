package com.alosir.card.ui.screen.paymentmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.domain.util.Money
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

    val remaining = ui.remainingCents
    val currentCents = Money.parseToCents(ui.amountText) ?: 0L
    val allFilled = remaining > 0L && currentCents == remaining
    val oddAvailable = remaining > 0L && remaining % 10_000L != 0L &&
        viewModel.oddAmountForClick(0) != null
    var oddClick by remember { mutableStateOf(0) }

    val quickLabel = when {
        oddAvailable -> "还零头"
        remaining > 0L && !allFilled -> "全部"
        else -> null
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
        quickActionLabel = quickLabel,
        quickAction = when (quickLabel) {
            "还零头" -> {
                {
                    viewModel.fillOdd(oddClick)
                    oddClick += 1
                }
            }
            "全部" -> viewModel::fillAllOutstanding
            else -> null
        },
        onClear = if (ui.amountText.isNotEmpty() && ui.amountText != "0") {
            { viewModel.onAmountChange("") }
        } else {
            null
        },
        confirmText = "确认标记",
        onConfirm = viewModel::save,
        confirmEnabled = !ui.saving && ui.amountError == null && ui.amountText.isNotBlank(),
        blockedReason = ui.blockedReason,
    )
}
