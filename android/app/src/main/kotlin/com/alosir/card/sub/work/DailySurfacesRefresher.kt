package com.alosir.card.sub.work

import android.content.Context
import com.alosir.card.domain.DateEngine
import com.alosir.card.domain.repository.BillingGroupRepository
import com.alosir.card.domain.util.DateProvider
import com.alosir.card.domain.util.Money
import com.alosir.card.sub.notification.AppNotifier
import com.alosir.card.widget.GracePeriodWidgetReceiver
import com.alosir.card.widget.WidgetRow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 每日信息位刷新（PRD F7.5 / ADR-0012）：
 * 同一份「今日免息期排序」数据，小组件按尺寸展示，常驻通知展示前 3 行，
 * 保证两个被动信息位口径一致。
 *
 * 刷新时机：每日 00:05（DailyRefreshWorker）+ 每次 App 打开/回前台。
 */
@Singleton
class DailySurfacesRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val billingGroups: BillingGroupRepository,
    private val notifier: AppNotifier,
) {

    suspend fun refresh() {
        val groups = billingGroups.observeGroupsWithCards().first()
        val today = DateProvider.today()

        // 全部可用组按今日免息期降序；实际展示数量由每个组件的尺寸决定
        val ranked = groups
            .filter { it.billingDay != null && it.dueOffsetDays != null }
            .map { group ->
                group to DateEngine.gracePeriod(group.billingDay!!, group.dueOffsetDays!!, today)
            }
            .filter { it.second != null }
            .sortedByDescending { it.second }

        GracePeriodWidgetReceiver.render(
            context,
            ranked.map { (group, days) -> WidgetRow(bankName = group.name, days = days) },
        )

        // 每组一行；总额、笔数保留全部已录入且未还清账单的口径
        data class DueEntry(val dueDays: Int, val outstandingCents: Long, val groupId: Long, val bankName: String)
        val dueEntries = groups.mapNotNull { group ->
            val billingDay = group.billingDay ?: return@mapNotNull null
            val offset = group.dueOffsetDays ?: return@mapNotNull null
            val dueDays = DateEngine.daysUntilDue(billingDay, offset, today) ?: return@mapNotNull null
            val outstanding = group.currentBill?.outstandingCents ?: return@mapNotNull null
            if (outstanding <= 0L) return@mapNotNull null
            DueEntry(dueDays, outstanding, group.id, group.name)
        }
        val dueRows = dueEntries
            .sortedWith(compareBy({ if (it.dueDays < 0) 0 else 1 }, { it.dueDays }))
            .map { entry ->
                val label = when {
                    entry.dueDays < 0 -> "逾期"
                    entry.dueDays == 0 -> "今天"
                    entry.dueDays == 1 -> "明天"
                    entry.dueDays == 2 -> "后天"
                    else -> "${entry.dueDays}天后"
                }
                com.alosir.card.widget.DueWidgetRow(
                    bankName = entry.bankName,
                    label = "${entry.bankName} · $label",
                    amountCents = entry.outstandingCents,
                    groupId = entry.groupId,
                )
            }
        com.alosir.card.widget.DueBillsWidgetReceiver.render(
            context,
            dueRows,
            dueEntries.sumOf { it.outstandingCents },
        )

        // 2. 常驻通知：有临近到期 → 还款提醒内容；否则 → 免息期建议（DESIGN.md 5.9）
        val dueLines = groups.mapNotNull { group ->
            val billingDay = group.billingDay ?: return@mapNotNull null
            val offset = group.dueOffsetDays ?: return@mapNotNull null
            val dueDays = DateEngine.daysUntilDue(billingDay, offset, today) ?: return@mapNotNull null
            val bill = group.currentBill ?: return@mapNotNull null
            val settled = bill.billAmountCents != null && bill.paidAmountCents >= bill.billAmountCents
            if (settled || dueDays > 0) return@mapNotNull null
            val dueText = if (dueDays == 0) "今天还款" else "已逾期 ${-dueDays} 天"
            val outstanding = bill.billAmountCents?.let {
                Money.format((it - bill.paidAmountCents).coerceAtLeast(0L))
            } ?: "金额待录入"
            "${group.name} · $dueText · 待还 $outstanding"
        }

        if (dueLines.isNotEmpty()) {
            notifier.updatePersistentNotification(AppNotifier.PersistentContent.DueAlerts(dueLines))
        } else {
            val weekday = "一二三四五六日"
            val todayLabel = "${today.monthValue}/${today.dayOfMonth} 周${weekday[today.dayOfWeek.value - 1]}"
            notifier.updatePersistentNotification(
                AppNotifier.PersistentContent.TopGrace(
                    todayLabel = todayLabel,
                    lines = ranked.take(3).map { (group, days) -> "${group.name} 免息 ${days} 天" },
                )
            )
        }
    }
}
