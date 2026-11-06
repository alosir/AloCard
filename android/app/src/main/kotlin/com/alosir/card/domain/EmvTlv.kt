package com.alosir.card.domain

/** EMV/NFC 公开数据解析（不依赖 Android，可 JVM 测试）。 */

/** BCD 字节数组 → 数字字符串（去掉尾部填充 F） */
fun bcdToDigits(bytes: ByteArray): String {
    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
        sb.append(String.format("%02X", b))
    }
    return sb.toString().trimEnd('F', 'f')
}

/** EMV 应用失效日期 5F24（BCD YYMMDD，3 字节）→ YYYY-MM */
fun expiryToYm(bytes: ByteArray): String {
    require(bytes.size >= 2) { "有效期数据过短" }
    val yy = String.format("%02X", bytes[0])
    val mm = String.format("%02X", bytes[1])
    return "20$yy-$mm"
}

/** AID → 卡组织 */
fun schemeOfAid(aidHex: String): String = when {
    aidHex.equals("A000000333010101", ignoreCase = true) -> "union"
    aidHex.equals("A0000000031010", ignoreCase = true) -> "visa"
    aidHex.equals("A0000000041010", ignoreCase = true) -> "mastercard"
    else -> "unknown"
}

/** 十六进制字符串 → 字节数组 */
fun hexToBytes(hex: String): ByteArray {
    val clean = hex.replace(" ", "")
    require(clean.length % 2 == 0) { "非法十六进制" }
    return ByteArray(clean.length / 2) { i ->
        clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

/** APDU 响应：去掉 SW1SW2；非 9000 抛错 */
fun apduData(response: ByteArray): ByteArray {
    require(response.size >= 2) { "APDU 响应过短" }
    val sw = ((response[response.size - 2].toInt() and 0xFF) shl 8) or
        (response[response.size - 1].toInt() and 0xFF)
    require(sw == 0x9000) { "卡片返回状态 ${sw.toString(16)}" }
    return response.copyOfRange(0, response.size - 2)
}
