package com.alocard.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * AloCard 账期日期计算引擎（Kotlin 版）
 *
 * 本文件是 [com.alocard.app.engine.dateEngine.js] 的严格移植。两者的行为必须逐条一致：
 * JS 版的单元测试用例在本工程的测试中以等价方式复跑。
 *
 * 规则来源：
 * - ADR-0002：账单日取值 1–28，不做月末兜底；账单日当日的消费计入以该账单日出账的本期账单；
 *   每个账单周期自上一账单日次日起算，至当前账单日结束。
 * - ADR-0003：最后还款日存储为「账单日 + N 天」。
 * - ADR-0001：免息期为「今日视角」的动态值。
 * - ADR-0008：距还款日 0 为「到期」而非「逾期」。
 *
 * 实现要点：
 * - 所有日期运算在本地日历日层面完成，不涉及时区偏移问题（LocalDate 无时区）。
 * - 「账单日 + 一个月」使用 plusMonths(1)，Kotlin/Java 的 LocalDate.plusMonths 自带
 *   月末钳制（1 月 31 日 + 1 月 = 2 月 28/29 日），与 JS 版的 clamp 行为一致。
 */
object DateEngine {

    /** 账单日取值范围：银行实际只开 1–28 号（ADR-0002） */
    const val MIN_BILLING_DAY = 1
    const val MAX_BILLING_DAY = 28

    // ---------- 时效标记 ----------

    /** 距还款日 > 0 */
    const val STATUS_NORMAL = "normal"

    /** 距还款日 = 0，今天正是最后还款日，尚未逾期 */
    const val STATUS_DUE = "due"

    /** 距还款日 < 0 */
    const val STATUS_OVERDUE = "overdue"

    // ---------- 账单状态 ----------

    /** 已出账但账单金额尚未录入 */
    const val BILL_PENDING_ENTRY = "pending_entry"

    /** 已录入账单金额，已还款金额为 0 */
    const val BILL_DUE_UNPAID = "due_unpaid"

    /** 0 < 已还款金额 < 账单金额 */
    const val BILL_PARTIALLY_PAID = "partially_paid"

    /** 已还款金额 ≥ 账单金额 */
    const val BILL_SETTLED = "settled"

    private val ISO = DateTimeFormatter.ISO_LOCAL_DATE

    // ---------- 内部工具 ----------

    /** 某年某月的天数（month 为 1–12） */
    private fun daysInMonth(year: Int, month: Int): Int =
        java.time.YearMonth.of(year, month).lengthOfMonth()

    /** 校验并归一化账单日：必须是 1–28 的整数 */
    fun normalizeBillingDay(billingDay: Int?): Int {
        requireNotNull(billingDay) { "billingDay 不能为 null" }
        require(billingDay in MIN_BILLING_DAY..MAX_BILLING_DAY) {
            "billingDay 必须落在 $MIN_BILLING_DAY–$MAX_BILLING_DAY" +
                "（银行账单日实际取值范围，不做月末兜底），收到 $billingDay"
        }
        return billingDay
    }

    /** 校验并归一化偏移天数 N：必须是正整数 */
    fun normalizeOffset(dueOffsetDays: Int?): Int {
        requireNotNull(dueOffsetDays) { "dueOffsetDays 不能为 null" }
        require(dueOffsetDays >= 1) {
            "dueOffsetDays 必须 ≥ 1（还款日必须晚于账单日），收到 $dueOffsetDays"
        }
        return dueOffsetDays
    }

    private fun isMissing(value: Any?): Boolean =
        value == null || (value is String && value.isEmpty())

    /** 解析参考日期：接受 LocalDate 或 'YYYY-MM-DD' 字符串 */
    fun parseRefDate(value: Any?): LocalDate {
        return when (value) {
            is LocalDate -> value
            is String -> {
                val text = value.trim()
                require(DATE_RE.matches(text)) {
                    "refDate 必须是 'YYYY-MM-DD' 字符串或 LocalDate，收到 $value"
                }
                LocalDate.parse(text, ISO)
            }
            else -> throw IllegalArgumentException(
                "refDate 必须是 'YYYY-MM-DD' 字符串或 LocalDate，收到 $value"
            )
        }
    }

    private val DATE_RE = Regex("""^\d{4}-\d{1,2}-\d{1,2}$""")

    /** 某账单组在指定参考日期的「本期账单日」 */
    private fun billingDateOf(billingDay: Int, date: LocalDate): LocalDate {
        val day = minOf(billingDay, date.lengthOfMonth())
        return date.withDayOfMonth(day)
    }

    // ---------- 规则 1 / 规则 2 ----------

    /**
     * 规则 1：消费所属账单日（最近下一次账单日）
     * = 满足 billDate >= D 的最小账单日；账单日当天（相等）计入以该账单日出账的本期账单。
     */
    fun nextBillingDate(billingDay: Int, refDate: Any?): LocalDate {
        val day = normalizeBillingDay(billingDay)
        val ref = parseRefDate(refDate)
        val sameMonth = billingDateOf(day, ref)
        if (!sameMonth.isBefore(ref)) return sameMonth
        return billingDateOf(day, ref.plusMonths(1))
    }

