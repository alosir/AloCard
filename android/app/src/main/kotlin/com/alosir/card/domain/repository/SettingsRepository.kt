package com.alosir.card.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 设置仓库（PRD 9.2 的 app_settings 键值对）。
 *
 * 只放跨会话需要保留的偏好；其余状态走各自的状态源。
 */
interface SettingsRepository {

    /** 首页排序方式：grace（免息期降序，默认）/ due（距还款日升序）。PRD F5.1 规则 6 */
    fun observeSortMode(): Flow<String>
    suspend fun setSortMode(mode: String)

    /** 应用锁开关（PRD F10.1）；未设置 PIN 时无法开启 */
    fun observeLockEnabled(): Flow<Boolean>
    suspend fun setLockEnabled(enabled: Boolean)

    /** 指纹锁开关：与密码锁并列；开启需先通过本机指纹验证 */
    fun observeBiometricEnabled(): Flow<Boolean>
    suspend fun setBiometricEnabled(enabled: Boolean)

    /** 号段表快照时间（ISO 日期串），用于「识别结果仅供参考」的诚实标注 */
    suspend fun binTableSnapshot(): String?
    suspend fun setBinTableSnapshot(snapshot: String)

    /** 上次备份导出的 epoch millis；设置页展示 */
    fun observeLastBackupAt(): Flow<Long?>
    suspend fun setLastBackupAt(at: Long)
}
