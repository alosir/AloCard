package com.alocard.app.domain.repository

import com.alocard.app.domain.model.Card
import kotlinx.coroutines.flow.Flow

/**
 * 卡片仓库。
 *
 * 卡号的**明文**只在本接口的入参与 [decryptCardNumber] 的返回值中出现——
 * 仓库实现负责与 crypto 层协作完成加密写入与解密读取（技术规范 C3）。
 */
interface CardRepository {

    /** 观察单个卡片（编辑页） */
    fun observeCard(cardId: Long): Flow<Card?>

    /** 新建卡片。plainCardNumber 为完整卡号明文，可空；null 表示未填卡号。 */
    suspend fun createCard(
        groupId: Long,
        plainCardNumber: String?,
        cardNumberLast4: String?,
        expiryYm: String?,
        cardScheme: String,
        cardLabel: String?,
        bankName: String?,
    ): Long

    /**
     * 更新卡片基础信息。
     * [plainCardNumber] 传 null 表示「不动已存卡号」；传非空时立即重新加密存储、
     * 旧密文清除（PRD F1.2）；传空串 "" 表示清除卡号。
     */
    suspend fun updateCard(
        cardId: Long,
        plainCardNumber: String?,
        cardNumberLast4: String?,
        expiryYm: String?,
        cardScheme: String,
        cardLabel: String?,
        bankName: String?,
    )

    /** 删除卡片（不影响账单组与其账单，PRD F1.3） */
    suspend fun deleteCard(cardId: Long)

    /**
     * 解密完整卡号。仅应在用户通过安全密码校验后调用（PRD F1.4）。
     * 无卡号或解密失败返回 null。
     */
    suspend fun decryptCardNumber(cardId: Long): String?
}
