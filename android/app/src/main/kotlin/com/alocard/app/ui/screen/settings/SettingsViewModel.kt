package com.alocard.app.ui.screen.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alocard.app.BuildConfig
import com.alocard.app.data.backup.BackupManager
import com.alocard.app.data.security.SecurityManager
import com.alocard.app.data.update.LatestRelease
import com.alocard.app.data.update.UpdateCheckResult
import com.alocard.app.data.update.UpdateManager
import com.alocard.app.domain.repository.BinDirectory
import com.alocard.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 设置页状态（P11） */
data class SettingsUiState(
    val binSnapshot: String? = null,
    val hasPin: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val security: SecurityManager,
    private val backup: BackupManager,
    private val updateManager: UpdateManager,
    @ApplicationContext private val context: Context,
    binDirectory: BinDirectory,
) : ViewModel() {

    private val _ui = MutableStateFlow(SettingsUiState(binSnapshot = binDirectory.snapshot()))
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    /** 当前版本号（BuildConfig 由 gradle.properties 注入） */
    val versionName: String = "v${BuildConfig.VERSION_NAME}"

    private val _updateCheck = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheck: StateFlow<UpdateCheckResult?> = _updateCheck.asStateFlow()

    /** 下载进度 0f-1f；-1f 表示未知总长；-2f 表示未在下载 */
    private val _downloadProgress = MutableStateFlow(-2f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _changelog = MutableStateFlow<String?>(null)
    val changelog: StateFlow<String?> = _changelog.asStateFlow()

    val lockEnabled: StateFlow<Boolean> = settings.observeLockEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val lastBackupAt: StateFlow<Long?> = settings.observeLastBackupAt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        _ui.update { it.copy(hasPin = security.hasPin()) }
    }

    fun setLockEnabled(enabled: Boolean) {
        // 未设密码时不允许开应用锁
        if (enabled && !security.hasPin()) {
            _ui.update { it.copy(error = "请先设置安全密码") }
            return
        }
        viewModelScope.launch { settings.setLockEnabled(enabled) }
    }

    // ---------- 更新 ----------

    fun checkUpdate() {
        if (_updateCheck.value == UpdateCheckResult.Checking) return
        viewModelScope.launch {
            _updateCheck.value = UpdateCheckResult.Checking
            _updateCheck.value = runCatching {
                val latest = updateManager.checkLatestRelease()
                if (updateManager.isNewer(latest.tagName, BuildConfig.VERSION_NAME)) {
                    UpdateCheckResult.Available(latest)
                } else {
                    UpdateCheckResult.Latest
                }
            }.getOrElse { UpdateCheckResult.Failed(it.message ?: "检查更新失败") }
        }
    }

    fun dismissUpdateResult() {
        _updateCheck.value = null
    }

    /** 下载并触发安装 */
    fun downloadAndInstall(release: LatestRelease) {
        viewModelScope.launch {
            _downloadProgress.value = 0f
            runCatching {
                updateManager.downloadAndInstall(release.downloadUrl) { p -> _downloadProgress.value = p }
                _ui.update { it.copy(message = "下载完成，点击通知或在下载目录安装") }
            }.onFailure { e ->
                _ui.update { it.copy(error = "下载失败：${e.message}") }
            }
            _downloadProgress.value = -2f
        }
    }

    /** 读取随包 CHANGELOG.md（构建时从仓库根拷贝，AGENTS.md 约定的单一事实来源） */
    fun loadChangelog() {
        if (_changelog.value != null) return
        viewModelScope.launch {
            _changelog.value = withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }
                }.getOrNull() ?: "（本版本未附带更新日志）"
            }
        }
    }

    fun dismissChangelog() {
        _changelog.value = null
    }

    // ---------- 备份 ----------

    /** 导出加密备份（PRD F9.1）。返回容器字节，由界面写入用户选择的位置 */
    suspend fun exportBackup(pin: String): ByteArray {
        _ui.update { it.copy(busy = true, error = null) }
        return try {
            backup.export(pin).also {
                settings.setLastBackupAt(System.currentTimeMillis())
                _ui.update { it.copy(busy = false, message = "备份已导出") }
            }
        } catch (e: Exception) {
            _ui.update { it.copy(busy = false, error = "导出失败：${e.message}") }
            throw e
        }
    }

    /** 恢复备份（PRD F9.2）：全量替换，恢复前自动快照 */
    fun importBackup(data: ByteArray, pin: String) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            try {
                backup.import(data, pin)
                _ui.update { it.copy(busy = false, message = "恢复完成", hasPin = security.hasPin()) }
            } catch (e: Exception) {
                _ui.update { it.copy(busy = false, error = "恢复失败：${e.message}") }
            }
        }
    }

    fun clearMessage() = _ui.update { it.copy(message = null, error = null) }
}

