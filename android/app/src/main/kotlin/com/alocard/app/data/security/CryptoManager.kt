package com.alocard.app.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 卡号加密与查重指纹（技术规范 §1.4.3）。
 *
 * - 加密：AES-256-GCM，密钥由 Android Keystore 持有（别名 alocard.card.aes.v1，不可导出）；
 *   每次加密新生成 12 字节 IV；密文封装为 `v1:<base64(iv)>:<base64(ct‖tag)>`。
 * - 查重：HMAC-SHA256（Keystore 密钥 alocard.card.hmac.v1）对规范化卡号取摘要，
 *   截断 16 字节 Base64 存为盲索引——密文不可查重，指纹不含卡号本身。
 *
 * 不用 SQLCipher 整库加密：会废掉额度汇总需要的 SQL SUM（PRD 9.5）——
 * 真正敏感的只有完整卡号，字段级加密恰好覆盖它（技术规范 §1.4.1）。
 */
@Singleton
class CryptoManager @Inject constructor() {

    // ---------- 卡号加密 ----------

    /** 加密卡号，返回封装字符串；输入 null/空白返回 null */
    fun encryptCardNumber(plain: String?): String? {
        if (plain.isNullOrBlank()) return null
        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, aesKey())
        }
        val iv = cipher.iv
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return "v1:" +
            Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(ct, Base64.NO_WRAP)
    }

    /** 解密卡号。格式非法、密文被篡改或密钥失效时返回 null */
    fun decryptCardNumber(sealed: String?): String? {
        if (sealed.isNullOrBlank()) return null
        return try {
            val parts = sealed.split(":")
            if (parts.size != 3 || parts[0] != "v1") return null
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val ct = Base64.decode(parts[2], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, aesKey(), GCMParameterSpec(128, iv))
            }
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    // ---------- 查重盲指纹 ----------

    /**
     * 规范化卡号的查重指纹（HMAC 截断 16 字节后 Base64）。
     * 同一卡号指纹恒定，不同卡号不可关联——不含卡号本身的任何信息。
     */
    fun fingerprint(normalizedDigits: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM).apply { init(hmacKey()) }
        val digest = mac.doFinal(normalizedDigits.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest.copyOfRange(0, 16), Base64.NO_WRAP)
    }

    // ---------- 密钥 ----------

    private fun aesKey(): SecretKey = getOrCreate(
        alias = AES_ALIAS,
        algorithm = KeyProperties.KEY_ALGORITHM_AES,
    ) {
        KeyGenParameterSpec.Builder(
            AES_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // 不设 setUserAuthenticationRequired——产品用 App 内 6 位安全密码做卡号闸门
            // （ADR-0010），不与系统锁屏绑定（技术规范 §1.4.3）
            .build()
    }

    private fun hmacKey(): SecretKey = getOrCreate(
        alias = HMAC_ALIAS,
        algorithm = KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
    ) {
        KeyGenParameterSpec.Builder(HMAC_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setKeySize(256)
            .build()
    }

    private fun getOrCreate(
        alias: String,
        algorithm: String,
        spec: () -> KeyGenParameterSpec,
    ): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(algorithm, KEYSTORE)
        generator.init(spec())
        return generator.generateKey()
    }

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val AES_ALIAS = "alocard.card.aes.v1"
        private const val HMAC_ALIAS = "alocard.card.hmac.v1"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }
}
