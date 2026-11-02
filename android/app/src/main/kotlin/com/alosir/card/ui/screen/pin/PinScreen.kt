package com.alosir.card.ui.screen.pin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.data.security.SecurityManager
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.common.goldBrush
import com.alosir.card.ui.theme.AloCardTheme
import kotlinx.coroutines.delay

/**
 * 安全密码页 · v2：4 位数字，金点即时点亮，满 4 位自动提交。
 * 校验在后台线程（PBKDF2），界面先显示输入态，再显示「验证中…」。
 */
@Composable
fun PinScreen(
    mode: PinMode,
    title: String,
    onBack: (() -> Unit)?,
    onDone: () -> Unit,
    viewModel: PinViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(mode) { viewModel.configure(mode) }
    LaunchedEffect(ui.done) { if (ui.done) onDone() }
    LaunchedEffect(ui.lockedSeconds) {
        while (ui.lockedSeconds > 0) {
            delay(1_000)
            viewModel.tickLock()
        }
    }

    PageScaffold(title = title, onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 拨号盘式：输入区垂直居中，键盘沉底（项 9）
            Spacer(Modifier.weight(1f))
            Text(
                text = if (ui.verifying) "验证中…" else ui.hint,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(24.dp))

            PinDots(count = SecurityManager.PIN_LENGTH, filled = ui.display.length, error = ui.error != null)

            Spacer(Modifier.height(12.dp))
            if (ui.lockedSeconds > 0) {
                HintText(
                    "错误次数过多，请 ${ui.lockedSeconds} 秒后再试",
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                HintText(ui.error ?: "密码仅保存在本机，无法找回。", color = ui.error?.let {
                    MaterialTheme.colorScheme.error
                } ?: MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.weight(1f))
            PinKeypad(onKey = viewModel::onKey)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 密码位 · v2：金色实心圆点，逐位即时弹亮点亮 */
@Composable
fun PinDots(count: Int, filled: Int, error: Boolean = false) {
    val colors = AloCardTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(count) { index ->
            val on = index < filled
            val scale by animateFloatAsState(
                targetValue = if (on) 1.15f else 1f,
                animationSpec = spring(dampingRatio = 0.55f),
                label = "pinDot",
            )
            Box(
                Modifier
                    .size(18.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            on && error -> MaterialTheme.colorScheme.error
                            on -> colors.gold
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

private val KEY_LAYOUT = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("退格", "0", "确定"),
)

/** 数字键盘 · v2：圆角白键；满 4 位自动提交，「确定」仅作占位 */
@Composable
fun PinKeypad(onKey: (String) -> Unit) {
    val colors = AloCardTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KEY_LAYOUT.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val isFn = key == "退格" || key == "确定"
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isFn) androidx.compose.ui.graphics.Color.Transparent else colors.paperRaised)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                when (key) {
                                    "退格" -> onKey("del")
                                    "确定" -> Unit
                                    else -> onKey(key)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isFn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
