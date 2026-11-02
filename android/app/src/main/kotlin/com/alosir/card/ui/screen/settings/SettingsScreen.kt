package com.alosir.card.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.data.update.UpdateCheckResult
import com.alosir.card.ui.theme.AloCardTheme
import com.alosir.card.ui.common.FilledField
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.KeyValueRow
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.common.Section
import com.alosir.card.ui.common.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设置页（P11）：应用锁、安全密码、加密备份、数据说明。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSetupPin: () -> Unit,
    onChangePin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val lockEnabled by viewModel.lockEnabled.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val updateCheck by viewModel.updateCheck.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val changelogText by viewModel.changelog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showChangelog by remember { mutableStateOf(false) }

    val updateHint = when (val r = updateCheck) {
        null -> "从 GitHub 获取最新版本"
        UpdateCheckResult.Checking -> "正在检查…"
        UpdateCheckResult.Latest -> "已是最新版本"
        is UpdateCheckResult.Failed -> "检查失败：${r.message}"
        is UpdateCheckResult.Available -> "发现新版本 ${r.release.tagName}"
    }

    // 备份流程状态机：等待 PIN / 等待选文件
    var pendingExportPin by remember { mutableStateOf<String?>(null) }
    var pendingImportPin by remember { mutableStateOf<String?>(null) }
    var importBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pinDialog by remember { mutableStateOf<PinDialog?>(null) }
    var confirmImport by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val pin = pendingExportPin
        pendingExportPin = null
        if (uri != null && pin != null) {
            scope.launch {
                runCatching {
                    val bytes = viewModel.exportBackup(pin)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: error("无法写入所选位置")
                    }
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: error("无法读取所选文件")
                    }
                    importBytes = bytes
                    pinDialog = PinDialog.IMPORT
                }
            }
        }
    }

    // 一次性消息
    LaunchedEffect(ui.message, ui.error) {
        if (ui.message != null || ui.error != null) {
            kotlinx.coroutines.delay(3_000)
            viewModel.clearMessage()
        }
    }

    PageScaffold(title = "设置", onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
        ) {
            Section {
                SectionTitle("安全")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("应用锁", style = MaterialTheme.typography.bodyLarge)
                        HintText("指纹优先，4 位数字密码兜底；后台超时需重新验证。")
                    }
                    Switch(
                        checked = lockEnabled,
                        onCheckedChange = { viewModel.setLockEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = AloCardTheme.colors.gold,
                        ),
                    )
                }
                SettingsRow(
                    title = if (ui.hasPin) "修改安全密码" else "设置安全密码",
                    hint = if (ui.hasPin) "用于解锁应用锁与查看完整卡号" else "设置后可用应用锁与卡号查看保护",
                    onClick = { if (ui.hasPin) onChangePin() else onSetupPin() },
                )
            }
            Section {
                SectionTitle("备份")
                SettingsRow(
                    title = "导出加密备份",
                    hint = "全部账单组、卡片（含卡号）与账单历史，加密为 .aloc 文件",
                    onClick = {
                        if (!ui.hasPin) {
                            onSetupPin()
                        } else {
                            pinDialog = PinDialog.EXPORT
                        }
                    },
                )
                SettingsRow(
                    title = "从备份恢复",
                    hint = "全量替换当前数据；恢复前会自动保留一份本地快照",
                    onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                )
                lastBackupAt?.let {
                    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    KeyValueRow("上次备份", fmt.format(Date(it)))
                }
            }
            Section {
                SectionTitle("数据")
                KeyValueRow("号段表快照", ui.binSnapshot ?: "未知")
                HintText(
                    "卡号识别来自离线号段表，结果仅供参考；长尾银行可能识别不到，可手动覆盖。",
                    Modifier.padding(top = 4.dp),
                )
                KeyValueRow("数据位置", "仅本机（Room 数据库）")
                HintText(
                    "AloCard 不连接银行、没有账号、不出网。卡号以 AES-256-GCM 加密存储。",
                    Modifier.padding(top = 4.dp),
                )
            }
            Section {
                SectionTitle("关于")
                KeyValueRow("版本", viewModel.versionName)
                KeyValueRow("包名", "com.alosir.card")
                KeyValueRow("渠道", "GitHub Release · 仅手动检查更新")
                SettingsRow(
                    title = "检查更新",
                    hint = updateHint,
                    onClick = { viewModel.checkUpdate() },
                )
                SettingsRow(
                    title = "更新日志",
                    hint = "本版本的更新内容",
                    onClick = { viewModel.loadChangelog(); showChangelog = true },
                )
                HintText(
                    "剩余可用额度为估算值（仅统计已出账账单）；免息期基于手工录入的账单日推算。",
                    Modifier.padding(top = 4.dp),
                )
            }
            ui.message?.let {
                HintText(it, Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(4.dp))
            }
            ui.error?.let {
                HintText(it, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ---- 更新日志弹层 ----
    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            title = { Text("更新日志") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(changelogText ?: "加载中…", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelog = false }) { Text("关闭") }
            },
        )
    }

    // ---- 新版本弹层：Release 说明 + 下载 ----
    val check = updateCheck
    if (check is UpdateCheckResult.Available) {
        val release = check.release
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateResult() },
            title = { Text("发现新版本 ${release.tagName}") },
            text = {
                Column {
                    if (release.body.isNotBlank()) {
                        Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 280.dp)) {
                            Text(release.body, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    when {
                        downloadProgress == -2f -> HintText("点击「下载更新」获取 APK，完成后系统安装器会引导安装。")
                        downloadProgress < 0f -> HintText("下载中…（进度未知）")
                        else -> {
                            HintText("下载中 ${(downloadProgress * 100).toInt()}%")
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = downloadProgress == -2f,
                    onClick = { viewModel.downloadAndInstall(release) },
                ) { Text("下载更新") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateResult() }) { Text("稍后") }
            },
        )
    }
    (updateCheck as? UpdateCheckResult.Failed)?.let { failed ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateResult() },
            title = { Text("检查更新失败") },
            text = { Text(failed.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissUpdateResult() }) { Text("知道了") }
            },
        )
    }

    // ---- PIN 输入弹层（备份的加密口令） ----
    pinDialog?.let { dialog ->
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pinDialog = null },
            title = { Text(if (dialog == PinDialog.EXPORT) "输入安全密码" else "输入备份密码") },
            text = {
                FilledField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = "4 位数字密码",
                    numeric = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = pin.length == 4,
                    onClick = {
                        pinDialog = null
                        if (dialog == PinDialog.EXPORT) {
                            pendingExportPin = pin
                            exportLauncher.launch("alocard-backup.aloc")
                        } else {
                            pendingImportPin = pin
                            confirmImport = true
                        }
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { pinDialog = null }) { Text("取消") }
            },
        )
    }

    // ---- 恢复前二次确认 ----
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("从备份恢复？") },
            text = { Text("当前全部数据将被备份内容替换，且不可撤销（本地已自动保留快照）。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    val bytes = importBytes
                    val pin = pendingImportPin
                    importBytes = null
                    pendingImportPin = null
                    if (bytes != null && pin != null) viewModel.importBackup(bytes, pin)
                }) { Text("恢复", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) { Text("取消") }
            },
        )
    }
}

private enum class PinDialog { EXPORT, IMPORT }

@Composable
private fun SettingsRow(title: String, hint: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        HintText(hint)
    }
}
