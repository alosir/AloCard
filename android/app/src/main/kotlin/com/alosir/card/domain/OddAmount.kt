package com.alosir.card.domain

object OddAmount {
    fun forClick(outstandingCents: Long, clickIndex: Int): Long? {
        if (outstandingCents <= 0L) return null
        val candidates = (1..4).map { level ->
            val modulus = POWERS[level + 2]
            outstandingCents % modulus
        }.filter { it > 0L && it < outstandingCents }
        if (candidates.isEmpty()) return null
        return candidates[clickIndex.mod(candidates.size)]
    }

    private val POWERS = longArrayOf(1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L)
}
