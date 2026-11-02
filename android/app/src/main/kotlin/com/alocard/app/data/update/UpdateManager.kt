package com.alocard.app.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.alocard.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** GitHub 最新 Release 信息 */
data class LatestRelease(
    val tagName: String,
    val name: String,
    /** Release 说明（Markdown，展示为新版本更新日志） */
    val body: String,
    /** APK 直链（GitHub Release 资产） */
    val downloadUrl: String,
)

sealed interface UpdateCheckResult {
    data object Checking : UpdateCheckResult
    /** 已是最新 */
    data object Latest : UpdateCheckResult
    data class Available(val release: LatestRelease) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

/**
 * 应用自我更新（GitHub Releases 通道）。
 *
 * 检查走匿名 GitHub API（公开仓库无需凭证）；下载到应用专属目录后经
 * FileProvider 交给系统安装器。除此之外无任何网络用途——
 * 不连接银行、无账号、无云同步（INTERNET 权限的唯一用途，见 Manifest 注释）。
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** 查询 github.com/alosir/AloCard 的最新 Release */
    suspend fun checkLatestRelease(): LatestRelease = withContext(Dispatchers.IO) {
        val conn = URL(API_URL).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) {
                throw IllegalStateException("检查更新失败：HTTP ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val assets = json.getJSONArray("assets")
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            if (apkUrl == null) throw IllegalStateException("最新 Release 里没有 APK 文件")
            LatestRelease(
                tagName = json.getString("tag_name"),
                name = json.optString("name", json.getString("tag_name")),
                body = json.optString("body", ""),
                downloadUrl = apkUrl,
            )
        } finally {
            conn.disconnect()
        }
    }

    /** 语义化版本比较：latest 比 current 新返回 true（v 前缀可省略，缺段按 0） */
    fun isNewer(latest: String, current: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").split(".")
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val l = parts(latest)
        val c = parts(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    /**
     * 下载 APK 并触发系统安装。
     * 进度通过 [onProgress](0f-1f, unknown = -1f) 回调；完成后发通知点击安装。
     */
    suspend fun downloadAndInstall(
        url: String,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        dir.mkdirs()
        val apk = File(dir, "alocard_update.apk")
        if (apk.exists()) apk.delete()

        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 20_000
            conn.readTimeout = 120_000
            conn.instanceFollowRedirects = true
            if (conn.responseCode != 200) throw IllegalStateException("下载失败：HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                apk.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var read: Int
                    var done = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        done += read
                        onProgress(if (total > 0) done.toFloat() / total else -1f)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        notifyInstallReady(apk)
        apk
    }

    /** 下载完成通知：点击走系统安装器（需 REQUEST_INSTALL_PACKAGES） */
    private fun notifyInstallReady(apk: File) {
        val channelId = "app_update"
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "应用更新", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_alocard)
            .setContentTitle("AloCard 更新已下载")
            .setContentText("点击安装新版本")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(UPDATE_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val API_URL = "https://api.github.com/repos/alosir/AloCard/releases/latest"
        private const val UPDATE_NOTIFICATION_ID = 1001
    }
}
