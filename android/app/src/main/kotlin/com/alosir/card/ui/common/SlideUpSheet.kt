package com.alosir.card.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 全屏浮层容器：独立 Dialog 窗口（遮住底部导航），
 * 打开从底部向上滑入，关闭向下滑出且动画播完再真正关闭。
 * 底层页面始终留在原地，不做任何划出转场。
 */
@Composable
fun SlideUpSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val transition = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            visibleState = transition,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(240)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(240)),
        ) {
            content()
        }
        // 出场动画播完再真正关闭
        LaunchedEffect(transition.targetState, transition.isIdle) {
            if (!transition.targetState && transition.isIdle) onDismiss()
        }
    }
}
