package com.alosir.card.data.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.alosir.card.domain.apduData
import com.alosir.card.domain.bcdToDigits
import com.alosir.card.domain.expiryToYm
import com.alosir.card.domain.hexToBytes
import com.alosir.card.domain.schemeOfAid

/**
 * NFC 读取银行卡公开信息（EMV：卡号 PAN + 有效期 + 卡组织）。
 * 全程本机完成，不联网；读不到时由调用方回退到 OCR/手输。
 */
object EmvCardReader {

    data class Result(
        val cardNumber: String,
        val expiryYm: String,
        val scheme: String,
    )

    fun isNfcAvailable(context: Context): Boolean =
        NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    /** 前台监听：读到即回调一次并停止监听。 */
    fun startListening(
        activity: Activity,
        onResult: (Result) -> Unit,
        onError: (String) -> Unit = {},
    ) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        adapter.enableReaderMode(
            activity,
            object : NfcAdapter.ReaderCallback {
                override fun onTagDiscovered(tag: Tag) {
                    val result = runCatching { read(tag) }.getOrElse { e ->
                        onError(e.message ?: "读取失败")
                        null
                    }
                    if (result != null) {
                        onResult(result)
                        adapter.disableReaderMode(activity)
                    }
                }
            },
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null,
        )
    }

    fun stopListening(activity: Activity) {
        NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
    }

    private val SELECT_PPSE = hexToBytes("00A404000E325041592E5359532E444446303100")
    private val GET_PAN = hexToBytes("80CA005A00")
    private val GET_EXPIRY = hexToBytes("80CA5F2400")

    private fun transact(isoDep: IsoDep, command: ByteArray): ByteArray =
        apduData(isoDep.transceive(command))

    private fun read(tag: Tag): Result {
        val isoDep = IsoDep.get(tag) ?: error("该卡不支持 NFC 读取")
        isoDep.connect()
        try {
            transact(isoDep, SELECT_PPSE)
            // 卡组织 AID：银联 QuickPass / Visa / Mastercard 各尝试一次
            val aidCandidates = listOf("A000000333010101", "A0000000031010", "A0000000041010")
            var scheme = "unknown"
            for (aidHex in aidCandidates) {
                val aid = hexToBytes(aidHex)
                val cmd = ByteArray(5 + aid.size)
                cmd[0] = 0x00.toByte()
                cmd[1] = 0xA4.toByte()
                cmd[2] = 0x04.toByte()
                cmd[3] = 0x00.toByte()
                cmd[4] = aid.size.toByte()
                aid.copyInto(cmd, 5)
                runCatching { transact(isoDep, cmd) }.onSuccess {
                    scheme = schemeOfAid(aidHex)
                }
                if (scheme != "unknown") break
            }
            val pan = bcdToDigits(transact(isoDep, GET_PAN))
            require(pan.length in 13..19) { "未读到有效卡号" }
            val ym = expiryToYm(transact(isoDep, GET_EXPIRY))
            return Result(pan, ym, scheme)
        } finally {
            runCatching { isoDep.close() }
        }
    }
}
