package com.alosir.card.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.alosir.card.data.local.entity.AppSettingsEntity
import com.alosir.card.data.local.entity.BillEntity
import com.alosir.card.data.local.entity.BillingGroupEntity
import com.alosir.card.data.local.entity.CardEntity
import com.alosir.card.data.local.entity.NotificationLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * 账单组 DAO。
 */
@Dao
interface BillingGroupDao {

    @Query("SELECT * FROM billing_groups ORDER BY created_at ASC")
    fun observeAll(): Flow<List<BillingGroupEntity>>

    @Query("SELECT * FROM billing_groups WHERE id = :groupId")
    fun observeById(groupId: Long): Flow<BillingGroupEntity?>

    @Query("SELECT * FROM billing_groups WHERE id = :groupId")
    suspend fun getById(groupId: Long): BillingGroupEntity?

    @Insert
    suspend fun insert(group: BillingGroupEntity): Long

    @Update
    suspend fun update(group: BillingGroupEntity)

    @Query("DELETE FROM billing_groups WHERE id = :groupId")
    suspend fun deleteById(groupId: Long)

    /** 组匹配：同一银行 + 账单日相同（PRD F2.2） */
    @Query(
        "SELECT * FROM billing_groups " +
            "WHERE bank_name = :bankName AND billing_day = :billingDay LIMIT 1"
    )
    suspend fun findSharedGroup(bankName: String, billingDay: Int): BillingGroupEntity?

    /** 备份导出用：一次性全量读取 */
    @Query("SELECT * FROM billing_groups ORDER BY id ASC")
    suspend fun getAllOnce(): List<BillingGroupEntity>

    /** 备份恢复用：清空（先于子表调用） */
    @Query("DELETE FROM billing_groups")
    fun deleteAll(): Int
}

/**
 * 卡片 DAO。
 */
@Dao
interface CardDao {

