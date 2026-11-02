package com.alosir.card.ui.screen.amountentry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/**
 * 录入账单金额页（P8 / F3.1）。
 * 覆盖式：重复录入为最后一次的值，不累加（ADR-0004）。
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

    PageScaffold(title = "录入账单金额", onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
        ) {
            Section {
                SectionTitle(ui.groupName)
                HintText("本期账单日：${ui.billDateText}")
                Spacer(Modifier.height(12.dp))

                if (!ui.entryOpen) {
                    HintText(ui.closedReason ?: "录入未开放", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                } else {
                    FilledField(
                        value = ui.amountText,
                        onValueChange = viewModel::onAmountChange,
                        label = "本期账单金额",
                        placeholder = "0.00",
                        numeric = true,
                        isError = ui.amountError != null,
                        supportingText = ui.amountError ?: ui.hint
                            ?: "录入后可在卡片详情中随时修改，重复录入为覆盖而非累加。",
                    )
                    ui.hint?.let {
                        HintText(it, Modifier.padding(top = 6.dp))
                    }
                }
            }

            if (ui.entryOpen) {
                ActionRow(
                    cancelText = "取消",
                    onCancel = onBack,
                    confirmText = "保存",
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
