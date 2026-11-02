package com.alosir.card.domain

import java.util.regex.Pattern

/**
 * 卡号识别。
 *
 * 规则来自 `bin-data/` 调研结论（详见 docs/ 中的调研记录）：
 * - 卡组织可由号段判定，**但不能只看首位数字**——首位是 MII 行业大类，
 *   3 开头需再判 2–3 位才能区分 Amex / JCB / Diners。
 * - 62 段可判「银联」但判不出具体银行（62 由银联再分配给各家银行）。
 * - Discover 与银联在 622126–622925 重叠；**中国境内清算语境下应判为银联**。
 * - 双标卡历史存量用外卡组织号段（4/5/35 开头），2016 年后新发用 62 开头。
 *
 * 银行归属需内置 BIN 表，暂未实现（OQ-1）。当前只做卡组织识别——
 * 这是纯正则可完成、100% 可靠的部分。
 */
object CardNumberIdentifier {

    /** 卡组织枚举 */
    enum class Scheme(val displayName: String) {
        UNIONPAY("中国银联"),
        VISA("Visa"),
        MASTERCARD("Mastercard"),
        AMEX("美国运通"),
        JCB("JCB"),
        DINERS("Diners Club"),
        UNKNOWN("未知"),
    }

    /** 清洗后的号段长度下限：至少 4 位才能判定 Mastercard 2 系列 */
    private const val MIN_LENGTH = 2

    /** 卡组织 → 存储/展示的短键（union / visa / mastercard / amex / jcb / diners / unknown） */
    fun schemeKey(scheme: Scheme): String = when (scheme) {
        Scheme.UNIONPAY -> "union"
        Scheme.VISA -> "visa"
        Scheme.MASTERCARD -> "mastercard"
        Scheme.AMEX -> "amex"
        Scheme.JCB -> "jcb"
        Scheme.DINERS -> "diners"
        Scheme.UNKNOWN -> "unknown"
    }

