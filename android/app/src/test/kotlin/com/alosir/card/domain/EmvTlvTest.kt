package com.alosir.card.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EmvTlvTest {

    @Test
    fun `bcd to digits strips trailing F padding`() {
        assertEquals("622588013668", bcdToDigits(hexToBytes("622588013668")))
        assertEquals("62258801366", bcdToDigits(hexToBytes("62258801366F")))
    }

    @Test
    fun `expiry yyMMdd to YYYY-MM`() {
        assertEquals("2029-09", expiryToYm(hexToBytes("290901")))
    }

    @Test
    fun `apdu data strips SW and rejects non-9000`() {
        assertEquals(2, apduData(hexToBytes("12349000")).size)
        var threw = false
        try { apduData(hexToBytes("12346A82")) } catch (e: IllegalArgumentException) { threw = true }
        assertEquals(true, threw)
    }

    @Test
    fun `scheme of aid`() {
        assertEquals("union", schemeOfAid("A000000333010101"))
        assertEquals("visa", schemeOfAid("A0000000031010"))
        assertEquals("mastercard", schemeOfAid("A0000000041010"))
        assertEquals("unknown", schemeOfAid("A0000000000000"))
    }
}
