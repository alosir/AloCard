package com.alosir.card.data.repository

import com.alosir.card.data.local.dao.CardDao
import com.alosir.card.data.local.entity.CardEntity
import com.alosir.card.data.security.CryptoManager
import com.alosir.card.domain.CardNumberIdentifier
import com.alosir.card.domain.model.Card
import com.alosir.card.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao,
    private val crypto: CryptoManager,
) : CardRepository {

    override fun observeCard(cardId: Long): Flow<Card?> =
        cardDao.observeById(cardId).map { it?.toDomain() }

    override suspend fun createCard(
        groupId: Long,
        plainCardNumber: String?,
        cardNumberLast4: String?,
        expiryYm: String?,
        cardScheme: String,
        cardLabel: String?,
        bankName: String?,
    ): Long {
        val now = System.currentTimeMillis()
        val normalized = plainCardNumber?.let { CardNumberIdentifier.normalize(it) }
        return cardDao.insert(
            CardEntity(
                groupId = groupId,
                cardNumberCipher = crypto.encryptCardNumber(normalized),
                cardNumberLast4 = cardNumberLast4,
                cardNumberFingerprint = normalized?.takeIf { it.length >= 6 }
                    ?.let { crypto.fingerprint(it) },
                expiryYm = expiryYm,
                cardScheme = cardScheme,
                cardLabel = cardLabel,
                bankName = bankName,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    override suspend fun updateCard(
        cardId: Long,
        plainCardNumber: String?,
        cardNumberLast4: String?,
        expiryYm: String?,
        cardScheme: String,
        cardLabel: String?,
        bankName: String?,
    ) {
        val existing = cardDao.getById(cardId) ?: return
        // null = 不动已存卡号；非 null（含空串）= 重加密/清除
        val normalized = plainCardNumber?.let { CardNumberIdentifier.normalize(it) }
        val newCipher = when {
            plainCardNumber == null -> existing.cardNumberCipher
            normalized.isNullOrEmpty() -> null
            else -> crypto.encryptCardNumber(normalized)
        }
        val newFingerprint = when {
            plainCardNumber == null -> existing.cardNumberFingerprint
            normalized.isNullOrEmpty() -> null
            normalized.length >= 6 -> crypto.fingerprint(normalized)
            else -> null
        }
        cardDao.update(
            existing.copy(
                cardNumberCipher = newCipher,
                cardNumberLast4 = cardNumberLast4 ?: existing.cardNumberLast4,
                cardNumberFingerprint = newFingerprint,
                expiryYm = expiryYm,
                cardScheme = cardScheme,
                cardLabel = cardLabel,
                bankName = bankName,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun deleteCard(cardId: Long) = cardDao.deleteById(cardId)

    override suspend fun decryptCardNumber(cardId: Long): String? {
        val entity = cardDao.getById(cardId) ?: return null
        return crypto.decryptCardNumber(entity.cardNumberCipher)
    }

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
}
