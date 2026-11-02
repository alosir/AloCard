package com.alocard.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/**
 * JUnit4 断言工具：替代 kotlin.test 的 assertFailsWith。
 * 本工程不引入 kotlin-test（Maven 上的 artifact 不含实现），统一用 JUnit4。
 */
private inline fun assertThrows(expected: Class<out Throwable>, block: () -> Unit) {
    try {
        block()
        fail("期望抛出 ${expected.simpleName}，但没有抛出")
    } catch (e: Throwable) {
        if (!expected.isInstance(e)) throw e
    }
}

private fun assertIllegalArgument(block: () -> Unit) =
    assertThrows(IllegalArgumentException::class.java, block)

/**
 * 日期引擎测试。
 *
 * 这些用例与 `engine/dateEngine.test.js`（JS 版）逐条等价，
 * 目的是锁住 Kotlin 版与 JS 版行为一致。任一侧修改规则，两侧都要同步。
 */
class DateEngineTest {

    // ---------- 用例 1：账单日 5 号、N=20、ref=2026-03-06 ----------

    @Test
    fun `用例1 - 账单日5号N20参考日0306`() {
        val ref = LocalDate.of(2026, 3, 6)
        // next = 2026-04-05（当月 03-05 已过，取 >= D 的最小账单日）
        assertEquals(LocalDate.of(2026, 4, 5), DateEngine.nextBillingDate(5, ref))
        // grace = (04-05 + 20) - 03-06 = 2026-04-25 - 2026-03-06 = 50
        assertEquals(50, DateEngine.gracePeriod(5, 20, ref))
        // current = 2026-03-05
        assertEquals(LocalDate.of(2026, 3, 5), DateEngine.currentBillingDate(5, ref))
        // daysUntilDue = (03-05 + 20) - 03-06 = 2026-03-25 - 2026-03-06 = 19
        assertEquals(19, DateEngine.daysUntilDue(5, 20, ref))
    }

    // ---------- 用例 2：参考日恰好是账单日当天 ----------

    @Test
    fun `用例2 - 参考日恰为账单日当天`() {
        val ref = LocalDate.of(2026, 3, 5)
        // 规则 1 与规则 2 结果相同（都是 D 本身）
        assertEquals(LocalDate.of(2026, 3, 5), DateEngine.nextBillingDate(5, ref))
        assertEquals(LocalDate.of(2026, 3, 5), DateEngine.currentBillingDate(5, ref))
        // 今天的消费计入今天出账的这期账单，免息期与距还款日都等于 N
        assertEquals(20, DateEngine.gracePeriod(5, 20, ref))
        assertEquals(20, DateEngine.daysUntilDue(5, 20, ref))
    }

    // ---------- 用例 3：ref=2026-03-04 ----------

    @Test
    fun `用例3 - 参考日在账单日之前一天`() {
        val ref = LocalDate.of(2026, 3, 4)
        assertEquals(LocalDate.of(2026, 3, 5), DateEngine.nextBillingDate(5, ref))
        // grace = (03-05 + 20) - 03-04 = 03-25 - 03-04 = 21
        assertEquals(21, DateEngine.gracePeriod(5, 20, ref))
        // current = 2026-02-05
        assertEquals(LocalDate.of(2026, 2, 5), DateEngine.currentBillingDate(5, ref))
    }

    // ---------- 用例 4：跨月边界（账单日 28 号） ----------

    @Test
    fun `用例4 - 跨月边界账单日28号`() {
        // 1 月 29 日 → 2 月 28 日（2 月 28 天）
        val ref = LocalDate.of(2026, 1, 29)
        assertEquals(LocalDate.of(2026, 2, 28), DateEngine.nextBillingDate(28, ref))
        assertEquals(LocalDate.of(2026, 1, 28), DateEngine.currentBillingDate(28, ref))

        // 账单日当天：规则 1/2 均等于 D 本身，不推进到次月。
        // 这条与 ADR-0001「账单日当天免息期最短 ≈ N」一致，用于钉死口径。
        assertEquals(
            LocalDate.of(2026, 1, 28),
            DateEngine.nextBillingDate(28, LocalDate.of(2026, 1, 28))
        )

        // 周期长度：1/28 → 2/28 = 31 天；2/28 → 3/28 = 28 天（2 月短月）
        assertEquals(31, DateEngine.cycleLength(28, LocalDate.of(2026, 1, 29)))
        assertEquals(28, DateEngine.cycleLength(28, LocalDate.of(2026, 3, 1)))
    }

