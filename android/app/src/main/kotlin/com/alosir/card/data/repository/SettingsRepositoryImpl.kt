package com.alosir.card.data.repository

import com.alosir.card.data.local.dao.AppSettingsDao
import com.alosir.card.data.local.entity.AppSettingsEntity
import com.alosir.card.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: AppSettingsDao,
) : SettingsRepository {

    override fun observeSortMode(): Flow<String> =
        dao.observe(KEY_SORT_MODE).map { it ?: SORT_GRACE }

    override suspend fun setSortMode(mode: String) {
        dao.put(AppSettingsEntity(KEY_SORT_MODE, mode))
    }

    override fun observeLockEnabled(): Flow<Boolean> =
        dao.observe(KEY_LOCK_ENABLED).map { it == "true" }

    override suspend fun setLockEnabled(enabled: Boolean) {
        dao.put(AppSettingsEntity(KEY_LOCK_ENABLED, enabled.toString()))
    }

    override fun observeBiometricEnabled(): Flow<Boolean> =
        dao.observe(KEY_BIOMETRIC_ENABLED).map { it == "true" }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dao.put(AppSettingsEntity(KEY_BIOMETRIC_ENABLED, enabled.toString()))
    }

    override suspend fun binTableSnapshot(): String? = dao.get(KEY_BIN_SNAPSHOT)

    override suspend fun setBinTableSnapshot(snapshot: String) {
        dao.put(AppSettingsEntity(KEY_BIN_SNAPSHOT, snapshot))
    }

    override fun observeLastBackupAt(): Flow<Long?> =
        dao.observe(KEY_LAST_BACKUP_AT).map { it?.toLongOrNull() }

    override suspend fun setLastBackupAt(at: Long) {
        dao.put(AppSettingsEntity(KEY_LAST_BACKUP_AT, at.toString()))
    }

    companion object {
        const val SORT_GRACE = "grace"
        const val SORT_DUE = "due"

        private const val KEY_SORT_MODE = "sort_mode"
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_BIN_SNAPSHOT = "bin_table_snapshot"
        private const val KEY_LAST_BACKUP_AT = "last_backup_at"
    }
}
