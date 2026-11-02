package com.alocard.app.data.bin

import android.content.Context
import com.alocard.app.domain.repository.BinDirectory
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 离线号段查表（技术规范 §1.9 数据源）。
 *
 * 数据文件 `assets/bin-table.json` 由 `bin-data/build-bin-table.js` 生成，
 * 结构：bins 映射「6/8 位前缀 → [[银行代码, 卡类型, 卡号长度], ...]」，
 * banks 映射「银行代码 → 中文名」。
 *
 * 查法：先精确匹配 8 位 IIN，再退回 6 位 BIN。识别不到返回 null——
 * 长尾小银行不在表内是预期行为，调用方提供「未知发卡行」降级与人工覆盖（PRD F1.1）。
 */
@Singleton
class BinDirectoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : BinDirectory {

    private val lock = Any()
    private var banks: Map<String, String> = emptyMap()
    private var bins6: Map<String, List<Triple<String, String?, Int?>>> = emptyMap()
    private var bins8: Map<String, List<Triple<String, String?, Int?>>> = emptyMap()
    private var snapshot: String? = null

    init {
        runCatching {
            val json = JSONObject(context.assets.open(ASSET).bufferedReader().use { it.readText() })
            snapshot = json.optJSONObject("meta")?.optString("snapshot")

            val banksJson = json.getJSONObject("banks")
            banks = buildMap {
                banksJson.keys().forEach { code -> put(code, banksJson.getString(code)) }
            }

            fun entryList(arr: org.json.JSONArray): List<Triple<String, String?, Int?>> {
                val out = ArrayList<Triple<String, String?, Int?>>(arr.length())
                for (i in 0 until arr.length()) {
                    val row = arr.getJSONArray(i)
                    out.add(
                        Triple(
                            row.getString(0),
                            if (row.isNull(1)) null else row.getString(1),
                            if (row.isNull(2)) null else row.getInt(2),
                        )
                    )
                }
                return out
            }

            val binsJson = json.getJSONObject("bins")
            val map6 = HashMap<String, List<Triple<String, String?, Int?>>>()
            val map8 = HashMap<String, List<Triple<String, String?, Int?>>>()
            binsJson.keys().forEach { prefix ->
                val list = entryList(binsJson.getJSONArray(prefix))
                if (prefix.length == 8) map8[prefix] = list else map6[prefix] = list
            }
            bins6 = map6
            bins8 = map8
        }.onFailure {
            // 表损坏或缺失：识别整体降级为「不可用」，不影响其他功能
            banks = emptyMap()
            bins6 = emptyMap()
            bins8 = emptyMap()
        }
    }

    override fun lookup(digits: String): BinDirectory.BankMatch? {
        if (digits.length < 6) return null
        synchronized(lock) {
            val hit = when {
                digits.length >= 8 -> bins8[digits.substring(0, 8)]
                else -> null
            } ?: bins6[digits.substring(0, 6)] ?: return null

            val first = hit.firstOrNull() ?: return null
            val bankName = banks[first.first] ?: return null
            return BinDirectory.BankMatch(
                bankName = bankName,
                cardType = first.second,
                length = first.third,
            )
        }
    }

    override fun snapshot(): String? = snapshot

    companion object {
        private const val ASSET = "bin-table.json"
    }
}
