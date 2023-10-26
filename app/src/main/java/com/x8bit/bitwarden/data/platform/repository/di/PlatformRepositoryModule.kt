package com.x8bit.bitwarden.data.platform.repository.di

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepositoryImpl
import com.x8bit.bitwarden.data.platform.repository.NetworkConfigRepository
import com.x8bit.bitwarden.data.platform.repository.NetworkConfigRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Provides repositories in the auth package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformRepositoryModule {

    @Provides
    @Singleton
    fun provideEnvironmentRepository(
        environmentDiskSource: EnvironmentDiskSource,
    ): EnvironmentRepository =
        EnvironmentRepositoryImpl(
            environmentDiskSource = environmentDiskSource,
            dispatcher = Dispatchers.IO,
        )

    @Provides
    @Singleton
    fun provideNetworkConfigRepository(
        authRepository: AuthRepository,
        authTokenInterceptor: AuthTokenInterceptor,
        environmentRepository: EnvironmentRepository,
        baseUrlInterceptors: BaseUrlInterceptors,
    ): NetworkConfigRepository =
        NetworkConfigRepositoryImpl(
            authRepository = authRepository,
            authTokenInterceptor = authTokenInterceptor,
            environmentRepository = environmentRepository,
            baseUrlInterceptors = baseUrlInterceptors,
            dispatcher = Dispatchers.IO,
        )
}
