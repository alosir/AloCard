package com.alocard.app.ui.screen.pin

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alocard.app.ui.common.HintText
import com.alocard.app.ui.common.PageScaffold
import com.alocard.app.ui.theme.AloCardTheme
import kotlinx.coroutines.delay

/**
 * 安全密码页（P10）：设置 / 修改 / 验证。
 * 数字键盘 56dp 键帽；密码位用**下边框**表示（空心框会被误读为六个输入框——DESIGN.md 5.8）。
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = ui.hint,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(24.dp))

            // 六位密码位：下边框样式
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(6) { index ->
                    val filled = index < ui.display.length
                    Box(
                        Modifier
                            .width(36.dp)
                            .height(44.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(
                                    if (filled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                        if (filled) {
                            Text(
                                text = "●",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }
            }

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

            Spacer(Modifier.height(32.dp))
            PinKeypad(onKey = viewModel::onKey)
        }
    }
}

private val KEY_LAYOUT = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("退格", "0", "确定"),
)

/** 数字键盘（DESIGN.md 5.8：3 列、56dp 键帽、下沉底）。onKey 收到 "del" 表示退格。 */
@Composable
fun PinKeypad(onKey: (String) -> Unit) {
    val colors = AloCardTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KEY_LAYOUT.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 56.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.paperSunken)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                when (key) {
                                    "退格" -> onKey("del")
                                    "确定" -> Unit // 满 6 位自动提交，确定键仅作视觉占位
                                    else -> onKey(key)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
