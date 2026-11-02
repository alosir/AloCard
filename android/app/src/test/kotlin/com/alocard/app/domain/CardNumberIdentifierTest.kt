package com.alocard.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 卡号识别测试。
 *
 * 覆盖调研结论中的关键边界：首位数字不足以判定组织、
 * Mastercard 2 系列是 4 位边界、JCB 仅 3528–3589、
 * Discover 重叠段在境内判为银联、Luhn 适用于银联卡。
 */
class CardNumberIdentifierTest {

    // ---------- 卡组织识别 ----------

    @Test
    fun `识别银联62段`() {
        assertEquals(
            CardNumberIdentifier.Scheme.UNIONPAY,
            CardNumberIdentifier.detectScheme("6217001234567890123")
        )
        assertEquals(
            CardNumberIdentifier.Scheme.UNIONPAY,
            CardNumberIdentifier.detectScheme("6222021234567890")
        )
        // 银联的 60 / 67 / 81 段
        assertEquals(
            CardNumberIdentifier.Scheme.UNIONPAY,
            CardNumberIdentifier.detectScheme("6013821234567890")
        )
    }

    @Test
    fun `识别Visa`() {
        assertEquals(
            CardNumberIdentifier.Scheme.VISA,
            CardNumberIdentifier.detectScheme("4111111111111111")
        )
    }

    @Test
    fun `Mastercard旧系列51到55`() {
        for (p in listOf("51", "52", "53", "54", "55")) {
            assertEquals(
                "前缀 $p 应识别为 Mastercard",
                CardNumberIdentifier.Scheme.MASTERCARD,
                CardNumberIdentifier.detectScheme("${p}5555555555554444")
            )
        }
    }

    @Test
    fun `Mastercard新2系列是4位边界`() {
        // 2221 是、2220 不是、2721 不是
        assertEquals(
            CardNumberIdentifier.Scheme.MASTERCARD,
            CardNumberIdentifier.detectScheme("2221000000000000")
        )
        assertEquals(
            CardNumberIdentifier.Scheme.MASTERCARD,
            CardNumberIdentifier.detectScheme("2720000000000000")
        )
        // 2220 不在 2221–2720 区间，不应判为 Mastercard
        assertFalse(
            CardNumberIdentifier.detectScheme("2220000000000000") ==
                CardNumberIdentifier.Scheme.MASTERCARD
        )
        assertFalse(
            CardNumberIdentifier.detectScheme("2721000000000000") ==
                CardNumberIdentifier.Scheme.MASTERCARD
        )
    }

    @Test
    fun `识别美国运通34和37`() {
        assertEquals(
            CardNumberIdentifier.Scheme.AMEX,
            CardNumberIdentifier.detectScheme("378282246310005")
        )
        assertEquals(
            CardNumberIdentifier.Scheme.AMEX,
            CardNumberIdentifier.detectScheme("340000000000009")
        )
    }

    @Test
    fun `JCB仅3528到3589区间`() {
        assertEquals(
            CardNumberIdentifier.Scheme.JCB,
            CardNumberIdentifier.detectScheme("3528000000000000")
        )
        assertEquals(
            CardNumberIdentifier.Scheme.JCB,
            CardNumberIdentifier.detectScheme("3589000000000000")
        )
        // 3527 与 3590 不在区间内
        assertFalse(
            CardNumberIdentifier.detectScheme("3527000000000000") ==
                CardNumberIdentifier.Scheme.JCB
        )
        assertFalse(
            CardNumberIdentifier.detectScheme("3590000000000000") ==
                CardNumberIdentifier.Scheme.JCB
        )
    }

    @Test
    fun `识别Diners`() {
        assertEquals(
            CardNumberIdentifier.Scheme.DINERS,
            CardNumberIdentifier.detectScheme("30569309025904")
        )
        assertEquals(
            CardNumberIdentifier.Scheme.DINERS,
            CardNumberIdentifier.detectScheme("36000000000000")
        )
    }

    @Test
    fun `3开头分流不混淆`() {
        // 34/37 → Amex，36 → Diners，3528 → JCB
        assertTrue(
            CardNumberIdentifier.detectScheme("341111111111111").let {
                it == CardNumberIdentifier.Scheme.AMEX
            }
        )
        assertTrue(
            CardNumberIdentifier.detectScheme("36111111111111").let {
                it == CardNumberIdentifier.Scheme.DINERS
            }
        )
        assertTrue(
            CardNumberIdentifier.detectScheme("3528111111111111").let {
                it == CardNumberIdentifier.Scheme.JCB
            }
        )
    }

    @Test
    fun `Discover重叠段在境内判为银联`() {
        // 622126–622925 与美国 Discover 重叠；境内清算语境应判银联
        assertEquals(
            CardNumberIdentifier.Scheme.UNIONPAY,
            CardNumberIdentifier.detectScheme("6221261234567890")
        )
        assertEquals(
            CardNumberIdentifier.Scheme.UNIONPAY,
            CardNumberIdentifier.detectScheme("6229251234567890")
        )
    }

