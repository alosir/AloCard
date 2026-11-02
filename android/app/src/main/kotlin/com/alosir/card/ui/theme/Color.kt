package com.alosir.card.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * AloCard 色彩令牌 · v2「鎏金卡夹」。
 *
 * 浅色为默认（白天模式）：瓷白地面 + 白卡片 + 黑金品牌色。
 * 曜石黑金仅用于卡面与品牌点缀；深色主题是独立配色，不是程序反色。
 * 数值与 `prototype/v2/index.html`（变体 A）一一对应。
 */

// ---------- 纸面层级（浅色 = 瓷白） ----------

val PaperLight = Color(0xFFF5F3EE)   // 主地面
val PaperDark = Color(0xFF0B0B0E)

val PaperRaisedLight = Color(0xFFFFFFFF)  // 卡片、导航栏、弹层
val PaperRaisedDark = Color(0xFF16161B)

val PaperSunkenLight = Color(0xFFEFEDE6)  // 输入框底、分段控件底
val PaperSunkenDark = Color(0xFF1D1D23)

val PaperTabLight = Color(0xFFFAF9F5)     // 中间面：tonal 按钮
val PaperTabDark = Color(0xFF24242B)

// ---------- 墨色 ----------

val InkLight = Color(0xFF1C1A16)
val InkDark = Color(0xFFF4F1E9)

val Ink2Light = Color(0xFF57544C)
val Ink2Dark = Color(0xFFA8A6AD)

val Ink3Light = Color(0xFF8B877C)
val Ink3Dark = Color(0xFF6F6E76)

// ---------- 线条 ----------

val RuleLight = Color(0xFFE6E2D8)   // ≈ rgba(28,24,16,.09) 于瓷白上
val RuleDark = Color(0xFF26262C)

val RuleStrongLight = Color(0xFFC9C4B8)
val RuleStrongDark = Color(0xFF3A3A42)

// ---------- 黑金品牌色 ----------

/** 图标/通用金 */
val GoldLight = Color(0xFF9A7B24)
val GoldDark = Color(0xFFE0B84F)

/** 香槟高光：渐变亮端、深色下的金字 */
val GoldHi = Color(0xFFF0DFA0)

/** 深金：渐变暗端 */
val GoldDeep = Color(0xFF7D621A)

/** 文字用金（浅底上必须加深保证对比） */
val GoldTextLight = Color(0xFF8A6A14)
val GoldTextDark = Color(0xFFF6E6AD)

val GoldSoftLight = Color(0x1A9A7B24)  // rgba(154,123,36,.10)
val GoldSoftDark = Color(0x1FE0B84F)

/** 渐变按钮/金面上的深字 */
val OnGold = Color(0xFF171205)

// ---------- 曜石卡面（深浅主题下卡面一致） ----------

val CardBg1 = Color(0xFF26262C)
val CardBg2 = Color(0xFF131317)
val CardBg3 = Color(0xFF1C160B)
val OnCard = Color(0xFFF2ECDC)
val OnCardDim = Color(0xFFB9AE93)

// ---------- 银行身份色板（渐变芯片起止色；身份标识，不是状态） ----------

val TabIndigoLight = Color(0xFF5F7FAE)   // 建设蓝
val TabIndigoDark = Color(0xFF9DB8E0)

val TabTealLight = Color(0xFF639584)     // 青
val TabTealDark = Color(0xFFB3D1C4)

val TabMustardLight = Color(0xFFD9B558)  // 招商金
val TabMustardDark = Color(0xFFF6E6AD)

val TabPersimmonLight = Color(0xFFB06A4A)
val TabPersimmonDark = Color(0xFFE0A98E)

val TabPlumLight = Color(0xFF8288CF)     // Visa 紫
val TabPlumDark = Color(0xFFC7CDF5)

// ---------- 状态色（只用于状态） ----------

val StateOverdueLight = Color(0xFFC2503F)
val StateOverdueBgLight = Color(0x1AC2503F)
val StateOverdueDark = Color(0xFFF0826C)
val StateOverdueBgDark = Color(0x1FF0826C)

val StateDueLight = Color(0xFF8A6A14)
val StateDueBgLight = GoldSoftLight
val StateDueDark = Color(0xFFF6E6AD)
val StateDueBgDark = GoldSoftDark

val StatePendingLight = Color(0xFF6E6A5E)
val StatePendingBgLight = Color(0x1F6E6A5E)
val StatePendingDark = Color(0xFFA8A6AD)
val StatePendingBgDark = Color(0x24A8A6AD)

val StateDoneLight = Color(0xFF2C7A5F)
val StateDoneBgLight = Color(0x1F2C7A5F)
val StateDoneDark = Color(0xFF7CC7AE)
val StateDoneBgDark = Color(0x1F7CC7AE)

// ---------- 额度 ----------

val MeterTrackLight = Color(0xFFEFEDE6)
val MeterTrackDark = Color(0xFF1D1D23)
val MeterFillLight = Color(0xFF9A7B24)
val MeterFillDark = Color(0xFFE0B84F)
