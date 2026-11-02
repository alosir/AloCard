package com.alosir.card.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alosir.card.ui.theme.AloCardTheme

/**
 * 有效期输入 · MM / YYYY 双区。
 * 中间「/」只读展示无需手输；MM 输满 2 位自动跳到 YYYY；YYYY 清空退格回到 MM。
 * 存储仍为 YYYY-MM（模型不变）。
 */
@Composable
fun ExpiryField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var month by remember { mutableStateOf(value.substringAfter("-", "")) }
    var year by remember { mutableStateOf(value.substringBefore("-", "")) }
    val yearFocus = remember { FocusRequester() }
    val monthFocus = remember { FocusRequester() }

    fun emit() {
        val next = when {
            month.isEmpty() && year.isEmpty() -> ""
            month.length == 2 && year.length == 4 -> "$year-$month"
            else -> return // 输入途中不写回，避免半成品触发校验错误
        }
        onValueChange(next)
    }

    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExpiryBox(
                value = month,
                hint = "MM",
                maxLength = 2,
                focusRequester = monthFocus,
                onChange = {
                    month = it
                    if (it.length == 2) yearFocus.requestFocus()
                    emit()
                },
            )
            Text(
                "/",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            ExpiryBox(
                value = year,
                hint = "YYYY",
                maxLength = 4,
                focusRequester = yearFocus,
                onChange = {
                    year = it
                    if (it.isEmpty()) monthFocus.requestFocus()
                    emit()
                },
            )
        }
    }
}

@Composable
private fun ExpiryBox(
    value: String,
    hint: String,
    maxLength: Int,
    focusRequester: FocusRequester,
    onChange: (String) -> Unit,
) {
    val colors = AloCardTheme.colors
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength && it.all { c -> c.isDigit() }) onChange(it) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(colors.gold),
        modifier = Modifier.width(if (maxLength == 2) 64.dp else 88.dp).focusRequester(focusRequester),
        decorationBox = { inner ->
            Box(
                Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.paperSunken)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
                inner()
            }
        },
    )
}

/**
 * 账单日选择 · 7 列 × 4 行日期矩阵（1–28 单选）。
 * 替代手输：点选即得，不可能输错范围（ADR-0002）。
 */
@Composable
fun BillingDayPicker(
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AloCardTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(7) { col ->
                    val day = row * 7 + col + 1
                    val on = selected == day
                    val shape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(shape)
                            .then(
                                if (on) Modifier.background(goldBrush())
                                else Modifier.background(colors.paperSunken)
                            )
                            .clickable { onSelect(day) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$day",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            color = if (on) colors.onGold else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
