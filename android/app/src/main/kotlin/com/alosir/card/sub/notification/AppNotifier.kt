package com.alosir.card.sub.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.alosir.card.MainActivity
import com.alosir.card.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知发送器。只负责「构造与发送」——该不该发由调用方（Worker/用例）判定
 * （技术规范 §2.2：sub/notification 不决定业务语义）。
 */
@Singleton
class AppNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    channels: NotificationChannels,
) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        channels.ensureCreated()
    }

    private fun openAppIntent(groupId: Long = -1L): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (groupId > 0) putExtra(MainActivity.EXTRA_GROUP_ID, groupId)
        }
        return PendingIntent.getActivity(
            context,
            if (groupId > 0) groupId.toInt() else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** 还款提醒（PRD F7.1）：组名 + 距还款日 + 待还金额，点击直达该组 */
    fun notifyRepayment(groupId: Long, groupName: String, dueText: String, outstandingText: String) {
        val publicVersion = NotificationCompat.Builder(context, NotificationChannels.CH_REPAYMENT)
            .setSmallIcon(R.drawable.ic_stat_alocard)
            .setContentTitle(groupName)
            .setContentText("$dueText · 待还 $outstandingText")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        val notification = NotificationCompat.Builder(context, NotificationChannels.CH_REPAYMENT)
            .setSmallIcon(R.drawable.ic_stat_alocard)
            .setContentTitle(groupName)
            .setContentText("$dueText · 待还 $outstandingText")
            .setContentIntent(openAppIntent(groupId))
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build()
        manager.notify(ID_REPAY_BASE + groupId.toInt(), notification)
    }

    /** 账单录入引导（PRD F7.2） */
    fun notifyBillEntry(groupId: Long, groupName: String) {
        val notification = NotificationCompat.Builder(context, NotificationChannels.CH_BILL_ENTRY)
            .setSmallIcon(R.drawable.ic_stat_alocard)
            .setContentTitle(groupName)
            .setContentText("账单已出账，请录入金额")
            .setContentIntent(openAppIntent(groupId))
            .setAutoCancel(true)
            .build()
        manager.notify(ID_ENTRY_BASE + groupId.toInt(), notification)
    }

    /** 卡片到期提醒（PRD F7.3） */
    fun notifyCardExpiry(cardId: Long, cardName: String, expiryYm: String) {
        val notification = NotificationCompat.Builder(context, NotificationChannels.CH_CARD_EXPIRY)
            .setSmallIcon(R.drawable.ic_stat_alocard)
            .setContentTitle("卡片即将到期")
            .setContentText("$cardName · $expiryYm")
            .setAutoCancel(true)
            .build()
        manager.notify(ID_EXPIRY_BASE + cardId.toInt(), notification)
    }

    /**
     * 常驻通知（PRD F7.5 / ADR-0012）。
     * 有临近到期 → 逾期/到期提醒 + 待还金额；否则 → 今日免息期 TOP3。
     * ongoing，用户不可滑动清除；被清除后 App 打开/每日刷新会重建。
     */
    fun updatePersistentNotification(content: PersistentContent) {
        val styleLines = content.lines.joinToString("\n") { it }
        val notification = NotificationCompat.Builder(context, NotificationChannels.CH_TOP_GRACE)
            .setSmallIcon(R.drawable.ic_stat_alocard)
            .setContentTitle(content.title)
            .setContentText(content.lines.firstOrNull() ?: content.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(styleLines))
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setSilent(true)
            .build()
        manager.notify(ID_PERSISTENT, notification)
    }

    sealed interface PersistentContent {
        val title: String
        val lines: List<String>

        data class DueAlerts(override val lines: List<String>) : PersistentContent {
            override val title: String = "还款提醒"
        }

        data class TopGrace(val todayLabel: String, override val lines: List<String>) : PersistentContent {
            override val title: String = "今日免息期 · $todayLabel"
        }
    }

    companion object {
        private const val ID_REPAY_BASE = 10_000
        private const val ID_ENTRY_BASE = 20_000
        private const val ID_EXPIRY_BASE = 30_000
        private const val ID_PERSISTENT = 1
    }
}
