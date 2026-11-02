package com.alosir.card.ui.screen.paymentmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alosir.card.domain.repository.BillingGroupRepository
import com.alosir.card.domain.repository.BillRepository
import com.alosir.card.domain.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 标记还款页状态（P9 / F4.1） */
data class PaymentMarkUiState(
    val loading: Boolean = true,
    val groupId: Long = -1L,
    val groupName: String = "",
    /** 账单日（本期账单日） */
    val billDate: java.time.LocalDate? = null,
    val billAmountCents: Long? = null,
    val paidAmountCents: Long? = null,
    val amountText: String = "",
    val amountError: String? = null,
    /** 账单金额未录入时不允许标记（PRD F4.1 规则 7） */
    val blockedReason: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    /** 动态提示：「本次标记 ¥X，当前已还 ¥Y / 账单 ¥Z」 */
    val liveHint: String
        get() {
            val bill = billAmountCents
            val paid = paidAmountCents ?: 0L
            val marked = Money.parseToCents(amountText) ?: 0L
            return if (bill == null) "账单金额待录入"
            else "本次标记 ${Money.format(marked)}，当前已还 ${Money.format(paid)} / 账单 ${Money.format(bill)}"
        }
}

@HiltViewModel
class PaymentMarkViewModel @Inject constructor(
    private val billingGroups: BillingGroupRepository,
    private val bills: BillRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(PaymentMarkUiState())
    val ui: StateFlow<PaymentMarkUiState> = _ui.asStateFlow()

    fun load(groupId: Long) {
        if (!_ui.value.loading) return
        viewModelScope.launch {
            billingGroups.observeGroup(groupId).first()
                ?.let { group ->
                    val bill = group.currentBill
                    _ui.update {
                        it.copy(
                            loading = false,
                            groupId = group.id,
                            groupName = group.name,
                            billDate = bill?.billingDate,
                            billAmountCents = bill?.billAmountCents,
                            paidAmountCents = bill?.paidAmountCents,
                            blockedReason = if (bill?.billAmountCents == null) {
                                "请先录入账单金额，再标记还款"
                            } else null,
                        )
                    }
                }
        }
    }

    fun onAmountChange(value: String) {
        val error = when {
            value.isBlank() -> null
            Money.parseToCents(value) == null -> "请输入非负数字，最多两位小数"
            Money.parseToCents(value) == 0L -> "标记金额须大于 0"
            else -> null
        }
        _ui.update { it.copy(amountText = value, amountError = error) }
    }

    fun save() {
        val state = _ui.value
        val cents = Money.parseToCents(state.amountText)
        val billDate = state.billDate
        if (cents == null || cents <= 0L || billDate == null) {
            _ui.update { it.copy(amountError = "请输入有效金额") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(saving = true) }
            val ok = bills.recordPayment(state.groupId, billDate, cents)
            _ui.update { it.copy(saving = false, saved = ok) }
        }
    }

    fun markSavedHandled() = _ui.update { it.copy(saved = false) }
}
