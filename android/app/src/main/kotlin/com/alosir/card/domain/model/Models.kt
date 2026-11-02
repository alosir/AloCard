package com.alosir.card.domain.model

import java.time.LocalDate

/**
 * 领域模型。
 *
 * 这些是 UI 层直接消费的模型，与 Room 实体解耦——
 * 实体是持久化形态，模型是业务形态。两者字段不一定要一一对应。
 *
 * 金额一律以「分」为单位的 Long（PRD 4.4：避免浮点误差）；
 * 日历日一律 java.time.LocalDate（PRD 5.0：本地日历日层面运算）。
 */

/** 账单组：账期、信用额度的唯一事实来源（ADR-0006） */
data class BillingGroup(
    val id: Long,
    val name: String,
    /** 账单日 1–28；null = 待完善 */
    val billingDay: Int?,
    /** 最后还款日偏移 N；null = 待完善 */
    val dueOffsetDays: Int?,
    /** 信用额度（分）；null = 待完善 */
    val creditLimitCents: Long?,
    val bankName: String?,
)

/** 一张卡片（只含基础信息，不承载账期与额度） */
data class Card(
    val id: Long,
    val groupId: Long,
    val cardNumberLast4: String?,
    /** 是否存在完整卡号密文（有则允许「查看完整卡号」） */
    val hasCardNumber: Boolean,
    /** 有效期 YYYY-MM；用于到期前 30 天提醒 */
    val expiryYm: String?,
    /** 卡组织：union / visa / mastercard / amex / jcb / diners / unknown */
    val cardScheme: String,
    val cardLabel: String?,
    val bankName: String?,
)

/** 一期账单（账单组下按期滚动的历史序列，ADR-0004） */
data class Bill(
    val id: Long,
    val groupId: Long,
    /** 该期账单日 */
    val billingDate: LocalDate,
    /** 账单金额（分）；null = 未录入 → 待录入 */
    val billAmountCents: Long?,
    /** 已还款金额（分），累计式（ADR-0007） */
    val paidAmountCents: Long,
    /** 存在可撤销的上一次标记（PRD F4.2） */
    val canUndoLastPayment: Boolean,
) {
    /** 待还金额；未录入时为 null（显示「待录入」而非 0，PRD 5.7.1） */
    val outstandingCents: Long?
        get() = billAmountCents?.let { (it - paidAmountCents).coerceAtLeast(0L) }
}

/** 账单组及其卡片与本期账单 —— 列表页/详情页的数据形态 */
data class GroupWithCards(
    val id: Long,
    val name: String,
    val billingDay: Int?,
    val dueOffsetDays: Int?,
    val creditLimitCents: Long?,
    val bankName: String?,
    val cards: List<Card>,
    /** 本期账单（最近一期）；尚无记录时为 null */
    val currentBill: Bill?,
)

/** 账单组额度占用（额度明细页 P7） */
data class GroupQuota(
    val groupId: Long,
    val name: String,
    val bankName: String?,
    val creditLimitCents: Long?,
    /** 本期账单占用（账单金额 − 已还款，不小于 0；未录入按 0） */
    val usedCents: Long,
    /** 本期账单是否未录入 */
    val pendingEntry: Boolean,
)

/** 额度总览聚合（PRD 5.7） */
data class CreditTotals(
    /** 总授信额度（分）：对账单组求和，组内多卡不重复计（ADR-0006） */
    val totalLimitCents: Long,
    /** 已用额度（分）：Σ(本期账单金额 − 已还款金额)，仅未结清 */
    val usedCents: Long,
) {
    /** 剩余可用额度（分）。估算值——不含未出账消费（ADR-0007） */
    val remainCents: Long get() = (totalLimitCents - usedCents).coerceAtLeast(0L)
}
