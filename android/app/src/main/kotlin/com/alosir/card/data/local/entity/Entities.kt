package com.alosir.card.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room 实体（技术规范 §3.2）。
 *
 * 金额一律「分」（Long）；日历日一律 epochDay（Long，距 1970-01-01 的天数）。
 * epochDay 是本地日历日的无歧义表示，SQLite 可直接比较排序（PRD 5.0）。
 */

/**
 * 账单组：账期与信用额度的唯一事实来源（ADR-0006）。
 * billingDay / dueOffsetDays / creditLimitCents 任一缺失 → 「待完善」（ADR-0009）。
 */
@Entity(
    tableName = "billing_groups",
    indices = [Index(value = ["bank_name"], name = "index_billing_groups_bank_name")],
)
data class BillingGroupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    /** 账单日 1–28；null = 待完善 */
    @ColumnInfo(name = "billing_day")
    val billingDay: Int? = null,

    /** 最后还款日偏移 N（1–28）；null = 待完善。建表不带 DEFAULT——由 Repository 显式写 20 */
    @ColumnInfo(name = "due_offset_days")
    val dueOffsetDays: Int? = null,

    /** 信用额度（分）；null = 待完善 */
    @ColumnInfo(name = "credit_limit_cents")
    val creditLimitCents: Long? = null,

    @ColumnInfo(name = "bank_name")
    val bankName: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

/**
 * 卡片：只承载基础信息（ADR-0006）。
 * 完整卡号以 AES-256-GCM 密文存储（card_number_cipher），末四位明文便于免解密渲染列表。
 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = BillingGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["group_id"], name = "index_cards_group_id"),
        // 查重盲索引：SQLite UNIQUE 允许多个 NULL，可空列也能用唯一索引
        Index(value = ["card_number_fingerprint"], name = "index_cards_fingerprint", unique = true),
    ],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    /** 完整卡号密文，封装 v1:<base64(iv)>:<base64(ct‖tag)>；未填卡号为 null */
    @ColumnInfo(name = "card_number_cipher")
    val cardNumberCipher: String? = null,

    /** 末四位明文，列表渲染无需解密（PRD 10.1） */
    @ColumnInfo(name = "card_number_last4")
    val cardNumberLast4: String? = null,

    /** 查重盲索引：HMAC-SHA256(Keystore 密钥, 规范化卡号) 截断 16 字节后 Base64 */
    @ColumnInfo(name = "card_number_fingerprint")
    val cardNumberFingerprint: String? = null,

    /** 有效期 YYYY-MM；到期前 30 天提醒（PRD F7.3） */
    @ColumnInfo(name = "expiry_ym")
    val expiryYm: String? = null,

    /** union / visa / mastercard / amex / jcb / diners / unknown */
    @ColumnInfo(name = "card_scheme")
    val cardScheme: String = "unknown",

    /** 卡名称，用户自定义，≤ 20 字 */
    @ColumnInfo(name = "card_label")
    val cardLabel: String? = null,

    @ColumnInfo(name = "bank_name")
    val bankName: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

/**
 * 账单：账单组下按期滚动的历史序列（ADR-0004）。
 * (group_id, billing_date) 业务唯一——覆盖式录入即更新此行，不插新行。
 */
@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = BillingGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["group_id", "billing_date"], name = "index_bills_group_date", unique = true),
        Index(value = ["billing_date"], name = "index_bills_billing_date"),
    ],
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    /** 该期账单日，epochDay */
    @ColumnInfo(name = "billing_date")
    val billingDate: Long,

    /** 账单金额（分）；null = 未录入 → 待录入 */
    @ColumnInfo(name = "bill_amount_cents")
    val billAmountCents: Long? = null,

    /** 已还款金额（分），累计式（ADR-0007） */
    @ColumnInfo(name = "paid_amount_cents")
    val paidAmountCents: Long = 0L,

    /** 最近一次标记还款的金额（分），用于「撤销上次标记」（PRD F4.2） */
    @ColumnInfo(name = "last_paid_cents")
    val lastPaidCents: Long? = null,

    /** 最近一次标记前的已还款金额（分）；首次标记时 = 0 */
    @ColumnInfo(name = "paid_before_last_cents")
    val paidBeforeLastCents: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

/** 键值对设置（PRD 9.2 的 app_settings） */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "value")
    val value: String,
)

/**
 * 通知幂等台账（技术规范 §1.7.4）。
 * 唯一索引 (kind, group_id, bill_date, scheduled_date) 保证「同一提醒只发一次」。
 */
@Entity(
    tableName = "notification_log",
    indices = [
        Index(
            value = ["kind", "group_id", "bill_date", "scheduled_date"],
            name = "index_notification_log_idem",
            unique = true,
        ),
    ],
)
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** repay_d3 / repay_d1 / repay_d0 / entry_hint / card_expiry */
    @ColumnInfo(name = "kind")
    val kind: String,

    /** 账单组 id；非组级提醒用 -1 哨兵（UNIQUE 视 NULL 互不相等，不能用 NULL） */
    @ColumnInfo(name = "group_id")
    val groupId: Long,

    /** 该期账单日 epochDay；非组级提醒用 -1 哨兵 */
    @ColumnInfo(name = "bill_date")
    val billDate: Long,

    /** 该提醒归属的本地日历日 epochDay */
    @ColumnInfo(name = "scheduled_date")
    val scheduledDate: Long,

    /** 实际发出时刻，epoch millis */
    @ColumnInfo(name = "delivered_at")
    val deliveredAt: Long,
)
