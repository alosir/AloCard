package com.alosir.card.data.repository

import com.alosir.card.data.local.dao.BillDao
import com.alosir.card.data.local.dao.BillingGroupDao
import com.alosir.card.data.local.entity.BillEntity
import com.alosir.card.domain.DateEngine
import com.alosir.card.domain.model.Bill
import com.alosir.card.domain.repository.BillRepository
import com.alosir.card.domain.util.DateProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillRepositoryImpl @Inject constructor(
    private val billDao: BillDao,
    private val groupDao: BillingGroupDao,
) : BillRepository {

    override fun observeBills(groupId: Long): Flow<List<Bill>> =
        billDao.observeByGroup(groupId).map { list -> list.map { it.toDomain() } }

    /**
     * 补齐账单期（PRD F3.3）：
     * 对每个账期齐全的账单组，创建「本期账单日」（currentBillingDate）及之前
     * 缺失的期记录（金额 null = 待录入）。不生成未来期。
     * 即使 App 数月未打开，也只补齐到本期——历史期不遗漏、未来期不预建。
     */
    override suspend fun syncBillPeriods() {
        val today = DateProvider.today()
        val now = System.currentTimeMillis()
        val groups = groupDao.observeAll().first()
        for (group in groups) {
            val billingDay = group.billingDay ?: continue
            val current = DateEngine.currentBillingDate(billingDay, today)
            val currentEpoch = current.toEpochDay()

            var cursor = billDao.maxBillingDate(group.id)
            if (cursor == null) {
                billDao.insertIgnore(
                    BillEntity(
                        groupId = group.id,
                        billingDate = currentEpoch,
                        billAmountCents = null,
                        paidAmountCents = 0L,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
                continue
            }
            // 从最近一期逐期推进到本期，缺的补上
            while (cursor != null && cursor < currentEpoch) {
                val next = DateEngine.nextBillingDate(
                    billingDay,
                    LocalDate.ofEpochDay(cursor).plusDays(1),
                ).toEpochDay()
                if (next > currentEpoch) break
                billDao.insertIgnore(
                    BillEntity(
                        groupId = group.id,
                        billingDate = next,
                        billAmountCents = null,
                        paidAmountCents = 0L,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
                cursor = next
            }
        }
    }

    override suspend fun saveBillAmount(groupId: Long, billDate: LocalDate, amountCents: Long) {
        billDao.overwriteBillAmount(
            groupId = groupId,
            billingDate = billDate.toEpochDay(),
            amountCents = amountCents,
            now = System.currentTimeMillis(),
        )
    }

    override suspend fun recordPayment(
        groupId: Long,
        billDate: LocalDate,
        amountCents: Long,
    ): Boolean = billDao.appendPayment(
        groupId = groupId,
        billingDate = billDate.toEpochDay(),
        paymentCents = amountCents,
        now = System.currentTimeMillis(),
    )

    override suspend fun undoLastPayment(groupId: Long, billDate: LocalDate): Boolean =
        billDao.undoLastPayment(
            groupId = groupId,
            billingDate = billDate.toEpochDay(),
            now = System.currentTimeMillis(),
        )

    private fun BillEntity.toDomain() = Bill(
        id = id,
        groupId = groupId,
        billingDate = LocalDate.ofEpochDay(billingDate),
        billAmountCents = billAmountCents,
        paidAmountCents = paidAmountCents,
        canUndoLastPayment = paidBeforeLastCents != null,
    )
}