    // ---------- 用例 5：闰年（2028 年 2 月有 29 天） ----------

    @Test
    fun `用例5 - 闰年二月有29天但账单日仍是28日`() {
        // 按规则 1 + ADR-0002（不做月末兜底）：账单日固定 28 号，
        // 2028 年 2 月虽有 29 天，2 月账单日仍是 2028-02-28。
        // 要得到 02-29 需要账单日为 29/30/31，已超出合法区间。
        assertEquals(LocalDate.of(2028, 2, 28), DateEngine.nextBillingDate(28, LocalDate.of(2028, 1, 29)))
        assertEquals(LocalDate.of(2028, 2, 28), DateEngine.nextBillingDate(28, LocalDate.of(2028, 2, 1)))
        assertEquals(LocalDate.of(2028, 2, 28), DateEngine.currentBillingDate(28, LocalDate.of(2028, 3, 1)))

        // 闰年的真实影响体现在账单周期长度上
        assertEquals(29, DateEngine.cycleLength(28, LocalDate.of(2028, 3, 1)))
        assertEquals(28, DateEngine.cycleLength(28, LocalDate.of(2026, 3, 1)))

        // 2 月 29 日是真实存在的日历日，且会被正确跳过（它不是账单日）
        assertEquals(LocalDate.of(2028, 3, 28), DateEngine.nextBillingDate(28, LocalDate.of(2028, 2, 29)))
        assertEquals(LocalDate.of(2028, 2, 28), DateEngine.currentBillingDate(28, LocalDate.of(2028, 2, 29)))
    }

    // ---------- 用例 6：周期长度 ----------

    @Test
    fun `用例6 - 周期长度随月份在28到31天变动`() {
        // 1/5 → 2/5 = 31 天
        assertEquals(31, DateEngine.cycleLength(5, LocalDate.of(2026, 1, 10)))
        // 2/5 → 3/5 = 28 天（2026 非闰年）
        assertEquals(28, DateEngine.cycleLength(5, LocalDate.of(2026, 2, 10)))
        // 3/5 → 4/5 = 31 天
        assertEquals(31, DateEngine.cycleLength(5, LocalDate.of(2026, 3, 10)))
        // 4/5 → 5/5 = 30 天
        assertEquals(30, DateEngine.cycleLength(5, LocalDate.of(2026, 4, 10)))
    }

    // ---------- 用例 7：时效标记三档 ----------

    @Test
    fun `用例7 - 时效标记三档`() {
        assertEquals("normal", DateEngine.dueStatus(5))
        assertEquals("due", DateEngine.dueStatus(0))
        assertEquals("overdue", DateEngine.dueStatus(-1))
    }

    // ---------- 用例 8：账单状态机四种 + 0 金额边界 ----------

    @Test
    fun `用例8 - 账单状态机与零金额边界`() {
        // 待录入：账单金额为 null
        assertEquals("pending_entry", DateEngine.billStatus(null, 0.0))
        // 待还款：已录金额、未还款
        assertEquals("due_unpaid", DateEngine.billStatus(8320.0, 0.0))
        // 部分还款
        assertEquals("partially_paid", DateEngine.billStatus(8320.0, 3000.0))
        // 已结清
        assertEquals("settled", DateEngine.billStatus(8320.0, 8320.0))
        // 超额还款也算已结清
        assertEquals("settled", DateEngine.billStatus(8320.0, 9000.0))
        // 边界：账单金额 0 且已还 0 → 待还款（不是已结清）
        assertEquals("due_unpaid", DateEngine.billStatus(0.0, 0.0))
    }

    // ---------- 用例 9：非法输入 ----------

