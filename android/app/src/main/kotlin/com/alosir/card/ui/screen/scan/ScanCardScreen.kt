package com.alosir.card.ui.screen.scan

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** 扫描结果总线：实时识别到卡号+有效期后回传添加卡片页 */
object ScanResultBus {
    var lastResult: CardOcr.Result? = null
}

/**
 * 实时卡面识别：CameraX 预览 + ML Kit 文字识别。
 * 取景框引导 + 「正在识别…」状态；识别到卡号与有效期后自动关闭并回填。
 */
@Composable
fun ScanCardScreen(
    onBack: () -> Unit,
    onDetected: (CardOcr.Result) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val colors = AloCardTheme.colors
    var detected by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        // 取景框：卡片放置位置指引（横向，引导横持手机）
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

        // 状态行：正在识别 / 已识别
        Text(
            if (detected) "已识别，正在回填…" else "正在识别…",
            style = MaterialTheme.typography.labelLarge,
            color = if (detected) colors.goldText else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        )
    }

    LaunchedEffect(Unit) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        var capturing = false
        var done = false

        val capture = ImageCapture.Builder().build()
        val analyzer = ImageAnalysis.Analyzer { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage == null || done || capturing) {
                imageProxy.close()
                return@Analyzer
            }
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // 画面里出现任意文本 → 卡片已入框，自动拍静态照片做精确 OCR
                    if (!done && !capturing && visionText.text.isNotBlank()) {
                        capturing = true
                        capture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val media = image.image
                                    if (media == null) {
                                        image.close()
                                        capturing = false
                                        return
                                    }
                                    val still = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
                                    recognizer.process(still)
                                        .addOnSuccessListener { text ->
                                            val result = CardOcr.parse(text.text)
                                            if (!done && result.cardNumber != null && result.expiryYm != null) {
                                                done = true
                                                detected = true
                                                onDetected(result)
                                            }
                                            capturing = false
                                        }
                                        .addOnFailureListener { capturing = false }
                                    image.close()
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    capturing = false
                                }
                            },
                        )
                    }
                }
                .addOnFailureListener { /* 忽略单帧失败 */ }
            imageProxy.close()
        }

        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            // 限制到 720p，提高识别速度
            .setTargetResolution(android.util.Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer) }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview, analysis, capture,
        )
    }
}
