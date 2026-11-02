package com.alocard.app.ui.screen.paymentmark

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
import com.alocard.app.ui.common.ActionRow
import com.alocard.app.ui.common.FilledField
import com.alocard.app.ui.common.HintText
import com.alocard.app.ui.common.PageScaffold
import com.alocard.app.ui.common.Section
import com.alocard.app.ui.common.SectionTitle

/**
 * 标记还款页（P9 / F4.1）。累计式：多次标记金额相加。
 * 标记后已用额度即时释放、剩余可用恢复（ADR-0007）。
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

    PageScaffold(title = "标记还款", onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
        ) {
            Section {
                SectionTitle(ui.groupName)
                HintText(ui.liveHint)
                Spacer(Modifier.height(12.dp))

                if (ui.blockedReason != null) {
                    HintText(ui.blockedReason!!, color = MaterialTheme.colorScheme.error)
                } else {
                    FilledField(
                        value = ui.amountText,
                        onValueChange = viewModel::onAmountChange,
                        label = "本次还款金额",
                        placeholder = "0.00",
                        numeric = true,
                        isError = ui.amountError != null,
                        supportingText = ui.amountError
                            ?: "可标记部分或全部；多次标记为累计。",
                    )
                    HintText("标记后可用额度即时释放。", Modifier.padding(top = 6.dp))
                }
            }

            if (ui.blockedReason == null) {
                ActionRow(
                    cancelText = "取消",
                    onCancel = onBack,
                    confirmText = "确认标记",
                    onConfirm = viewModel::save,
                    confirmEnabled = !ui.saving && ui.amountError == null &&
                        ui.amountText.isNotBlank(),
                )
            } else {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
