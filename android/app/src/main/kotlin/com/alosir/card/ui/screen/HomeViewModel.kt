package com.alosir.card.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alosir.card.domain.DateEngine
import com.alosir.card.domain.model.CreditTotals
import com.alosir.card.domain.model.GroupWithCards
import com.alosir.card.domain.repository.BillingGroupRepository
import com.alosir.card.domain.repository.BillRepository
import com.alosir.card.domain.repository.SettingsRepository
import com.alosir.card.domain.util.DateProvider
import com.alosir.card.ui.components.BillState
import com.alosir.card.ui.components.DueState
import com.alosir.card.ui.theme.TabHue
import com.alosir.card.ui.theme.tabHueFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** 排序模式（PRD F5.1） */
enum class SortMode(val label: String) {
    /** 按免息期降序 —— 今日刷卡推荐顺序（默认，ADR-0001） */
    GRACE("免息期"),

    /** 按距还款日升序 —— 紧迫度优先 */
    DUE("距还款日"),
}

/** 列表中的一张卡 */
data class CardItem(
    val cardId: Long,
    val displayName: String,
    val tail: String,
    /** 卡组织：union / visa / mastercard / unknown… */
    val scheme: String = "unknown",
)

/** 列表中的一个账单组（同银行多卡合并出账） */
data class GroupItem(
    val groupId: Long,
    val groupName: String,
    val bankName: String,
    val cards: List<CardItem>,
    val hue: TabHue,
    /** 免息期天数；数据不完整时为 null，此时行显示「待完善」 */
    val gracePeriodDays: Int?,
    /** 距还款日天数（可为负 = 逾期）；数据不完整时为 null */
    val dueDays: Int?,
    val dueText: String,
    val dueSubText: String,
    val dueState: DueState,
    val billState: BillState,
    val billDate: LocalDate,
    val dueDate: LocalDate,
    /** 本期账单金额（分）；null = 待录入 */
    val billAmountCents: Long? = null,
    /** 已还款金额（分），累计式 */
    val paidAmountCents: Long = 0L,
    /** 下一个还款日距今的天数（已结清时排序与展示用） */
    val nextDueDays: Int? = null,
    /** 下期账单日（已结清时右侧「下期出账」用） */
    val nextBillDate: LocalDate? = null,
)

/** 首页 UI 状态 */
data class HomeUiState(
    val today: LocalDate = DateProvider.today(),
    val todayLabel: String = today.label(),
    val groups: List<GroupItem> = emptyList(),
    val totals: CreditTotals = CreditTotals(0L, 0L),
    val todoSummary: String = "",
    /** 待办聚合条点击时直达的账单组（第一个有待办项的组） */
    val todoTargetGroupId: Long? = null,
    val sortMode: SortMode = SortMode.GRACE,
)