    /**
     * 规则 2：本期账单日（最近一次已出账账单）
     * = 满足 billDate <= D 的最大账单日；D 恰为账单日时与规则 1 结果相同。
     */
    fun currentBillingDate(billingDay: Int, refDate: Any?): LocalDate {
        val day = normalizeBillingDay(billingDay)
        val ref = parseRefDate(refDate)
        val sameMonth = billingDateOf(day, ref)
        if (!sameMonth.isAfter(ref)) return sameMonth
        return billingDateOf(day, ref.minusMonths(1))
    }

    // ---------- 规则 6 ----------

    /** 规则 6：当前账单周期长度（天）= 本期账单日到下一个账单日的天数 */
    fun cycleLength(billingDay: Int, refDate: Any?): Int {
        val day = normalizeBillingDay(billingDay)
        val ref = parseRefDate(refDate)
        val start = currentBillingDate(day, ref)
        val end = billingDateOf(day, start.plusMonths(1))
        return java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
    }

    /** 规则 6：校验偏移天数 N 是否落在当前周期内 */
    fun validateOffset(dueOffsetDays: Int, billingDay: Int, refDate: Any?) {
        val offset = normalizeOffset(dueOffsetDays)
        val len = cycleLength(billingDay, refDate)
        require(offset <= len) {
            "dueOffsetDays ($offset) 超过当前账单周期长度 ($len 天)：会导致还款日早于下一个账单日、" +
                "或免息期超过一个完整周期，属于非法配置"
        }
    }

    // ---------- 规则 3 / 规则 4 ----------

    /**
     * 规则 3：免息期（今日视角的动态值）
     * = (消费所属账单日 + N) − D
     * 数据不完整（缺 billingDay 或 dueOffsetDays）时返回 null（不可用标记）。
     */
    fun gracePeriod(billingDay: Int?, dueOffsetDays: Int?, refDate: Any?): Int? {
        if (isMissing(billingDay) || isMissing(dueOffsetDays)) return null
        val day = normalizeBillingDay(billingDay)
        val offset = normalizeOffset(dueOffsetDays)
        val ref = parseRefDate(refDate)
        validateOffset(offset, day, ref)
        val bill = nextBillingDate(day, ref)
        val due = bill.plusDays(offset.toLong())
        return java.time.temporal.ChronoUnit.DAYS.between(ref, due).toInt()
    }

    /**
     * 规则 4：距还款日
     * = (本期账单日 + N) − D
     * 数据不完整时返回 null。
     */
    fun daysUntilDue(billingDay: Int?, dueOffsetDays: Int?, refDate: Any?): Int? {
        if (isMissing(billingDay) || isMissing(dueOffsetDays)) return null
        val day = normalizeBillingDay(billingDay)
        val offset = normalizeOffset(dueOffsetDays)
        val ref = parseRefDate(refDate)
        validateOffset(offset, day, ref)
        val bill = currentBillingDate(day, ref)
        val due = bill.plusDays(offset.toLong())
        return java.time.temporal.ChronoUnit.DAYS.between(ref, due).toInt()
    }

    // ---------- 规则 5 / 规则 7 / 规则 8 ----------

    /** 规则 5：时效标记 */
    fun dueStatus(days: Int?): String {
        requireNotNull(days) { "days 不能为 null" }
        return when {
            days > 0 -> STATUS_NORMAL
            days == 0 -> STATUS_DUE
            else -> STATUS_OVERDUE
        }
    }

    /**
     * 规则 7：账单状态机（纯金额判定，无日期维度）
     * 判定顺序：待录入 → 待还款 → 部分还款 → 已结清。
     */
    fun billStatus(billAmount: Double?, paidAmount: Double?): String {
        if (billAmount == null) return BILL_PENDING_ENTRY
        require(billAmount >= 0) { "billAmount 不能为负数，收到 $billAmount" }
        val paid = paidAmount ?: 0.0
        require(paid >= 0) { "paidAmount 不能为负数，收到 $paid" }
        return when {
            paid == 0.0 -> BILL_DUE_UNPAID
            paid < billAmount -> BILL_PARTIALLY_PAID
            else -> BILL_SETTLED
        }
    }

    /** 规则 8：账单组完整度。缺少 billingDay 或 dueOffsetDays → 'incomplete' */
    fun groupCompleteness(billingDay: Int?, dueOffsetDays: Int?): String {
        return if (isMissing(billingDay) || isMissing(dueOffsetDays)) "incomplete" else "complete"
    }

    // ---------- 便利方法 ----------

    /** 本期账单的最后还款日 */
    fun dueDateOf(billingDay: Int, dueOffsetDays: Int, refDate: Any?): LocalDate {
        val day = normalizeBillingDay(billingDay)
        val offset = normalizeOffset(dueOffsetDays)
        val bill = currentBillingDate(day, refDate)
        return bill.plusDays(offset.toLong())
    }

    /** 下期账单的最后还款日（即免息期的终点） */
    fun nextDueDateOf(billingDay: Int, dueOffsetDays: Int, refDate: Any?): LocalDate {
        val day = normalizeBillingDay(billingDay)
        val offset = normalizeOffset(dueOffsetDays)
        val bill = nextBillingDate(day, refDate)
        return bill.plusDays(offset.toLong())
    }
}
