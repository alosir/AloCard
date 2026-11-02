package com.alosir.card.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * AloCard 扩展色板 · v2「鎏金卡夹」。
 *
 * Material 3 的 colorScheme 装不下全部语义色，因此通过 CompositionLocal 暴露。
 * Material 角色负责结构与通用控件；扩展色负责本产品特有的语义：
 * 黑金品牌渐变、曜石卡面、银行身份芯片与四种业务状态。
 */
@Immutable
data class AloCardColors(
    // 纸面层级
    val paper: Color,
    val paperRaised: Color,
    val paperSunken: Color,
    val paperTab: Color,
    // 银行身份色板
    val tabIndigo: Color,
    val tabTeal: Color,
    val tabMustard: Color,
    val tabPersimmon: Color,
    val tabPlum: Color,
    // 状态：已逾期
    val overdue: Color,
    val overdueBg: Color,
    // 状态：今天到期 / 部分还款
    val due: Color,
    val dueBg: Color,
    // 状态：待录入 / 待还款
    val pending: Color,
    val pendingBg: Color,
    // 状态：已结清
    val done: Color,
    val doneBg: Color,
    // 额度
    val meterTrack: Color,
    val meterFill: Color,
    // 黑金品牌
    val gold: Color,
    val goldHi: Color,
    val goldDeep: Color,
    val goldText: Color,
    val goldSoft: Color,
    val onGold: Color,
    // 曜石卡面
    val cardBg1: Color,
    val cardBg2: Color,
    val cardBg3: Color,
    val onCard: Color,
    val onCardDim: Color,
    /** 银行身份芯片上的字色（芯片为浅金渐变，两主题统一深字） */
    val onTab: Color,
    /** 格带已过段（深色下需比纸面更亮） */
    val countdownPast: Color,
)

private val LightAloCardColors = AloCardColors(
    paper = PaperLight,
    paperRaised = PaperRaisedLight,
    paperSunken = PaperSunkenLight,
    paperTab = PaperTabLight,
    tabIndigo = TabIndigoLight,
    tabTeal = TabTealLight,
    tabMustard = TabMustardLight,
    tabPersimmon = TabPersimmonLight,
    tabPlum = TabPlumLight,
    overdue = StateOverdueLight,
    overdueBg = StateOverdueBgLight,
    due = StateDueLight,
    dueBg = StateDueBgLight,
    pending = StatePendingLight,
    pendingBg = StatePendingBgLight,
    done = StateDoneLight,
    doneBg = StateDoneBgLight,
    meterTrack = MeterTrackLight,
    meterFill = MeterFillLight,
    gold = GoldLight,
    goldHi = GoldHi,
    goldDeep = GoldDeep,
    goldText = GoldTextLight,
    goldSoft = GoldSoftLight,
    onGold = OnGold,
    cardBg1 = CardBg1,
    cardBg2 = CardBg2,
    cardBg3 = CardBg3,
    onCard = OnCard,
    onCardDim = OnCardDim,
    onTab = OnGold,
    countdownPast = RuleStrongLight,
)

private val DarkAloCardColors = AloCardColors(
    paper = PaperDark,
    paperRaised = PaperRaisedDark,
    paperSunken = PaperSunkenDark,
    paperTab = PaperTabDark,
    tabIndigo = TabIndigoDark,
    tabTeal = TabTealDark,
    tabMustard = TabMustardDark,
    tabPersimmon = TabPersimmonDark,
    tabPlum = TabPlumDark,
    overdue = StateOverdueDark,
    overdueBg = StateOverdueBgDark,
    due = StateDueDark,
    dueBg = StateDueBgDark,
    pending = StatePendingDark,
    pendingBg = StatePendingBgDark,
    done = StateDoneDark,
    doneBg = StateDoneBgDark,
    meterTrack = MeterTrackDark,
    meterFill = MeterFillDark,
    gold = GoldDark,
    goldHi = GoldHi,
    goldDeep = GoldDeep,
    goldText = GoldTextDark,
    goldSoft = GoldSoftDark,
    onGold = OnGold,
    cardBg1 = CardBg1,
    cardBg2 = CardBg2,
    cardBg3 = CardBg3,
    onCard = OnCard,
    onCardDim = OnCardDim,
    onTab = OnGold,
    countdownPast = Color(0xFF4D545C),
)

private val LightScheme = lightColorScheme(
    primary = GoldLight,
    onPrimary = Color.White,
    secondary = GoldDeep,
    background = PaperLight,
    onBackground = InkLight,
    surface = PaperLight,
    onSurface = InkLight,
    surfaceVariant = PaperSunkenLight,
    onSurfaceVariant = Ink2Light,
    surfaceContainerLowest = PaperRaisedLight,
    surfaceContainerLow = PaperRaisedLight,
    surfaceContainer = PaperTabLight,
    surfaceContainerHigh = PaperTabLight,
    surfaceContainerHighest = PaperSunkenLight,
    outline = RuleStrongLight,
    outlineVariant = RuleLight,
    error = StateOverdueLight,
    onError = Color.White,
    errorContainer = StateOverdueBgLight,
    onErrorContainer = StateOverdueLight,
)

private val DarkScheme = darkColorScheme(
    primary = GoldDark,
    onPrimary = OnGold,
    secondary = GoldHi,
    background = PaperDark,
    onBackground = InkDark,
    surface = PaperDark,
    onSurface = InkDark,
    surfaceVariant = PaperSunkenDark,
    onSurfaceVariant = Ink2Dark,
    surfaceContainerLowest = PaperDark,
    surfaceContainerLow = PaperRaisedDark,
    surfaceContainer = PaperRaisedDark,
    surfaceContainerHigh = PaperTabDark,
    surfaceContainerHighest = PaperSunkenDark,
    outline = RuleStrongDark,
    outlineVariant = RuleDark,
    error = StateOverdueDark,
    onError = OnGold,
    errorContainer = StateOverdueBgDark,
    onErrorContainer = StateOverdueDark,
)

val LocalAloCardColors = staticCompositionLocalOf { LightAloCardColors }
val LocalAloCardSpacing = staticCompositionLocalOf { AloCardSpacing() }

/**
 * AloCard 主题。
 *
 * 浅色为默认（白天模式）；深色主题是独立配色方案，不是程序反色。
 * 不使用 Material You 动态取色——那会让黑金品牌色与状态色不可控。
 */
@Composable
fun AloCardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAloCardColors else LightAloCardColors
    val scheme = if (darkTheme) DarkScheme else LightScheme

    CompositionLocalProvider(
        LocalAloCardColors provides colors,
        LocalAloCardSpacing provides AloCardSpacing(),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AloCardTypography,
            shapes = AloCardShapes,
            content = content,
        )
    }
}

/** 便捷访问：`AloCardTheme.colors` 风格的对象式取值 */
object AloCardTheme {
    val colors: AloCardColors
        @Composable get() = LocalAloCardColors.current
    val spacing: AloCardSpacing
        @Composable get() = LocalAloCardSpacing.current
}
