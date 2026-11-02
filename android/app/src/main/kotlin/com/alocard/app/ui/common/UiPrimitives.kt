package com.alocard.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alocard.app.ui.theme.AloCardTheme

/**
 * 点阵网格纸地面（DESIGN.md 第 1 节）。
 * 16dp 间距灰点，Bullet Journal 的真实标记系统。
 * 颜色由调用方传入（drawBehind 内不能读 CompositionLocal）。
 */
fun Modifier.dotGrid(color: Color): Modifier = this.drawBehind {
    val spacing = DotSpacing.toPx()
    val radius = DotRadius.toPx()
    var y = spacing / 2
    while (y < size.height) {
        var x = spacing / 2
        while (x < size.width) {
            drawCircle(color, radius, Offset(x, y))
            x += spacing
        }
        y += spacing
    }
}

private val DotSpacing: Dp = 16.dp
private val DotRadius: Dp = 1.1.dp

/** 页面脚手架：顶栏（返回 + 标题）+ 点阵纸内容区 */
@Composable
fun PageScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val paper = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface

    Column(Modifier.fillMaxSize().background(paper)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = ink,
                    )
                }
            } else {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = ink,
                )
            }
            if (onBack != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ink,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            actions()
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .dotGrid(AloCardTheme.colors.dot),
        ) {
            content()
        }
        bottomBar()
    }
}

/** 区块标题（--t-title 600，19sp） */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

/** 键值行（账期/账单等 kv 列表） */
@Composable
fun KeyValueRow(key: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 1dp 发丝分隔线区块 */
@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/** 页内区块：上下留白 + 底部分隔线 */
@Composable
fun Section(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        content()
    }
    SectionDivider()
}

/** 填充式金额/数字输入框（弹层内不用描边，避免卡片套卡片——DESIGN.md 5.7） */
@Composable
fun FilledField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    numeric: Boolean = false,
    supportingText: String? = null,
    isError: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val colors = AloCardTheme.colors
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, style = textStyle) },
            isError = isError,
            singleLine = true,
            textStyle = textStyle,
            shape = RoundedCornerShape(4.dp),
            keyboardOptions = if (numeric) {
                KeyboardOptions(keyboardType = KeyboardType.Decimal)
            } else {
                KeyboardOptions.Default
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.paperSunken,
                unfocusedContainerColor = colors.paperSunken,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** 主按钮（filled，底 --ink）。flex 权重由调用方包 Row */
@Composable
fun FilledButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** tonal 次按钮 */
@Composable
fun TonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.filledTonalButtonColors(),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** 步进器（- 值 +，DESIGN.md 5.x 录入控件） */
@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 28,
) {
    val colors = AloCardTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.paperSunken),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−", value > min) { onChange((value - 1).coerceAtLeast(min)) }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp),
        )
        StepperButton("+", value < max) { onChange((value + 1).coerceAtMost(max)) }
    }
}

@Composable
private fun StepperButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

/** 页面底部双按钮区（取消 tonal + 确认 filled，DESIGN.md 5.7 操作区比例） */
@Composable
fun ActionRow(
    cancelText: String,
    onCancel: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TonalButton(cancelText, onCancel, Modifier.weight(1f))
        FilledButton(confirmText, onConfirm, Modifier.weight(2f), confirmEnabled)
    }
}

/** 说明小字（--t-caption） */
@Composable
fun HintText(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
