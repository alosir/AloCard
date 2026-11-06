package com.alosir.card.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alosir.card.ui.common.goldBrush
import com.alosir.card.ui.theme.AloCardTheme
import java.time.LocalDate

/** 卡组织中文显示名（卡面/列表共用；union → 中国银联，不显示 UNION） */
fun schemeLabelFor(scheme: String): String = when (scheme) {
    "union" -> "中国银联"
    "visa" -> "Visa"
    "mastercard" -> "Mastercard"
    "amex" -> "美国运通"
    "jcb" -> "JCB"
    "diners" -> "Diners Club"
    else -> "未知"
}

/**
 * 曜石卡面 · v2「鎏金卡夹」签名组件。
 * 黑金渐变 + 香槟芯片 + 脱敏卡号；深浅主题下卡面一致。
 */
@Composable
fun CreditCardFace(
    bankName: String,
    orgText: String,
    tail: String,
    graceDays: Int?,
    dueText: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = AloCardTheme.colors
    Box(
        modifier = modifier
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(c.cardBg1, c.cardBg2, c.cardBg3)))
            .border(1.dp, c.onCard.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(20.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 银行小 logo（有素材时）；无素材直接文字，不占位
                    BankLogos.drawableFor(bankName)?.let { res ->
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(res),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(7.dp)),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            bankName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.onCard,
                        )
                        Text(
                            orgText.uppercase(),
                            fontSize = 9.sp,
                            letterSpacing = 2.sp,
                            color = c.onCardDim,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
                Box(
                    Modifier
                        .size(34.dp, 25.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(goldBrush()),
                )
            }
            Text(
                "•••• •••• •••• $tail",
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = c.onCard,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("免息期 ${graceDays ?: "—"} 天", fontSize = 10.sp, color = c.onCardDim)
                Text("还款日 $dueText", fontSize = 10.sp, color = c.onCardDim)
            }
        }
    }
}

/** 银行渐变身份芯片（账簿行左侧；身份标识，不是状态） */
@Composable
fun BankChip(color: Color, glyph: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp, 44.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.35f), color))),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            glyph,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = AloCardTheme.colors.onTab,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

/**
 * 银行账簿行 · v2.1。
 * 一行 = 一个银行账单组（同银行多卡合并出账）。
 * 状态徽标固定卡片右上角；还款信息单行「M/d 还款（N 天后）」；格带通栏。
 */
@Composable
fun BankLedgerRow(
    bankName: String,
    subLabel: String,
    outstandingCents: Long?,
    dueDays: Int?,
    dueText: String,
    dueSubText: String,
    dueState: DueState,
    billState: BillState,
    billDate: LocalDate,
    dueDate: LocalDate,
    today: LocalDate,
    tabColor: Color,
    tabGlyph: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AloCardTheme.colors
    val dateOnly = dueSubText.removeSuffix(" 还款")
    // 单行还款文案：日期 + 相对天数合并，不再两行重复
    val dueLine = when {
        dueState == DueState.OVERDUE -> "$dueText（$dateOnly 到期）"
        billState == BillState.SETTLED -> "本期已结清"
        dueDays == null -> "补充账期后可用"
        dueDays == 0 -> "今天还款（$dateOnly）"
        else -> "$dateOnly 还款（$dueDays 天后）"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.paperRaised)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BankLogo(bankName = bankName, glyph = tabGlyph, color = tabColor)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        bankName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        subLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 徽标位 = 卡片右上角（不再挤在行内偏左）
                Spacer(Modifier.width(8.dp))
                StatusChip(billState = billState, dueState = dueState)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // 大数字 = 本期应还金额（替代原免息天数）
                when {
                    billState == BillState.PENDING_ENTRY -> Text(
                        "待录入",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    outstandingCents == null -> Text(
                        "未出账",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> {
                        Text(
                            com.alosir.card.domain.util.Money.format(outstandingCents),
                            style = MaterialTheme.typography.displayLarge,
                            fontSize = 22.sp,
                            color = colors.goldText,
                        )
                        Text(
                            "本期应还",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    dueLine,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (dueState == DueState.NORMAL) FontWeight.Normal else FontWeight.Medium,
                    color = when (dueState) {
                        DueState.OVERDUE -> colors.overdue
                        DueState.DUE -> colors.due
                        DueState.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            CountdownStrip(
                    billDate = billDate,
                    dueDate = dueDate,
                    today = today,
                    isDueToday = dueState == DueState.DUE,
                    overdue = dueState == DueState.OVERDUE,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
        }
    }
}

/**
 * 通知权限引导条 · v2。
 * 非阻塞、可关闭、克制金色；修复旧版点击闪退（Intent 由调用方安全处理）。
 */
@Composable
fun NoticeBanner(
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AloCardTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.paperRaised)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colors.goldSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = colors.gold, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("开启还款提醒通知", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "到期前 3 天与逾期当天提醒你",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(goldBrush())
                .clickable(onClick = onEnable)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("开启", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = colors.onGold)
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Default.Close,
            contentDescription = "关闭",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(30.dp).clickable(onClick = onDismiss).padding(6.dp),
        )
    }
}
