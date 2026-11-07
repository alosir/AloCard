package com.alosir.card.domain

data class ParsedCardText(
    val cardNumber: String?,
    val expiryYm: String?,
    val bankName: String?,
)

object CardTextParser {
    fun parse(text: String): ParsedCardText {
        val normalized = normalizeOcrCharacters(text)
        val bank = BANK_KEYWORDS.firstOrNull { text.contains(it) }
        val expiry = Regex("""(0[1-9]|1[0-2])\s*/\s*(\d{2}(?:\d{2})?)""").find(normalized)?.let { match ->
            val month = match.groupValues[1]
            val year = match.groupValues[2]
            "${if (year.length == 4) year else "20$year"}-$month"
        }
        val number = Regex("""(?<!\d)\d(?:[ \t-]?\d){12,18}(?!\d)""").findAll(normalized)
            .map { CardNumberIdentifier.normalize(it.value) }
            .filter { CardNumberIdentifier.isValidLength(it) }
            .distinct()
            .firstOrNull { CardNumberIdentifier.luhnCheck(it) }
        return ParsedCardText(number, expiry, bank)
    }

    private fun normalizeOcrCharacters(text: String): String = buildString(text.length) {
        for (char in text) {
            when (char) {
                in '０'..'９' -> append('0' + (char - '０'))
                'O', 'o', 'Q', 'D' -> append('0')
                'I', 'l', '|' -> append('1')
                'Z', 'z' -> append('2')
                'S', 's' -> append('5')
                'G' -> append('6')
                'B' -> append('8')
                else -> append(char)
            }
        }
    }

    private val BANK_KEYWORDS = listOf(
        "招商银行", "工商银行", "建设银行", "农业银行", "中国银行", "交通银行",
        "中信银行", "浦发银行", "光大银行", "民生银行", "兴业银行", "广发银行",
        "平安银行", "华夏银行", "浙商银行", "北京银行", "上海银行", "邮储银行", "邮政储蓄",
    )
}
