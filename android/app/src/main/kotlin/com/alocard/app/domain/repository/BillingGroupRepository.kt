package com.alocard.app.domain.repository

import com.alocard.app.domain.model.BillingGroup
import com.alocard.app.domain.model.GroupQuota
import com.alocard.app.domain.model.GroupWithCards
import com.alocard.app.domain.model.CreditTotals
import kotlinx.coroutines.flow.Flow

/**
 * 账单组仓库。
 *
 * 接口在 domain 层，实现在 data 层——UI 层只依赖接口，
 * 不关心数据来自 Room 还是别的来源（技术规范 §1.2 依赖方向铁律）。
 */
interface BillingGroupRepository {

    /** 观察全部账单组及其卡片与本期账单，供列表页使用 */
    fun observeGroupsWithCards(): Flow<List<GroupWithCards>>

    /** 观察单个账单组（详情页） */
    fun observeGroup(groupId: Long): Flow<GroupWithCards?>

    /** 观察全部账单组（账单组管理页 P14） */
    fun observeGroups(): Flow<List<BillingGroup>>

    /** 新建账单组，返回自增 id */
    suspend fun createGroup(group: BillingGroup): Long

    /** 更新账单组的名称/账期/额度（PRD F2.4） */
    suspend fun updateGroup(group: BillingGroup)

    /** 删除账单组：级联删除其下卡片与账单（PRD F2.5、ADR-0006） */
    suspend fun deleteGroup(groupId: Long)

    /**
     * 组匹配：「同一银行 + 账单日相同」命中已有组（PRD F2.2）。
     * 命中则提示用户加入，避免共享账单的卡被建成两个组、额度重复计数。
     */
    suspend fun matchGroup(bankName: String, billingDay: Int): BillingGroup?

    /** 观察额度总览（PRD 5.7：对账单组求和） */
    fun observeTotals(): Flow<CreditTotals>

    /** 观察各账单组的额度占用（额度明细页 P7） */
    fun observeGroupQuotas(): Flow<List<GroupQuota>>
}
