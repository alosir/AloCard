package com.alosir.card.domain

import com.alosir.card.domain.model.GroupQuota

object QuotaAggregation {
    fun byBank(rows: List<GroupQuota>): List<GroupQuota> {
        return rows.groupBy { row ->
            row.bankName?.trim()?.takeIf { it.isNotEmpty() } ?: "group:${row.groupId}"
        }.map { (key, values) ->
            val first = values.first()
            GroupQuota(
                groupId = first.groupId,
                name = first.bankName?.trim()?.takeIf { it.isNotEmpty() } ?: first.name,
                bankName = first.bankName,
                creditLimitCents = values.mapNotNull { it.creditLimitCents }.maxOrNull(),
                usedCents = values.sumOf { it.usedCents },
                pendingEntry = values.any { it.pendingEntry },
            )
        }
    }
}
