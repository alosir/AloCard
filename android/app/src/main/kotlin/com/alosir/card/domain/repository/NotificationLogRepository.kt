package com.alosir.card.domain.repository

/**
 * 通知幂等台账（技术规范 §1.7.4）。
 *
 * WorkManager 是「至少一次」投递语义；没有台账，Worker 重试或设备重启
 * 就会重复打扰用户。这里提供「该提醒是否已发过」的判定与登记。
 */
interface NotificationLogRepository {

    /** 某提醒是否已发送过（幂等键：kind + groupId + billDate + scheduledDate） */
    suspend fun hasBeenSent(
        kind: String,
        groupId: Long,
        billDate: Long,
        scheduledDate: Long,
    ): Boolean

    /** 登记一条已发送的提醒 */
    suspend fun logSent(
        kind: String,
        groupId: Long,
        billDate: Long,
        scheduledDate: Long,
        deliveredAt: Long,
    )

    companion object {
        // 提醒种类（技术规范 §1.7.4）
        const val KIND_REPAY_D3 = "repay_d3"
        const val KIND_REPAY_D1 = "repay_d1"
        const val KIND_REPAY_D0 = "repay_d0"
        const val KIND_ENTRY_HINT = "entry_hint"
        const val KIND_CARD_EXPIRY = "card_expiry"

        /** 非组级提醒的哨兵值（SQLite UNIQUE 视 NULL 互不相等，不能用 NULL） */
        const val SENTINEL_GROUP: Long = -1
        const val SENTINEL_BILL_DATE: Long = -1
    }
}
