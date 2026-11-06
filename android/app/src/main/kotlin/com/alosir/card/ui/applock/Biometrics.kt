package com.alosir.card.ui.applock

import android.app.Activity
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** 本机生物识别（指纹优先）的统一入口。 */
object Biometrics {

    /** 设备是否具备可用的 Class 3 生物识别 */
    fun isStrongAvailable(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /** 唤起系统指纹/生物识别框；宿主必须是 FragmentActivity */
    fun prompt(
        activity: Activity?,
        title: String,
        subtitle: String,
        negativeText: String,
        onSuccess: () -> Unit,
        onDismiss: () -> Unit = {},
    ) {
        val fragmentActivity = activity as? FragmentActivity ?: return
        val prompt = BiometricPrompt(
            fragmentActivity,
            ContextCompat.getMainExecutor(fragmentActivity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onDismiss()
                }
            },
        )
        runCatching {
            val builder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(negativeText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            if (subtitle.isNotBlank()) builder.setSubtitle(subtitle)
            prompt.authenticate(builder.build())
        }
        // 个别 ROM 弹窗异常时降级为密码输入，不崩溃
    }
}