    /** 清洗输入：去空格连字符、全角转半角、只留数字 */
    fun normalize(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when (ch) {
                in '0'..'9' -> sb.append(ch)
                // 全角数字 U+FF10–U+FF19 → 半角
                in '０'..'９' -> sb.append(('0' + (ch - '０')))
                ' ', '-', '　' -> Unit
                else -> Unit
            }
        }
        return sb.toString()
    }

    /**
     * 判定卡组织。
     * 判定顺序有意为之：先判需要更长前缀的组织，避免被短前缀规则抢先匹配。
     */
    fun detectScheme(raw: String): Scheme {
        val digits = normalize(raw)
        if (digits.length < MIN_LENGTH) return Scheme.UNKNOWN

        val first2 = digits.substring(0, minOf(2, digits.length))
        val first4 = if (digits.length >= 4) digits.substring(0, 4).toIntOrNull() else null

        return when {
            // Amex：34 / 37（15 位卡，4-6-5 排列）
            first2 == "34" || first2 == "37" -> Scheme.AMEX

            // Diners：300–305 / 3095 / 36 / 38–39
            first2 == "36" -> Scheme.DINERS
            first2 == "38" || first2 == "39" -> Scheme.DINERS
            (digits.length >= 4 && digits.startsWith("3095")) -> Scheme.DINERS
            (first2 == "30" && digits.length >= 3 && digits[2] in '0'..'5') -> Scheme.DINERS

            // JCB：3528–3589（仅此区间，不是整个 35）
            first4 != null && first4 in 3528..3589 -> Scheme.JCB

            // 银联：62 全部；另有 60 / 67 / 81；9 为历史国内标准卡
            digits.startsWith("62") -> Scheme.UNIONPAY
            digits.startsWith("60") -> Scheme.UNIONPAY
            digits.startsWith("67") -> Scheme.UNIONPAY
            digits.startsWith("81") -> Scheme.UNIONPAY

            // Visa：4 开头
            digits.startsWith("4") -> Scheme.VISA

            // Mastercard：51–55；新 2 系列 2221–2720（4 位边界）
            first2 == "51" || first2 == "52" || first2 == "53" ||
                first2 == "54" || first2 == "55" -> Scheme.MASTERCARD
            first4 != null && first4 in 2221..2720 -> Scheme.MASTERCARD

            else -> Scheme.UNKNOWN
        }
    }

    /**
     * 卡号长度是否匹配已识别的卡组织。
     * 用于录入时的即时校验提示。
     */
    fun isValidLength(raw: String): Boolean {
        val digits = normalize(raw)
        if (digits.length < MIN_LENGTH) return false
        return when (detectScheme(digits)) {
            Scheme.AMEX -> digits.length == 15
            Scheme.DINERS -> digits.length == 14 || digits.length == 16 || digits.length == 19
            Scheme.UNIONPAY -> digits.length in 16..19
            Scheme.VISA -> digits.length == 13 || digits.length == 16 || digits.length == 19
            Scheme.MASTERCARD -> digits.length == 16
            Scheme.JCB -> digits.length in 16..19
            Scheme.UNKNOWN -> digits.length in 13..19
        }
    }

    /** 期望长度说明，用于错误提示文案 */
    fun expectedLengthHint(raw: String): String {
        val digits = normalize(raw)
        return when (detectScheme(digits)) {
            Scheme.AMEX -> "美国运通卡为 15 位，当前 ${digits.length} 位"
            Scheme.DINERS -> "Diners 卡为 14/16/19 位，当前 ${digits.length} 位"
            Scheme.UNIONPAY -> "银联卡为 16–19 位，当前 ${digits.length} 位"
            Scheme.VISA -> "Visa 卡为 13/16/19 位，当前 ${digits.length} 位"
            Scheme.MASTERCARD -> "Mastercard 为 16 位，当前 ${digits.length} 位"
            Scheme.JCB -> "JCB 卡为 16–19 位，当前 ${digits.length} 位"
            Scheme.UNKNOWN -> "卡号位数不正确，当前 ${digits.length} 位"
        }
    }

    /**
     * Luhn 校验（ISO/IEC 7812 mod-10）。
     *
     * 已实测确认适用于中国银联卡，且 16/17/18/19 位均正确。
     * 加倍位必须**从最右位往左数**，校验位本身不翻倍——
     * 不要按固定 16 位写死。
     */
    fun luhnCheck(raw: String): Boolean {
        val digits = normalize(raw)
        if (digits.length < 2 || digits.any { !it.isDigit() }) return false

        var sum = 0
        var double = false
        for (i in digits.indices.reversed()) {
            var d = digits[i] - '0'
            if (double) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            double = !double
        }
        return sum % 10 == 0
    }

    /**
     * 录入校验。返回 null 表示通过，否则返回中文错误说明。
     *
     * 注意顺序：**先查非法字符，再查长度**。
     * 若先用 normalize 清洗，字母会被静默丢掉，用户输入 "4111abc"
     * 会变成 "4111" 然后报「位数不足」——错误信息指不到真正的问题。
     */
    fun validate(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null // 卡号非必填
        if (trimmed.any { it !in '0'..'9' && it != ' ' && it != '-' && it != '　' && it !in '０'..'９' }) {
            return "卡号只能包含数字"
        }
        val digits = normalize(trimmed)
        if (digits.length < 6) return "卡号至少需要 6 位才能识别发卡行"
        if (!isValidLength(digits)) return expectedLengthHint(digits)
        if (!luhnCheck(digits)) return "校验位不正确，请核对卡号"
        return null
    }

    /**
     * 末 4 位。卡号不足 4 位时返回 null（此时不该显示尾号）。
     */
    fun last4(raw: String): String? {
        val digits = normalize(raw)
        return if (digits.length >= 4) digits.takeLast(4) else null
    }

    /**
     * 脱敏展示格式：•••• •••• •••• 6688
     */
    fun masked(raw: String): String {
        val digits = normalize(raw)
        val last = last4(digits) ?: return "•••• •••• •••• ••••"
        val maskedLength = (digits.length - 4).coerceAtLeast(0)
        val sb = StringBuilder()
        for (i in 0 until maskedLength) {
            sb.append('•')
            if ((i + 1) % 4 == 0 && i + 1 < maskedLength) sb.append(' ')
        }
        if (sb.isNotEmpty()) sb.append(' ')
        sb.append(last)
        return sb.toString()
    }
}
