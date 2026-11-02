package com.alosir.card.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.alosir.card.data.local.dao.AppSettingsDao
import com.alosir.card.data.local.dao.BillDao
import com.alosir.card.data.local.dao.BillingGroupDao
import com.alosir.card.data.local.dao.CardDao
import com.alosir.card.data.local.dao.NotificationLogDao
import com.alosir.card.data.local.dao.QuotaDao
import com.alosir.card.data.local.entity.AppSettingsEntity
import com.alosir.card.data.local.entity.BillEntity
import com.alosir.card.data.local.entity.BillingGroupEntity
import com.alosir.card.data.local.entity.CardEntity
import com.alosir.card.data.local.entity.NotificationLogEntity

/**
 * AloCard 数据库（技术规范 §3）。
 *
 * 版本 1：当前阶段即最终形态（pre-release 无历史库）。
 * 以后任何 schema 变更必须：
 * 1. 版本号 +1；
 * 2. 提供显式 Migration 并加入 [MIGRATIONS]；
 * 3. **禁止** fallbackToDestructiveMigration——用户数据全部手工录入，丢了就没了。
 */
@Database(
    entities = [
        BillingGroupEntity::class,
        CardEntity::class,
        BillEntity::class,
        AppSettingsEntity::class,
        NotificationLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AloCardDatabase : RoomDatabase() {

    abstract fun billingGroupDao(): BillingGroupDao
    abstract fun cardDao(): CardDao
    abstract fun billDao(): BillDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun quotaDao(): QuotaDao

    companion object {
        const val NAME = "alocard.db"

        /** 后续版本的迁移数组，按版本号升序 */
        val MIGRATIONS = emptyArray<androidx.room.migration.Migration>()

        fun build(context: Context): AloCardDatabase =
            Room.databaseBuilder(context, AloCardDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
