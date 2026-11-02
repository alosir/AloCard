package com.alocard.app.ui.screen.amountentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alocard.app.domain.DateEngine
import com.alocard.app.domain.repository.BillingGroupRepository
import com.alocard.app.domain.repository.BillRepository
import com.alocard.app.domain.util.DateProvider
import com.alocard.app.domain.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 录入账单金额页状态（P8 / F3.1） */
data class AmountEntryUiState(
    val loading: Boolean = true,
    val groupName: String = "",
    val groupId: Long = -1L,
    /** 本期账单日 */
    val billDate: java.time.LocalDate? = null,
    val billDateText: String = "",
    /** 录入窗口是否开启（账单日次日起，PRD F3.1 规则 1） */
    val entryOpen: Boolean = false,
    val closedReason: String? = null,
    val amountText: String = "",
    val amountError: String? = null,
    val paidAmountCents: Long? = null,
    val currentAmountCents: Long? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    /** 差额提示：「已还 ¥3,000 / 待还 ¥5,600」 */
    val hint: String?
        get() {
            val paid = paidAmountCents ?: return null
            val bill = currentAmountCents ?: return null
            return "已还 ${Money.format(paid)} / 待还 ${Money.format((bill - paid).coerceAtLeast(0L))}"
        }
}

@HiltViewModel
class AmountEntryViewModel @Inject constructor(
    private val billingGroups: BillingGroupRepository,
    private val bills: BillRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AmountEntryUiState())
    val ui: StateFlow<AmountEntryUiState> = _ui.asStateFlow()

    fun load(groupId: Long) {
        if (!_ui.value.loading) return
        viewModelScope.launch {
            billingGroups.observeGroup(groupId).first()
                ?.let { group ->
                    val today = DateProvider.today()
                    val billingDay = group.billingDay
                    val current = billingDay?.let { DateEngine.currentBillingDate(it, today) }
                    val bill = group.currentBill
                    // 录入窗口：账单日当天不可录入，次日起开放（ADR-0002/0004）
                    val open = billingDay != null && current != null &&
                        (bill?.billingDate ?: current) < today
                    _ui.update {
                        it.copy(
                            loading = false,
                            groupId = group.id,
                            groupName = group.name,
                            billDate = current,
                            billDateText = current?.let { d -> "${d.monthValue} 月 ${d.dayOfMonth} 日" } ?: "—",
                            entryOpen = open,
                            closedReason = when {
                                billingDay == null -> "账单组账期未完善，无法定位本期账单"
                                !open -> "本期账单尚未开启录入（账单日次日开放）"
                                else -> null
                            },
                            paidAmountCents = bill?.paidAmountCents,
                            currentAmountCents = bill?.billAmountCents,
                        )
                    }
                }
        }
    }

    fun onAmountChange(value: String) {
        val error = if (value.isBlank()) null
        else if (Money.parseToCents(value) == null) "请输入非负数字，最多两位小数"
        else null
        _ui.update { it.copy(amountText = value, amountError = error) }
    }

    fun save() {
        val state = _ui.value
        val cents = Money.parseToCents(state.amountText)
        if (cents == null) {
            _ui.update { it.copy(amountError = "请输入有效金额") }
            return
        }
        viewModelScope.launch {
            val billDate = state.billDate ?: return@launch
            _ui.update { it.copy(saving = true) }
            bills.saveBillAmount(
                groupId = state.groupId,
                billDate = billDate,
                amountCents = cents,
            )
            _ui.update { it.copy(saving = false, saved = true) }
        }
    }

    fun markSavedHandled() = _ui.update { it.copy(saved = false) }
}
