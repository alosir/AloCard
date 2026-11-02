package com.alocard.app.di

import com.alocard.app.data.bin.BinDirectoryImpl
import com.alocard.app.domain.repository.BinDirectory
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
