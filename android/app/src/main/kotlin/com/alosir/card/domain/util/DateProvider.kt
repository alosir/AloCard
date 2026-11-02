package com.alosir.card.domain.util

import java.time.LocalDate

/**
 * 「今天」的唯一来源。
 *
 * 界面与用例不直接调用 LocalDate.now()，统一走这里——
 * 需要固定日期的测试（或调试某个账单日场景）只需改这一处。
 */
object DateProvider {
    fun today(): LocalDate = LocalDate.now()
}
