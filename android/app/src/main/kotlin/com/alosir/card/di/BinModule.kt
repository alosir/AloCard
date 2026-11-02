package com.alosir.card.di

import com.alosir.card.data.bin.BinDirectoryImpl
import com.alosir.card.domain.repository.BinDirectory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BinModule {

    @Binds
    @Singleton
    abstract fun bindBinDirectory(impl: BinDirectoryImpl): BinDirectory
}
