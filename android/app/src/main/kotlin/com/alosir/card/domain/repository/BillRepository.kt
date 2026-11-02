package com.alosir.card.domain.repository

import com.alosir.card.domain.model.Bill
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 账单仓库：按期滚动的账单序列（ADR-0004）。
 */
interface BillRepository {

    /** 观察某账单组的全部账单，账单日倒序（账单历史页 P6） */
    fun observeBills(groupId: Long): Flow<List<Bill>>

    /**
     * 补齐账单期（PRD F3.3）：
     * 为每个账单组创建「本期账单日」及之前缺失的期记录（金额 null = 待录入），
     * 不生成未来期。App 打开时调用；滚动不依赖用户操作。
     */
    suspend fun syncBillPeriods()

    /**
     * 覆盖式录入账单金额（ADR-0004）：同一期内多次录入为最后一次的值，不累加。
     * 不影响免息期与距还款日——那两个坐标只依赖账单日与 N。
     */
    suspend fun saveBillAmount(groupId: Long, billDate: LocalDate, amountCents: Long)

    /**
     * 累计式标记还款（ADR-0007）：paidAmount 累加，释放已用额度。
     * 金额未录入时不允许标记（返回 false，PRD F4.1 规则 7）。
     */
    suspend fun recordPayment(groupId: Long, billDate: LocalDate, amountCents: Long): Boolean

    /** 撤销最近一次标记（PRD F4.2），返回是否撤销成功 */
    suspend fun undoLastPayment(groupId: Long, billDate: LocalDate): Boolean
}