    @Test
    fun `用例9 - 非法输入抛错`() {
        assertIllegalArgument { DateEngine.normalizeBillingDay(0) }
        assertIllegalArgument { DateEngine.normalizeBillingDay(29) }
        assertIllegalArgument { DateEngine.normalizeBillingDay(31) }
        assertIllegalArgument { DateEngine.normalizeOffset(0) }

        // N 超过当前周期长度：1 月周期 31 天，N=32 非法
        assertIllegalArgument {
            DateEngine.validateOffset(32, 5, LocalDate.of(2026, 1, 10))
        }
        // N 等于周期长度是合法的
        DateEngine.validateOffset(31, 5, LocalDate.of(2026, 1, 10))
    }

    // ---------- 用例 10：账单组完整度 ----------

    @Test
    fun `用例10 - 账单组完整度`() {
        assertEquals("incomplete", DateEngine.groupCompleteness(5, null))
        assertEquals("incomplete", DateEngine.groupCompleteness(null, 20))
        assertEquals("complete", DateEngine.groupCompleteness(5, 20))
    }

    // ---------- 用例 11：字符串与 LocalDate 等价 ----------

    @Test
    fun `用例11 - 字符串与LocalDate输入等价`() {
        val viaDate = DateEngine.gracePeriod(5, 20, LocalDate.of(2026, 10, 24))
        val viaString = DateEngine.gracePeriod(5, 20, "2026-10-24")
        assertEquals(viaDate, viaString)
        assertEquals(32, viaString)
    }

    // ---------- 用例 12：免息期取值范围自检 ----------

    @Test
    fun `用例12 - 免息期最短在账单日当天最长在账单日次日`() {
        // 账单日 5 号、N=20
        val onBillDay = DateEngine.gracePeriod(5, 20, LocalDate.of(2026, 3, 5))!!
        val afterBillDay = DateEngine.gracePeriod(5, 20, LocalDate.of(2026, 3, 6))!!
        assertEquals(20, onBillDay)   // 最短，等于 N
        assertEquals(50, afterBillDay) // 最长，约 N + 一个完整周期
        assert(onBillDay < afterBillDay)
    }

    // ---------- 用例 13：距还款日连续性 ----------

    @Test
    fun `用例13 - 距还款日逐日递减`() {
        val d1 = DateEngine.daysUntilDue(5, 20, LocalDate.of(2026, 3, 6))!!
        val d2 = DateEngine.daysUntilDue(5, 20, LocalDate.of(2026, 3, 7))!!
        assertEquals(19, d1)
        assertEquals(18, d2)
    }

    // ---------- 用例 14：正交性 —— 账单状态与时效标记互不影响 ----------

    @Test
    fun `用例14 - 账单状态与时效标记正交`() {
        // 已结清 + 已到期可以同时成立
        val due = DateEngine.daysUntilDue(5, 20, LocalDate.of(2026, 3, 25))
        assertEquals(0, due)
        assertEquals("due", DateEngine.dueStatus(due))
        assertEquals("settled", DateEngine.billStatus(8320.0, 8320.0))

        // 待还款 + 已逾期可以同时成立
        val overdue = DateEngine.daysUntilDue(5, 20, LocalDate.of(2026, 3, 30))
        assertEquals(-5, overdue)
        assertEquals("overdue", DateEngine.dueStatus(overdue))
        assertEquals("due_unpaid", DateEngine.billStatus(8320.0, 0.0))
    }

    // ---------- 用例 15：数据不完整时返回 null ----------

    @Test
    fun `用例15 - 数据不完整返回null`() {
        assertNull(DateEngine.gracePeriod(null, 20, LocalDate.of(2026, 3, 6)))
        assertNull(DateEngine.gracePeriod(5, null, LocalDate.of(2026, 3, 6)))
        assertNull(DateEngine.daysUntilDue(null, 20, LocalDate.of(2026, 3, 6)))
        assertNull(DateEngine.daysUntilDue(5, null, LocalDate.of(2026, 3, 6)))
    }

    // ---------- 用例 16：到期日与免息期终点 ----------

    @Test
    fun `用例16 - 到期日与免息期终点`() {
        val ref = LocalDate.of(2026, 10, 24)
        // 本期账单日 10/5，+20 天 = 10/25
        assertEquals(LocalDate.of(2026, 10, 25), DateEngine.dueDateOf(5, 20, ref))
        // 下期账单日 11/5，+20 天 = 11/25（免息期终点）
        assertEquals(LocalDate.of(2026, 11, 25), DateEngine.nextDueDateOf(5, 20, ref))
    }
}
