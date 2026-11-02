package com.alosir.card.ui.screen.carddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alosir.card.domain.DateEngine
import com.alosir.card.data.security.SecurityManager
import com.alosir.card.domain.repository.BillingGroupRepository
import com.alosir.card.domain.repository.CardRepository
import com.alosir.card.domain.util.DateProvider
import com.alosir.card.domain.util.Money
import com.alosir.card.ui.components.BillState
import com.alosir.card.ui.components.DueState
import com.alosir.card.ui.theme.TabHue
import com.alosir.card.ui.theme.tabHueFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** 卡片详情页状态（P2，结构见 DESIGN.md 6.3） */
data class CardDetailUiState(
    val loading: Boolean = true,
    val groupId: Long = -1L,
    val groupName: String = "",
    val bankName: String? = null,
    val hue: TabHue = TabHue.INDIGO,
    val billingDay: Int? = null,
    val dueOffsetDays: Int? = null,
    val creditLimitCents: Long? = null,
    val cards: List<CardRow> = emptyList(),
    val gracePeriodDays: Int? = null,
    val dueDays: Int? = null,
    val dueText: String = "",
    val dueState: DueState = DueState.NORMAL,
    val billState: BillState = BillState.DUE_UNPAID,
    val billDate: LocalDate = DateProvider.today(),
    val dueDate: LocalDate = DateProvider.today(),
    val nextBillDate: LocalDate = DateProvider.today(),
    val billAmountCents: Long? = null,
    val paidAmountCents: Long? = null,
    /** 已解锁展示的完整卡号（会话内有效；3 秒后自动复脱敏） */
    val revealedNumber: String? = null,
) {
    /** 待还金额文案 */
    val outstandingText: String get() = Money.format(
        billAmountCents?.let { (it - (paidAmountCents ?: 0L)).coerceAtLeast(0L) }
    )
}

/** 详情页卡片行 */
data class CardRow(
    val cardId: Long,
    val displayName: String,
    val tail: String,
    val expiryYm: String?,
    val schemeDisplay: String,
    val hasNumber: Boolean,
)

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val billingGroups: BillingGroupRepository,
    private val cards: CardRepository,
    private val security: SecurityManager,
) : ViewModel() {

    /** 查看卡号闸门（PRD F1.4）：会话内已解锁则放行，否则要求输入安全密码 */
    fun isUnlocked(): Boolean = security.isSessionUnlocked()

    private val revealed = MutableStateFlow<Pair<Long, String>?>(null)
    private val flowCache = mutableMapOf<Long, StateFlow<CardDetailUiState>>()

    /* 按 groupId 缓存，避免组合期重复建流导致内容闪烁 */
    fun uiState(groupId: Long): StateFlow<CardDetailUiState> =
        flowCache.getOrPut(groupId) {
            combine(billingGroups.observeGroup(groupId), revealed) { group, rev ->
            if (group == null) {
                return@combine CardDetailUiState(loading = true, groupId = groupId)
            }
            val today = DateProvider.today()
            val grace = DateEngine.gracePeriod(group.billingDay, group.dueOffsetDays, today)
            val dueDays = DateEngine.daysUntilDue(group.billingDay, group.dueOffsetDays, today)

            val bill = group.currentBill
            val billState = when (DateEngine.billStatus(
                bill?.billAmountCents?.toDouble(),
                bill?.paidAmountCents?.toDouble(),
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
            val dueText = when {
                rawDue == DueState.OVERDUE -> "已逾期 ${-dueDays!!} 天"
                rawDue == DueState.DUE -> "今天还款"
                billState == BillState.SETTLED -> "本期已结清"
                dueDays == null -> "补充账期后可用"
                else -> "$dueDays 天后还款"
            }
            val dueState = if (billState == BillState.SETTLED && rawDue != DueState.OVERDUE) {
                DueState.NORMAL
            } else {
                rawDue
            }

            val fallbackDay = group.billingDay ?: 1
            val fallbackOffset = group.dueOffsetDays ?: 20
            val billDate = DateEngine.currentBillingDate(fallbackDay, today)

            CardDetailUiState(
                loading = false,
                groupId = group.id,
                groupName = group.name,
                bankName = group.bankName,
                hue = tabHueFor(group.bankName),
                billingDay = group.billingDay,
                dueOffsetDays = group.dueOffsetDays,
                creditLimitCents = group.creditLimitCents,
                cards = group.cards.map { card ->
                    CardRow(
                        cardId = card.id,
                        displayName = card.cardLabel?.takeIf { it.isNotBlank() }
                            ?: "${card.bankName ?: group.bankName ?: "银行卡"} · 尾号 ${card.cardNumberLast4 ?: "????"}",
                        tail = card.cardNumberLast4 ?: "????",
                        expiryYm = card.expiryYm,
                        schemeDisplay = schemeDisplay(card.cardScheme),
                        hasNumber = card.hasCardNumber,
                    )
                },
                gracePeriodDays = grace,
                dueDays = dueDays,
                dueText = dueText,
                dueState = dueState,
                billState = billState,
                billDate = billDate,
                dueDate = DateEngine.dueDateOf(fallbackDay, fallbackOffset, today),
                nextBillDate = DateEngine.nextBillingDate(fallbackDay, today),
                billAmountCents = bill?.billAmountCents,
                paidAmountCents = bill?.paidAmountCents,
                revealedNumber = rev?.second,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardDetailUiState(groupId = groupId))
        }

    /** 查看完整卡号（须在安全密码校验后进行，PRD F1.4） */
    fun revealCardNumber(cardId: Long) {
        if (!security.isSessionUnlocked()) return
        viewModelScope.launch {
            val plain = cards.decryptCardNumber(cardId) ?: return@launch
            revealed.value = cardId to plain
            delay(30_000) // 明文展示 30 秒后自动复脱敏（替换敏感信息不常驻屏幕）
            if (revealed.value?.first == cardId) revealed.value = null
        }
    }

    fun hideCardNumber() {
        revealed.value = null
    }

    companion object {
        fun schemeDisplay(scheme: String): String = when (scheme) {
            "union" -> "中国银联"
            "visa" -> "Visa"
            "mastercard" -> "Mastercard"
            "amex" -> "美国运通"
            "jcb" -> "JCB"
            "diners" -> "Diners Club"
            else -> "未知"
        }
    }
}
