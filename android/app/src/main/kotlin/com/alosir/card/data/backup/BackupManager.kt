package com.alosir.card.data.backup

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.alosir.card.data.local.AloCardDatabase
import com.alosir.card.data.local.entity.BillEntity
import com.alosir.card.data.local.entity.BillingGroupEntity
import com.alosir.card.data.local.entity.CardEntity
import com.alosir.card.data.security.CryptoManager
import com.alosir.card.data.security.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 加密备份导出/恢复（PRD F9、技术规范 §1.4.5）。
 *
 * 容器格式（二进制）：
 * ```
 * MAGIC(8B "ALOCARD1") ‖ version(1B) ‖ kdfId(1B) ‖ iterations(4B BE)
 * ‖ salt(16B) ‖ iv(12B) ‖ ciphertext ‖ tag(16B)
 * ```
 * 密钥 = PBKDF2-HMAC-SHA256(安全密码, salt, 600_000 次) → AES-256-GCM。
 * 备份可跨设备恢复（密钥来自用户记忆的密码，与设备 Keystore 无关——ADR-0013）。
 *
 * 载荷内卡号是**明文**（受容器密钥保护）：恢复时用当前设备 Keystore 密钥重新加密（PRD F9.2 规则 3）。
 * 恢复语义是全量替换；替换前自动快照数据库文件，失败可回滚。
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AloCardDatabase,
    private val crypto: CryptoManager,
    private val security: SecurityManager,
) {

    /** 导出加密备份。pin 必须已设置（备份密钥的口令） */
    suspend fun export(pin: String): ByteArray {
        require(security.hasPin()) { "设置安全密码后才能导出备份" }
        val payload = buildPayload()

        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(pin, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
        val ct = cipher.doFinal(payload.toString().toByteArray(Charsets.UTF_8))

        return ByteArray(MAGIC.size + 1 + 1 + 4 + salt.size + iv.size + ct.size).apply {
            var pos = 0
            System.arraycopy(MAGIC, 0, this, pos, MAGIC.size); pos += MAGIC.size
            this[pos++] = VERSION.toByte()
            this[pos++] = KDF_PBKDF2.toByte()
            this[pos++] = (ITERATIONS shr 24).toByte()
            this[pos++] = (ITERATIONS shr 16).toByte()
            this[pos++] = (ITERATIONS shr 8).toByte()
            this[pos++] = ITERATIONS.toByte()
            System.arraycopy(salt, 0, this, pos, salt.size); pos += salt.size
            System.arraycopy(iv, 0, this, pos, iv.size); pos += iv.size
            System.arraycopy(ct, 0, this, pos, ct.size)
        }
    }

    /** 恢复备份：校验 → 快照 → 全量替换 → 重加密卡号 → 恢复凭据 */
    suspend fun import(data: ByteArray, pin: String) {
        val parsed = parseContainer(data)
        val key = deriveKey(pin, parsed.salt, parsed.iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, parsed.iv))
        }
        val json = try {
            JSONObject(String(cipher.doFinal(parsed.ciphertext), Charsets.UTF_8))
        } catch (e: Exception) {
            throw IllegalArgumentException("备份文件损坏或密码错误")
        }

        snapshotDatabase()

        val now = System.currentTimeMillis()
        db.withTransaction {
            // 清空（外键级联顺序：先子表）
            db.billDao().deleteAll()
            db.cardDao().deleteAll()
            db.billingGroupDao().deleteAll()

            val groupsJson = json.getJSONArray("groups")
            val idMap = HashMap<Long, Long>(groupsJson.length())
            for (i in 0 until groupsJson.length()) {
                val g = groupsJson.getJSONObject(i)
                val oldId = g.getLong("id")
                val newId = db.billingGroupDao().insert(
                    BillingGroupEntity(
                        name = g.getString("name"),
                        billingDay = if (g.isNull("billingDay")) null else g.getInt("billingDay"),
                        dueOffsetDays = if (g.isNull("dueOffsetDays")) null else g.getInt("dueOffsetDays"),
                        creditLimitCents = if (g.isNull("creditLimitCents")) null else g.getLong("creditLimitCents"),
                        bankName = if (g.isNull("bankName")) null else g.getString("bankName"),
                        createdAt = now,
                        updatedAt = now,
                    )
                )
                idMap[oldId] = newId
            }

            val cardsJson = json.getJSONArray("cards")
            for (i in 0 until cardsJson.length()) {
                val c = cardsJson.getJSONObject(i)
                val plain = if (c.isNull("number")) null else c.getString("number")
                db.cardDao().insert(
                    CardEntity(
                        groupId = idMap.getValue(c.getLong("groupId")),
                        cardNumberCipher = crypto.encryptCardNumber(plain),
                        cardNumberLast4 = if (c.isNull("last4")) null else c.getString("last4"),
                        cardNumberFingerprint = plain?.takeIf { it.length >= 6 }
                            ?.let { crypto.fingerprint(it) },
                        expiryYm = if (c.isNull("expiryYm")) null else c.getString("expiryYm"),
                        cardScheme = c.optString("scheme", "unknown"),
                        cardLabel = if (c.isNull("label")) null else c.getString("label"),
                        bankName = if (c.isNull("bankName")) null else c.getString("bankName"),
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }

            val billsJson = json.getJSONArray("bills")
            for (i in 0 until billsJson.length()) {
                val b = billsJson.getJSONObject(i)
                db.billDao().insert(
                    BillEntity(
                        groupId = idMap.getValue(b.getLong("groupId")),
                        billingDate = b.getLong("billingDate"),
                        billAmountCents = if (b.isNull("amountCents")) null else b.getLong("amountCents"),
                        paidAmountCents = b.optLong("paidCents", 0L),
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }
        }

        // 恢复安全密码凭据（备份是完整替换语义）
        val cred = json.optJSONObject("credential")
        if (cred != null) {
            security.restoreCredentialState(
                SecurityManager.CredentialState(
                    pinSalt = if (cred.isNull("salt")) null else cred.getString("salt"),
                    pinHash = if (cred.isNull("hash")) null else cred.getString("hash"),
                    pinIterations = cred.optInt("iterations", 310_000),
                )
            )
        }
    }

    private suspend fun buildPayload(): JSONObject {
        val groups = db.billingGroupDao().getAllOnce()
        val cards = db.cardDao().getAllOnce()
        val bills = db.billDao().getAllOnce()

        return JSONObject().apply {
            put("version", VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("groups", JSONArray().apply {
                groups.forEach { g ->
                    put(JSONObject().apply {
                        put("id", g.id)
                        put("name", g.name)
                        put("billingDay", g.billingDay)
                        put("dueOffsetDays", g.dueOffsetDays)
                        put("creditLimitCents", g.creditLimitCents)
                        put("bankName", g.bankName)
                    })
                }
            })
            put("cards", JSONArray().apply {
                cards.forEach { c ->
                    put(JSONObject().apply {
                        put("groupId", c.groupId)
                        put("number", crypto.decryptCardNumber(c.cardNumberCipher))
                        put("last4", c.cardNumberLast4)
                        put("expiryYm", c.expiryYm)
                        put("scheme", c.cardScheme)
                        put("label", c.cardLabel)
                        put("bankName", c.bankName)
                    })
                }
            })
            put("bills", JSONArray().apply {
                bills.forEach { b ->
                    put(JSONObject().apply {
                        put("groupId", b.groupId)
                        put("billingDate", b.billingDate)
                        put("amountCents", b.billAmountCents)
                        put("paidCents", b.paidAmountCents)
                    })
                }
            })
            val cred = security.exportCredentialState()
            put("credential", JSONObject().apply {
                put("salt", cred.pinSalt)
                put("hash", cred.pinHash)
                put("iterations", cred.pinIterations)
            })
        }
    }

    private data class Container(
        val salt: ByteArray,
        val iv: ByteArray,
        val iterations: Int,
        val ciphertext: ByteArray,
    )

    private fun parseContainer(data: ByteArray): Container {
        require(data.size > MAGIC.size + 6 + SALT_LEN + IV_LEN + 16) { "备份文件不完整" }
        require(MAGIC.indices.all { data[it] == MAGIC[it] }) { "不是 AloCard 备份文件" }
        var pos = MAGIC.size
        val version = data[pos++].toInt()
        require(version == VERSION) { "不支持的备份版本：$version" }
        val kdf = data[pos++].toInt()
        require(kdf == KDF_PBKDF2) { "不支持的密钥派生方式" }
        val iterations = ((data[pos++].toInt() and 0xFF) shl 24) or
            ((data[pos++].toInt() and 0xFF) shl 16) or
            ((data[pos++].toInt() and 0xFF) shl 8) or
            (data[pos++].toInt() and 0xFF)
        val salt = data.copyOfRange(pos, pos + SALT_LEN); pos += SALT_LEN
        val iv = data.copyOfRange(pos, pos + IV_LEN); pos += IV_LEN
        return Container(salt, iv, iterations, data.copyOfRange(pos, data.size))
    }

    private fun deriveKey(pin: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    /** 恢复前快照：复制数据库文件到应用私有目录（PRD F9.2 规则 2 的本地实现） */
    private fun snapshotDatabase() {
        val dbFile = context.getDatabasePath(AloCardDatabase.NAME)
        if (!dbFile.exists()) return
        val snap = File(context.filesDir, SNAPSHOT_NAME)
        dbFile.copyTo(snap, overwrite = true)
    }

    companion object {
        private val MAGIC = "ALOCARD1".encodeToByteArray()
        private const val VERSION = 1
        private const val KDF_PBKDF2 = 1
        private const val ITERATIONS = 600_000
        private const val SALT_LEN = 16
        private const val IV_LEN = 12
        private const val SNAPSHOT_NAME = "pre_restore_snapshot.db"
    }
}
