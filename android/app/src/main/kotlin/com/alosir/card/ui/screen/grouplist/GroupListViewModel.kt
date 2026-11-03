package com.alosir.card.ui.screen.grouplist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alosir.card.domain.DateEngine
import com.alosir.card.domain.repository.BillingGroupRepository
import com.alosir.card.ui.components.BillState
import com.alosir.card.ui.components.DueState
import com.alosir.card.ui.theme.TabHue
import com.alosir.card.ui.theme.tabHueFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/** 银行账单行（账单页 = 按银行呈现，同银行多卡合并出账） */
data class BankBillRow(
    val groupId: Long,
    val bankName: String,
    val cardCount: Int,
    val firstTail: String,
    val hue: TabHue,
    val gracePeriodDays: Int?,
    val dueDays: Int?,
    val dueText: String,
    val dueSubText: String,
    val dueState: DueState,
    val billState: BillState,
    val billDate: LocalDate,
    val dueDate: LocalDate,
    val billAmountCents: Long?,
    val paidAmountCents: Long,
    val creditLimitCents: Long?,
)

/** 账单页状态 */
data class GroupListUiState(
    val loading: Boolean = true,
    val rows: List<BankBillRow> = emptyList(),
)

@HiltViewModel
class GroupListViewModel @Inject constructor(
    billingGroups: BillingGroupRepository,
) : ViewModel() {

    val uiState: StateFlow<GroupListUiState> =
        billingGroups.observeGroupsWithCards().map { groups ->
            val today = com.alosir.card.domain.util.DateProvider.today()
            val rows = groups.map { group ->
                val grace = DateEngine.gracePeriod(group.billingDay, group.dueOffsetDays, today)
                val dueDays = DateEngine.daysUntilDue(group.billingDay, group.dueOffsetDays, today)
                val bill = group.currentBill
                val billState = when (DateEngine.billStatus(
                    bill?.billAmountCents?.toDouble(), bill?.paidAmountCents?.toDouble(),
                )) {
                    DateEngine.BILL_PENDING_ENTRY -> BillState.PENDING_ENTRY
                    DateEngine.BILL_PARTIALLY_PAID -> BillState.PARTIALLY_PAID
                    DateEngine.BILL_SETTLED -> BillState.SETTLED
                    else -> BillState.DUE_UNPAID
                }
                val rawDue = if (dueDays == null) DueState.NORMAL else when (DateEngine.dueStatus(dueDays)) {
                    DateEngine.STATUS_DUE -> DueState.DUE
                    DateEngine.STATUS_OVERDUE -> DueState.OVERDUE
                    else -> DueState.NORMAL
                }
                val billDay = group.billingDay ?: 1
                val offset = group.dueOffsetDays ?: 20
                BankBillRow(
                    groupId = group.id,
                    bankName = group.bankName ?: group.name,
                    cardCount = group.cards.size,
                    firstTail = group.cards.firstOrNull()?.cardNumberLast4 ?: "????",
                    hue = tabHueFor(group.bankName),
                    gracePeriodDays = grace,
                    dueDays = dueDays,
                    dueText = when {
                        rawDue == DueState.OVERDUE -> "已逾期 ${-dueDays!!} 天"
                        rawDue == DueState.DUE -> "今天还款"
                        billState == BillState.SETTLED -> "本期已结清"
                        dueDays == null -> "补充账期后可用"
                        else -> "$dueDays 天后还款"
                    },
                    dueSubText = DateEngine.dueDateOf(billDay, offset, today).let {
                        "${it.monthValue}/${it.dayOfMonth} 还款"
                    },
                    dueState = if (billState == BillState.SETTLED) DueState.NORMAL else rawDue,
                    billState = billState,
                    billDate = DateEngine.currentBillingDate(billDay, today),
                    dueDate = DateEngine.dueDateOf(billDay, offset, today),
                    billAmountCents = bill?.billAmountCents,
                    paidAmountCents = bill?.paidAmountCents ?: 0L,
                    creditLimitCents = group.creditLimitCents,
                )
            }.sortedWith(compareBy { it.dueDays ?: Int.MAX_VALUE })
            GroupListUiState(loading = false, rows = rows)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupListUiState())
}
