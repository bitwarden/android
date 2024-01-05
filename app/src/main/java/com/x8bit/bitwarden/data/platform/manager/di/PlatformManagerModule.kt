package com.x8bit.bitwarden.data.platform.manager.di

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.RefreshAuthenticator
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.x8bit.bitwarden.data.platform.manager.NetworkConfigManager
import com.x8bit.bitwarden.data.platform.manager.NetworkConfigManagerImpl
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import com.x8bit.bitwarden.data.platform.manager.SdkClientManagerImpl
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManagerImpl
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides repositories in the auth package.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformManagerModule {

    @Provides
    @Singleton
    fun provideBitwardenDispatchers(): DispatcherManager = DispatcherManagerImpl()

    @Provides
    @Singleton
    fun provideSdkClientManager(): SdkClientManager = SdkClientManagerImpl()

    @Provides
    @Singleton
    fun provideNetworkConfigManager(
        authRepository: AuthRepository,
        authTokenInterceptor: AuthTokenInterceptor,
        environmentRepository: EnvironmentRepository,
        baseUrlInterceptors: BaseUrlInterceptors,
        refreshAuthenticator: RefreshAuthenticator,
        dispatcherManager: DispatcherManager,
    ): NetworkConfigManager =
        NetworkConfigManagerImpl(
            authRepository = authRepository,
            authTokenInterceptor = authTokenInterceptor,
            environmentRepository = environmentRepository,
            baseUrlInterceptors = baseUrlInterceptors,
            refreshAuthenticator = refreshAuthenticator,
            dispatcherManager = dispatcherManager,
        )
}
