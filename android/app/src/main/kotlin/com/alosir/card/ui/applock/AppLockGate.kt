package com.alosir.card.ui.applock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.collectAsState
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
import com.alosir.card.data.security.SecurityManager
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.screen.pin.PinDots
import com.alosir.card.ui.screen.pin.PinKeypad
import com.alosir.card.ui.theme.AloCardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 应用锁闸门 · v2.1。
 * 密码锁或指纹锁任一开启即上锁；指纹锁开启且设备可用时，进入即弹系统指纹（优先），
 * 4 位密码兜底。校验在后台线程，输入逐位即时点亮。
 */
@Composable
fun AppLockGate(
    security: SecurityManager,
    biometricEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val unlocked by security.sessionUnlocked.collectAsState()

    // 覆盖层而非替换：内容始终组合，锁住时只盖锁屏——
    // 解锁后回到原页面（含表单的输入中状态），不会丢回首页
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        content()
        if (!unlocked) {
            LockScreen(security = security, biometricEnabled = biometricEnabled, onUnlocked = { /* 流驱动 */ })
        }
    }
}

@Composable
private fun LockScreen(
    security: SecurityManager,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalContext.current as? android.app.Activity
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var verifying by remember { mutableStateOf(false) }
    val colors = AloCardTheme.colors

    val canBiometric = remember {
        biometricEnabled && Biometrics.isStrongAvailable(context)
    }

    fun promptBiometric() {
        Biometrics.prompt(
            activity = activity,
            title = "解锁 AloCard",
            subtitle = "验证指纹以继续使用",
            negativeText = "使用安全密码",
            onSuccess = {
                security.unlockSession()
                onUnlocked()
            },
        )
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

    // 拨号盘式布局：输入区居中，键盘沉底（项 9）
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
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
        error?.let {
            HintText(it, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error)
        }

        // 指纹解锁（优先，指纹锁开启时显示）
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
            }
        }

        Spacer(Modifier.weight(1f))
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
                // 第 4 位先点亮（状态已更新），再异步校验
                if (pin.length == SecurityManager.PIN_LENGTH) submit(pin)
            },
        )
        Spacer(Modifier.height(20.dp))
    }
}
