package com.alocard.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * AloCard 色彩令牌。
 *
 * 数值严格来自 `DESIGN.md` 第 2 章，与 `prototype/tokens.css` 一一对应。
 * 深色主题是独立配色方案，不是浅色的程序反色。
 */

// ---------- 纸面层级 ----------

/** 纸面（主地面）。浅色为冷调中性灰白——刻意避开奶油米色。 */
val PaperLight = Color(0xFFF4F5F5)
val PaperDark = Color(0xFF16181C)

/** 抬起面：导航栏、弹层、输入框底 */
val PaperRaisedLight = Color(0xFFFFFFFF)
val PaperRaisedDark = Color(0xFF1D2025)

/** 下沉面：分段控件底、键帽 */
val PaperSunkenLight = Color(0xFFE9EBEB)
val PaperSunkenDark = Color(0xFF101215)

/** 中间面：悬停态、tonal 按钮 */
val PaperTabLight = Color(0xFFEFF0F0)
val PaperTabDark = Color(0xFF23262C)

// ---------- 墨色 ----------

val InkLight = Color(0xFF1C1B19)
val InkDark = Color(0xFFE8E9EC)

val Ink2Light = Color(0xFF4A4842)
val Ink2Dark = Color(0xFFB3B8C0)

/** 次要说明文字，需满足 WCAG AA 4.5:1 */
val Ink3Light = Color(0xFF5F5C55)
val Ink3Dark = Color(0xFF8D929B)

// ---------- 线条 ----------

val RuleLight = Color(0xFFDCDEDD)
val RuleDark = Color(0xFF2B2F35)

val RuleStrongLight = Color(0xFFC3C6C6)
val RuleStrongDark = Color(0xFF3D434B)

// ---------- 点阵网格纸 ----------

/** 点阵颜色。深色主题下转为浅墨点。 */
val DotLight = Color(0x131C1B19) // rgba(28,27,25,.075)
val DotDark = Color(0x16E2E5EA)  // rgba(226,229,234,.085)

// ---------- washi 索引标签色板 ----------
//
// 账单组颜色是身份标识，不是状态。状态色只用于状态。
// 浅色下标签字为白，深色下为深墨——因为深色主题的标签底色被提亮。

val TabIndigoLight = Color(0xFF3C5A80)
val TabIndigoDark = Color(0xFF7EA2CF)

val TabTealLight = Color(0xFF2C6A5F)
val TabTealDark = Color(0xFF63B3A1)

val TabMustardLight = Color(0xFF9A7520)
val TabMustardDark = Color(0xFFD2AC4E)

val TabPersimmonLight = Color(0xFFA84F34)
val TabPersimmonDark = Color(0xFFE08A6E)

val TabPlumLight = Color(0xFF6B5578)
val TabPlumDark = Color(0xFFA892C4)

// ---------- 状态色 ----------

val StateOverdueLight = Color(0xFFA33123)
val StateOverdueBgLight = Color(0xFFF7E6E2)
val StateOverdueDark = Color(0xFFEF8A76)
val StateOverdueBgDark = Color(0xFF3A2320)

val StateDueLight = Color(0xFF8A6414)
val StateDueBgLight = Color(0xFFF7EFDC)
val StateDueDark = Color(0xFFE0BB5E)
val StateDueBgDark = Color(0xFF382D15)

val StatePendingLight = Color(0xFF7A7466)
val StatePendingBgLight = Color(0xFFEEEBE3)
val StatePendingDark = Color(0xFF9AA0A8)
val StatePendingBgDark = Color(0xFF262A30)

val StateDoneLight = Color(0xFF2C6A5F)
val StateDoneBgLight = Color(0xFFE3EFEB)
val StateDoneDark = Color(0xFF63B3A1)
val StateDoneBgDark = Color(0xFF1C2F2B)

// ---------- 额度相关 ----------

val MeterTrackLight = Color(0xFFE4E1D9)
val MeterTrackDark = Color(0xFF2B2F35)
val MeterFillLight = Color(0xFF3C5A80)
val MeterFillDark = Color(0xFF7EA2CF)

// ---------- 工装背景（仅原型用，App 内不出现） ----------

val StageBgLight = Color(0xFFDCDCDA)
val StageBgDark = Color(0xFF0B0D10)
