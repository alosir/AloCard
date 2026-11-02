package com.alosir.card.sub.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * 每日刷新 Worker：跨过 0 点后重算今日 TOP5 → 小组件 + 常驻通知。
 * 执行完自续期到下一个 00:05。
 */
class DailyRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RefreshEntryPoint {
        fun refresher(): DailySurfacesRefresher
    }

    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(
            applicationContext,
            RefreshEntryPoint::class.java,
        )
        runCatching { ep.refresher().refresh() }
        AloCardWorkManager.scheduleNextRefresh(applicationContext)
        return Result.success()
    }
}
