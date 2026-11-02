package com.alocard.app.di

import android.content.Context
import com.alocard.app.data.local.AloCardDatabase
import com.alocard.app.data.local.dao.AppSettingsDao
import com.alocard.app.data.local.dao.BillDao
import com.alocard.app.data.local.dao.BillingGroupDao
import com.alocard.app.data.local.dao.CardDao
import com.alocard.app.data.local.dao.NotificationLogDao
import com.alocard.app.data.local.dao.QuotaDao
import com.alocard.app.data.repository.BillRepositoryImpl
import com.alocard.app.data.repository.BillingGroupRepositoryImpl
import com.alocard.app.data.repository.CardRepositoryImpl
import com.alocard.app.data.repository.NotificationLogRepositoryImpl
import com.alocard.app.data.repository.SettingsRepositoryImpl
import com.alocard.app.data.security.CryptoManager
import com.alocard.app.domain.repository.BillRepository
import com.alocard.app.domain.repository.BillingGroupRepository
import com.alocard.app.domain.repository.CardRepository
import com.alocard.app.domain.repository.NotificationLogRepository
import com.alocard.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据层依赖模块。
 *
 * 提供 Database、DAO 与安全组件，并把 Repository 实现绑定到 domain 层接口——
 * UI 层只依赖接口，不关心实现（技术规范 §1.2 依赖方向铁律）。
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AloCardDatabase =
        AloCardDatabase.build(context)

    @Provides
    fun provideBillingGroupDao(db: AloCardDatabase): BillingGroupDao = db.billingGroupDao()

    @Provides
    fun provideCardDao(db: AloCardDatabase): CardDao = db.cardDao()

    @Provides
    fun provideBillDao(db: AloCardDatabase): BillDao = db.billDao()

    @Provides
    fun provideAppSettingsDao(db: AloCardDatabase): AppSettingsDao = db.appSettingsDao()

    @Provides
    fun provideNotificationLogDao(db: AloCardDatabase): NotificationLogDao = db.notificationLogDao()

    @Provides
    fun provideQuotaDao(db: AloCardDatabase): QuotaDao = db.quotaDao()

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager = CryptoManager()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBillingGroupRepository(impl: BillingGroupRepositoryImpl): BillingGroupRepository

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Binds
    @Singleton
    abstract fun bindBillRepository(impl: BillRepositoryImpl): BillRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindNotificationLogRepository(
        impl: NotificationLogRepositoryImpl,
    ): NotificationLogRepository
}