internal fun LocalDate.label(): String {
    val weekdays = "一二三四五六日"
    return "${monthValue} 月 ${dayOfMonth} 日 · 周${weekdays[dayOfWeek.value - 1]}"
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val billingGroups: BillingGroupRepository,
    private val bills: BillRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    init {
        // 账单滚动不依赖用户操作（PRD F3.3）：打开首页即补齐本期
        viewModelScope.launch { bills.syncBillPeriods() }
    }

    private val sortModeFlow = settings.observeSortMode().map { stored ->
        if (stored == "due") SortMode.DUE else SortMode.GRACE
    }

    val uiState: StateFlow<HomeUiState> =
        combine(
            billingGroups.observeGroupsWithCards(),
            billingGroups.observeTotals(),
            sortModeFlow,
        ) { groups, totals, mode ->
            buildState(groups, totals, mode)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    fun onSortModeChange(mode: SortMode) {
        viewModelScope.launch { settings.setSortMode(mode.name.lowercase()) }
    }

    private fun buildState(
        groups: List<GroupWithCards>,
        totals: CreditTotals,
        sort: SortMode,
    ): HomeUiState {
        val today = DateProvider.today()

        val items = groups.map { group ->
            val grace = DateEngine.gracePeriod(group.billingDay, group.dueOffsetDays, today)
            val dueDays = DateEngine.daysUntilDue(group.billingDay, group.dueOffsetDays, today)

            val bill = group.currentBill
            val billStatus = DateEngine.billStatus(
                bill?.billAmountCents?.toDouble(),
                bill?.paidAmountCents?.toDouble(),
            )
            val billState = when (billStatus) {
                DateEngine.BILL_PENDING_ENTRY -> BillState.PENDING_ENTRY
                DateEngine.BILL_PARTIALLY_PAID -> BillState.PARTIALLY_PAID
                DateEngine.BILL_SETTLED -> BillState.SETTLED
                else -> BillState.DUE_UNPAID
            }
            val rawDueState = if (dueDays == null) DueState.NORMAL else when (DateEngine.dueStatus(dueDays)) {
                DateEngine.STATUS_DUE -> DueState.DUE
                DateEngine.STATUS_OVERDUE -> DueState.OVERDUE
                else -> DueState.NORMAL
            }

            val fallbackBillDay = group.billingDay ?: 1
            val fallbackOffset = group.dueOffsetDays ?: 20
            val billDate = DateEngine.currentBillingDate(fallbackBillDay, today)
            val dueDate = DateEngine.dueDateOf(fallbackBillDay, fallbackOffset, today)

            // 呈现规则（PRD 5.6.4 + HTML 原型已确认的处理）
            val dueText = when {
                rawDueState == DueState.OVERDUE -> "已逾期 ${-dueDays!!} 天"
                rawDueState == DueState.DUE -> "今天还款"
                billState == BillState.SETTLED -> "本期已结清"
                dueDays == null -> "补充账期后可用"
                else -> "$dueDays 天后还款"
            }
            // 已结清（剩余待还 ≤ 0）永不逾期——即使已过最后还款日
            val dueState = if (billState == BillState.SETTLED) {
                DueState.NORMAL
            } else {
                rawDueState
            }

            GroupItem(
                groupId = group.id,
                groupName = group.name,
                bankName = group.bankName ?: group.name,
                cards = group.cards.map { card ->
                    CardItem(
                        cardId = card.id,
                        displayName = card.cardLabel?.takeIf { it.isNotBlank() }
                            ?: "${card.bankName ?: group.bankName ?: "银行卡"} · 尾号 ${card.cardNumberLast4 ?: "????"}",
                        tail = card.cardNumberLast4 ?: "????",
                        scheme = card.cardScheme,
                    )
                },
                hue = tabHueFor(group.bankName),
                gracePeriodDays = grace,
                dueDays = dueDays,
                dueText = dueText,
                dueSubText = "${dueDate.monthValue}/${dueDate.dayOfMonth} 还款",
                dueState = dueState,
                billState = billState,
                billDate = billDate,
                dueDate = dueDate,
                billAmountCents = group.currentBill?.billAmountCents,
                paidAmountCents = group.currentBill?.paidAmountCents ?: 0L,
                nextDueDays = java.time.temporal.ChronoUnit.DAYS.between(
                    today,
                    DateEngine.nextBillingDate(fallbackBillDay, today).plusDays(fallbackOffset.toLong()),
                ).toInt(),
                nextBillDate = DateEngine.nextBillingDate(fallbackBillDay, today),
            )
        }

        // 排序对象是账单组（PRD F5.1 规则 3）；待完善（null）恒排在最后
        val sorted = when (sort) {
            SortMode.GRACE -> items.sortedWith(
                compareByDescending<GroupItem> { it.gracePeriodDays ?: Int.MIN_VALUE }
                    .thenBy { it.groupName }
            )
            SortMode.DUE -> items.sortedWith(
                // 已逾期 > 正常待还 > 已结清；未还清按本期还款日，已结清按下个还款日
                compareBy<GroupItem> {
                    when {
                        it.dueState == DueState.OVERDUE -> 0
                        it.billState == BillState.SETTLED -> 2
                        else -> 1
                    }
                }.thenBy { it.dueDays ?: it.nextDueDays ?: Int.MAX_VALUE },
            )
        }

        val pendingItems = items.filter {
            it.billState == BillState.PENDING_ENTRY && it.dueState != DueState.OVERDUE
        }
        val dueItems = items.filter {
            it.dueState != DueState.NORMAL && it.billState != BillState.SETTLED
        }
        val todo = buildList {
            if (pendingItems.isNotEmpty()) add("${pendingItems.size} 个账单组待录入")
            if (dueItems.isNotEmpty()) add("${dueItems.size} 个账单组${if (dueItems.any { it.dueState == DueState.DUE }) "今天还款" else "临近还款"}")
        }.joinToString(" · ")

        return HomeUiState(
            today = today,
            todayLabel = today.label(),
            groups = sorted,
            totals = totals,
            todoSummary = todo,
            todoTargetGroupId = (pendingItems.firstOrNull() ?: dueItems.firstOrNull())?.groupId,
            sortMode = sort,
        )
    }
}
