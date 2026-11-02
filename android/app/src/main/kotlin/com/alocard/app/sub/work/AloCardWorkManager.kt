package com.alocard.app.sub.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 调度编排（技术规范 §1.7.3：自续期 OneTimeWorkRequest，不用 PeriodicWorkRequest——
 * 周期任务的时间会随系统调度漂移，自续期按当前本地时间重算，天然对齐时区与夏令时）。
 */
object AloCardWorkManager {

    private const val UNIQUE_REMINDER = "daily_reminder"
    private const val UNIQUE_REFRESH = "daily_refresh"
    private const val UNIQUE_CATCHUP = "reminder_catchup"

    private val REMINDER_TIME: LocalTime = LocalTime.of(9, 0)
    private val REFRESH_TIME: LocalTime = LocalTime.of(0, 5)

    /** 续期下一次「每日提醒」（下一个本地 09:00） */
    fun scheduleNextReminder(context: Context) {
        val delay = delayToNext(REMINDER_TIME)
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay)
            .addTag(UNIQUE_REMINDER)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_REMINDER, ExistingWorkPolicy.REPLACE, request)
    }

    /** 续期下一次「每日刷新」（下一个本地 00:05） */
    fun scheduleNextRefresh(context: Context) {
        val delay = delayToNext(REFRESH_TIME)
        val request = OneTimeWorkRequestBuilder<DailyRefreshWorker>()
            .setInitialDelay(delay)
            .addTag(UNIQUE_REFRESH)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_REFRESH, ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * App 启动时的幂等补排：续期链在进程被杀/重启后可能断裂，
     * 若队列里没有待执行的提醒 Worker 则补排（REPLACE 策略天然幂等）。
     */
    fun ensureScheduled(context: Context) {
        scheduleNextReminder(context)
        scheduleNextRefresh(context)
    }

    /**
     * 今日提醒补发：Worker 在 Doze 期间可能延迟数小时，
     * 打开 App 且已过 09:00 时立即补跑一遍——台账保证不重复发送。
     */
    fun catchUpReminders(context: Context) {
        val now = LocalDateTime.now()
        if (now.toLocalTime() < REMINDER_TIME) return
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .addTag(UNIQUE_CATCHUP)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_CATCHUP, ExistingWorkPolicy.KEEP, request)
    }

    private fun delayToNext(time: LocalTime): Duration {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
