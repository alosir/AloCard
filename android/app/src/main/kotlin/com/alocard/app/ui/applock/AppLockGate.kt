package com.alocard.app.ui.applock

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.alocard.app.data.security.SecurityManager
import com.alocard.app.ui.common.HintText
import com.alocard.app.ui.screen.pin.PinKeypad
import com.alocard.app.ui.theme.AloCardTheme

/**
 * 应用锁闸门（PRD F10）。
 *
 * 锁定时替换整个内容区；生物识别可用时优先弹系统生物识别，
 * 取消/失败回退到 PIN 键盘。会话有效期与锁定策略由 MainActivity 控制
 * （后台超 5 分钟回前台需重新验证；进程死亡会话自然失效——PRD F10.2）。
 */
@Composable
fun AppLockGate(
    security: SecurityManager,
    content: @Composable () -> Unit,
) {
    var unlocked by remember { mutableStateOf(security.isSessionUnlocked()) }
    // 会话状态随时间变化（解锁有效期/后台超时锁定），秒级轮询触发重组
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1_000)
            unlocked = security.isSessionUnlocked()
        }
    }
    if (unlocked) {
        content()
    } else {
        LockScreen(security = security, onUnlocked = { unlocked = true })
    }
}

@Composable
private fun LockScreen(
    security: SecurityManager,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val colors = AloCardTheme.colors

    // 生物识别可用则直接弹系统认证（Class 3）
    val canBiometric = remember {
        activity != null && BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
    LaunchedEffect(canBiometric) {
        if (!canBiometric) return@LaunchedEffect
        val fragmentActivity = activity as? androidx.fragment.app.FragmentActivity ?: return@LaunchedEffect
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            fragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    security.unlockSession()
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 用户取消/失败：留在 PIN 键盘
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁 AloCard")
            .setSubtitle("验证以继续使用")
            .setNegativeButtonText("使用安全密码")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Spacer(Modifier.height(64.dp))
            Box(
                Modifier
                    .width(56.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.paperSunken),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(16.dp))
            Text("AloCard 已锁定", style = MaterialTheme.typography.titleLarge)
            HintText("输入 6 位安全密码解锁", Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(24.dp))

            // 密码位（下边框样式）
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(6) { index ->
                    Box(
                        Modifier
                            .width(32.dp)
                            .height(40.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(
                                    if (index < pin.length) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
            }
            error?.let {
                HintText(it, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            PinKeypad(
                onKey = { key ->
                    when {
                        key == "del" -> pin = pin.dropLast(1)
                        pin.length < 6 -> pin += key
                    }
                    if (pin.length == 6) {
                        when (val result = security.verifyPin(pin)) {
                            is SecurityManager.VerifyResult.Success -> onUnlocked()
                            is SecurityManager.VerifyResult.Wrong -> {
                                error = "密码错误，还可试 ${result.remainingAttempts} 次"
                                pin = ""
                            }
                            is SecurityManager.VerifyResult.Locked -> {
                                error = "已锁定，请 ${result.remainingSeconds} 秒后再试"
                                pin = ""
                            }
                        }
                    }
                },
            )
        }
    }
}
