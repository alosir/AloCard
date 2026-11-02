package com.alosir.card.data.repository

import com.alosir.card.data.local.dao.BillDao
import com.alosir.card.data.local.dao.BillingGroupDao
import com.alosir.card.data.local.dao.CardDao
import com.alosir.card.data.local.dao.QuotaDao
import com.alosir.card.data.local.entity.BillEntity
import com.alosir.card.data.local.entity.BillingGroupEntity
import com.alosir.card.data.local.entity.CardEntity
import com.alosir.card.domain.model.Bill
import com.alosir.card.domain.model.BillingGroup
import com.alosir.card.domain.model.Card
import com.alosir.card.domain.model.CreditTotals
import com.alosir.card.domain.model.GroupQuota
import com.alosir.card.domain.model.GroupWithCards
import com.alosir.card.domain.repository.BillingGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingGroupRepositoryImpl @Inject constructor(
    private val groupDao: BillingGroupDao,
    private val cardDao: CardDao,
    private val billDao: BillDao,
    private val quotaDao: QuotaDao,
) : BillingGroupRepository {

    /**
     * 列表页数据源：组 × 卡片 × 账单三流合并。
     * 每组的「本期账单」= 该组 billing_date 最大的一行（PRD 4.1：本期 = 最近一次已出账）。
     */
    override fun observeGroupsWithCards(): Flow<List<GroupWithCards>> =
        combine(
            groupDao.observeAll(),
            cardDao.observeAll(),
            billDao.observeAll(),
        ) { groups, cards, bills ->
            groups.map { group ->
                group.toDomain(
                    cards = cards.filter { it.groupId == group.id },
                    latestBill = bills
                        .filter { it.groupId == group.id }
                        .maxByOrNull { it.billingDate },
                )
            }
        }

    override fun observeGroup(groupId: Long): Flow<GroupWithCards?> =
        combine(
            groupDao.observeById(groupId),
            cardDao.observeByGroup(groupId),
            billDao.observeByGroup(groupId),
        ) { group, cards, bills ->
            group?.toDomain(
                cards = cards,
                latestBill = bills.maxByOrNull { it.billingDate },
            )
        }

    override fun observeGroups(): Flow<List<BillingGroup>> =
        groupDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun createGroup(group: BillingGroup): Long {
        val now = System.currentTimeMillis()
        return groupDao.insert(
            BillingGroupEntity(
                name = group.name,
                billingDay = group.billingDay,
                dueOffsetDays = group.dueOffsetDays,
                creditLimitCents = group.creditLimitCents,
                bankName = group.bankName,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    override suspend fun updateGroup(group: BillingGroup) {
        val existing = groupDao.getById(group.id) ?: return
        groupDao.update(
            existing.copy(
                name = group.name,
                billingDay = group.billingDay,
                dueOffsetDays = group.dueOffsetDays,
                creditLimitCents = group.creditLimitCents,
                bankName = group.bankName,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun deleteGroup(groupId: Long) = groupDao.deleteById(groupId)

    override suspend fun matchGroup(bankName: String, billingDay: Int): BillingGroup? =
        groupDao.findSharedGroup(bankName, billingDay)?.toDomain()

    override fun observeTotals(): Flow<CreditTotals> =
        combine(quotaDao.observeTotalLimitCents(), quotaDao.observeTotalUsedCents()) { limit, used ->
            CreditTotals(totalLimitCents = limit, usedCents = used)
        }

    override fun observeGroupQuotas(): Flow<List<GroupQuota>> =
        quotaDao.observeGroupUsage().map { rows ->
            rows.map {
                GroupQuota(
                    groupId = it.groupId,
                    name = it.name,
                    bankName = it.bankName,
                    creditLimitCents = it.creditLimitCents,
                    usedCents = it.usedCents,
                    pendingEntry = it.pendingEntry,
                )
            }
        }

    // ---------- 映射 ----------

    private fun BillingGroupEntity.toDomain() = BillingGroup(
        id = id,
        name = name,
        billingDay = billingDay,
        dueOffsetDays = dueOffsetDays,
        creditLimitCents = creditLimitCents,
        bankName = bankName,
    )

    private fun BillingGroupEntity.toDomain(
        cards: List<CardEntity>,
        latestBill: BillEntity?,
    ) = GroupWithCards(
        id = id,
        name = name,
        billingDay = billingDay,
        dueOffsetDays = dueOffsetDays,
        creditLimitCents = creditLimitCents,
        bankName = bankName,
        cards = cards.map { it.toDomain() },
        currentBill = latestBill?.toDomain(),
    )

    private fun CardEntity.toDomain() = Card(
        id = id,
        groupId = groupId,
        cardNumberLast4 = cardNumberLast4,
        hasCardNumber = cardNumberCipher != null,
        expiryYm = expiryYm,
        cardScheme = cardScheme,
        cardLabel = cardLabel,
        bankName = bankName,
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
