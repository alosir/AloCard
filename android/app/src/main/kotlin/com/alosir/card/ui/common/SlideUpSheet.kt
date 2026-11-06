package com.alosir.card.ui.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/**
 * 全屏底部浮层容器（Material3 ModalBottomSheet）：
 * 贴屏幕底部弹出（上滑入/下滑出），遮住底部导航。
 * 弹层高度随内容自适应；键盘与屏幕底部间距固定，不会留空白。
 * 底层页面始终留在原地，不做划出转场。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideUpSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        content = content,
    )
}
