package com.alosir.card.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankLogoNameResolverTest {
    private val banks = setOf("中国银行", "工商银行", "农业银行", "建设银行")

    @Test
    fun `中国银行优先按原名匹配`() {
        assertEquals("中国银行", BankLogoNameResolver.resolve("中国银行", banks))
    }

    @Test
    fun `带中国前缀的全称可回退到短名`() {
        assertEquals("工商银行", BankLogoNameResolver.resolve("中国工商银行", banks))
        assertEquals("建设银行", BankLogoNameResolver.resolve("中国建设银行", banks))
    }

    @Test
    fun `未知名称返回空`() {
        assertNull(BankLogoNameResolver.resolve("未知银行", banks))
        assertNull(BankLogoNameResolver.resolve(null, banks))
    }
}
