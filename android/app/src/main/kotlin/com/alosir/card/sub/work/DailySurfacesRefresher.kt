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
 * 同一份「今日 TOP 账单组」数据，渲染成小组件（5 行）与常驻通知（3 行），
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

        // 今日免息期降序 TOP5（待完善组不参与，PRD F7.5 规则 8）
        val ranked = groups
            .filter { it.billingDay != null && it.dueOffsetDays != null }
            .map { group ->
                group to DateEngine.gracePeriod(group.billingDay!!, group.dueOffsetDays!!, today)
            }
            .filter { it.second != null }
            .sortedByDescending { it.second }
            .take(5)

        // 1. 桌面小组件（RemoteViews，静态渲染入口在 Receiver）
        GracePeriodWidgetReceiver.render(
            context,
            ranked.mapIndexed { index, (group, days) ->
                WidgetRow(rank = index + 1, bankName = group.name, days = days, highlight = index == 0)
            },
        )

        // 1b. 最近待还记录小组件：逾期合并一行，其余按天分组汇总
        data class DueEntry(val dueDays: Int, val outstandingCents: Long)
        val dueEntries = groups.mapNotNull { group ->
            val billingDay = group.billingDay ?: return@mapNotNull null
            val offset = group.dueOffsetDays ?: return@mapNotNull null
            val dueDays = DateEngine.daysUntilDue(billingDay, offset, today) ?: return@mapNotNull null
            val outstanding = group.currentBill?.outstandingCents ?: return@mapNotNull null
            if (outstanding <= 0L) return@mapNotNull null
            DueEntry(dueDays, outstanding)
        }
        val dueRows = buildList {
            val overdueTotal = dueEntries.filter { it.dueDays < 0 }.sumOf { it.outstandingCents }
            if (overdueTotal > 0L) {
                add(com.alosir.card.widget.DueWidgetRow("逾期", overdueTotal))
            }
            dueEntries.filter { it.dueDays >= 0 }
                .groupBy { it.dueDays }
                .entries
                .sortedBy { it.key }
                .forEach { (days, entries) ->
                    val label = when (days) {
                        0 -> "今天"
                        1 -> "明天"
                        else -> "${days}天后"
                    }
                    add(com.alosir.card.widget.DueWidgetRow(label, entries.sumOf { it.outstandingCents }))
                }
        }.take(com.alosir.card.widget.DueBillsWidgetReceiver.MAX_ROWS)
        com.alosir.card.widget.DueBillsWidgetReceiver.render(context, dueRows)

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
