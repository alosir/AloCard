package com.alosir.card.ui.screen.billhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alosir.card.domain.DateEngine
import com.alosir.card.domain.repository.BillingGroupRepository
import com.alosir.card.domain.repository.BillRepository
import com.alosir.card.domain.util.DateProvider
import com.alosir.card.domain.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** 历史账单行（P6 / F3.2 / F3.4） */
data class BillHistoryRow(
    val billId: Long,
    val billingDate: LocalDate,
    val amountText: String,
    val paidText: String,
    val outstandingText: String,
    /** 账单状态：未录入 / 待还款 / 部分还款 / 已结清 */
    val statusLabel: String,
    /** 历史逾期判定（F3.4）：该期自身到期日已过且未结清，如「已过期 7 天」 */
    val overdueText: String?,
    val canUndoLastPayment: Boolean,
)

data class BillHistoryUiState(
    val loading: Boolean = true,
    val groupName: String = "",
    val rows: List<BillHistoryRow> = emptyList(),
)

@HiltViewModel
class BillHistoryViewModel @Inject constructor(
    private val billingGroups: BillingGroupRepository,
    private val bills: BillRepository,
) : ViewModel() {

    private val flowCache = mutableMapOf<Long, StateFlow<BillHistoryUiState>>()

    /* 按 groupId 缓存，避免组合期重复建流导致内容闪烁 */
    fun uiState(groupId: Long): StateFlow<BillHistoryUiState> =
        flowCache.getOrPut(groupId) {
            combine(billingGroups.observeGroup(groupId), bills.observeBills(groupId)) { group, billsList ->
            if (group == null) return@combine BillHistoryUiState()
            val today = DateProvider.today()
            val offset = group.dueOffsetDays

            val rows = billsList.map { bill ->
                val status = DateEngine.billStatus(
                    bill.billAmountCents?.toDouble(),
                    bill.paidAmountCents.toDouble(),
                )
                val statusLabel = when (status) {
                    DateEngine.BILL_PENDING_ENTRY -> "未录入"
                    DateEngine.BILL_DUE_UNPAID -> "待还款"
                    DateEngine.BILL_PARTIALLY_PAID -> "部分还款"
                    else -> "已结清"
                }
                // 历史逾期判定独立于主坐标（PRD F3.4）：
                // 该期 billingDate + N 早于今天即逾期；已结清不显示。
                // 改 N 不追溯历史（PRD F2.4 规则 4）——用当前 N 判定是既定口径的近似；
                // 未设置 N（待完善组）时跳过时效判定。
                val overdueText = if (offset != null && status != DateEngine.BILL_SETTLED) {
                    val due = bill.billingDate.plusDays(offset.toLong())
                    val days = ChronoUnit.DAYS.between(due, today)
                    if (days > 0) "已过期 $days 天" else null
                } else {
                    null
                }
                BillHistoryRow(
                    billId = bill.id,
                    billingDate = bill.billingDate,
                    amountText = Money.format(bill.billAmountCents),
                    paidText = Money.format(bill.paidAmountCents),
                    outstandingText = Money.format(bill.outstandingCents),
                    statusLabel = statusLabel,
                    overdueText = overdueText,
                    canUndoLastPayment = bill.canUndoLastPayment,
                )
            }
            BillHistoryUiState(loading = false, groupName = group.name, rows = rows)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillHistoryUiState())
        }

    /** 撤销最近一次标记（PRD F4.2：仅在账单历史页提供） */
    fun undoLastPayment(groupId: Long, row: BillHistoryRow) {
        viewModelScope.launch {
            bills.undoLastPayment(groupId, row.billingDate)
        }
    }
}
