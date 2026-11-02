package com.alosir.card

import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.alosir.card.data.security.SecurityManager
import com.alosir.card.domain.repository.SettingsRepository
import com.alosir.card.sub.work.AloCardWorkManager
import com.alosir.card.sub.work.DailySurfacesRefresher
import com.alosir.card.ui.applock.AppLockGate
import com.alosir.card.ui.navigation.AloCardNavHost
import com.alosir.card.ui.theme.AloCardTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var refresher: DailySurfacesRefresher

    @Inject
    lateinit var security: SecurityManager

    @Inject
    lateinit var settings: SettingsRepository

    /** 进入后台时刻（elapsedRealtime），用于 5 分钟会话超时判定 */
    private var backgroundedAt: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 提醒调度自愈：进程被杀/设备重启导致续期链断裂时补排（幂等）
        AloCardWorkManager.ensureScheduled(this)

        handleSurfaceRefresh()

        setContent {
            AloCardTheme {
                val startGroupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
                val lockEnabled by remember { settings.observeLockEnabled() }
                    .collectAsState(initial = false)
                val biometricEnabled by remember { settings.observeBiometricEnabled() }
                    .collectAsState(initial = false)

                // 密码锁或指纹锁任一开启：未解锁会话一律先过闸门（指纹优先，PIN 兜底）
                if (lockEnabled || biometricEnabled) {
                    AppLockGate(security = security, biometricEnabled = biometricEnabled) {
                        AloCardNavHost(launchGroupId = startGroupId)
                    }
                } else {
                    AloCardNavHost(launchGroupId = startGroupId)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSurfaceRefresh()
    }

    override fun onResume() {
        super.onResume()
        // 后台超过 5 分钟回前台：会话作废，需重新验证（PRD F10.2 规则 1）
        if (backgroundedAt > 0L &&
            SystemClock.elapsedRealtime() - backgroundedAt > SESSION_TIMEOUT_MS
        ) {
            security.lockNow()
        }
        backgroundedAt = 0L
        // 每次回前台刷新信息位（小组件 + 常驻通知），并补发今日应发未发的提醒
        handleSurfaceRefresh()
        lifecycleScope.launch {
            AloCardWorkManager.catchUpReminders(this@MainActivity)
        }
    }

    override fun onPause() {
        super.onPause()
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    private fun handleSurfaceRefresh() {
        lifecycleScope.launch {
            runCatching { refresher.refresh() }
        }
    }

    companion object {
        /** 通知点击直达的账单组 id（AppNotifier 注入） */
        const val EXTRA_GROUP_ID = "com.alosir.card.extra.GROUP_ID"

        /** 会话有效期：后台超过 5 分钟需重新验证（PRD F10.2 规则 1） */
        private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
