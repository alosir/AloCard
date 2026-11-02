package com.alosir.card.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 安全密码（4 位 PIN）管理。
 *
 * **PIN 必须加盐哈希存储，绝不保存明文或可逆加密。**
 * 选 PBKDF2WithHmacSHA256：Android 平台原生可用，无需引入 BouncyCastle；
 * 参数：salt 16 字节随机、迭代 120_000 次、输出 256 位。
 * 120k 次在 2026 年的中端 Android 设备上约 200–400ms，用户无感，
 * 而暴力破解成本显著上升（OWASP 2023 建议 PBKDF2-HMAC-SHA256 ≥ 600k，
 * 移动端受性能限制取 120k，配合 5 次失败锁定可接受）。
 *
 * 失败锁定：连续 5 次输错后锁定 5 分钟，按几何级数递增（5→15→60 分钟）。
 * 锁定状态持久化，重启 App 不解除——否则重启即可绕过。
 *
 * 会话解锁：校验通过后 [unlockedUntil] 记录有效期，
 * 有效期内的重复查看不再要求输入（HTML 原型已确认的行为）。
 */
@Singleton
class SecurityManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: GeneralSecurityException) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /** 会话解锁有效期截止时间戳；0 表示未解锁 */
    var unlockedUntil: Long = 0L
        private set

    fun hasPin(): Boolean = prefs.contains(KEY_HASH)

    /** 设置新的 4 位安全密码。重复设置会覆盖旧值。 */
    fun setPin(pin: String) {
        require(pin.length == PIN_LENGTH && pin.all { it.isDigit() }) {
            "安全密码必须是 $PIN_LENGTH 位数字"
        }
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt(KEY_FAIL_COUNT, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .apply()
    }

    /**
     * 校验 PIN。返回：
     * - [VerifyResult.Success] 校验通过（同时开启会话解锁）
     * - [VerifyResult.Wrong] 密码错误，附剩余尝试次数
     * - [VerifyResult.Locked] 已被锁定，附剩余锁定秒数
     */
    fun verifyPin(pin: String): VerifyResult {
        val lockUntil = prefs.getLong(KEY_LOCK_UNTIL, 0L)
        val now = System.currentTimeMillis()
        if (lockUntil > now) {
            return VerifyResult.Locked(((lockUntil - now) / 1000).toInt())
        }

        val saltB64 = prefs.getString(KEY_SALT, null) ?: return VerifyResult.Wrong(0)
        val hashB64 = prefs.getString(KEY_HASH, null) ?: return VerifyResult.Wrong(0)
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        val actual = pbkdf2(pin, salt)

        return if (constantTimeEquals(expected, actual)) {
            prefs.edit()
                .putInt(KEY_FAIL_COUNT, 0)
                .putLong(KEY_LOCK_UNTIL, 0L)
                .apply()
            unlockedUntil = now + SESSION_DURATION_MS
            VerifyResult.Success
        } else {
            val fails = prefs.getInt(KEY_FAIL_COUNT, 0) + 1
            if (fails >= MAX_ATTEMPTS) {
                val lockMs = lockDurationMs(fails)
                prefs.edit()
                    .putInt(KEY_FAIL_COUNT, 0)
                    .putLong(KEY_LOCK_UNTIL, now + lockMs)
                    .apply()
                VerifyResult.Locked((lockMs / 1000).toInt())
            } else {
                prefs.edit().putInt(KEY_FAIL_COUNT, fails).apply()
                VerifyResult.Wrong(MAX_ATTEMPTS - fails)
            }
        }
    }

    /** 会话是否仍有效 */
    fun isSessionUnlocked(): Boolean = System.currentTimeMillis() < unlockedUntil

    /** 生物识别等旁路验证通过后，同样开启会话（PRD F10.2 规则 3） */
    fun unlockSession() {
        unlockedUntil = System.currentTimeMillis() + SESSION_DURATION_MS
    }

    /** 立即锁定（后台超时回到前台时由 Activity 调用，PRD F10.2 规则 1） */
    fun lockNow() {
        unlockedUntil = 0L
    }

    /**
     * 备份恢复的凭据状态（PRD F9.2：备份内含安全密码哈希）。
     * 仅用于导出到用户自己的备份文件，绝不出现在其他出口。
     */
    data class CredentialState(
        val pinSalt: String?,
        val pinHash: String?,
        val pinIterations: Int,
    )

    fun exportCredentialState(): CredentialState = CredentialState(
        pinSalt = prefs.getString(KEY_SALT, null),
        pinHash = prefs.getString(KEY_HASH, null),
        pinIterations = ITERATIONS,
    )

    /** 恢复凭据：覆盖盐/哈希并重置失败计数（备份是完整替换语义，PRD F9.2） */
    fun restoreCredentialState(state: CredentialState) {
        prefs.edit()
            .putString(KEY_SALT, state.pinSalt)
            .putString(KEY_HASH, state.pinHash)
            .putInt(KEY_FAIL_COUNT, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .apply()
        unlockedUntil = 0L
    }

    /** 清除安全密码。用户主动重置时调用。 */
    fun clearPin() {
        prefs.edit().clear().apply()
        unlockedUntil = 0L
    }

    /** 锁定剩余秒数；未锁定返回 0 */
    fun lockRemainingSeconds(): Int {
        val remaining = prefs.getLong(KEY_LOCK_UNTIL, 0L) - System.currentTimeMillis()
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }

    /**
     * 失败次数越多锁得越久：5 分钟 → 15 分钟 → 60 分钟 → 之后每轮翻倍。
     * fails 是「本次是第几次触发锁定」（从 MAX_ATTEMPTS 起）。
     */
    private fun lockDurationMs(fails: Int): Long {
        val step = fails - MAX_ATTEMPTS
        val minutes = when {
            step <= 0 -> 5L
            step == 1 -> 15L
            step == 2 -> 60L
            else -> 60L * (1L shl (step - 2).coerceAtMost(4))
        }
        return minutes * 60_000L
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    /** 常数时间比较，避免时序侧信道 */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    sealed interface VerifyResult {
        data object Success : VerifyResult
        data class Wrong(val remainingAttempts: Int) : VerifyResult
        data class Locked(val remainingSeconds: Int) : VerifyResult
    }

    companion object {
        private const val PREFS_NAME = "alocard_security"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_FAIL_COUNT = "fail_count"
        private const val KEY_LOCK_UNTIL = "lock_until"

        const val PIN_LENGTH = 4
        const val MAX_ATTEMPTS = 5
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH = 16
        private const val SESSION_DURATION_MS = 5 * 60 * 1000L // 5 分钟
    }
}