    @Query("SELECT * FROM cards WHERE id = :cardId")
    fun observeById(cardId: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getById(cardId: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE group_id = :groupId ORDER BY created_at ASC")
    fun observeByGroup(groupId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE group_id = :groupId ORDER BY created_at ASC")
    suspend fun getByGroup(groupId: Long): List<CardEntity>

    @Insert
    suspend fun insert(card: CardEntity): Long

    @Update
    suspend fun update(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteById(cardId: Long)

    /** 全部卡片（列表页与组流合并用） */
    @Query("SELECT * FROM cards ORDER BY created_at ASC")
    fun observeAll(): Flow<List<CardEntity>>

    /** 卡号查重：命中盲指纹即视为同一张卡已录入 */
    @Query("SELECT id FROM cards WHERE card_number_fingerprint = :fingerprint LIMIT 1")
    suspend fun findIdByFingerprint(fingerprint: String): Long?

    /** 备份导出用：一次性全量读取 */
    @Query("SELECT * FROM cards ORDER BY id ASC")
    suspend fun getAllOnce(): List<CardEntity>

    /** 备份恢复用：清空 */
    @Query("DELETE FROM cards")
    fun deleteAll(): Int
}

/**
 * 账单 DAO。
 *
 * 写入三件套（覆盖式录入 / 累计式还款 / 撤销上次标记）刻意不用
 * OnConflictStrategy.REPLACE——REPLACE 是「先 DELETE 再 INSERT」，
 * 会重置主键与 created_at，还会触发外键级联（技术规范 §3.3）。
 */
@Dao
interface BillDao {

    @Query("SELECT * FROM bills WHERE group_id = :groupId ORDER BY billing_date DESC")
    fun observeByGroup(groupId: Long): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills ORDER BY billing_date DESC")
    fun observeAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE group_id = :groupId AND billing_date = :billingDate LIMIT 1")
    suspend fun findByGroupAndDate(groupId: Long, billingDate: Long): BillEntity?

    @Query("SELECT * FROM bills WHERE group_id = :groupId ORDER BY billing_date DESC LIMIT 1")
    suspend fun latestForGroup(groupId: Long): BillEntity?

    /** 该组最近一次出账的账单日 epochDay；无记录返回 null */
    @Query("SELECT MAX(billing_date) FROM bills WHERE group_id = :groupId")
    suspend fun maxBillingDate(groupId: Long): Long?

    @Query(
        "UPDATE bills SET bill_amount_cents = :amountCents, updated_at = :now " +
            "WHERE group_id = :groupId AND billing_date = :billingDate"
    )
    suspend fun updateAmount(groupId: Long, billingDate: Long, amountCents: Long, now: Long): Int

    @Insert
    suspend fun insert(bill: BillEntity): Long

    /** 补齐期用：同 (group_id, billing_date) 已存在则忽略（唯一索引兜底） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(bill: BillEntity): Long

    /**
     * 覆盖式录入（ADR-0004）：同一期内多次录入为最后一次的值，不累加。
     * 已还款金额不受影响——录入金额与标记还款是两个独立操作（PRD F3.1 规则 4）。
     */
    @Transaction
    suspend fun overwriteBillAmount(
        groupId: Long,
        billingDate: Long,
        amountCents: Long,
        now: Long,
    ) {
        val updated = updateAmount(groupId, billingDate, amountCents, now)
        if (updated == 0) {
            insert(
                BillEntity(
                    groupId = groupId,
                    billingDate = billingDate,
                    billAmountCents = amountCents,
                    paidAmountCents = 0L,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    @Query(
        "UPDATE bills SET paid_amount_cents = :paidAmount, last_paid_cents = :lastPaid, " +
            "paid_before_last_cents = :paidBeforeLast, updated_at = :now " +
            "WHERE group_id = :groupId AND billing_date = :billingDate"
    )
    suspend fun updatePaid(
        groupId: Long,
        billingDate: Long,
        paidAmount: Long,
        lastPaid: Long,
        paidBeforeLast: Long,
        now: Long,
    ): Int

    /**
     * 累计式标记还款（ADR-0007）。金额未录入返回 false（PRD F4.1 规则 7）。
     * 超额还款保留在 paidAmount 中，状态机判为已结清（PRD F4.1 规则 6）。
     */
    @Transaction
    suspend fun appendPayment(
        groupId: Long,
        billingDate: Long,
        paymentCents: Long,
        now: Long,
    ): Boolean {
        val bill = findByGroupAndDate(groupId, billingDate) ?: return false
        if (bill.billAmountCents == null) return false
        updatePaid(
            groupId = groupId,
            billingDate = billingDate,
            paidAmount = bill.paidAmountCents + paymentCents,
            lastPaid = paymentCents,
            paidBeforeLast = bill.paidAmountCents,
            now = now,
        )
        return true
    }

    /** 撤销最近一次标记（PRD F4.2）：精确回退到上一次标记之前 */
    @Transaction
    suspend fun undoLastPayment(groupId: Long, billingDate: Long, now: Long): Boolean {
        val bill = findByGroupAndDate(groupId, billingDate) ?: return false
        val before = bill.paidBeforeLastCents ?: return false
        updatePaid(
            groupId = groupId,
            billingDate = billingDate,
            paidAmount = before,
            lastPaid = 0L,
            paidBeforeLast = 0L,
            now = now,
        )
        return true
    }

    /** 备份导出用：一次性全量读取 */
    @Query("SELECT * FROM bills ORDER BY group_id ASC, billing_date ASC")
    suspend fun getAllOnce(): List<BillEntity>

    /** 备份恢复用：清空 */
    @Query("DELETE FROM bills")
    fun deleteAll(): Int
}

/** 设置 DAO（键值对） */
@Dao
interface AppSettingsDao {

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: AppSettingsEntity)
}

/** 通知幂等台账 DAO */
@Dao
interface NotificationLogDao {

    @Query(
        "SELECT COUNT(*) > 0 FROM notification_log " +
            "WHERE kind = :kind AND group_id = :groupId " +
            "AND bill_date = :billDate AND scheduled_date = :scheduledDate"
    )
    suspend fun hasBeenSent(
        kind: String,
        groupId: Long,
        billDate: Long,
        scheduledDate: Long,
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: NotificationLogEntity): Long
}

/**
 * 额度聚合 DAO（PRD 9.5：走 SQL SUM，不在内存中遍历计算）。
 */
@Dao
interface QuotaDao {

    /** 总授信额度：所有组 credit_limit 之和；NULL 组不计入 */
    @Query("SELECT COALESCE(SUM(credit_limit_cents), 0) FROM billing_groups")
    fun observeTotalLimitCents(): Flow<Long>

    /**
     * 已用额度：各组「本期账单」（最近一期）的 账单金额 − 已还款，仅未结清。
     * 未录入金额（NULL）的期按 0 计入。
     */
    @Query(
        """
        SELECT COALESCE(SUM(
            COALESCE(
                (SELECT MAX(0, b.bill_amount_cents - b.paid_amount_cents)
                 FROM bills b
                 WHERE b.group_id = g.id
                 ORDER BY b.billing_date DESC LIMIT 1),
                0)
        ), 0)
        FROM billing_groups g
        """
    )
    fun observeTotalUsedCents(): Flow<Long>

    /** 各组额度占用（额度明细页 P7） */
    @Query(
        """
        SELECT g.id AS groupId, g.name AS name, g.bank_name AS bankName,
               g.credit_limit_cents AS creditLimitCents,
               COALESCE((SELECT MAX(0, b.bill_amount_cents - b.paid_amount_cents)
                    FROM bills b
                    WHERE b.group_id = g.id
                    ORDER BY b.billing_date DESC LIMIT 1), 0) AS usedCents,
               COALESCE((SELECT CASE WHEN b.bill_amount_cents IS NULL THEN 1 ELSE 0 END
                    FROM bills b
                    WHERE b.group_id = g.id
                    ORDER BY b.billing_date DESC LIMIT 1), 0) AS pendingEntry
        FROM billing_groups g
        ORDER BY g.created_at ASC
        """
    )
    fun observeGroupUsage(): Flow<List<GroupUsageRow>>
}

/** 额度明细行（Room 投影） */
data class GroupUsageRow(
    val groupId: Long,
    val name: String,
    val bankName: String?,
    val creditLimitCents: Long?,
    val usedCents: Long,
    val pendingEntry: Boolean,
)
