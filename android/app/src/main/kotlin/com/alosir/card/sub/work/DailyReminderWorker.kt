package com.alosir.card.sub.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.alosir.card.domain.DateEngine
import com.alosir.card.domain.model.GroupWithCards
import com.alosir.card.domain.repository.BillingGroupRepository
import com.alosir.card.domain.repository.BillRepository
import com.alosir.card.domain.repository.CardRepository
import com.alosir.card.domain.repository.NotificationLogRepository
import com.alosir.card.domain.util.DateProvider
import com.alosir.card.domain.util.Money
import com.alosir.card.sub.notification.AppNotifier
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 每日提醒 Worker（技术规范 §1.7.3，自续期 OneTimeWorkRequest）。
 *
 * 每天 09:00 左右执行（WorkManager 不保证分钟级准时，日粒度提醒可接受）：
 * 1. 扫描全部账单组，判定今天应触发的提醒；
 * 2. 查 notification_log 台账去重——无论 Worker 重试多少次，同一提醒只发一次；
 * 3. 发送通知；
 * 4. 续期明天的同一时刻。
 *
 * 触发判定（PRD F7.1/7.2/7.3，均日粒度）：
 * - repay_d3 / d1 / d0：本期账单距还款日 = 3 / 1 / 0 天，且未结清；
 * - entry_hint：本期账单金额未录入，且今天已过账单日（次日开放录入）；
 * - card_expiry：卡片有效期月份最后一天往前推 30 天，每张卡一次。
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderEntryPoint {
        fun billingGroups(): BillingGroupRepository
        fun bills(): BillRepository
        fun cards(): CardRepository
        fun log(): NotificationLogRepository
        fun notifier(): AppNotifier
    }

    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        // 先补齐账单期，再扫描——否则长期未开后首次运行会漏判
        ep.bills().syncBillPeriods()

        val today = DateProvider.today()
        val todayEpoch = today.toEpochDay()
        val now = System.currentTimeMillis()
        val groups = ep.billingGroups().observeGroupsWithCards().first()

        for (group in groups) {
            scanRepayment(group, today, todayEpoch, now, ep)
            scanEntryHint(group, today, todayEpoch, now, ep)
        }
        scanCardExpiry(groups, today, todayEpoch, now, ep)

        // 自续期：下一个本地 09:00
        AloCardWorkManager.scheduleNextReminder(applicationContext)
        return Result.success()
    }

    /** 还款三段提醒（PRD F7.1） */
    private suspend fun scanRepayment(
        group: GroupWithCards,
        today: LocalDate,
        todayEpoch: Long,
        now: Long,
        ep: ReminderEntryPoint,
    ) {
        val billingDay = group.billingDay ?: return
        val offset = group.dueOffsetDays ?: return
        val dueDays = DateEngine.daysUntilDue(billingDay, offset, today) ?: return
        val bill = group.currentBill ?: return
        // 已结清的账单组不触发还款提醒（PRD F7.1 规则 5）
        if (bill.billAmountCents != null && bill.paidAmountCents >= bill.billAmountCents) return

        val kind = when (dueDays) {
            3 -> NotificationLogRepository.KIND_REPAY_D3
            1 -> NotificationLogRepository.KIND_REPAY_D1
            0 -> NotificationLogRepository.KIND_REPAY_D0
            else -> return
        }
        val billDate = bill.billingDate.toEpochDay()
        if (ep.log().hasBeenSent(kind, group.id, billDate, todayEpoch)) return
        ep.log().logSent(kind, group.id, billDate, todayEpoch, now)

        val dueText = when (dueDays) {
            0 -> "今天还款"
            else -> "$dueDays 天后还款"
        }
        val outstanding = bill.billAmountCents?.let {
            Money.format((it - bill.paidAmountCents).coerceAtLeast(0L))
        } ?: "金额待录入"
        ep.notifier().notifyRepayment(group.id, group.name, dueText, outstanding)
    }

    /** 账单录入引导（PRD F7.2）：本期金额未录入且今天已过账单日，每期一次 */
    private suspend fun scanEntryHint(
        group: GroupWithCards,
        today: LocalDate,
        todayEpoch: Long,
        now: Long,
        ep: ReminderEntryPoint,
    ) {
        val bill = group.currentBill ?: return
        if (bill.billAmountCents != null) return
        if (!bill.billingDate.isBefore(today)) return // 账单日当天未出账，次日才引导

        val billDate = bill.billingDate.toEpochDay()
        if (ep.log().hasBeenSent(
                NotificationLogRepository.KIND_ENTRY_HINT, group.id, billDate, todayEpoch,
            )
        ) return
        ep.log().logSent(
            NotificationLogRepository.KIND_ENTRY_HINT, group.id, billDate, todayEpoch, now,
        )
        ep.notifier().notifyBillEntry(group.id, group.name)
    }

    /** 卡片到期提醒（PRD F7.3）：有效期月份最后一天往前推 30 天，每张卡一次 */
    private suspend fun scanCardExpiry(
        groups: List<GroupWithCards>,
        today: LocalDate,
        todayEpoch: Long,
        now: Long,
        ep: ReminderEntryPoint,
    ) {
        for (group in groups) {
            for (card in group.cards) {
                val expiryYm = card.expiryYm ?: continue
                val ym = runCatching { YearMonth.parse(expiryYm) }.getOrNull() ?: continue
                // 到期月最后一天
                val lastDay = ym.atEndOfMonth()
                // 往前推 30 天
                val remindDate = lastDay.minusDays(30)
                if (!remindDate.isEqual(today)) continue

                if (ep.log().hasBeenSent(
                        NotificationLogRepository.KIND_CARD_EXPIRY,
                        group.id,
                        NotificationLogRepository.SENTINEL_BILL_DATE,
                        todayEpoch,
                    )
                ) continue
                ep.log().logSent(
                    NotificationLogRepository.KIND_CARD_EXPIRY,
                    group.id,
                    NotificationLogRepository.SENTINEL_BILL_DATE,
                    todayEpoch,
                    now,
                )
                ep.notifier().notifyCardExpiry(card.id, card.cardLabel ?: "尾号 ${card.cardNumberLast4 ?: ""}", expiryYm)
            }
        }
    }
}
