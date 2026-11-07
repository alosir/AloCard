package com.alosir.card.ui.screen.scan

import android.os.Handler
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alosir.card.data.ocr.CardOcr
import com.alosir.card.ui.theme.AloCardTheme
import java.util.concurrent.Executors

object ScanResultBus {
    var lastResult: CardOcr.Result? = null
}

@Composable
fun ScanCardScreen(
    onBack: () -> Unit,
    onDetected: (CardOcr.Result) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val capture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    val ocrExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(context.mainLooper) }
    val colors = AloCardTheme.colors
    var capturing by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("对准卡片后点击拍照识别") }

    fun captureStill() {
        if (done || capturing) return
        capturing = true
        status = "正在拍照并进行 OCR…"
        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val rotation = image.imageInfo.rotationDegrees
                    ocrExecutor.execute {
                        val result = try {
                            CardOcr.recognize(image.toBitmap(), rotation)
                        } catch (error: Exception) {
                            Log.w(TAG, "静态照片 OCR 失败", error)
                            null
                        } finally {
                            image.close()
                        }
                        mainHandler.post {
                            capturing = false
                            if (!done && result?.cardNumber != null && result.expiryYm != null) {
                                done = true
                                status = "已识别，正在回填…"
                                onDetected(result)
                            } else if (!done) {
                                status = if (result == null) {
                                    "识别失败，请重新拍照"
                                } else when {
                                    result.cardNumber == null && result.expiryYm == null ->
                                        "未识别完整，请对准卡片后重拍"
                                    result.cardNumber == null ->
                                        "卡号未通过校验，请重新拍照"
                                    else -> "未识别有效期，请重新拍照"
                                }
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w(TAG, "拍照失败", exception)
                    capturing = false
                    status = "拍照失败，请重试"
                }
            },
        )
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f)
                .aspectRatio(1.586f)
                .border(2.dp, colors.gold, RoundedCornerShape(20.dp)),
        )
        Text(
            "将卡片放在框内",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
        )
        Text(
            "建议横持手机以获得更大取景画面",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 76.dp),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
        ) {
            Text(
                status,
                style = MaterialTheme.typography.labelLarge,
                color = if (done) colors.goldText else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { captureStill() },
                enabled = !capturing && !done,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(if (capturing) "正在识别…" else "拍照识别")
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview, capture,
            )
        } catch (error: Exception) {
            Log.e(TAG, "相机启动失败", error)
            status = "相机启动失败，请返回重试"
        }
    }

    DisposableEffect(Unit) {
        onDispose { ocrExecutor.shutdown() }
    }
}

private const val TAG = "ScanCardScreen"
