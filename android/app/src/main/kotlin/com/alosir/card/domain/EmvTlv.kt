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

/** 在 EMV 数据里按 tag 查找 value（支持 1/2 字节 tag 与多字节长度） */
fun tlvFind(data: ByteArray, tag: Int): ByteArray? {
    var i = 0
    while (i + 2 <= data.size) {
        var t = data[i].toInt() and 0xFF
        if (t == 0xFF) break
        var tagLen = 1
        if (t and 0x1F == 0x1F) {
            if (i + 2 > data.size) break
            t = (t shl 8) or (data[i + 1].toInt() and 0xFF)
            tagLen = 2
        }
        i += tagLen
        if (i >= data.size) break
        var len = data[i].toInt() and 0xFF
        i++
        if (len and 0x80 != 0) {
            val n = len and 0x7F
            if (n == 0) break
            len = 0
            repeat(n) {
                if (i >= data.size) return null
                len = (len shl 8) or (data[i].toInt() and 0xFF)
                i++
            }
        }
        if (i + len > data.size) return null
        if (t == tag) return data.copyOfRange(i, i + len)
        i += len
    }
    return null
}

/** GPO 响应里的 AFL（tag 94）→ (SFI, firstRec, lastRec) 列表 */
fun parseAfl(data: ByteArray): List<Triple<Int, Int, Int>> {
    val afl = tlvFind(data, 0x94) ?: return emptyList()
    val out = mutableListOf<Triple<Int, Int, Int>>()
    var i = 0
    while (i + 4 <= afl.size) {
        val sfi = afl[i].toInt() and 0xFF
        val first = afl[i + 1].toInt() and 0xFF
        val last = afl[i + 2].toInt() and 0xFF
        out.add(Triple(sfi, first, last))
        i += 4
    }
    return out
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