    // ---------- Luhn ----------

    @Test
    fun `Luhn接受合法卡号`() {
        // ISO 官方样例
        assertTrue(CardNumberIdentifier.luhnCheck("79927398713"))
        // Visa 测试卡
        assertTrue(CardNumberIdentifier.luhnCheck("4111111111111111"))
        // Mastercard 测试卡
        assertTrue(CardNumberIdentifier.luhnCheck("5555555555554444"))
        // Amex 测试卡（15 位）
        assertTrue(CardNumberIdentifier.luhnCheck("378282246310005"))
        // Discover 测试卡
        assertTrue(CardNumberIdentifier.luhnCheck("6011111111111117"))
        // JCB 测试卡
        assertTrue(CardNumberIdentifier.luhnCheck("3566002020360505"))
    }

    @Test
    fun `Luhn拒绝末位错误的卡号`() {
        assertFalse(CardNumberIdentifier.luhnCheck("4111111111111112"))
        assertFalse(CardNumberIdentifier.luhnCheck("79927398714"))
    }

    @Test
    fun `Luhn适用于19位银联卡`() {
        // 19 位借记卡形态：合法 Luhn 校验位（用真实算法算出，不是手编）
        assertTrue(CardNumberIdentifier.luhnCheck("6217007777777777772"))
        // 末位改错一位即不合法
        assertFalse(CardNumberIdentifier.luhnCheck("6217007777777777773"))
        // 18 位也适用
        assertTrue(CardNumberIdentifier.luhnCheck("622848777777777772"))
        // 16 位贷记卡
        assertTrue(CardNumberIdentifier.luhnCheck("6225887777777778"))
    }

    // ---------- 清洗 ----------

    @Test
    fun `清洗空格与连字符`() {
        assertEquals(
            CardNumberIdentifier.Scheme.VISA,
            CardNumberIdentifier.detectScheme("4111 1111 1111 1111")
        )
        assertEquals(
            CardNumberIdentifier.Scheme.VISA,
            CardNumberIdentifier.detectScheme("4111-1111-1111-1111")
        )
        // 全角数字
        assertEquals(
            CardNumberIdentifier.Scheme.VISA,
            CardNumberIdentifier.detectScheme("４１１１１１１１１１１１１１１")
        )
    }

    // ---------- 长度校验 ----------

    @Test
    fun `各组织长度校验`() {
        assertTrue(CardNumberIdentifier.isValidLength("4111111111111111"))   // Visa 16
        assertTrue(CardNumberIdentifier.isValidLength("378282246310005"))    // Amex 15
        assertTrue(CardNumberIdentifier.isValidLength("5555555555554444"))   // MC 16
        assertTrue(CardNumberIdentifier.isValidLength("6217001234567890"))   // 银联 16
        assertTrue(CardNumberIdentifier.isValidLength("6217003810026589904")) // 银联 19

        assertFalse(CardNumberIdentifier.isValidLength("411111"))            // 太短
        assertFalse(CardNumberIdentifier.isValidLength("37828224631000"))    // Amex 不是 15
    }

    // ---------- 校验入口 ----------

    @Test
    fun `validate对合法卡号返回null`() {
        assertNull(CardNumberIdentifier.validate("4111111111111111"))
    }

    @Test
    fun `validate对错误卡号给出中文说明`() {
        assertEquals("卡号只能包含数字", CardNumberIdentifier.validate("4111abc"))
        assertEquals("卡号只能包含数字", CardNumberIdentifier.validate("4111 1111 1111 111x"))
        assertEquals("校验位不正确，请核对卡号", CardNumberIdentifier.validate("4111111111111112"))
        // 空串合法——卡号非必填
        assertNull(CardNumberIdentifier.validate(""))
        assertNull(CardNumberIdentifier.validate("   "))
    }

    @Test
    fun `validate对长度不足给出提示`() {
        assertEquals("卡号至少需要 6 位才能识别发卡行", CardNumberIdentifier.validate("4111"))
        // 带空格的短号同样先报长度
        assertEquals("卡号至少需要 6 位才能识别发卡行", CardNumberIdentifier.validate("41 11"))
    }

    // ---------- 脱敏 ----------

    @Test
    fun `脱敏只显示末4位`() {
        assertEquals(
            "•••• •••• •••• 6688",
            CardNumberIdentifier.masked("6217851288466688")
        )
    }

    @Test
    fun `last4提取末4位`() {
        assertEquals("6688", CardNumberIdentifier.last4("6217851288466688"))
        // 不足 4 位返回 null
        assertNull(CardNumberIdentifier.last4("123"))
    }
}
