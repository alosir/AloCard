package com.alosir.card.ui.applock

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.alosir.card.data.security.SecurityManager
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.screen.pin.PinDots
import com.alosir.card.ui.screen.pin.PinKeypad
import com.alosir.card.ui.theme.AloCardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 应用锁闸门 · v2（PRD F10）。
 *
 * 指纹优先：进入即弹系统生物识别，指纹按钮可随时重新唤起；
 * 4 位数字密码兜底，逐位即时点亮，PBKDF2 校验在后台线程。
 * 会话有效期与锁定策略由 MainActivity 控制（后台超 5 分钟回前台需重新验证）。
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
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var verifying by remember { mutableStateOf(false) }
    val colors = AloCardTheme.colors

    val canBiometric = remember {
        activity != null && BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun promptBiometric() {
        val fragmentActivity = activity as? androidx.fragment.app.FragmentActivity ?: return
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
            .setSubtitle("验证指纹以继续使用")
            .setNegativeButtonText("使用安全密码")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    // 指纹优先：进入锁屏即唤起系统认证
    LaunchedEffect(canBiometric) {
        if (canBiometric) promptBiometric()
    }

    fun submit(value: String) {
        verifying = true
        error = null
        scope.launch {
            val result = withContext(Dispatchers.Default) { security.verifyPin(value) }
            verifying = false
            when (result) {
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
            Spacer(Modifier.height(48.dp))
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.paperRaised),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = colors.goldText)
            }
            Spacer(Modifier.height(16.dp))
            Text("欢迎回来", style = MaterialTheme.typography.titleLarge)
            HintText(
                if (verifying) "验证中…" else "验证指纹或输入 ${SecurityManager.PIN_LENGTH} 位密码",
                Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(20.dp))

            PinDots(count = SecurityManager.PIN_LENGTH, filled = pin.length, error = error != null)

            // 指纹解锁（优先）
            if (canBiometric) {
                Spacer(Modifier.height(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.goldSoft)
                            .clickable { promptBiometric() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = "指纹解锁",
                            tint = colors.goldText,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    HintText("指纹解锁", Modifier.padding(top = 8.dp))
                    HintText("优先使用本机生物识别")
                }
            }

            error?.let {
                HintText(it, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            PinKeypad(
                onKey = { key ->
                    if (verifying) return@PinKeypad
                    when {
                        key == "del" -> { pin = pin.dropLast(1); error = null }
                        pin.length < SecurityManager.PIN_LENGTH && key.all { it.isDigit() } -> {
                            pin += key
                            error = null
                        }
                    }
                    // 第 4 位先点亮（上面的状态已更新），再异步校验
                    if (pin.length == SecurityManager.PIN_LENGTH) submit(pin)
                },
            )
        }
    }
}
