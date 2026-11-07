package com.alosir.card.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardTextParserTest {

    @Test
    fun `提取通过校验的卡号和有效期`() {
        val result = CardTextParser.parse("招商银行\n4111 1111 1111 1111\n12/30")
        assertEquals("4111111111111111", result.cardNumber)
        assertEquals("2030-12", result.expiryYm)
        assertEquals("招商银行", result.bankName)
    }

    @Test
    fun `纠正常见字母与数字混淆`() {
        val result = CardTextParser.parse("4lll llll llll llll\nO1/3O")
        assertEquals("4111111111111111", result.cardNumber)
        assertEquals("2030-01", result.expiryYm)
    }

    @Test
    fun `拒绝校验位错误的卡号`() {
        val result = CardTextParser.parse("4111 1111 1111 1112\n12/30")
        assertNull(result.cardNumber)
        assertEquals("2030-12", result.expiryYm)
    }

    @Test
    fun `拒绝多余数字且不截取错误子串`() {
        val result = CardTextParser.parse("41111111111111111111")
        assertNull(result.cardNumber)
    }

    @Test
    fun `多个数字候选优先选择有效卡号`() {
        val result = CardTextParser.parse("20261107\n4111111111111112\n5555555555554444")
        assertEquals("5555555555554444", result.cardNumber)
    }
}
