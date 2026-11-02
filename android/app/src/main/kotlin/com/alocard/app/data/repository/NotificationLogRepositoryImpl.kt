package com.alocard.app.data.repository

import com.alocard.app.data.local.dao.NotificationLogDao
import com.alocard.app.data.local.entity.NotificationLogEntity
import com.alocard.app.domain.repository.NotificationLogRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationLogRepositoryImpl @Inject constructor(
    private val dao: NotificationLogDao,
) : NotificationLogRepository {

    override suspend fun hasBeenSent(
        kind: String,
        groupId: Long,
        billDate: Long,
        scheduledDate: Long,
    ): Boolean = dao.hasBeenSent(kind, groupId, billDate, scheduledDate)

    override suspend fun logSent(
        kind: String,
        groupId: Long,
        billDate: Long,
        scheduledDate: Long,
        deliveredAt: Long,
    ) {
        dao.insert(
            NotificationLogEntity(
                kind = kind,
                groupId = groupId,
                billDate = billDate,
                scheduledDate = scheduledDate,
                deliveredAt = deliveredAt,
            )
        )
    }
}
