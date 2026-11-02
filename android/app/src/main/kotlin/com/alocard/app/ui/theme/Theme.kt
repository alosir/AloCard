package com.alocard.app.ui.theme

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
 * AloCard 扩展色板。
 *
 * Material 3 的 colorScheme 装不下本设计的全部语义色，因此通过
 * CompositionLocal 暴露给组件。**这些色不是 Material 角色的替代**——
 * Material 角色负责结构与通用控件，扩展色负责本产品特有的语义：
 * washi 索引标签（账单组身份）与四种业务状态。
 */
@Immutable
data class AloCardColors(
    // 纸面层级
    val paper: Color,
    val paperRaised: Color,
    val paperSunken: Color,
    val paperTab: Color,
    // 点阵
    val dot: Color,
    // washi 索引标签色板（账单组身份）
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
    /** 深色主题下索引标签的字色（标签底色提亮，故配深墨） */
    val onTab: Color,
    /** 深色主题下格带已过段的颜色（需比纸面更亮才分得开） */
    val countdownPast: Color,
)

private val LightAloCardColors = AloCardColors(
    paper = PaperLight,
    paperRaised = PaperRaisedLight,
    paperSunken = PaperSunkenLight,
    paperTab = PaperTabLight,
    dot = DotLight,
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
    onTab = Color(0xFFFFFFFF),
    countdownPast = RuleStrongLight,
)

private val DarkAloCardColors = AloCardColors(
    paper = PaperDark,
    paperRaised = PaperRaisedDark,
    paperSunken = PaperSunkenDark,
    paperTab = PaperTabDark,
    dot = DotDark,
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
    onTab = Color(0x0E1013),
    countdownPast = Color(0xFF4D545C),
)

private val LightScheme = lightColorScheme(
    primary = TabIndigoLight,
    onPrimary = Color.White,
    secondary = TabTealLight,
    background = PaperLight,
    onBackground = InkLight,
    surface = PaperLight,
    onSurface = InkLight,
    surfaceVariant = PaperSunkenLight,
    onSurfaceVariant = Ink2Light,
    surfaceContainerLowest = PaperRaisedLight,
    surfaceContainerLow = PaperRaisedLight,
    surfaceContainer = PaperLight,
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
    primary = TabIndigoDark,
    onPrimary = Color(0xFF0E1013),
    secondary = TabTealDark,
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
    onError = Color(0xFF0E1013),
    errorContainer = StateOverdueBgDark,
    onErrorContainer = StateOverdueDark,
)

val LocalAloCardColors = staticCompositionLocalOf { LightAloCardColors }
val LocalAloCardSpacing = staticCompositionLocalOf { AloCardSpacing() }

/**
 * AloCard 主题。
 *
 * 深色主题是独立配色方案（DESIGN.md 第 2.2 节），不是浅色的程序反色：
 * 纸面变深板岩、washi 色板提亮以保对比、状态色单独调校。
 * 因此这里手动指定 darkColorScheme，**不使用 dynamicColorScheme**——
 * Material You 动态取色会让 washi 色板与状态色不可控，与本设计的前提冲突。
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
