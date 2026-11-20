package com.alosir.card.ui.screen.icons

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.alosir.card.ui.common.ActionRow
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.theme.AloCardTheme

@Composable
fun IconCropScreen(
    uri: Uri? = null,
    originalPath: String? = null,
    initialCenterX: Float = 0.5f,
    initialCenterY: Float = 0.5f,
    initialZoom: Float = 1f,
    onCancel: () -> Unit,
    onSave: (Float, Float, Float) -> Unit,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, uri, originalPath) {
        value = when {
            uri != null -> context.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
            originalPath != null -> BitmapFactory.decodeFile(originalPath)?.asImageBitmap()
            else -> null
        }
    }
    var zoom by remember { mutableStateOf(initialZoom.coerceIn(1f, 5f)) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }
    var viewWidthPx by remember { mutableStateOf(0f) }
    val colors = AloCardTheme.colors

    PageScaffold(title = "裁剪图标", onBack = onCancel) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            BoxWithConstraints(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(CircleShape)
                    .background(colors.paperSunken)
                    .border(2.dp, colors.gold, CircleShape)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
            ) {
                val viewSize = constraints.maxWidth.toFloat()
                viewWidthPx = viewSize
                if (!initialized && viewSize > 0f) {
                    offsetX = (0.5f - initialCenterX) * viewSize * zoom
                    offsetY = (0.5f - initialCenterY) * viewSize * zoom
                    initialized = true
                }
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "裁剪原图",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoom,
                                scaleY = zoom,
                                translationX = offsetX,
                                translationY = offsetY,
                            ),
                    )
                }
            }
            HintText("拖动调整位置，双指缩放；圆形区域将保存为图标", Modifier.padding(vertical = 14.dp))
            ActionRow(
                cancelText = "取消",
                onCancel = onCancel,
                confirmText = "保存",
                onConfirm = {
                    onSave(
                        0.5f - offsetX / (viewWidthPx * zoom),
                        0.5f - offsetY / (viewWidthPx * zoom),
                        zoom,
                    )
                },
                confirmEnabled = bitmap != null,
            )
        }
    }
}
