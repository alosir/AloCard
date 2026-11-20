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
import com.alosir.card.data.local.entity.CustomIconEntity
import com.alosir.card.data.local.entity.NotificationLogEntity

/**
 * AloCard 数据库（技术规范 §3）。
 *
 * 版本 3：新增自定义图标与账单组 Logo 引用。
 * 任何 schema 变更必须：
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
        CustomIconEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AloCardDatabase : RoomDatabase() {

    abstract fun billingGroupDao(): BillingGroupDao
    abstract fun cardDao(): CardDao
    abstract fun billDao(): BillDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun quotaDao(): QuotaDao
    abstract fun customIconDao(): com.alosir.card.data.local.dao.CustomIconDao

    companion object {
        const val NAME = "alocard.db"

        /** 后续版本的迁移数组，按版本号升序 */
        val MIGRATIONS = arrayOf(
            object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE billing_groups ADD COLUMN due_mode TEXT NOT NULL DEFAULT 'offset'")
                    db.execSQL("ALTER TABLE billing_groups ADD COLUMN due_day_of_month INTEGER")
                }
            },
            object : androidx.room.migration.Migration(2, 3) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE billing_groups ADD COLUMN logo_key TEXT")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS custom_icons (" +
                            "id TEXT NOT NULL PRIMARY KEY, original_path TEXT NOT NULL, cropped_path TEXT NOT NULL, " +
                            "center_x REAL NOT NULL, center_y REAL NOT NULL, zoom REAL NOT NULL, " +
                            "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)"
                    )
                }
            },
        )

        fun build(context: Context): AloCardDatabase =
            Room.databaseBuilder(context, AloCardDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
