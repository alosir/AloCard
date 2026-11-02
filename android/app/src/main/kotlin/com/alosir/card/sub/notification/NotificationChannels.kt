package com.alosir.card.sub.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知渠道（技术规范 §1.8.4）。四个渠道各司其职，重要性遵守系统分级：
 * - repayment：还款提醒（HIGH，对用户有行动价值）
 * - bill_entry：账单录入引导（DEFAULT）
 * - card_expiry：卡片到期提醒（DEFAULT）
 * - top_grace：免息期 TOP 常驻（LOW，无声音不震动，纯信息位）
 */
@Singleton
class NotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun ensureCreated() {
        val manager = context.getSystemService(NotificationManager::class.java)
        CHANNELS.forEach { (id, name, importance) ->
            manager.createNotificationChannel(
                NotificationChannel(id, name, importance).apply {
                    when (id) {
                        CH_TOP_GRACE -> setShowBadge(false)
                        else -> Unit
                    }
                }
            )
        }
    }

    companion object {
        const val CH_REPAYMENT = "repayment"
        const val CH_BILL_ENTRY = "bill_entry"
        const val CH_CARD_EXPIRY = "card_expiry"
        const val CH_TOP_GRACE = "top_grace"

        private val CHANNELS = listOf(
            Triple(CH_REPAYMENT, "还款提醒", NotificationManager.IMPORTANCE_HIGH),
            Triple(CH_BILL_ENTRY, "账单录入引导", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CH_CARD_EXPIRY, "卡片到期提醒", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CH_TOP_GRACE, "今日免息期", NotificationManager.IMPORTANCE_LOW),
        )
    }
}
