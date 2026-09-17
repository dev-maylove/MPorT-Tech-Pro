package com.mporttech.pro.di

import com.mporttech.pro.data.repository.DiagnosticsRepositoryImpl
import com.mporttech.pro.data.repository.NetworkRepositoryImpl
import com.mporttech.pro.data.repository.SpeedTestRepositoryImpl
import com.mporttech.pro.domain.repository.DiagnosticsRepository
import com.mporttech.pro.domain.repository.NetworkRepository
import com.mporttech.pro.domain.repository.SpeedTestRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindNetworkRepository(impl: NetworkRepositoryImpl): NetworkRepository

    @Binds @Singleton
    abstract fun bindSpeedTestRepository(impl: SpeedTestRepositoryImpl): SpeedTestRepository

    @Binds @Singleton
    abstract fun bindDiagnosticsRepository(impl: DiagnosticsRepositoryImpl): DiagnosticsRepository
}
