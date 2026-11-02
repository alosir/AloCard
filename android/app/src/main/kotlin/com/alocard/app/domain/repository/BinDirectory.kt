package com.alocard.app.domain.repository

/**
 * 号段查表（离线 BIN 识别）。
 *
 * 数据源是内置 `assets/bin-table.json`（快照时间见 meta.snapshot）。
 * 识别是尽力而为：长尾小银行可能识别不到，调用方必须允许用户覆盖（PRD F1.1）。
 */
interface BinDirectory {

    /** 识别结果 */
    data class BankMatch(
        /** 发卡行名称（中文） */
        val bankName: String,
        /** 卡类型，如 CC（信用卡）；可能为空 */
        val cardType: String?,
        /** 期望卡号长度；可能为空 */
        val length: Int?,
    )

    /**
     * 按卡号前缀查发卡行。
     * 先精确匹配 8 位 IIN，再退回 6 位 BIN；未命中返回 null。
     */
    fun lookup(digits: String): BankMatch?

    /** 号段表快照时间（ISO 日期串），用于界面标注数据时点 */
    fun snapshot(): String?
}
