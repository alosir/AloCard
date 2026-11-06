package com.alosir.card.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alosir.card.ui.theme.AloCardTheme

/**
 * 金额浮层脚手架 · v0.0.3。
 * 顶部半透明遮罩（点击关闭）+ 底部圆角弹层 + APP 内数字键盘。
 * 录入账单金额与标记还款共用，避免系统输入法遮挡问题。
 */
@Composable
fun AmountSheetScaffold(
    title: String,
    onDismiss: () -> Unit,
    amountText: String,
    onAmountChange: (String) -> Unit,
    amountLabel: String,
    error: String? = null,
    infoLine: String? = null,
    fieldHint: String? = null,
    onFillAll: (() -> Unit)? = null,
    confirmText: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
    blockedReason: String? = null,
) {
    val colors = AloCardTheme.colors
    // 直接作为 ModalBottomSheet 的内容：官方容器负责遮罩与贴底，不再自绘遮罩
    Column(
        Modifier
            .fillMaxWidth()
            // 弹层高度不超过屏高 92%，内容超出时上部滚动、键盘固定沉底
            .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.92f).dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(colors.paperRaised)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            // 抓取条
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp).clickable { onDismiss() }.padding(4.dp),
                )
            }
            infoLine?.let {
                HintText(it, Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(14.dp))

            if (blockedReason != null) {
                HintText(blockedReason, color = MaterialTheme.colorScheme.error)
                return@Column
            }

            Text(amountLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            // 金额框：只读展示（输入走下方 APP 键盘）；右侧内嵌「全部」
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.paperSunken)
                    .border(
                        1.dp,
                        if (error != null) MaterialTheme.colorScheme.error else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    ),
            ) {
                Text(
                    amountText.ifEmpty { "0.00" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 14.dp),
                )
                if (onFillAll != null) {
                    Text(
                        "全部",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.goldText,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable { onFillAll() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
            HintText(
                error ?: fieldHint ?: "支持小数，最多 2 位小数",
                Modifier.padding(top = 6.dp),
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TonalButton("取消", onDismiss, Modifier.weight(1f))
                FilledButton(confirmText, onConfirm, Modifier.weight(2f), confirmEnabled)
            }
            }
            if (blockedReason == null) {
                Spacer(Modifier.height(12.dp))
                AmountKeypad(onKey = { key -> onAmountChange(amountInputFilter(amountText, key)) })
            }
    }
}

/** 金额输入过滤：单点小数点、最多 2 位小数、前导 0 替换 */
fun amountInputFilter(current: String, key: String): String {
    return when (key) {
    "del" -> current.dropLast(1)
    "." -> when {
        current.contains('.') -> current
        current.isEmpty() -> "0."
        else -> "$current."
    }
    else -> {
        if (!key.all { it.isDigit() }) return current
        when {
            current.contains('.') && current.substringAfter('.').length >= 2 -> current
            current == "0" -> key
            current.length >= 10 -> current
            else -> current + key
        }
    }
}
}

/** APP 内金额数字键盘：数字 + 小数点 + 退格（无「确定」，保存走弹层按钮） */
@Composable
fun AmountKeypad(onKey: (String) -> Unit) {
    val colors = AloCardTheme.colors
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.paperSunken)
                            .clickable { onKey(if (key == "⌫") "del" else key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            key,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
