package com.alosir.card.domain

object BankLogoNameResolver {
    fun resolve(bankName: String?, knownBanks: Set<String>): String? {
        val raw = bankName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (raw in knownBanks) return raw
        val shortened = raw.removePrefix("中国")
        return knownBanks.firstOrNull { it == shortened }
    }
}
